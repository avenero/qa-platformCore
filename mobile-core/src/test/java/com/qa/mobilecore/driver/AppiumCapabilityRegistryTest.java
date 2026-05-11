package com.qa.mobilecore.driver;

import com.qa.common.api.pool.CapabilityDescriptor;
import com.qa.common.api.pool.DeviceHandle;
import com.qa.mobilecore.model.DeviceDescriptor;
import com.qa.mobilecore.model.DeviceStatus;
import com.qa.mobilecore.model.DeviceType;
import com.qa.mobilecore.pool.DevicePool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppiumCapabilityRegistry")
class AppiumCapabilityRegistryTest {

    @Mock
    private DevicePool pool;

    private AppiumCapabilityRegistry registry;

    // --- fixtures ---

    private static final DeviceDescriptor PIXEL_7 = DeviceDescriptor
            .builder("pixel7", DeviceType.ANDROID_PHYSICAL)
            .platformName("Android")
            .platformVersion("13")
            .deviceName("Pixel 7")
            .build();

    private static final DeviceDescriptor IPHONE_15 = DeviceDescriptor
            .builder("iphone15", DeviceType.IOS_PHYSICAL)
            .platformName("iOS")
            .platformVersion("17.0")
            .deviceName("iPhone 15")
            .build();

    private static final DeviceDescriptor ANDROID_EMU = DeviceDescriptor
            .builder("emu-avd", DeviceType.ANDROID_EMULATOR)
            .platformName("Android")
            .platformVersion("14")
            .deviceName("Pixel_8_API_34")
            .build();

    @BeforeEach
    void setUp() {
        registry = new AppiumCapabilityRegistry(pool);
    }

    // =========================================================================
    // Constructor
    // =========================================================================

    @Test
    @DisplayName("constructor lanza NPE si pool es null")
    void constructor_nullPool_throwsNPE() {
        assertThatThrownBy(() -> new AppiumCapabilityRegistry(null))
                .isInstanceOf(NullPointerException.class);
    }

    // =========================================================================
    // platformId()
    // =========================================================================

    @Test
    @DisplayName("platformId() retorna MOBILE")
    void platformId_returnsMOBILE() {
        assertThat(registry.platformId()).isEqualTo("MOBILE");
    }

    // =========================================================================
    // list()
    // =========================================================================

    @Nested
    @DisplayName("list()")
    class ListTests {

