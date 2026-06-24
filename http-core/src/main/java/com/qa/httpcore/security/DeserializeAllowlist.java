package com.qa.httpcore.security;

import com.qa.common.api.config.ConfigLoaderHolder;
import com.qa.common.api.config.ExecutionProfile;
import com.qa.common.api.exception.FrameworkTechnicalException;
import com.qa.common.api.runtime.ExecutionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Default-deny allowlist for the class resolved when a scenario asks the framework to
 * deserialize an HTTP response into a typed Java object ({@code serializo la respuesta
 * en la clase {string}}). Closes hallazgo M-2 (ADR-M2 Alt A): {@code className} comes from
 * Gherkin and was previously fed straight into {@code Class.forName}, allowing arbitrary
 * class loading (RCE / gadget vector).
 *
 * <p>A class is resolvable only if it lives under an allow-listed package prefix. The
 * default prefixes are the model packages the framework already shipped plus
 * {@code java.util.}, so existing scenarios keep working; the RCE vector — arbitrary FQCNs
 * such as {@code java.lang.Runtime}, {@code ProcessBuilder} and gadget chains — is denied.
 * Additional prefixes are added via {@code http.deserialize.allowed-packages} — never
 * hardcoded inline.
 *
 * <p>The legacy escape hatch ({@code http.deserialize.legacy-mode}) restores the old
 * allow-any behaviour, but it is fail-closed in PROD: the {@link ExecutionProfile} resolved
 * from the per-thread {@link ExecutionContext} (PROD when absent) makes the flag a no-op
 * under PROD, mirroring the SSL trust-all gate. Sunset v1.1 (card W2-M2-LEGACY-SUNSET).
 *
 * <p>Pure http-core, no Spring: usable from the qa-module-test CLI without a BE.
 *
 * @since 1.0.0 (W2-M2)
 */
public final class DeserializeAllowlist {

    /**
     * Built-in allow-listed package prefixes. The six model packages previously inlined in
     * {@code ApiHelper.loadClass}, plus {@code java.util.} for generic collection targets.
     * Always present; {@link #ALLOWED_PACKAGES_KEY} adds to them.
     *
     * <p>{@code java.util.} (Lead 2026-06-23) covers existing scenarios that deserialize a
     * response into a generic {@code Map}/{@code LinkedHashMap}; it is RCE-safe because the
     * dangerous gadgets ({@code java.lang.Runtime}, {@code ProcessBuilder}, deserialization
     * gadget chains) live under {@code java.lang.}/library packages, which stay denied.
     */
    static final List<String> DEFAULT_ALLOWED_PACKAGES = List.of(
        "com.module.models.",
        "com.module.dto.",
        "com.module.responses.",
        "com.test.models.",
        "models.",
        "dto.",
        "java.util."
    );

    /** Config key (comma-separated) whose prefixes are added to {@link #DEFAULT_ALLOWED_PACKAGES}. */
    static final String ALLOWED_PACKAGES_KEY = "http.deserialize.allowed-packages";

    /** Config key for the allow-any escape hatch; ignored under PROD. Sunset v1.1. */
    static final String LEGACY_MODE_KEY = "http.deserialize.legacy-mode";

    /**
     * Nota Q2 (Lead 2026-06-07): single source of truth for the denial message — a named
     * {@code static final} constant, not an i18n bundle (Core has none for errors) nor a
     * hardcoded literal at the throw site. Class-name-free by design (§D.4: no log of
     * class-names), so logging this message cannot leak the probed FQCN.
     */
    static final String MSG_CLASS_NOT_ALLOWED =
        "Deserialization target class is not in an allow-listed package. "
        + "Configure http.deserialize.allowed-packages to permit additional packages.";

    private DeserializeAllowlist() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Resolves {@code className} to a {@link Class} only if it lives under an allow-listed
     * package prefix (default-deny).
     *
     * @param className FQCN or simple name from the scenario
     * @return the loaded class
     * @throws ClassNotFoundException     class is allow-listed (or legacy-mode) but not on the classpath
     * @throws FrameworkTechnicalException class is outside the allowlist (denied)
     */
    public static Class<?> resolve(String className)
            throws ClassNotFoundException, FrameworkTechnicalException {
        if (className == null || className.isBlank()) {
            throw new FrameworkTechnicalException(MSG_CLASS_NOT_ALLOWED);
        }

        List<String> allowed = effectiveAllowedPackages();

        // Already an FQCN under an allowed prefix → load directly (may legitimately not exist).
        if (startsWithAllowed(className, allowed)) {
            return Class.forName(className);
        }

        // Simple name → resolve against each allowed prefix.
        Class<?> bySimpleName = tryAllowedPrefixes(className, allowed);
        if (bySimpleName != null) {
            return bySimpleName;
        }

        // Not in the allowlist. Legacy escape hatch restores allow-any, but only outside PROD.
        if (isLegacyAllowAnyEffective()) {
            return Class.forName(className);
        }

        throw new FrameworkTechnicalException(MSG_CLASS_NOT_ALLOWED);
    }

    private static boolean startsWithAllowed(String className, List<String> allowed) {
        for (String prefix : allowed) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static Class<?> tryAllowedPrefixes(String simpleName, List<String> allowed) {
        for (String prefix : allowed) {
            try {
                return Class.forName(prefix + simpleName);
            } catch (ClassNotFoundException ignored) {
                // Try the next allowed prefix.
            }
        }
        return null;
    }

    private static List<String> effectiveAllowedPackages() {
        String configured = ConfigLoaderHolder.get().getRaw(ALLOWED_PACKAGES_KEY, "");
        if (configured == null || configured.isBlank()) {
            return DEFAULT_ALLOWED_PACKAGES;
        }
        List<String> result = new ArrayList<>(DEFAULT_ALLOWED_PACKAGES);
        for (String token : configured.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                // Pin a trailing dot so prefix matching cannot bleed into sibling packages
                // (e.g. "com.acme.dto" must not match "com.acme.dtoEvil").
                result.add(trimmed.endsWith(".") ? trimmed : trimmed + ".");
            }
        }
        return result;
    }

    private static boolean isLegacyAllowAnyEffective() {
        boolean requested = Boolean.parseBoolean(
            ConfigLoaderHolder.get().getRaw(LEGACY_MODE_KEY, "false"));
        return requested && !resolveProfile().isProd();
    }

    /**
     * Resolves the {@link ExecutionProfile} from the per-thread {@link ExecutionContext},
     * fail-closed to {@link ExecutionProfile#PROD} when absent (CLI without BE, tests).
     * Mirror of {@code ApacheHttpClientImpl.resolveProfileForTrustAllGuard}.
     */
    private static ExecutionProfile resolveProfile() {
        return ExecutionContext.current()
            .map(ctx -> ctx.config().getProfile())
            .orElse(ExecutionProfile.PROD);
    }
}
