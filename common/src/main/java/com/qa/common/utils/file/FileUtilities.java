package com.qa.common.utils.file;
import com.qa.common.utils.security.SecurityUtilities;

import com.qa.common.api.logging.TestLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Utilidades para operaciones con archivos y directorios.
 *
 * <p>Consolida las operaciones de I/O de archivos que antes estaban dispersas en
 * {@link DataUtilities}, siguiendo el principio de responsabilidad única.</p>
 *
 * @author Abel Venero
 * @since 2.1.0
 */
public final class FileUtilities {

    private FileUtilities() {
        throw new UnsupportedOperationException("FileUtilities es una clase de utilidad");
    }

    /**
     * Escribe un string a un archivo (crea o sobreescribe).
     *
     * @param content  contenido a escribir
     * @param filepath ruta del archivo
     * @throws IOException              si hay error de escritura
     * @throws IllegalArgumentException si {@code content} o {@code filepath} son null
     */
    public static void writeStringToFile(String content, String filepath) throws IOException {
        if (content == null || filepath == null) {
            throw new IllegalArgumentException("content y filepath no pueden ser null");
        }
        Files.writeString(Path.of(filepath), content,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        TestLogger.logDebug("FILE_UTILITIES",
            "Contenido escrito a archivo: " + filepath, null);
    }

    /**
     * Lee el contenido de un archivo como String.
     *
     * @param filepath ruta del archivo
     * @return contenido del archivo como String
     * @throws IOException              si hay error de lectura
     * @throws IllegalArgumentException si {@code filepath} es null
     */
    public static String readStringFromFile(String filepath) throws IOException {
        if (filepath == null) {
            throw new IllegalArgumentException("filepath no puede ser null");
        }
        String content = Files.readString(Path.of(filepath));
        TestLogger.logDebug("FILE_UTILITIES",
            "Archivo leído: " + filepath + " (" + content.length() + " chars)", null);
        return content;
    }

    /**
     * Crea un directorio (y todos sus padres) si no existe.
     *
     * @param dirPath ruta del directorio
     * @throws IOException              si hay error creando el directorio
     * @throws IllegalArgumentException si {@code dirPath} es null o vacío
     */
    public static void createDirectoryIfNotExists(String dirPath) throws IOException {
        if (dirPath == null || dirPath.trim().isEmpty()) {
            throw new IllegalArgumentException("dirPath no puede ser null o vacío");
        }
        Path path = Path.of(dirPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            TestLogger.logDebug("FILE_UTILITIES", "Directorio creado: " + dirPath, null);
        }
    }

    /**
     * Verifica si un archivo existe y tiene contenido.
     *
     * @param filepath ruta del archivo
     * @return {@code true} si el archivo existe y tiene más de 0 bytes
     */
    public static boolean fileExistsAndNotEmpty(String filepath) {
        if (filepath == null) {
            return false;
        }
        try {
            Path path = Path.of(filepath);
            return Files.exists(path) && Files.size(path) > 0;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Elimina un archivo si existe.
     *
     * @param filepath ruta del archivo
     * @return {@code true} si el archivo fue eliminado
     */
    public static boolean deleteIfExists(String filepath) {
        if (filepath == null) {
            return false;
        }
        try {
            return Files.deleteIfExists(Path.of(filepath));
        } catch (IOException e) {
            TestLogger.logWarning("FILE_UTILITIES",
                "No se pudo eliminar: " + filepath + " — " + e.getMessage(), null);
            return false;
        }
    }
}