        @Test
        @DisplayName("pool vacío retorna lista vacía (nunca null)")
        void list_emptyPool_returnsEmptyList() {
            when(pool.getAllDevices()).thenReturn(List.of());

            assertThat(registry.list()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("retorna un descriptor por dispositivo registrado")
        void list_twoDevices_returnsTwo() {
            when(pool.getAllDevices()).thenReturn(List.of(PIXEL_7, IPHONE_15));
            when(pool.getStatus("pixel7")).thenReturn(Optional.of(DeviceStatus.AVAILABLE));
            when(pool.getStatus("iphone15")).thenReturn(Optional.of(DeviceStatus.AVAILABLE));

            assertThat(registry.list()).hasSize(2);
        }

        @Test
        @DisplayName("mapping: device AVAILABLE → available=true, inUse=false")
        void list_availableDevice_mappedCorrectly() {
            when(pool.getAllDevices()).thenReturn(List.of(PIXEL_7));
            when(pool.getStatus("pixel7")).thenReturn(Optional.of(DeviceStatus.AVAILABLE));

            CapabilityDescriptor d = registry.list().get(0);

            assertThat(d.id()).isEqualTo("pixel7");
            assertThat(d.available()).isTrue();
            assertThat(d.inUse()).isFalse();
        }

        @Test
        @DisplayName("mapping: device BUSY → available=false, inUse=true")
        void list_busyDevice_mappedCorrectly() {
            when(pool.getAllDevices()).thenReturn(List.of(PIXEL_7));
            when(pool.getStatus("pixel7")).thenReturn(Optional.of(DeviceStatus.BUSY));
            when(pool.estimatedFreeSeconds("pixel7")).thenReturn(null);

            CapabilityDescriptor d = registry.list().get(0);

            assertThat(d.available()).isFalse();
            assertThat(d.inUse()).isTrue();
        }

        @Test
        @DisplayName("mapping: device OFFLINE → available=false, inUse=false")
        void list_offlineDevice_mappedCorrectly() {
            when(pool.getAllDevices()).thenReturn(List.of(PIXEL_7));
            when(pool.getStatus("pixel7")).thenReturn(Optional.of(DeviceStatus.OFFLINE));

            CapabilityDescriptor d = registry.list().get(0);

            assertThat(d.available()).isFalse();
            assertThat(d.inUse()).isFalse();
        }

        @Test
        @DisplayName("mapping: ANDROID_PHYSICAL → platformId=ANDROID, type=REAL")
        void list_androidPhysical_hasPlatformAndType() {
            when(pool.getAllDevices()).thenReturn(List.of(PIXEL_7));
            when(pool.getStatus("pixel7")).thenReturn(Optional.of(DeviceStatus.AVAILABLE));

            CapabilityDescriptor d = registry.list().get(0);

            assertThat(d.platformId()).isEqualTo("ANDROID");
            assertThat(d.type()).isEqualTo("REAL");
        }

        @Test
        @DisplayName("mapping: ANDROID_EMULATOR → platformId=ANDROID, type=EMULATOR")
        void list_androidEmulator_hasPlatformAndType() {
            when(pool.getAllDevices()).thenReturn(List.of(ANDROID_EMU));
            when(pool.getStatus("emu-avd")).thenReturn(Optional.of(DeviceStatus.AVAILABLE));

            CapabilityDescriptor d = registry.list().get(0);

            assertThat(d.platformId()).isEqualTo("ANDROID");
            assertThat(d.type()).isEqualTo("EMULATOR");
        }

        @Test
        @DisplayName("mapping: IOS_PHYSICAL → platformId=IOS, type=REAL")
        void list_iosPhysical_hasPlatformAndType() {
            when(pool.getAllDevices()).thenReturn(List.of(IPHONE_15));
            when(pool.getStatus("iphone15")).thenReturn(Optional.of(DeviceStatus.AVAILABLE));

            CapabilityDescriptor d = registry.list().get(0);

            assertThat(d.platformId()).isEqualTo("IOS");
            assertThat(d.type()).isEqualTo("REAL");
        }

        @Test
        @DisplayName("mapping: IOS_SIMULATOR → type=EMULATOR")
        void list_iosSimulator_typeIsEmulator() {
            DeviceDescriptor iosSimulator = DeviceDescriptor
                    .builder("sim-iphone14", DeviceType.IOS_SIMULATOR)
                    .platformName("iOS")
                    .platformVersion("16")
                    .deviceName("iPhone 14 Pro")
                    .build();

            when(pool.getAllDevices()).thenReturn(List.of(iosSimulator));
            when(pool.getStatus("sim-iphone14")).thenReturn(Optional.of(DeviceStatus.AVAILABLE));

            CapabilityDescriptor d = registry.list().get(0);

            assertThat(d.type()).isEqualTo("EMULATOR");
        }

        @Test
        @DisplayName("mapping: REMOTE_GRID → platformId=MOBILE, type=REAL")
        void list_remoteGrid_platformMobileTypeReal() {
            DeviceDescriptor remote = DeviceDescriptor
                    .builder("remote-device", DeviceType.REMOTE_GRID)
                    .build();

            when(pool.getAllDevices()).thenReturn(List.of(remote));
            when(pool.getStatus("remote-device")).thenReturn(Optional.of(DeviceStatus.AVAILABLE));

            CapabilityDescriptor d = registry.list().get(0);

            assertThat(d.platformId()).isEqualTo("MOBILE");
            assertThat(d.type()).isEqualTo("REAL");
        }

        @Test
        @DisplayName("friendly name incluye deviceName + platform + version")
        void list_friendlyName_includesAllParts() {
            when(pool.getAllDevices()).thenReturn(List.of(PIXEL_7));
            when(pool.getStatus("pixel7")).thenReturn(Optional.of(DeviceStatus.AVAILABLE));

            CapabilityDescriptor d = registry.list().get(0);

            assertThat(d.name()).contains("Pixel 7").contains("Android").contains("13");
        }

        @Test
        @DisplayName("friendly name cae back al id si deviceName es null")
        void list_friendlyName_fallsBackToId() {
            DeviceDescriptor noName = DeviceDescriptor
                    .builder("bare-device", DeviceType.ANDROID_EMULATOR)
                    .build(); // sin deviceName ni platformVersion

            when(pool.getAllDevices()).thenReturn(List.of(noName));
            when(pool.getStatus("bare-device")).thenReturn(Optional.of(DeviceStatus.AVAILABLE));

            CapabilityDescriptor d = registry.list().get(0);

            assertThat(d.name()).contains("bare-device");
        }

        @Test
        @DisplayName("metadata contiene osVersion aunque sea vacío")
        void list_metadataContainsOsVersion() {
            when(pool.getAllDevices()).thenReturn(List.of(PIXEL_7));
            when(pool.getStatus("pixel7")).thenReturn(Optional.of(DeviceStatus.AVAILABLE));

            CapabilityDescriptor d = registry.list().get(0);

            assertThat(d.metadata()).containsKey("osVersion");
        }

        @Test
        @DisplayName("estimatedFreeSeconds viene del pool cuando el dispositivo está en uso")
        void list_busyDevice_etaFromPool() {
            when(pool.getAllDevices()).thenReturn(List.of(PIXEL_7));
            when(pool.getStatus("pixel7")).thenReturn(Optional.of(DeviceStatus.BUSY));
            when(pool.estimatedFreeSeconds("pixel7")).thenReturn(120L);

            CapabilityDescriptor d = registry.list().get(0);

            assertThat(d.estimatedFreeInSeconds()).isEqualTo(120L);
        }
    }

    // =========================================================================
    // isAvailable()
    // =========================================================================

    @Nested
    @DisplayName("isAvailable()")
    class IsAvailableTests {

        @Test
        @DisplayName("retorna true cuando el pool reporta AVAILABLE")
        void isAvailable_availableStatus_returnsTrue() {
            when(pool.getStatus("pixel7")).thenReturn(Optional.of(DeviceStatus.AVAILABLE));

            assertThat(registry.isAvailable("pixel7")).isTrue();
        }

        @Test
        @DisplayName("retorna false cuando el pool reporta BUSY")
        void isAvailable_busyStatus_returnsFalse() {
            when(pool.getStatus("pixel7")).thenReturn(Optional.of(DeviceStatus.BUSY));

            assertThat(registry.isAvailable("pixel7")).isFalse();
        }

        @Test
        @DisplayName("retorna false cuando el pool reporta OFFLINE")
        void isAvailable_offlineStatus_returnsFalse() {
            when(pool.getStatus("pixel7")).thenReturn(Optional.of(DeviceStatus.OFFLINE));

            assertThat(registry.isAvailable("pixel7")).isFalse();
        }

        @Test
        @DisplayName("retorna false cuando el dispositivo no existe en el pool")
        void isAvailable_unknownDevice_returnsFalse() {
            when(pool.getStatus("unknown")).thenReturn(Optional.empty());

            assertThat(registry.isAvailable("unknown")).isFalse();
        }

        @Test
        @DisplayName("retorna false para null sin lanzar excepción")
        void isAvailable_null_returnsFalse() {
            assertThat(registry.isAvailable(null)).isFalse();
            verify(pool, never()).getStatus(anyString());
        }

        @ParameterizedTest
        @ValueSource(strings = {" ", "\t", ""})
        @DisplayName("retorna false para blank sin lanzar excepción")
        void isAvailable_blank_returnsFalse(String blank) {
            assertThat(registry.isAvailable(blank)).isFalse();
            verify(pool, never()).getStatus(anyString());
        }
    }

    // =========================================================================
    // acquire()
    // =========================================================================

    @Nested
    @DisplayName("acquire()")
    class AcquireTests {

        @Test
        @DisplayName("dispositivo disponible → retorna handle presente")
        void acquire_availableDevice_returnsHandle() {
            when(pool.tryAcquire(eq("pixel7"), any(Duration.class)))
                    .thenReturn(Optional.of(PIXEL_7));

            Optional<DeviceHandle> result = registry.acquire("pixel7", Duration.ofSeconds(30));

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("handle tiene capabilityId igual al id solicitado")
        void acquire_handle_hasCorrectCapabilityId() {
            when(pool.tryAcquire(eq("pixel7"), any(Duration.class)))
                    .thenReturn(Optional.of(PIXEL_7));

            DeviceHandle handle = registry.acquire("pixel7", Duration.ofSeconds(30)).orElseThrow();

            assertThat(handle.capabilityId()).isEqualTo("pixel7");
        }

        @Test
        @DisplayName("handle tiene sessionId único no vacío")
        void acquire_handle_hasUniqueSessionId() {
            when(pool.tryAcquire(eq("pixel7"), any(Duration.class)))
                    .thenReturn(Optional.of(PIXEL_7));

            String s1 = registry.acquire("pixel7", Duration.ofSeconds(1)).orElseThrow().sessionId();
            String s2 = registry.acquire("pixel7", Duration.ofSeconds(1)).orElseThrow().sessionId();

            assertThat(s1).isNotBlank();
            assertThat(s1).isNotEqualTo(s2);
        }

        @Test
        @DisplayName("handle tiene leaseDuration = MOBILE_LEASE")
        void acquire_handle_hasMobileLease() {
            when(pool.tryAcquire(eq("pixel7"), any(Duration.class)))
                    .thenReturn(Optional.of(PIXEL_7));

            DeviceHandle handle = registry.acquire("pixel7", Duration.ofSeconds(1)).orElseThrow();

            assertThat(handle.leaseDuration()).isEqualTo(AppiumCapabilityRegistry.MOBILE_LEASE);
        }

        @Test
        @DisplayName("handle no está expirado inmediatamente después de acquire")
        void acquire_handle_notExpiredImmediately() {
            when(pool.tryAcquire(eq("pixel7"), any(Duration.class)))
                    .thenReturn(Optional.of(PIXEL_7));

            DeviceHandle handle = registry.acquire("pixel7", Duration.ofSeconds(1)).orElseThrow();

            assertThat(handle.isExpired()).isFalse();
        }

        @Test
        @DisplayName("dispositivo ocupado → retorna Optional.empty")
        void acquire_busyDevice_returnsEmpty() {
            when(pool.tryAcquire(eq("pixel7"), any(Duration.class)))
                    .thenReturn(Optional.empty());

            Optional<DeviceHandle> result = registry.acquire("pixel7", Duration.ofSeconds(30));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("dispositivo inexistente → retorna Optional.empty")
        void acquire_unknownDevice_returnsEmpty() {
            when(pool.tryAcquire(eq("nonexistent"), any(Duration.class)))
                    .thenReturn(Optional.empty());

            assertThat(registry.acquire("nonexistent", Duration.ofSeconds(30))).isEmpty();
        }

        @Test
        @DisplayName("lanza NullPointerException si capabilityId es null")
        void acquire_nullCapabilityId_throwsNPE() {
            assertThatThrownBy(() -> registry.acquire(null, Duration.ofSeconds(1)))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        @DisplayName("lanza excepción si capabilityId es blank o null")
        void acquire_blankCapabilityId_throwsException(String badId) {
            if (badId == null) {
                assertThatThrownBy(() -> registry.acquire(badId, Duration.ofSeconds(1)))
                        .isInstanceOf(NullPointerException.class);
            } else {
                assertThatThrownBy(() -> registry.acquire(badId, Duration.ofSeconds(1)))
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }

        @Test
        @DisplayName("lanza NullPointerException si timeout es null")
        void acquire_nullTimeout_throwsNPE() {
            assertThatThrownBy(() -> registry.acquire("pixel7", null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si timeout es cero")
        void acquire_zeroTimeout_throwsIllegalArgument() {
            assertThatThrownBy(() -> registry.acquire("pixel7", Duration.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si timeout es negativo")
        void acquire_negativeTimeout_throwsIllegalArgument() {
            assertThatThrownBy(() -> registry.acquire("pixel7", Duration.ofSeconds(-1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // release()
    // =========================================================================

    @Nested
    @DisplayName("release()")
    class ReleaseTests {

        @Test
        @DisplayName("release delega en pool.release con el capabilityId del handle")
        void release_validHandle_delegatesToPool() {
            when(pool.tryAcquire(eq("pixel7"), any(Duration.class)))
                    .thenReturn(Optional.of(PIXEL_7));
            DeviceHandle handle = registry.acquire("pixel7", Duration.ofSeconds(1)).orElseThrow();

            registry.release(handle);

            verify(pool).release("pixel7");
        }

        @Test
        @DisplayName("release es idempotente — segunda llamada no falla")
        void release_calledTwice_isIdempotent() {
            when(pool.tryAcquire(eq("pixel7"), any(Duration.class)))
                    .thenReturn(Optional.of(PIXEL_7));
            DeviceHandle handle = registry.acquire("pixel7", Duration.ofSeconds(1)).orElseThrow();

            registry.release(handle);
            registry.release(handle); // pool.release es idempotente; no debe lanzar
        }

        @Test
        @DisplayName("release con null lanza NullPointerException")
        void release_null_throwsNPE() {
            assertThatThrownBy(() -> registry.release(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // =========================================================================
    // Ciclo completo: acquire → release → disponibilidad
    // =========================================================================

    @Test
    @DisplayName("ciclo completo: acquire de dispositivo disponible luego se libera via release")
    void fullCycle_acquireAndRelease() {
        when(pool.tryAcquire(eq("pixel7"), any(Duration.class)))
                .thenReturn(Optional.of(PIXEL_7));

        DeviceHandle handle = registry.acquire("pixel7", Duration.ofSeconds(30)).orElseThrow();
        assertThat(handle.capabilityId()).isEqualTo("pixel7");

        registry.release(handle);
        verify(pool).release("pixel7");
    }

    // =========================================================================
    // Mapping DeviceDescriptor → CapabilityDescriptor — tabla exhaustiva
    // =========================================================================

    @Nested
    @DisplayName("Mapping completo DeviceDescriptor → CapabilityDescriptor")
    class MappingTests {

        @Test
        @DisplayName("device UNKNOWN status → available=false, inUse=false")
        void mapping_unknownStatus_notAvailableNotInUse() {
            DeviceDescriptor device = DeviceDescriptor
                    .builder("dev1", DeviceType.ANDROID_PHYSICAL)
                    .build();

            when(pool.getAllDevices()).thenReturn(List.of(device));
            when(pool.getStatus("dev1")).thenReturn(Optional.of(DeviceStatus.UNKNOWN));

            CapabilityDescriptor d = registry.list().get(0);

            assertThat(d.available()).isFalse();
            assertThat(d.inUse()).isFalse();
        }

        @Test
        @DisplayName("device status Optional.empty → available=false")
        void mapping_emptyStatus_notAvailable() {
            DeviceDescriptor device = DeviceDescriptor
                    .builder("dev2", DeviceType.IOS_SIMULATOR)
                    .build();

            when(pool.getAllDevices()).thenReturn(List.of(device));
            when(pool.getStatus("dev2")).thenReturn(Optional.empty());

            CapabilityDescriptor d = registry.list().get(0);

            assertThat(d.available()).isFalse();
            assertThat(d.inUse()).isFalse();
        }

        @Test
        @DisplayName("estimatedFreeInSeconds es null cuando dispositivo está disponible")
        void mapping_availableDevice_etaIsNull() {
            when(pool.getAllDevices()).thenReturn(List.of(PIXEL_7));
            when(pool.getStatus("pixel7")).thenReturn(Optional.of(DeviceStatus.AVAILABLE));

            CapabilityDescriptor d = registry.list().get(0);

            assertThat(d.estimatedFreeInSeconds()).isNull();
        }
    }
}
