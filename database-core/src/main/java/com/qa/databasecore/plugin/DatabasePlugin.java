package com.qa.databasecore.plugin;

import com.qa.databasecore.components.DatabaseExecutionComponent;
import com.qa.databasecore.components.DatabaseSetupComponent;
import com.qa.databasecore.components.DatabaseValidationComponent;
import com.qa.databasecore.helper.DatabaseHelper;
import com.qa.common.runtime.CorePlugin;
import com.qa.common.runtime.ExecutionConfig;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.ServiceRegistry;
import com.qa.common.runtime.StepComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * Plugin de base de datos para el runtime de ejecución BDD.
 *
 * <p>Registra el servicio {@link DatabaseHelper} y declara el componente
 * de steps de base de datos. Se inicializa antes que los demás plugins
 * ({@link #getOrder()} = 0) porque puede proveer datos precondición
 * para escenarios API o Web.
 *
 * <p>Se descubre automáticamente vía Java SPI desde:
 * {@code META-INF/services/com.qa.common.runtime.CorePlugin}
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class DatabasePlugin implements CorePlugin {

    private static final Logger LOG = LoggerFactory.getLogger(DatabasePlugin.class);

    @Override
    public String getName() {
        return "database";
    }

    @Override
    public Set<String> getActivationTags() {
        return Set.of("@db", "@database", "@sql", "@jdbc");
    }

    @Override
    public int getOrder() {
        return 0; // Se inicializa primero — provee datos a otros plugins
    }

    @Override
    public void registerServices(ServiceRegistry registry, ExecutionConfig config) {
        LOG.debug("[DatabasePlugin] Registrando DatabaseHelper...");
        registry.registerLazy(DatabaseHelper.class, DatabaseHelper::new);
        LOG.info("[DatabasePlugin] Servicio registrado: DatabaseHelper");
    }

    @Override
    public void onScenarioStart(ExecutionContext context) {
        LOG.debug("[DatabasePlugin] onScenarioStart — DatabaseHelper disponible bajo demanda");
    }

    @Override
    public void onScenarioEnd(ExecutionContext context) {
        LOG.debug("[DatabasePlugin] onScenarioEnd — sin limpieza activa"
            + " (las conexiones se cierran en los hooks del step)");
    }

    /**
     * Declara los componentes de steps de base de datos, uno por fase BDD.
     *
     * <ul>
     *   <li>GIVEN — establecer conexión a un motor de BD</li>
     *   <li>WHEN  — ejecutar consultas/sentencias SQL</li>
     *   <li>THEN  — validar resultados y extraer valores de columnas</li>
     * </ul>
     */
    @Override
    public List<StepComponent> getComponents() {
        return List.of(
                new DatabaseSetupComponent(),
                new DatabaseExecutionComponent(),
                new DatabaseValidationComponent()
        );
    }
}

