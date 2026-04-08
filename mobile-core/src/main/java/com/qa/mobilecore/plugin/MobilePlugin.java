package com.qa.mobilecore.plugin;

import com.qa.mobilecore.components.*;
import com.qa.common.runtime.CorePlugin;
import com.qa.common.runtime.ExecutionConfig;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.ServiceRegistry;
import com.qa.common.runtime.StepComponent;
import com.qa.mobilecore.components.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * Plugin Mobile para el runtime de ejecucion BDD.
 *
 * <p>Declara los 10 componentes de steps Mobile creados en Fase 2
 * segun {@code DISENO-STEPS-POR-COMPONENTES.md §5}.
 * Incluye AppStateValidationComponent completado en revision 5-Abr-2026.
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

    @Override
    public void registerServices(ServiceRegistry registry, ExecutionConfig config) {
        log.info("[MobilePlugin] Plugin registrado (MobileDriverService se conecta en Fase 3)");
    }

    @Override
    public void onScenarioStart(ExecutionContext context) {
        log.debug("[MobilePlugin] onScenarioStart");
    }

    @Override
    public void onScenarioEnd(ExecutionContext context) {
        log.debug("[MobilePlugin] onScenarioEnd");
    }

    /**
     * Declara los 10 componentes de steps Mobile (Fase 2 + revision 5-Abr-2026).
     * Cada componente apunta a su clase especifica de steps.
     */
    @Override
    public List<StepComponent> getComponents() {
        return List.of(
                // GIVEN — Configuracion
                new DeviceConfigComponent(),
                new AppManagementComponent(),
                new DevicePermissionComponent(),
                // WHEN — Interaccion
                new GestureComponent(),
                new NativeElementComponent(),
                new ContextSwitchComponent(),
                new NotificationComponent(),
                new SensorComponent(),
                // THEN — Validacion
                new MobileElementValidationComponent(),
                new AppStateValidationComponent()   // Completado en revision 5-Abr-2026
        );
    }
}
