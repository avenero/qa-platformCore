package com.qa.webcore.plugin;

import com.qa.webcore.components.bdd.*;
import com.qa.common.runtime.CorePlugin;
import com.qa.common.runtime.ExecutionConfig;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.ServiceRegistry;
import com.qa.common.runtime.StepComponent;
import com.qa.webcore.components.bdd.*;
import com.qa.webcore.utils.WebHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * Plugin de Web para el runtime de ejecución BDD.
 *
 * <p>Registra los servicios Selenium ({@link WebHelper}) y declara los
 * 16 componentes de steps Web organizados por responsabilidad (Fase 2).
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class WebPlugin implements CorePlugin {

    private static final Logger log = LoggerFactory.getLogger(WebPlugin.class);

    @Override
    public String getName() { return "web"; }

    @Override
    public Set<String> getActivationTags() {
        return Set.of("@web", "@ui", "@browser", "@selenium");
    }

    @Override
    public int getOrder() { return 100; }

    @Override
    public void registerServices(ServiceRegistry registry, ExecutionConfig config) {
        log.debug("[WebPlugin] Registrando servicios Web...");
        registry.registerLazy(WebHelper.class, WebHelper::new);
        log.info("[WebPlugin] Servicio registrado: WebHelper");
    }

    @Override
    public void onScenarioStart(ExecutionContext context) {
        log.debug("[WebPlugin] onScenarioStart");
    }

    @Override
    public void onScenarioEnd(ExecutionContext context) {
        log.debug("[WebPlugin] onScenarioEnd");
    }

    /**
     * Declara los 16 componentes de steps Web (Fase 2).
     * Cada componente apunta a su clase específica según DISENO-STEPS-POR-COMPONENTES.md §4.
     */
    @Override
    public List<StepComponent> getComponents() {
        return List.of(
                // GIVEN — Configuración
                new BrowserConfigComponent(),
                new WebEnvironmentComponent(),
                // WHEN — Navegación
                new NavigationComponent(),
                new FrameComponent(),
                new WindowComponent(),
                // WHEN — Interacción
                new ClickComponent(),
                new InputComponent(),
                new SelectComponent(),
                new ScrollComponent(),
                new DragDropComponent(),
                new AlertComponent(),
                // WHEN — Esperas
                new WaitComponent(),
                // THEN — Validación
                new ElementValidationComponent(),
                new PageValidationComponent(),
                new TableValidationComponent(),
                new ScreenshotComponent()
        );
    }
}
