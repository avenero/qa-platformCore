package com.qa.common.reporting.core.bridge;

/**
 * Immutable bridge record for an attachment (screenshot, evidence) in a test report.
 * Content is pre-encoded as base64 so reports are self-contained.
 */
public record AttachmentData(
        String name,
        String base64Content,
        String mimeType
) {}
