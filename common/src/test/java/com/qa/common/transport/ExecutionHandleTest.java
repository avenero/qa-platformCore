package com.qa.common.transport;

import com.qa.common.runtime.ExecutionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExecutionHandle — handle async de un run (TASK-I01)")
class ExecutionHandleTest {

    private static ExecutionResult passedResult() {
        return new ExecutionResult.Builder()
                .totalScenarios(1)
                .passedScenarios(1)
                .startTime(Instant.now())
                .endTime(Instant.now())
                .status(ExecutionResult.Status.PASSED)
                .build();
    }

    @Test
    @DisplayName("rechaza executionId null/blank")
    void rejectsBlankExecutionId() {
        assertThatThrownBy(() -> new ExecutionHandle(null, new CompletableFuture<>(), () -> {}))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("executionId");
        assertThatThrownBy(() -> new ExecutionHandle("", new CompletableFuture<>(), () -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        assertThatThrownBy(() -> new ExecutionHandle("   ", new CompletableFuture<>(), () -> {}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rechaza future null")
    void rejectsNullFuture() {
        assertThatThrownBy(() -> new ExecutionHandle("exec-1", null, () -> {}))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("future");
    }

    @Test
    @DisplayName("rechaza cancelHook null")
    void rejectsNullCancelHook() {
        assertThatThrownBy(() -> new ExecutionHandle("exec-1", new CompletableFuture<>(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cancelHook");
    }

    @Test
    @DisplayName("isDone refleja el estado del future")
    void isDoneTracksFuture() {
        CompletableFuture<ExecutionResult> future = new CompletableFuture<>();
        ExecutionHandle handle = new ExecutionHandle("exec-1", future, () -> {});

        assertThat(handle.isDone()).isFalse();

        future.complete(passedResult());
        assertThat(handle.isDone()).isTrue();
        assertThat(handle.isCancelled()).isFalse();
    }

    @Test
    @DisplayName("isCancelled refleja cancelación del future")
    void isCancelledTracksFuture() {
        CompletableFuture<ExecutionResult> future = new CompletableFuture<>();
        ExecutionHandle handle = new ExecutionHandle("exec-2", future, () -> {});

        future.cancel(true);
        assertThat(handle.isDone()).isTrue();
        assertThat(handle.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("cancel() ejecuta el hook")
    void cancelExecutesHook() {
        AtomicInteger calls = new AtomicInteger();
        ExecutionHandle handle = new ExecutionHandle(
                "exec-3", new CompletableFuture<>(), calls::incrementAndGet);

        handle.cancel();
        assertThat(calls.get()).isOne();
    }

    @Test
    @DisplayName("cancel() es idempotente — invocar dos veces dispara el hook UNA sola vez")
    void cancelIsIdempotent() {
        AtomicInteger calls = new AtomicInteger();
        ExecutionHandle handle = new ExecutionHandle(
                "exec-4", new CompletableFuture<>(), calls::incrementAndGet);

        handle.cancel();
        handle.cancel();
        handle.cancel();

        assertThat(calls.get()).isOne();
    }

    @Test
    @DisplayName("cancel() concurrente desde múltiples hilos sigue siendo idempotente")
    void cancelIsThreadSafe() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ExecutionHandle handle = new ExecutionHandle(
                "exec-5", new CompletableFuture<>(), calls::incrementAndGet);

        Thread t1 = new Thread(handle::cancel);
        Thread t2 = new Thread(handle::cancel);
        Thread t3 = new Thread(handle::cancel);
        t1.start(); t2.start(); t3.start();
        t1.join(); t2.join(); t3.join();

        assertThat(calls.get()).isOne();
    }

    @Test
    @DisplayName("future completado con resultado se preserva")
    void futurePreservesResult() throws Exception {
        CompletableFuture<ExecutionResult> future = new CompletableFuture<>();
        ExecutionHandle handle = new ExecutionHandle("exec-6", future, () -> {});

        ExecutionResult result = passedResult();
        future.complete(result);

        assertThat(handle.future().get()).isSameAs(result);
        assertThat(handle.future().get().getStatus()).isEqualTo(ExecutionResult.Status.PASSED);
    }
}
