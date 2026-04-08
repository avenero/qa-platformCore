package com.qa.common.runtime.events;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bus de eventos Pub/Sub para el ciclo de ejecucion.
 *
 * <p>Permite publicar eventos y notificar a todos los suscriptores registrados.
 * Thread-safe gracias a {@link CopyOnWriteArrayList}.
 *
 * <p>Los suscriptores pueden filtrar por tipo de evento o recibir todos.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public final class EventBus {

    private static final Logger log = LoggerFactory.getLogger(EventBus.class);

    private final CopyOnWriteArrayList<EventSubscriber> subscribers = new CopyOnWriteArrayList<>();

    /**
     * Registra un suscriptor que recibira todos los eventos.
     * @param subscriber suscriptor, no null
     */
    public void subscribe(EventSubscriber subscriber) {
        Objects.requireNonNull(subscriber, "subscriber no puede ser null");
        subscribers.add(subscriber);
        log.debug("Suscriptor registrado. Total: {}", subscribers.size());
    }

    /**
     * Elimina un suscriptor.
     * @param subscriber suscriptor a remover
     * @return true si fue removido
     */
    public boolean unsubscribe(EventSubscriber subscriber) {
        return subscriber != null && subscribers.remove(subscriber);
    }

    /**
     * Publica un evento a todos los suscriptores registrados.
     * Los errores en suscriptores se logean pero no interrumpen la publicacion.
     *
     * @param event evento a publicar, no null
     */
    public void publish(ExecutionEvent event) {
        Objects.requireNonNull(event, "event no puede ser null");
        log.debug("Publicando evento: {} - {}", event.getType(), event.getName());
        for (EventSubscriber subscriber : subscribers) {
            try {
                subscriber.onEvent(event);
            } catch (Exception e) {
                log.error("Error en suscriptor procesando evento {}: {}",
                        event.getType(), e.getMessage(), e);
            }
        }
    }

    /**
     * Cantidad de suscriptores registrados.
     * @return cantidad
     */
    public int subscriberCount() {
        return subscribers.size();
    }

    /**
     * Limpia todos los suscriptores.
     */
    public void clear() {
        subscribers.clear();
        log.debug("EventBus limpiado");
    }
}
