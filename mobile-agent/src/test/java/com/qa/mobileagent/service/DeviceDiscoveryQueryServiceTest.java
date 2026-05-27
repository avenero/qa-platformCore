package com.qa.mobileagent.service;

import com.qa.common.api.transport.MobileDeviceDescriptor;
import com.qa.mobilecore.discovery.DeviceDiscoveryService;
import com.qa.mobilecore.model.DeviceDescriptor;
import com.qa.mobilecore.model.DeviceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests del mapeo {@link DeviceDescriptor} → {@link MobileDeviceDescriptor}
 * (TASK-K03COV-BE7 lado agente).
 */
@DisplayName("DeviceDiscoveryQueryService — wire mapping")
class DeviceDiscoveryQueryServiceTest {

    @Test
    @DisplayName("lista vacía cuando discovery no encuentra devices")
    void emptyDiscovery_returnsEmptyList() {
        DeviceDiscoveryService discovery = mock(DeviceDiscoveryService.class);
        when(discovery.discoverAll()).thenReturn(List.of());

        DeviceDiscoveryQueryService svc = new DeviceDiscoveryQueryService(discovery);
        assertThat(svc.listAvailable()).isEmpty();
    }

    @Test
    @DisplayName("Android emulator → platform=android, type=ANDROID_EMULATOR")
    void androidEmulator_mappedCorrectly() {
        DeviceDescriptor d = DeviceDescriptor.builder("emulator-5554", DeviceType.ANDROID_EMULATOR)
                .deviceName("Pixel_6_API_33")
                .platformVersion("13")
                .build();
        MobileDeviceDescriptor wire = DeviceDiscoveryQueryService.toWire(d);

        assertThat(wire.id()).isEqualTo("emulator-5554");
        assertThat(wire.name()).isEqualTo("Pixel_6_API_33");
        assertThat(wire.type()).isEqualTo("ANDROID_EMULATOR");
        assertThat(wire.platform()).isEqualTo("android");
        assertThat(wire.version()).isEqualTo("13");
        assertThat(wire.status()).isEqualTo("AVAILABLE");
    }

    @Test
    @DisplayName("iOS simulator → platform=ios, type=IOS_SIMULATOR")
    void iosSimulator_mappedCorrectly() {
        DeviceDescriptor d = DeviceDescriptor.builder("sim-uuid-1", DeviceType.IOS_SIMULATOR)
                .deviceName("iPhone 15")
                .platformVersion("17.4")
                .build();
        MobileDeviceDescriptor wire = DeviceDiscoveryQueryService.toWire(d);

        assertThat(wire.platform()).isEqualTo("ios");
        assertThat(wire.type()).isEqualTo("IOS_SIMULATOR");
    }

    @Test
    @DisplayName("Android physical → platform=android, type=ANDROID_PHYSICAL")
    void androidPhysical_mappedCorrectly() {
        DeviceDescriptor d = DeviceDescriptor.builder("ABC1234", DeviceType.ANDROID_PHYSICAL)
                .deviceName("Pixel 8")
                .platformVersion("14")
                .udid("ABC1234567890DEF")
                .build();
        MobileDeviceDescriptor wire = DeviceDiscoveryQueryService.toWire(d);

        assertThat(wire.platform()).isEqualTo("android");
        assertThat(wire.type()).isEqualTo("ANDROID_PHYSICAL");
    }

    @Test
    @DisplayName("UDID NO se publica en el wire (PII)")
    void udidIsNotExposed() {
        DeviceDescriptor d = DeviceDescriptor.builder("xyz", DeviceType.IOS_PHYSICAL)
                .udid("00008110-001234567890ABCD")
                .deviceName("iPhone")
                .platformVersion("17.0")
                .build();
        MobileDeviceDescriptor wire = DeviceDiscoveryQueryService.toWire(d);

        // El record solo tiene 6 componentes; udid no está entre ellos.
        // Verificación defensiva: ningún campo string del wire contiene el udid.
        assertThat(wire.id()).doesNotContain("00008110");
        assertThat(wire.name()).doesNotContain("00008110");
        assertThat(wire.version()).doesNotContain("00008110");
    }

    @Test
    @DisplayName("REMOTE_GRID → platform=unknown (no es ni android ni ios)")
    void remoteGrid_platformUnknown() {
        DeviceDescriptor d = DeviceDescriptor.builder("grid-1", DeviceType.REMOTE_GRID)
                .build();
        MobileDeviceDescriptor wire = DeviceDiscoveryQueryService.toWire(d);

        assertThat(wire.platform()).isEqualTo("unknown");
        assertThat(wire.type()).isEqualTo("REMOTE_GRID");
    }
}
