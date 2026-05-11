package com.qa.common.api.stepcatalog;

/**
 * Capa tecnológica a la que pertenece un step BDD.
 *
 * <p>Cada módulo Core registra sus steps bajo una de estas capas.
 * El BE usa el valor para agrupar y filtrar el catálogo i18n.
 */
public enum StepLayer {
    WEB,
    MOBILE,
    HTTP,
    DATABASE
}
