package com.qa.common.runtime.events;

/**
 * Interfaz funcional para suscriptores de eventos de ejecucion.
 *
 * @author Abel Venero
 * @since 2.0.0
 * @see EventBus
 * @see ExecutionEvent
 */
@FunctionalInterface
public interface EventSubscriber {

    /**
     * Procesa un evento publicado.
     * Las implementaciones deben ser thread-safe y no bloquear.
     *
     * @param event evento a procesar
     */
    void onEvent(ExecutionEvent event);
}
