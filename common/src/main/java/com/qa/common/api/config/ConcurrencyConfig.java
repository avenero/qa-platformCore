package com.qa.common.api.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Duration;

/**
 * Configuración de concurrencia del Core (TASK-K03; suelo para K07).
 *
 * <p>Prefijo: {@code concurrency}.
 *
 * @param maxParallelism      Threads concurrentes para ejecución paralela (1..64).
 * @param defaultRetryAttempts Reintentos default por operación (0..10).
 * @param defaultBackoffStrategy fixed | linear | exponential | jittered.
 * @param defaultInitialBackoff Backoff inicial para estrategias de retry.
 * @param threadNamePrefix    Prefijo de nombres de thread para debugging.
 *
 * @since TASK-K03
 */
public record ConcurrencyConfig(
        @Min(1) @Max(64) int maxParallelism,
        @Min(0) @Max(10) int defaultRetryAttempts,
        @NotBlank @Pattern(regexp = "fixed|linear|exponential|jittered",
                message = "defaultBackoffStrategy debe ser fixed/linear/exponential/jittered")
        String defaultBackoffStrategy,
        @NotNull Duration defaultInitialBackoff,
        @NotBlank String threadNamePrefix
) implements TypedConfig {

    public static ConcurrencyConfig defaults() {
        return new ConcurrencyConfig(
                Math.max(2, Runtime.getRuntime().availableProcessors()),
                0,
                "exponential",
                Duration.ofMillis(500),
                "qa-core"
        );
    }
}
