package com.qa.common.api.runtime;


/**
 * Fase BDD a la que pertenece un componente de steps.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public enum BddPhase {

    GIVEN("Given"),
    WHEN("When"),
    THEN("Then");

    private final String label;

    BddPhase(String label) {
        this.label = label;
    }

    /**
     * Etiqueta legible para UI/reporting.
     * @return etiqueta
     */
    public String getLabel() {
        return label;
    }
}
