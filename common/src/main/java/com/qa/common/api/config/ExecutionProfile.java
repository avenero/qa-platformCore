package com.qa.common.api.config;

import java.util.Locale;

/**
 * Execution profile signal propagated from the deploy environment (env var
 * {@code EXECUTION_PROFILE}) through the BE bootstrap into {@code ExecutionConfig}
 * and consumed by Core security guards (e.g. SSL trust-all gate, M-3).
 *
 * <p>Fail-closed by design (ADR-IEP-1, 2026-05-27): any null, blank or
 * unrecognised value resolves to {@link #PROD} so that a fresh production
 * install without explicit configuration cannot accidentally enable
 * non-prod-only relaxations (such as trust-all TLS).
 *
 * <p>This intentionally diverges from Spring Boot's {@code SPRING_PROFILES_ACTIVE}
 * (which defaults to permissive {@code default}); the security trade-off is
 * accepted in favour of safe defaults.
 *
 * @since 2.1.0 (INIT-EXEC-PROFILE / IEP-1)
 */
public enum ExecutionProfile {
    DEV, TEST, STAGING, PROD;

    /**
     * Resolves a raw env-var string into a profile. {@code null}, blank or
     * unknown values fall back to {@link #PROD} (fail-closed).
     *
     * @param envValue raw value, typically {@code System.getenv("EXECUTION_PROFILE")}
     * @return resolved profile, never {@code null}
     */
    public static ExecutionProfile fromEnv(String envValue) {
        if (envValue == null || envValue.isBlank()) {
            return PROD;
        }
        try {
            return valueOf(envValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return PROD;
        }
    }

    public boolean isProd() {
        return this == PROD;
    }

    public boolean isPrivate() {
        return this == DEV || this == TEST;
    }
}
