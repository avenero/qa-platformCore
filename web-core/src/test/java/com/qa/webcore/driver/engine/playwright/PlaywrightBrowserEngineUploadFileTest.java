package com.qa.webcore.driver.engine.playwright;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link PlaywrightBrowserEngine#uploadFile(String, String)}
 * (CORE-WEBAPI-UPLOAD / RFC-WEBAPI-01).
 */
@DisplayName("PlaywrightBrowserEngine#uploadFile (CORE-WEBAPI-UPLOAD)")
class PlaywrightBrowserEngineUploadFileTest {

    private Page page;
    private PlaywrightBrowserEngine engine;

    @BeforeEach
    void setUp() {
        page = mock(Page.class);
        engine = new PlaywrightBrowserEngine(page);
    }

    @Test
    @DisplayName("uploadFile delegates to Page.setInputFiles with the resolved path")
    void uploadFile_setsInputFiles(@TempDir Path tmp) throws IOException {
        Path fixture = Files.writeString(tmp.resolve("fixture.txt"), "hello");

        engine.uploadFile("#file-input", fixture.toString());

        verify(page).setInputFiles("#file-input", fixture);
    }

    @Test
    @DisplayName("uploadFile rejects a missing file with IllegalArgumentException")
    void uploadFile_missingFile_throws() {
        assertThatThrownBy(() -> engine.uploadFile("#input", "/non/existent/file.txt"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("File not found for upload");
    }

    @Test
    @DisplayName("uploadFile rejects a directory path with IllegalArgumentException")
    void uploadFile_directory_throws(@TempDir Path tmp) {
        assertThatThrownBy(() -> engine.uploadFile("#input", tmp.toString()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("File not found for upload");
    }
}
