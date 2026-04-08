package com.qa.common.runtime.events;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios para {@link EventBus}.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("EventBus")
class EventBusTest {

    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
    }

    @Nested
    @DisplayName("subscribe")
    class SubscribeTests {

        @Test
        @DisplayName("subscribe incrementa conteo de suscriptores")
        void subscribeIncrementaConteo() {
            assertThat(eventBus.subscriberCount()).isZero();

            eventBus.subscribe(event -> {});
            assertThat(eventBus.subscriberCount()).isEqualTo(1);

            eventBus.subscribe(event -> {});
            assertThat(eventBus.subscriberCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("subscribe con null lanza NullPointerException")
        void subscribeConNullLanzaNPE() {
            assertThatThrownBy(() -> eventBus.subscribe(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("unsubscribe")
    class UnsubscribeTests {

        @Test
        @DisplayName("unsubscribe remueve suscriptor existente")
        void unsubscribeRemueveSuscriptorExistente() {
            EventSubscriber subscriber = event -> {};
            eventBus.subscribe(subscriber);
            assertThat(eventBus.subscriberCount()).isEqualTo(1);

            boolean removed = eventBus.unsubscribe(subscriber);
            assertThat(removed).isTrue();
            assertThat(eventBus.subscriberCount()).isZero();
        }

        @Test
        @DisplayName("unsubscribe retorna false para suscriptor no registrado")
        void unsubscribeRetornaFalseParaNoRegistrado() {
            boolean removed = eventBus.unsubscribe(event -> {});
            assertThat(removed).isFalse();
        }

        @Test
        @DisplayName("unsubscribe retorna false para null")
        void unsubscribeRetornaFalseParaNull() {
            boolean removed = eventBus.unsubscribe(null);
            assertThat(removed).isFalse();
        }
    }

    @Nested
    @DisplayName("publish")
    class PublishTests {

        @Test
        @DisplayName("publish notifica a todos los suscriptores")
        void publishNotificaATodos() {
            List<String> received1 = new ArrayList<>();
            List<String> received2 = new ArrayList<>();

            eventBus.subscribe(event -> received1.add(event.getName()));
            eventBus.subscribe(event -> received2.add(event.getName()));

            ExecutionEvent event = ExecutionEvent.of(
                    ExecutionEvent.Type.SCENARIO_START, "Test Scenario");
            eventBus.publish(event);

            assertThat(received1).containsExactly("Test Scenario");
            assertThat(received2).containsExactly("Test Scenario");
        }

        @Test
        @DisplayName("publish con null lanza NullPointerException")
        void publishConNullLanzaNPE() {
            assertThatThrownBy(() -> eventBus.publish(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("publish sin suscriptores no falla")
        void publishSinSuscriptoresNoFalla() {
            ExecutionEvent event = ExecutionEvent.of(
                    ExecutionEvent.Type.CUSTOM, "No Subscribers");
            // No debe lanzar excepcion
            eventBus.publish(event);
        }

        @Test
        @DisplayName("publish multiples eventos entrega todos en orden")
        void publishMultiplesEventosEntregaEnOrden() {
            List<String> received = new ArrayList<>();
            eventBus.subscribe(event -> received.add(event.getName()));

            eventBus.publish(ExecutionEvent.of(ExecutionEvent.Type.STEP_START, "Step 1"));
            eventBus.publish(ExecutionEvent.of(ExecutionEvent.Type.STEP_END, "Step 1 Done"));
            eventBus.publish(ExecutionEvent.of(ExecutionEvent.Type.STEP_START, "Step 2"));

            assertThat(received).containsExactly("Step 1", "Step 1 Done", "Step 2");
        }

        @Test
        @DisplayName("Error en suscriptor no interrumpe otros suscriptores")
        void errorEnSuscriptorNoInterrumpeOtros() {
            List<String> received = new ArrayList<>();

            eventBus.subscribe(event -> { throw new RuntimeException("Boom!"); });
            eventBus.subscribe(event -> received.add(event.getName()));

            eventBus.publish(ExecutionEvent.of(
                    ExecutionEvent.Type.CUSTOM, "Resilient Event"));

            assertThat(received).containsExactly("Resilient Event");
        }
    }

    @Nested
    @DisplayName("clear")
    class ClearTests {

        @Test
        @DisplayName("clear elimina todos los suscriptores")
        void clearEliminaTodosLosSuscriptores() {
            eventBus.subscribe(event -> {});
            eventBus.subscribe(event -> {});
            assertThat(eventBus.subscriberCount()).isEqualTo(2);

            eventBus.clear();
            assertThat(eventBus.subscriberCount()).isZero();
        }

        @Test
        @DisplayName("Despues de clear, publish no notifica a nadie")
        void despuesDeClearPublishNoNotifica() {
            List<String> received = new ArrayList<>();
            eventBus.subscribe(event -> received.add(event.getName()));
            eventBus.clear();

            eventBus.publish(ExecutionEvent.of(ExecutionEvent.Type.CUSTOM, "Ghost"));
            assertThat(received).isEmpty();
        }
    }
}

