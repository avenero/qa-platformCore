package com.qa.common.reporting.core.model;

/**
 * Representa un archivo adjunto (screenshot, log, video, reporte).
 * Usado para evidencias de tests y reportes.
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
public class Attachment {

    private String name;
    private String path;
    private AttachmentType type;
    private byte[] content;
    private long sizeBytes;
    private String mimeType;

    public Attachment() {
    }

    public Attachment(String name, String path, AttachmentType type) {
        this.name = name;
        this.path = path;
        this.type = type;
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public AttachmentType getType() {
        return type;
    }

    public void setType(AttachmentType type) {
        this.type = type;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
        this.sizeBytes = content != null ? content.length : 0;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Tipos de attachments soportados
     */
    public enum AttachmentType {
        SCREENSHOT("image/png"),
        LOG("text/plain"),
        VIDEO("video/mp4"),
        REPORT("text/html"),
        JSON("application/json"),
        XML("application/xml"),
        PDF("application/pdf"),
        OTHER("application/octet-stream");

        private final String defaultMimeType;

        AttachmentType(String defaultMimeType) {
            this.defaultMimeType = defaultMimeType;
        }

        public String getDefaultMimeType() {
            return defaultMimeType;
        }
    }

    @Override
    public String toString() {
        return "Attachment{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", sizeBytes=" + sizeBytes +
                ", path='" + path + '\'' +
                '}';
    }
}

