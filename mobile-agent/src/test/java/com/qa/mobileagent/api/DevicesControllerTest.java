package com.qa.mobileagent.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.common.api.transport.MobileDeviceDescriptor;
import com.qa.mobileagent.api.controller.DevicesController;
import com.qa.mobileagent.service.DeviceDiscoveryQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests del wire-protocol v1 {@code GET /v1/devices}
 * (TASK-K03COV-BE7 lado agente).
 */
@DisplayName("DevicesController — GET /v1/devices")
class DevicesControllerTest {

    private DeviceDiscoveryQueryService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(DeviceDiscoveryQueryService.class);
        mvc = MockMvcBuilders.standaloneSetup(new DevicesController(service)).build();
    }

    @Test
    @DisplayName("200 con lista no vacía cuando hay devices")
    void returnsListOfDevices() throws Exception {
        when(service.listAvailable()).thenReturn(List.of(
                new MobileDeviceDescriptor("emu-1", "Pixel_6", "ANDROID_EMULATOR", "android", "13", "AVAILABLE"),
                new MobileDeviceDescriptor("sim-1", "iPhone 15", "IOS_SIMULATOR", "ios", "17.4", "AVAILABLE")
        ));

        mvc.perform(get("/v1/devices"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("emu-1"))
                .andExpect(jsonPath("$[0].platform").value("android"))
                .andExpect(jsonPath("$[1].type").value("IOS_SIMULATOR"));
    }

    @Test
    @DisplayName("200 con lista vacía cuando no hay devices conectados")
    void returnsEmptyListWhenNoDevices() throws Exception {
        when(service.listAvailable()).thenReturn(List.of());

        mvc.perform(get("/v1/devices"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("503 cuando discovery falla (ADB no disponible, permisos, etc.)")
    void returns503WhenDiscoveryThrows() throws Exception {
        when(service.listAvailable())
                .thenThrow(new RuntimeException("adb: command not found"));

        mvc.perform(get("/v1/devices"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("response contiene exactamente los 6 campos del wire — UDID nunca aparece")
    void responseDoesNotLeakUdid() throws Exception {
        when(service.listAvailable()).thenReturn(List.of(
                new MobileDeviceDescriptor("phys-1", "Pixel 8", "ANDROID_PHYSICAL", "android", "14", "AVAILABLE")
        ));

        var result = mvc.perform(get("/v1/devices"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // Sanidad: el body es un array de 6 keys conocidas, sin "udid".
        var parsed = new ObjectMapper().readTree(body);
        var first = parsed.get(0);
        org.assertj.core.api.Assertions.assertThat(first.fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder("id", "name", "type", "platform", "version", "status");
    }
}
