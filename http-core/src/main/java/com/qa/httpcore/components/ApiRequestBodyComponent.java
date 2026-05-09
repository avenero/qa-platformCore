package com.qa.httpcore.components;

import com.qa.httpcore.steps.config.RequestBodySteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;
import com.qa.common.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps: Request Body (DEPRECATED).
 *
 * <p>Este componente está marcado como <strong>deprecated</strong> desde la versión 2.1.0.
 * El stepId {@code "api.body"} fue renombrado a {@code "api.request.body"} para reflejar
 * con mayor precisión su responsabilidad y alinearse con la convención de nomenclatura.
 *
 * <p><strong>Migración:</strong> Reemplazar el uso de {@code "api.body"} por
 * {@code "api.request.body"} en todos los escenarios persistidos. El componente
 * sucesor {@code "api.request.body"} estará disponible desde la versión 2.2.0.
 *
 * <p>Este componente se mantiene activo durante el ciclo de deprecación para garantizar
 * retrocompatibilidad con escenarios persistidos en la base de datos antes de la migración.
 *
 * @author Abel Venero
 * @since 2.0.0
 * @deprecated Usar stepId {@code "api.request.body"} disponible desde v2.2.0.
 */
@SuppressWarnings("DeprecatedIsStillUsed") // el plugin lo registra durante el ciclo de deprecación
@Deprecated(since = "2.1.0", forRemoval = false)
@StepId(value = "api.body", deprecated = true, replacedBy = "api.request.body")
public class ApiRequestBodyComponent implements StepComponent {

    /** Display order for this component in the UI step palette. */
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
