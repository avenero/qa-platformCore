package com.qa.httpcore.components;

import com.qa.httpcore.steps.config.RequestBodySteps;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps: Request Body (canónico).
 *
 * <p>Es el componente sucesor de {@link ApiRequestBodyComponent} (stepId
 * deprecado {@code "api.body"}). Posee el stepId canónico {@code "api.request.body"}
 * prometido por el ciclo de deprecación (ver {@code replacedBy} en el predecesor) y
 * sigue la convención de nomenclatura punteada del resto de la capa HTTP
 * ({@code api.url}, {@code api.execution}, {@code api.response.body}).
 *
 * <p>Comparte la misma clase de steps que el predecesor ({@link RequestBodySteps}):
 * el cambio es exclusivamente el {@code @StepId} del componente, no el vocabulario
 * BDD. Durante la ventana de transición ambos componentes coexisten (zero-downtime):
 * el {@code ScenarioSuggestionEngine} del Backend prefiere {@code "api.request.body"}
 * y deja de caer al fallback {@code "api.body"} en cuanto este componente existe.
 *
 * <p>El predecesor {@link ApiRequestBodyComponent} se removerá en v2.3.0 (card aparte),
 * una vez migrados los escenarios persistidos.
 *
 * @author Abel Venero
 * @since 2.2.0
 * @see ApiRequestBodyComponent
 */
@StepId("api.request.body")
public class ApiRequestBodyComponentV2 implements StepComponent {

    /** Display order for this component in the UI step palette (slot de Request Body). */
    private static final int DISPLAY_ORDER = 60;
    @Override
    public String getName() { return "Request Body"; }
    @Override
    public String getDisplayName() { return "Request Body"; }
    @Override
    public String getDescription() {
        return "Cuerpo de la peticion: JSON, XML, form-data, template";
    }
    @Override
    public BddPhase getPhase() { return BddPhase.GIVEN; }
    @Override
    public String getCategory() { return "Configuracion de Peticion"; }
    @Override
    public String getIcon() { return "description"; }
    @Override
    public int getDisplayOrder() { return DISPLAY_ORDER; }
    @Override
    public Class<?> getStepDefinitionClass() { return RequestBodySteps.class; }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "body", "cuerpo", "payload", "json", "xml",
            "form-data", "multipart", "template", "raw", "request-body"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Request Body",
            "en", "Request Body",
            "fr", "Corps de la requete"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Cuerpo de la peticion: JSON, XML, form-data, template",
            "en", "Request body: JSON, XML, form-data, template",
            "fr", "Corps de la requete : JSON, XML, form-data, template"
        );
    }
}
