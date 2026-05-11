package com.qa.common.reporting.core.model;

import com.qa.common.utils.security.SecurityUtilities;

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

    /**
     * Constructor por defecto para deserialización.
     */
    public Attachment() {
    }

    /**
     * Constructor principal para crear un attachment con nombre, ruta y tipo.
     *
     * @param name nombre descriptivo del attachment
     * @param path ruta al archivo en el sistema de archivos
     * @param type tipo de attachment
     */
    public Attachment(String name, String path, AttachmentType type) {
        this.name = name;
        this.path = path;
        this.type = type;
    }

    /**
     * Obtiene el nombre descriptivo del attachment.
     *
     * @return nombre del attachment
     */
    public String getName() {
        return name;
    }

    /**
     * Establece el nombre descriptivo del attachment.
     *
     * @param name nombre del attachment
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Obtiene la ruta al archivo del attachment.
     *
     * @return ruta al archivo
     */
    public String getPath() {
        return path;
    }

    /**
     * Establece la ruta al archivo del attachment.
     *
     * @param path ruta al archivo
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Obtiene el tipo del attachment.
     *
     * @return tipo de attachment
     */
    public AttachmentType getType() {
        return type;
    }

    /**
     * Establece el tipo del attachment.
     *
     * @param type tipo de attachment
     */
    public void setType(AttachmentType type) {
        this.type = type;
    }

    /**
     * Obtiene el contenido binario del attachment.
     *
     * @return array de bytes con el contenido
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * Establece el contenido binario del attachment y actualiza el tamaño.
     *
     * @param content array de bytes con el contenido
     */
    public void setContent(byte[] content) {
        this.content = content;
        this.sizeBytes = content != null ? content.length : 0;
    }

    /**
     * Obtiene el tamaño del attachment en bytes.
     *
     * @return tamaño en bytes
     */
    public long getSizeBytes() {
        return sizeBytes;
    }

    /**
     * Establece el tamaño del attachment en bytes.
     *
     * @param sizeBytes tamaño en bytes
     */
    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    /**
     * Obtiene el MIME type del attachment.
     *
     * @return MIME type
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Establece el MIME type del attachment.
     *
     * @param mimeType MIME type del archivo
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Tipos de attachments soportados.
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

        /**
         * Constructor del enum con el MIME type por defecto.
         *
         * @param defaultMimeType MIME type predeterminado para este tipo de attachment
         */
        AttachmentType(String defaultMimeType) {
            this.defaultMimeType = defaultMimeType;
        }

        /**
         * Obtiene el MIME type por defecto para este tipo de attachment.
         *
         * @return MIME type por defecto
         */
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

