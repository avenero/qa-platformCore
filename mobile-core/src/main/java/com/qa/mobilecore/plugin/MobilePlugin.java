package com.qa.mobilecore.plugin;

import com.qa.common.config.ConfigManager;
import com.qa.common.runtime.CorePlugin;
import com.qa.common.runtime.ExecutionConfig;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.ServiceRegistry;
import com.qa.common.runtime.StepComponent;
import com.qa.mobilecore.components.AppManagementComponent;
import com.qa.mobilecore.components.AppStateValidationComponent;
import com.qa.mobilecore.components.ContextSwitchComponent;
import com.qa.mobilecore.components.DeviceConfigComponent;
import com.qa.mobilecore.components.DevicePermissionComponent;
import com.qa.mobilecore.components.GestureComponent;
import com.qa.mobilecore.components.MobileElementValidationComponent;
import com.qa.mobilecore.components.NativeElementComponent;
import com.qa.mobilecore.components.NotificationComponent;
import com.qa.mobilecore.components.SensorComponent;
import com.qa.mobilecore.config.MobileConfigKeys;
import com.qa.mobilecore.driver.MobileDriverFactory;
import com.qa.mobilecore.driver.MobileDriverManager;
import com.qa.mobilecore.helper.MobileHelper;
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
 * <h2>Ciclo de vida por escenario</h2>
 * <ol>
 *   <li>{@link #registerServices} — registra {@link MobileDriverFactory} (lazy) y
 *       {@link MobileHelper} (lazy, inyectado con la factory)</li>
 *   <li>{@link #onScenarioStart} — inicializa el pool si {@code mobile.discovery.auto.scan=true}</li>
 *   <li>[Steps BDD] — acceden a {@code MobileHelper} via {@code ExecutionContext.service(...)};
 *       el driver se crea lazy en la primera llamada a {@code MobileHelper.driver()}</li>
 *   <li>{@link #onScenarioEnd} — cierra la sesión Appium vía
 *       {@link MobileDriverFactory#quitIfCreated()} y libera el dispositivo del pool</li>
 * </ol>
 *
 * <h2>Cierre autoritativo del driver</h2>
 * <pre>
 * Cucumber Runtime
 *   ├─ @Before (MobileHooksSteps) → logging, driver lazy
 *   ├─ [steps del escenario]      → MobileHelper.driver() → factory.getOrCreateDriver()
 *   ├─ @After  (MobileHooksSteps) → screenshot si fallo
 *   └─ TestCaseFinished
 *         └─► ScenarioLifecycleBridge
 *               └─► MobilePlugin.onScenarioEnd()
 *                     ├─► MobileDriverFactory.quitIfCreated()  ← cierre autoritativo
 *                     ├─► MobileHelper.quitSession()           ← libera pool + safety-net
 *                     └─► MobileDriverManager.quitDriverSafely() ← limpieza ThreadLocal
 * </pre>
 *
 * <p><b>Tags de activación:</b> {@code @mobile}, {@code @ios}, {@code @android}, {@code @appium}
 *
 * @author Abel Venero
 * @since 2.0.0
 * @since 2.2.0 Registro de MobileDriverFactory + cierre autoritativo en onScenarioEnd
 */
public class MobilePlugin implements CorePlugin {

    private static final Logger LOG = LoggerFactory.getLogger(MobilePlugin.class);
    private static final int PLUGIN_ORDER = 150;

    @Override
    public String getName() { return "mobile"; }

    @Override
    public Set<String> getActivationTags() {
        return Set.of("@mobile", "@ios", "@android", "@appium");
    }

    @Override
    public int getOrder() { return PLUGIN_ORDER; }

    /**
     * Registra {@link MobileDriverFactory} y {@link MobileHelper} en el ServiceRegistry.
     *
     * <p>Ambos se registran de forma lazy: se instancian solo cuando el primer step
     * los necesita. {@code MobileHelper} recibe la misma instancia de factory que
     * el plugin usará para el cierre autoritativo en {@link #onScenarioEnd}.
     */
    @Override
    public void registerServices(ServiceRegistry registry, ExecutionConfig config) {
        LOG.debug("[MobilePlugin] Registrando servicios Mobile...");

        // 1. Factory de drivers: gestiona el ciclo de vida del AppiumDriver por escenario
        registry.registerLazy(MobileDriverFactory.class, MobileDriverFactory::new);

        // 2. Helper: fachada de Appium, inyectada con la factory ya registrada
        registry.registerLazy(MobileHelper.class, () -> {
            MobileDriverFactory factory = registry.get(MobileDriverFactory.class).orElseGet(MobileDriverFactory::new);
            return new MobileHelper(factory);
        });

        LOG.info("[MobilePlugin] Servicios registrados: MobileDriverFactory (lazy) + MobileHelper (lazy)");
    }

    /**
     * Al inicio del escenario: inicializa el DevicePool si está habilitado el auto-scan.
     * La sesión Appium (driver) se crea de forma lazy en {@code MobileHelper.driver()}.
     */
    @Override
    public void onScenarioStart(ExecutionContext context) {
        LOG.debug("[MobilePlugin] onScenarioStart");

        boolean autoScan = ConfigManager.getInstance().getBoolean(MobileConfigKeys.DISCOVERY_AUTO_SCAN, true);

        DevicePool pool = DevicePool.getInstance();
        if (!pool.hasDevices()) {
            pool.initialize(autoScan);
        }
    }

    /**
     * Al final del escenario: cierra la sesión Appium y libera el dispositivo del pool.
     *
     * <p>Opera de forma segura aunque la sesión nunca se haya iniciado:
     * <ul>
     *   <li>{@link MobileDriverFactory#quitIfCreated()} — cierre autoritativo del driver;
     *       idempotente si nunca se creó o ya fue cerrado.</li>
     *   <li>{@link MobileHelper#quitSession()} — libera el dispositivo del pool;
     *       idempotente si no hay dispositivo asignado.</li>
     *   <li>{@link MobileDriverManager#quitDriverSafely()} — limpieza del ThreadLocal
     *       como safety-net final.</li>
     * </ul>
     */
    @Override
    public void onScenarioEnd(ExecutionContext context) {
        LOG.debug("[MobilePlugin] onScenarioEnd — cerrando sesion Appium");

        // Cierre autoritativo del driver vía MobileDriverFactory
        context.registry().get(MobileDriverFactory.class).ifPresent(factory -> {
                   LOG.debug("[MobilePlugin] Invocando MobileDriverFactory.quitIfCreated()");
                   factory.quitIfCreated();
               });

        // Liberación del dispositivo del pool + safety-net ThreadLocal
        context.registry().get(MobileHelper.class).ifPresent(helper -> {
                   LOG.debug("[MobilePlugin] Invocando MobileHelper.quitSession()");
                   helper.quitSession();
               });

        // Safety-net final: limpiar ThreadLocal aunque el helper no estuviera registrado
        MobileDriverManager.quitDriverSafely();

        LOG.debug("[MobilePlugin] onScenarioEnd — completado");
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
