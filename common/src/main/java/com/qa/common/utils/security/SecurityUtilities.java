package com.qa.common.utils.security;

import com.qa.common.api.exception.FrameworkTechnicalException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilidades de seguridad sin dependencias de Spring.
 * Clase estática para manejo seguro de credenciales y encriptación.
 *
 * @author QA Automation Framework Team
 * @since 1.0.0
 */
public class SecurityUtilities {

    private static final String SHA_256 = "SHA-256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Longitud mínima para mostrar caracteres reales al enmascarar contraseñas. */
    private static final int MIN_PASSWORD_REVEAL_LENGTH = 4;

    /** Longitud mínima para mostrar caracteres reales al enmascarar tokens. */
    private static final int MIN_TOKEN_REVEAL_LENGTH = 8;

    /** Número de caracteres del inicio/final a revelar al enmascarar contraseñas. */
    private static final int PASSWORD_REVEAL_CHARS = 2;

    /** Número de caracteres del inicio/final a revelar al enmascarar tokens. */
    private static final int TOKEN_REVEAL_CHARS = 4;

    /** Longitud mínima requerida para una contraseña segura. */
    private static final int MIN_SECURE_PASSWORD_LENGTH = 8;

    private SecurityUtilities() {
        // Utility class - no instances
    }

    /**
     * Genera un token aleatorio seguro.
     *
     * @param length longitud en bytes del token (el resultado en Base64 será más largo)
     * @return token aleatorio en formato Base64 URL-safe sin padding
     */
    public static String generateSecureToken(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Genera una contraseña segura con letras, dígitos y caracteres especiales.
     *
     * @param length longitud de la contraseña
     * @return contraseña aleatoria segura
     */
    public static String generateSecurePassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }

        return password.toString();
    }

    /**
     * Enmascara una contraseña para logs, mostrando solo los primeros y últimos 2 caracteres.
     *
     * @param password contraseña a enmascarar
     * @return contraseña enmascarada segura para logging
     */
    public static String maskPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "***EMPTY***";
        }

        if (password.length() <= MIN_PASSWORD_REVEAL_LENGTH) {
            return "***HIDDEN***";
        }

        return password.substring(0, PASSWORD_REVEAL_CHARS)
                + "***"
                + password.substring(password.length() - PASSWORD_REVEAL_CHARS);
    }

    /**
     * Enmascara un token para logs, mostrando solo los primeros y últimos 4 caracteres.
     *
     * @param token token a enmascarar
     * @return token enmascarado seguro para logging
     */
    public static String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return "***EMPTY***";
        }

        if (token.length() <= MIN_TOKEN_REVEAL_LENGTH) {
            return "***HIDDEN***";
        }

        return token.substring(0, TOKEN_REVEAL_CHARS)
                + "***"
                + token.substring(token.length() - TOKEN_REVEAL_CHARS);
    }

    /**
     * Calcula el hash SHA-256 de un texto y lo devuelve en Base64.
     *
     * @param input texto a hashear
     * @return hash SHA-256 en formato Base64
     * @throws FrameworkTechnicalException si falla el algoritmo SHA-256
     */
    public static String sha256Hash(String input) throws FrameworkTechnicalException {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new FrameworkTechnicalException("sha256Hash", "Error calculating hash: " + e.getMessage());
        }
    }

    /**
     * Verifica si una contraseña cumple los requisitos mínimos de seguridad.
     *
     * <p>Requisitos: longitud mínima de 8, mayúsculas, minúsculas, dígitos y especiales.
     *
     * @param password contraseña a verificar
     * @return {@code true} si la contraseña es segura
     */
    public static boolean isSecurePassword(String password) {
        if (password == null || password.length() < MIN_SECURE_PASSWORD_LENGTH) {
            return false;
        }

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * Obtiene la instancia compartida de SecureRandom.
     *
     * <p>Método de conveniencia para otros utilities.
     *
     * @return instancia compartida de {@link SecureRandom}
     */
    public static SecureRandom getSecureRandomInstance() {
        return SECURE_RANDOM;
    }

    /**
     * Genera un número aleatorio seguro en un rango.
     *
     * @param min valor mínimo (inclusivo)
     * @param max valor máximo (inclusivo)
     * @return número aleatorio en el rango especificado
     */
    public static int generateRandomNumber(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("El valor mínimo no puede ser mayor que el máximo");
        }
        return SECURE_RANDOM.nextInt((max - min) + 1) + min;
    }

    /**
     * Genera un string aleatorio con las características especificadas.
     *
     * @param length longitud del string
     * @param includeLetters incluir letras (a-z, A-Z)
     * @param includeNumbers incluir números (0-9)
     * @param includeSpecialChars incluir caracteres especiales
     * @return string aleatorio generado
     */
    public static String generateRandomString(int length, boolean includeLetters,
                                              boolean includeNumbers, boolean includeSpecialChars) {
        if (length <= 0) {
            throw new IllegalArgumentException("La longitud debe ser mayor a 0");
        }

        StringBuilder chars = new StringBuilder();

        if (includeLetters) {
            chars.append("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
        }

        if (includeNumbers) {
            chars.append("0123456789");
        }

        if (includeSpecialChars) {
            chars.append("!@#$%^&*()_+-=[]{}|;:,.<>?");
        }

        if (chars.length() == 0) {
            throw new IllegalArgumentException(
                    "Debe incluir al menos un tipo de caracteres (letras, números o especiales)"
            );
        }

        StringBuilder result = new StringBuilder(length);
        String charPool = chars.toString();

        for (int i = 0; i < length; i++) {
            result.append(charPool.charAt(SECURE_RANDOM.nextInt(charPool.length())));
        }

        return result.toString();
    }
}
