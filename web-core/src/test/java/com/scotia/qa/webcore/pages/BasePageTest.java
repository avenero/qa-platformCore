package com.scotia.qa.webcore.pages;

import com.scotia.qa.webcore.pages.BasePage;
import com.scotia.qa.webcore.components.BaseComponent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.*;

public class BasePageTest {

    private static class DummyPage extends BasePage {
        public DummyPage(WebDriver driver) { super(driver); }

        // Método público para probar el createComponent protegido
        public <T extends BaseComponent> T createComp(Class<T> cls, By locator) {
            return createComponent(cls, locator);
        }
    }

    @Test
    void constructor_nullDriver_throws() {
        assertThrows(IllegalArgumentException.class, () -> new DummyPage(null));
    }

    @Test
    void createComponent_missingConstructor_throws() throws Exception {
        WebDriver mockDriver = Mockito.mock(WebDriver.class);
        DummyPage page = new DummyPage(mockDriver);

        // Clase que extiende BaseComponent pero NO define el constructor (WebDriver, By)
        class NoCtorComponent extends BaseComponent {
            public NoCtorComponent(WebDriver driver) {
                super(driver, By.id("dummy"));
            }
        }

        assertThrows(RuntimeException.class, () -> page.createComp(NoCtorComponent.class, By.id("x")));
    }
}
