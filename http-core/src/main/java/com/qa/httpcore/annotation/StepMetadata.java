package com.qa.httpcore.annotation;

/**
 * @deprecated Use {@link com.qa.common.runtime.annotation.StepMetadata} instead.
 *             This alias exists for backward compatibility; it will be removed in a future version.
 */
@Deprecated(since = "2.4.0", forRemoval = true)
public @interface StepMetadata {
    boolean canOverrideBaseUrl() default false;
    boolean usesLiteralUrl() default false;
}
