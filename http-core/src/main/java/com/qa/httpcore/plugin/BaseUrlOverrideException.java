package com.qa.httpcore.plugin;

/**
 * Lanzada cuando un step intenta sobrescribir la base URL del ambiente
 * en un proyecto que tiene {@code base.url.override.allowed=false}.
 *
 * <p>Esta excepción falla el paso BDD con un mensaje claro que guía
 * al equipo a usar paths relativos y el selector de ambiente de la plataforma.
 */
public class BaseUrlOverrideException extends RuntimeException {

    public BaseUrlOverrideException(String message) {
        super(message);
    }
}
