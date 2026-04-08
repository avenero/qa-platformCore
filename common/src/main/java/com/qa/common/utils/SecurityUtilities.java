package com.qa.common.utils;

import com.qa.common.http.exceptions.FrameworkTechnicalException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Utilidades de seguridad sin dependencias de Spring.
 * Clase estática para manejo seguro de credenciales y encriptación.
 *
 * @author QA Automation Framework Team
 * @since 1.0.0
 */
public class SecurityUtilities {

    private static final String SHA_256 = "SHA-256";
    private static final SecureRandom secureRandom = new SecureRandom();

    private SecurityUtilities() {
        // Utility class - no instances
    }

    /**
     * Genera un token aleatorio seguro.
     */
    public static String generateSecureToken(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Genera una contraseña segura.
     */
    public static String generateSecurePassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }

        return password.toString();
    }

    /**
     * Enmascara una contraseña para logs.
     */
    public static String maskPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "***EMPTY***";
        }

        if (password.length() <= 4) {
            return "***HIDDEN***";
        }

        return password.substring(0, 2) + "***" + password.substring(password.length() - 2);
    }

    /**
     * Enmascara un token para logs.
     */
    public static String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return "***EMPTY***";
        }

        if (token.length() <= 8) {
            return "***HIDDEN***";
        }

        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }

    /**
     * Calcula el hash SHA-256 de un texto.
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
     * Verifica si una contraseña es segura.
     */
    public static boolean isSecurePassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * Obtiene una instancia de SecureRandom.
     * Método de conveniencia para otros utilities.
     */
    public static SecureRandom getSecureRandomInstance() {
        return secureRandom;
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
        return secureRandom.nextInt((max - min) + 1) + min;
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
            result.append(charPool.charAt(secureRandom.nextInt(charPool.length())));
        }

        return result.toString();
    }
}
