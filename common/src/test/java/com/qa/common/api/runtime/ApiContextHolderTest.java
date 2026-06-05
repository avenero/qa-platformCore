package com.qa.common.api.runtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiContextHolder — puerto neutral ThreadLocal<Object> (FEC-API-SHIP-WEB-SHARE)")
class ApiContextHolderTest {

    @AfterEach
    void cleanup() {
        ApiContextHolder.clear();
    }

    @Test
    @DisplayName("current() es null por defecto (sin sesión de browser → standalone)")
    void currentNullByDefault() {
        assertThat(ApiContextHolder.current()).isNull();
    }

    @Test
    @DisplayName("set/current devuelve el objeto publicado")
    void setThenCurrentReturnsValue() {
        Object ctx = new Object();
        ApiContextHolder.set(ctx);
        assertThat(ApiContextHolder.current()).isSameAs(ctx);
    }

    @Test
    @DisplayName("clear() vacía el holder")
    void clearEmptiesHolder() {
        ApiContextHolder.set(new Object());
        ApiContextHolder.clear();
        assertThat(ApiContextHolder.current()).isNull();
    }

    @Test
    @DisplayName("set(null) deja current() en null (equivale a no tener sesión)")
    void setNullYieldsNull() {
        ApiContextHolder.set(new Object());
        ApiContextHolder.set(null);
        assertThat(ApiContextHolder.current()).isNull();
    }

    @Test
    @DisplayName("es ThreadLocal: el valor de un hilo no se ve en otro")
    void valueIsThreadLocal() throws InterruptedException {
        Object main = new Object();
        ApiContextHolder.set(main);

        // Init non-null para probar que el otro hilo realmente lo cambia a null.
        AtomicReference<Object> seenInOtherThread = new AtomicReference<>(main);
        Thread t = new Thread(() -> {
            seenInOtherThread.set(ApiContextHolder.current());
            ApiContextHolder.clear(); // higiene del ThreadLocal del hilo worker
        });
        t.start();
        t.join();

        assertThat(seenInOtherThread.get()).as("otro hilo no ve el valor").isNull();
        assertThat(ApiContextHolder.current()).as("el hilo principal sí").isSameAs(main);
    }
}
