package com.qa.mobilecore.plugin;

import com.qa.common.config.ConfigManager;
import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.CorePlugin;
import com.qa.common.runtime.ExecutionConfig;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.ServiceRegistry;
import com.qa.common.runtime.StepComponent;
import com.qa.mobilecore.components.*;
import com.qa.mobilecore.config.MobileConfigKeys;
import com.qa.mobilecore.helper.MobileHelper;
import com.qa.mobilecore.model.DeviceDescriptor;
import com.qa.mobilecore.pool.DevicePool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * Plugin Mobile para el runtime de ejecución BDD.
 *
 * <p>Implementa el contrato {@link CorePlugin} para integrar la capa mobile-core
 * con el motor de ejecución. Se descubre automáticamente por Java SPI.
 *
 * <p><b>Ciclo de vida por escenario:</b>
 * <ol>
 *   <li>{@link #registerServices} → registra {@link MobileHelper} en el registry (lazy)</li>
 *   <li>{@link #onScenarioStart} → inicializa el pool si {@code mobile.discovery.auto.scan=true}</li>
 *   <li>[Steps BDD] → acceden a {@code MobileHelper} via {@code ExecutionContext.service(...)}</li>
 *   <li>{@link #onScenarioEnd} → cierra la sesión Appium y libera el dispositivo del pool</li>
 * </ol>
 *
 * <p><b>Tags de activación:</b> {@code @mobile}, {@code @ios}, {@code @android}, {@code @appium}
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class MobilePlugin implements CorePlugin {

    private static final Logger log = LoggerFactory.getLogger(MobilePlugin.class);

    @Override
    public String getName() { return "mobile"; }

    @Override
    public Set<String> getActivationTags() {
        return Set.of("@mobile", "@ios", "@android", "@appium");
    }

    @Override
    public int getOrder() { return 150; }

    /**
     * Registra {@link MobileHelper} en el ServiceRegistry de la ejecución.
     * La instancia se crea de forma lazy — solo cuando el primer step la necesita.
     */
    @Override
    public void registerServices(ServiceRegistry registry, ExecutionConfig config) {
        log.debug("[MobilePlugin] Registrando servicios Mobile...");
        registry.registerLazy(MobileHelper.class, MobileHelper::new);
        log.info("[MobilePlugin] Servicio registrado: MobileHelper");
    }

    /**
     * Al inicio del escenario: inicializa el DevicePool si está habilitado el auto-scan.
     * La sesión Appium (driver) se crea de forma lazy en {@code MobileHelper.initSession()}.
     */
    @Override
    public void onScenarioStart(ExecutionContext context) {
        log.debug("[MobilePlugin] onScenarioStart");

        boolean autoScan = ConfigManager.getInstance()
            .getBoolean(MobileConfigKeys.DISCOVERY_AUTO_SCAN, true);

        DevicePool pool = DevicePool.getInstance();
        if (!pool.hasDevices()) {
            pool.initialize(autoScan);
        }
    }

    /**
     * Al final del escenario: cierra la sesión Appium y libera el dispositivo del pool.
     * Opera de forma segura aunque la sesión nunca se haya iniciado.
     */
    @Override
    public void onScenarioEnd(ExecutionContext context) {
        log.debug("[MobilePlugin] onScenarioEnd — cerrando sesion Appium");
        context.registry()
               .get(MobileHelper.class)
               .ifPresent(MobileHelper::quitSession);
    }

    /**
     * Declara los 10 componentes de steps Mobile.
     * Cada componente apunta a su clase específica de steps con metadatos para el FE/BE.
     */
    @Override
    public List<StepComponent> getComponents() {
        return List.of(
                // GIVEN — Configuración
                new DeviceConfigComponent(),
                new AppManagementComponent(),
                new DevicePermissionComponent(),
                // WHEN — Interacción
                new GestureComponent(),
                new NativeElementComponent(),
                new ContextSwitchComponent(),
                new NotificationComponent(),
                new SensorComponent(),
                // THEN — Validación
                new MobileElementValidationComponent(),
                new AppStateValidationComponent()
        );
    }
}
