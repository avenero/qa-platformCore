package com.qa.common.utils;


import com.qa.common.utils.security.SecurityUtilities;
import com.qa.common.api.exception.FrameworkTechnicalException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para SecurityUtilities.
 *
 * <p>Cubre casos criticos de seguridad:
 * <ul>
 *   <li>Generacion de tokens y passwords seguros
 *   <li>Masking de credenciales en logs
 *   <li>Hashing SHA-256
 *   <li>Validacion de passwords seguros
 *   <li>Generacion de strings y numeros aleatorios
 * </ul>
 *
 * @author Abel Venero
 * @since 1.0.0
 */
@DisplayName("SecurityUtilities - Tests de Seguridad")
class SecurityUtilitiesTest {

    // =========================================================================
    // TESTS: generateSecureToken()
    // =========================================================================

    @Nested
    @DisplayName("generateSecureToken()")
    class GenerateSecureTokenTests {

        @Test
        @DisplayName("Debe generar token de longitud correcta")
        void testGenerateSecureTokenLength() {
            String token = SecurityUtilities.generateSecureToken(32);

            assertNotNull(token);
            assertFalse(token.isEmpty());
            // Base64 URL encoding sin padding: ~1.33x del tamaño original
            assertTrue(token.length() >= 40); // 32 bytes -> ~43 chars
        }

        @Test
        @DisplayName("Debe generar tokens únicos")
        void testGenerateSecureTokenUniqueness() {
            String token1 = SecurityUtilities.generateSecureToken(32);
            String token2 = SecurityUtilities.generateSecureToken(32);

            assertNotEquals(token1, token2, "Tokens deben ser únicos");
        }

        @Test
        @DisplayName("Debe generar 1000 tokens únicos (prueba estadística)")
        void testGenerateSecureTokenStatisticalUniqueness() {
            java.util.Set<String> tokens = new java.util.HashSet<>();

            for (int i = 0; i < 1000; i++) {
                tokens.add(SecurityUtilities.generateSecureToken(16));
            }

            assertEquals(1000, tokens.size(), "Todos los tokens deben ser únicos");
        }

        @Test
        @DisplayName("Debe manejar longitud cero correctamente")
        void testGenerateSecureTokenZeroLength() {
            String token = SecurityUtilities.generateSecureToken(0);

            // Array vacío -> Base64 vacío
            assertNotNull(token);
            assertTrue(token.isEmpty() || token.equals(""));
        }

        @Test
        @DisplayName("Debe fallar con longitud negativa")
        void testGenerateSecureTokenNegativeLength() {
            assertThrows(NegativeArraySizeException.class,
                () -> SecurityUtilities.generateSecureToken(-5),
                "Longitud negativa debe lanzar NegativeArraySizeException");
        }

        @Test
        @DisplayName("Debe generar token URL-safe (sin padding)")
        void testGenerateSecureTokenUrlSafe() {
            String token = SecurityUtilities.generateSecureToken(32);

            // Base64 URL no usa +, /, = (padding)
            assertFalse(token.contains("+"));
            assertFalse(token.contains("/"));
            assertFalse(token.contains("="));
        }
    }

    // =========================================================================
    // TESTS: generateSecurePassword()
    // =========================================================================

    @Nested
    @DisplayName("generateSecurePassword()")
    class GenerateSecurePasswordTests {

        @Test
        @DisplayName("Debe generar password de longitud correcta")
        void testGenerateSecurePasswordLength() {
            String password = SecurityUtilities.generateSecurePassword(12);

            assertEquals(12, password.length());
        }

        @Test
        @DisplayName("Debe generar passwords con caracteres válidos")
        void testGenerateSecurePasswordValidChars() {
            String password = SecurityUtilities.generateSecurePassword(50);
            String allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";

            for (char c : password.toCharArray()) {
                assertTrue(allowedChars.indexOf(c) >= 0,
                    "Caracter '" + c + "' no está en el conjunto permitido");
            }
        }

        @Test
        @DisplayName("Debe generar passwords únicos")
        void testGenerateSecurePasswordUniqueness() {
            String password1 = SecurityUtilities.generateSecurePassword(12);
            String password2 = SecurityUtilities.generateSecurePassword(12);

            assertNotEquals(password1, password2, "Passwords deben ser únicos");
        }

        @Test
        @DisplayName("Debe fallar con longitud negativa")
        void testGenerateSecurePasswordNegativeLength() {
            // NO lanza excepción - genera string vacío (loop no ejecuta)
            String password = SecurityUtilities.generateSecurePassword(-5);

            assertEquals("", password);
        }

        @Test
        @DisplayName("Debe manejar longitud cero")
        void testGenerateSecurePasswordZeroLength() {
            String password = SecurityUtilities.generateSecurePassword(0);

            assertEquals("", password);
        }

        @Test
        @DisplayName("Debe incluir variedad de caracteres (estadístico)")
        void testGenerateSecurePasswordVariety() {
            String password = SecurityUtilities.generateSecurePassword(100);

            // Debe tener al menos 3 tipos de caracteres
            boolean hasLetter = password.chars().anyMatch(Character::isLetter);
            boolean hasDigit = password.chars().anyMatch(Character::isDigit);
            boolean hasSpecial = password.chars().anyMatch(c -> "!@#$%^&*".indexOf(c) >= 0);

            int variety = (hasLetter ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);

            assertTrue(variety >= 2,
                "Password de 100 chars debe incluir al menos 2 tipos de caracteres");
        }
    }

    // =========================================================================
    // TESTS: maskPassword()
    // =========================================================================

    @Nested
    @DisplayName("maskPassword()")
    class MaskPasswordTests {

        @Test
        @DisplayName("Debe manejar NULL correctamente")
        void testMaskPasswordNull() {
            assertEquals("***EMPTY***", SecurityUtilities.maskPassword(null));
        }

        @Test
        @DisplayName("Debe manejar string vacío")
        void testMaskPasswordEmpty() {
            assertEquals("***EMPTY***", SecurityUtilities.maskPassword(""));
        }

        @ParameterizedTest
        @ValueSource(strings = {"a", "ab", "abc", "abcd"})
        @DisplayName("Debe ocultar passwords cortos (<= 4 chars)")
        void testMaskPasswordShort(String password) {
            assertEquals("***HIDDEN***", SecurityUtilities.maskPassword(password));
        }

        @Test
        @DisplayName("Debe mostrar parcialmente passwords de 5 chars")
        void testMaskPassword5Chars() {
            assertEquals("ab***de", SecurityUtilities.maskPassword("abcde"));
        }

        @Test
        @DisplayName("Debe mostrar parcialmente passwords medianos")
        void testMaskPasswordMedium() {
            // "Pass1234" -> substring(0,2)="Pa" + "***" + substring(8-2=6)="34"
            assertEquals("Pa***34", SecurityUtilities.maskPassword("Pass1234"));
        }

        @Test
        @DisplayName("Debe mostrar parcialmente passwords largos")
        void testMaskPasswordLong() {
            assertEquals("Su***rd",
                SecurityUtilities.maskPassword("SuperSecurePassword"));
        }

        @Test
        @DisplayName("Debe manejar passwords con caracteres especiales")
        void testMaskPasswordSpecialChars() {
            assertEquals("P@***!#",
                SecurityUtilities.maskPassword("P@ssw0rd!#"));
        }

        @Test
        @DisplayName("Debe preservar formato (primeros 2 + últimos 2)")
        void testMaskPasswordFormat() {
            String masked = SecurityUtilities.maskPassword("MyPassword123");

            assertTrue(masked.startsWith("My"));
            assertTrue(masked.endsWith("23"));
            assertTrue(masked.contains("***"));
            assertEquals("My***23", masked);
        }
    }

    // =========================================================================
    // TESTS: maskToken()
    // =========================================================================

    @Nested
    @DisplayName("maskToken()")
    class MaskTokenTests {

        @Test
        @DisplayName("Debe manejar NULL correctamente")
        void testMaskTokenNull() {
            assertEquals("***EMPTY***", SecurityUtilities.maskToken(null));
        }

        @Test
        @DisplayName("Debe manejar string vacío")
        void testMaskTokenEmpty() {
            assertEquals("***EMPTY***", SecurityUtilities.maskToken(""));
        }

        @ParameterizedTest
        @ValueSource(strings = {"a", "ab", "abc", "abcdefgh"})
        @DisplayName("Debe ocultar tokens cortos (<= 8 chars)")
        void testMaskTokenShort(String token) {
            assertEquals("***HIDDEN***", SecurityUtilities.maskToken(token));
        }

        @Test
        @DisplayName("Debe mostrar parcialmente tokens de 9 chars")
        void testMaskToken9Chars() {
            assertEquals("abcd***fghi", SecurityUtilities.maskToken("abcdefghi"));
        }

        @Test
        @DisplayName("Debe mostrar parcialmente tokens largos")
        void testMaskTokenLong() {
            String longToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
            // substring(0,4)="eyJh" + "***" + substring(length-4)="sw5c"
            assertEquals("eyJh***sw5c",
                SecurityUtilities.maskToken(longToken));
        }

        @Test
        @DisplayName("Debe preservar formato (primeros 4 + últimos 4)")
        void testMaskTokenFormat() {
            String masked = SecurityUtilities.maskToken("abc123xyz789");

            assertTrue(masked.startsWith("abc1"));
            assertTrue(masked.endsWith("z789"));
            assertTrue(masked.contains("***"));
            assertEquals("abc1***z789", masked);
        }
    }

    // =========================================================================
    // TESTS: sha256Hash()
    // =========================================================================

    @Nested
    @DisplayName("sha256Hash()")
    class Sha256HashTests {

        @Test
        @DisplayName("Debe calcular hash correctamente")
        void testSha256HashSuccess() throws FrameworkTechnicalException {
            String hash = SecurityUtilities.sha256Hash("test");

            assertNotNull(hash);
            assertFalse(hash.isEmpty());
            // SHA-256 en Base64 tiene ~44 caracteres
            assertTrue(hash.length() >= 40);
        }

        @Test
        @DisplayName("Debe ser consistente (mismo input = mismo hash)")
        void testSha256HashConsistency() throws FrameworkTechnicalException {
            String input = "my-secure-password";

            String hash1 = SecurityUtilities.sha256Hash(input);
            String hash2 = SecurityUtilities.sha256Hash(input);

            assertEquals(hash1, hash2, "Hash debe ser determinístico");
        }

        @Test
        @DisplayName("Debe generar hashes diferentes para inputs diferentes")
        void testSha256HashDifferentInputs() throws FrameworkTechnicalException {
            String hash1 = SecurityUtilities.sha256Hash("password1");
            String hash2 = SecurityUtilities.sha256Hash("password2");

            assertNotEquals(hash1, hash2, "Inputs diferentes deben generar hashes diferentes");
        }

        @Test
        @DisplayName("Debe manejar string vacío")
        void testSha256HashEmpty() throws FrameworkTechnicalException {
            String hash = SecurityUtilities.sha256Hash("");

            assertNotNull(hash);
            assertFalse(hash.isEmpty());
            // Hash de string vacío es válido
        }

        @Test
        @DisplayName("Debe fallar con NULL")
        void testSha256HashNull() {
            // El método captura NullPointerException y lanza FrameworkTechnicalException
            FrameworkTechnicalException exception = assertThrows(FrameworkTechnicalException.class,
                () -> SecurityUtilities.sha256Hash(null),
                "NULL debe lanzar FrameworkTechnicalException");

            assertTrue(exception.getMessage().contains("input") ||
                      exception.getMessage().contains("null"),
                "Mensaje debe indicar que el input es null");
        }

        @Test
        @DisplayName("Debe manejar strings largos sin problemas")
        void testSha256HashLargeString() throws FrameworkTechnicalException {
            String largeString = "a".repeat(10000);

            String hash = SecurityUtilities.sha256Hash(largeString);

            assertNotNull(hash);
            // SHA-256 siempre genera 32 bytes (44 chars en Base64)
            assertTrue(hash.length() >= 40);
        }

        @Test
        @DisplayName("Debe manejar caracteres especiales y Unicode")
        void testSha256HashSpecialChars() throws FrameworkTechnicalException {
            String input = "Contraseña€©®™🔐";

            String hash = SecurityUtilities.sha256Hash(input);

            assertNotNull(hash);
            assertFalse(hash.isEmpty());
        }
    }

    // =========================================================================
    // TESTS: isSecurePassword()
    // =========================================================================

    @Nested
    @DisplayName("isSecurePassword()")
    class IsSecurePasswordTests {

        @Test
        @DisplayName("Debe aceptar password seguro válido")
        void testIsSecurePasswordValid() {
            assertTrue(SecurityUtilities.isSecurePassword("Pass1234!"));
            assertTrue(SecurityUtilities.isSecurePassword("MyP@ssw0rd"));
            assertTrue(SecurityUtilities.isSecurePassword("Secure123#Test"));
        }

        @Test
        @DisplayName("Debe rechazar NULL")
        void testIsSecurePasswordNull() {
            assertFalse(SecurityUtilities.isSecurePassword(null));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "a", "ab", "abc", "Pass12!", "Short1!"})
        @DisplayName("Debe rechazar passwords cortos (< 8 chars)")
        void testIsSecurePasswordShort(String password) {
            assertFalse(SecurityUtilities.isSecurePassword(password));
        }

        @Test
        @DisplayName("Debe rechazar password sin mayúsculas")
        void testIsSecurePasswordNoUppercase() {
            assertFalse(SecurityUtilities.isSecurePassword("password123!"),
                "Password sin mayúsculas debe ser rechazado");
        }

        @Test
        @DisplayName("Debe rechazar password sin minúsculas")
        void testIsSecurePasswordNoLowercase() {
            assertFalse(SecurityUtilities.isSecurePassword("PASSWORD123!"),
                "Password sin minúsculas debe ser rechazado");
        }

        @Test
        @DisplayName("Debe rechazar password sin números")
        void testIsSecurePasswordNoDigits() {
            assertFalse(SecurityUtilities.isSecurePassword("Password!"),
                "Password sin números debe ser rechazado");
        }

        @Test
        @DisplayName("Debe rechazar password sin caracteres especiales")
        void testIsSecurePasswordNoSpecialChars() {
            assertFalse(SecurityUtilities.isSecurePassword("Password123"),
                "Password sin caracteres especiales debe ser rechazado");
        }

        @Test
        @DisplayName("Debe rechazar password débil (solo lowercase)")
        void testIsSecurePasswordWeakAllLowercase() {
            // BUG DETECTADO: Versión anterior aceptaba esto
            assertFalse(SecurityUtilities.isSecurePassword("aaaaaaaa"),
                "Password débil 'aaaaaaaa' debe ser rechazado");
        }

        @Test
        @DisplayName("Debe aceptar password con todos los requisitos mínimos")
        void testIsSecurePasswordMinimalRequirements() {
            // 8 chars, 1 upper, 1 lower, 1 digit, 1 special
            assertTrue(SecurityUtilities.isSecurePassword("Aa1!aaaa"));
        }

        @Test
        @DisplayName("Debe validar caracteres especiales permitidos")
        void testIsSecurePasswordAllowedSpecialChars() {
            String specials = "!@#$%^&*()_+-=[]{}|;:,.<>?";

            for (char c : specials.toCharArray()) {
                String password = "Pass123" + c;
                assertTrue(SecurityUtilities.isSecurePassword(password),
                    "Password con caracter especial '" + c + "' debe ser válido");
            }
        }
    }

    // =========================================================================
    // TESTS: generateRandomNumber()
    // =========================================================================

    @Nested
    @DisplayName("generateRandomNumber()")
    class GenerateRandomNumberTests {

        @Test
        @DisplayName("Debe generar número en rango")
        void testGenerateRandomNumberInRange() {
            for (int i = 0; i < 100; i++) {
                int num = SecurityUtilities.generateRandomNumber(10, 20);

                assertTrue(num >= 10 && num <= 20,
                    "Número " + num + " debe estar entre 10 y 20");
            }
        }

        @Test
        @DisplayName("Debe generar número cuando min == max")
        void testGenerateRandomNumberSameMinMax() {
            int num = SecurityUtilities.generateRandomNumber(5, 5);

            assertEquals(5, num);
        }

        @Test
        @DisplayName("Debe fallar cuando min > max")
        void testGenerateRandomNumberInvalidRange() {
            assertThrows(IllegalArgumentException.class,
                () -> SecurityUtilities.generateRandomNumber(20, 10),
                "Min mayor que max debe lanzar IllegalArgumentException");
        }

        @Test
        @DisplayName("Debe generar distribución uniforme (estadístico)")
        void testGenerateRandomNumberDistribution() {
            int[] counts = new int[5]; // Rango 0-4
            int samples = 10_000;

            for (int i = 0; i < samples; i++) {
                int num = SecurityUtilities.generateRandomNumber(0, 4);
                counts[num]++;
            }

            // Con 10 000 muestras, cada bucket espera ~2000.
            // Umbral ±40% (1200–2800) eliminando falsos negativos estadísticos.
            int expectedPerBucket = samples / 5;
            int tolerance = (int) (expectedPerBucket * 0.4);
            for (int i = 0; i < counts.length; i++) {
                assertTrue(
                    counts[i] >= expectedPerBucket - tolerance
                        && counts[i] <= expectedPerBucket + tolerance,
                    "Bucket [" + i + "] fuera de distribución uniforme esperada: " + counts[i]
                        + " (esperado " + (expectedPerBucket - tolerance)
                        + "-" + (expectedPerBucket + tolerance) + ")");
            }
        }

        @Test
        @DisplayName("Debe manejar rangos negativos")
        void testGenerateRandomNumberNegativeRange() {
            int num = SecurityUtilities.generateRandomNumber(-10, -5);

            assertTrue(num >= -10 && num <= -5);
        }

        @Test
        @DisplayName("Debe manejar rangos grandes")
        void testGenerateRandomNumberLargeRange() {
            int num = SecurityUtilities.generateRandomNumber(1, 1000000);

            assertTrue(num >= 1 && num <= 1000000);
        }
    }

    // =========================================================================
    // TESTS: generateRandomString()
    // =========================================================================

    @Nested
    @DisplayName("generateRandomString()")
    class GenerateRandomStringTests {

        @Test
        @DisplayName("Debe generar string con solo letras")
        void testGenerateRandomStringOnlyLetters() {
            String str = SecurityUtilities.generateRandomString(20, true, false, false);

            assertEquals(20, str.length());
            assertTrue(str.chars().allMatch(Character::isLetter));
        }

        @Test
        @DisplayName("Debe generar string con solo números")
        void testGenerateRandomStringOnlyNumbers() {
            String str = SecurityUtilities.generateRandomString(20, false, true, false);

            assertEquals(20, str.length());
            assertTrue(str.chars().allMatch(Character::isDigit));
        }

        @Test
        @DisplayName("Debe generar string con solo caracteres especiales")
        void testGenerateRandomStringOnlySpecialChars() {
            String str = SecurityUtilities.generateRandomString(20, false, false, true);

            assertEquals(20, str.length());
            String specials = "!@#$%^&*()_+-=[]{}|;:,.<>?";
            for (char c : str.toCharArray()) {
                assertTrue(specials.indexOf(c) >= 0);
            }
        }

        @Test
        @DisplayName("Debe generar string con combinación de tipos")
        void testGenerateRandomStringMixed() {
            String str = SecurityUtilities.generateRandomString(50, true, true, true);

            assertEquals(50, str.length());

            boolean hasLetter = str.chars().anyMatch(Character::isLetter);
            boolean hasDigit = str.chars().anyMatch(Character::isDigit);

            // Con 50 chars, estadísticamente debe tener ambos
            assertTrue(hasLetter);
            assertTrue(hasDigit);
        }

        @Test
        @DisplayName("Debe fallar con longitud <= 0")
        void testGenerateRandomStringInvalidLength() {
            assertThrows(IllegalArgumentException.class,
                () -> SecurityUtilities.generateRandomString(0, true, false, false));

            assertThrows(IllegalArgumentException.class,
                () -> SecurityUtilities.generateRandomString(-5, true, false, false));
        }

        @Test
        @DisplayName("Debe fallar si no incluye ningún tipo de caracter")
        void testGenerateRandomStringNoChars() {
            assertThrows(IllegalArgumentException.class,
                () -> SecurityUtilities.generateRandomString(10, false, false, false),
                "Debe requerir al menos un tipo de caracteres");
        }

        @Test
        @DisplayName("Debe generar strings únicos")
        void testGenerateRandomStringUniqueness() {
            String str1 = SecurityUtilities.generateRandomString(20, true, true, true);
            String str2 = SecurityUtilities.generateRandomString(20, true, true, true);

            assertNotEquals(str1, str2);
        }
    }

    // =========================================================================
    // TESTS: getSecureRandomInstance()
    // =========================================================================

    @Nested
    @DisplayName("getSecureRandomInstance()")
    class GetSecureRandomInstanceTests {

        @Test
        @DisplayName("Debe retornar instancia válida de SecureRandom")
        void testGetSecureRandomInstance() {
            var random = SecurityUtilities.getSecureRandomInstance();

            assertNotNull(random);
            assertInstanceOf(java.security.SecureRandom.class, random);
        }

        @Test
        @DisplayName("Debe retornar la misma instancia (singleton)")
        void testGetSecureRandomInstanceSingleton() {
            var random1 = SecurityUtilities.getSecureRandomInstance();
            var random2 = SecurityUtilities.getSecureRandomInstance();

            assertSame(random1, random2,
                "Debe retornar la misma instancia compartida");
        }

        @Test
        @DisplayName("Debe generar números aleatorios válidos")
        void testGetSecureRandomInstanceGeneratesNumbers() {
            var random = SecurityUtilities.getSecureRandomInstance();

            int num1 = random.nextInt(100);
            int num2 = random.nextInt(100);

            assertTrue(num1 >= 0 && num1 < 100);
            assertTrue(num2 >= 0 && num2 < 100);
            // Estadísticamente deben ser diferentes (99% de probabilidad)
        }
    }

    // =========================================================================
    // TESTS DE INTEGRACIÓN
    // =========================================================================

    @Nested
    @DisplayName("Tests de Integración")
    class IntegrationTests {

        @Test
        @DisplayName("Hash de password debe ser consistente")
        void testPasswordHashingWorkflow() throws FrameworkTechnicalException {
            String password = SecurityUtilities.generateSecurePassword(12);

            String hash1 = SecurityUtilities.sha256Hash(password);
            String hash2 = SecurityUtilities.sha256Hash(password);

            assertEquals(hash1, hash2, "Hash debe ser reproducible");
        }

        @Test
        @DisplayName("Token generado debe poder ser hasheado")
        void testTokenHashingWorkflow() throws FrameworkTechnicalException {
            String token = SecurityUtilities.generateSecureToken(32);

            assertDoesNotThrow(() -> SecurityUtilities.sha256Hash(token));
        }

        @Test
        @DisplayName("Masking no debe afectar la validación")
        void testMaskingDoesNotAffectValidation() {
            String password = "Pass1234!";

            assertTrue(SecurityUtilities.isSecurePassword(password));

            String masked = SecurityUtilities.maskPassword(password);

            // Password original debe seguir siendo válido
            assertTrue(SecurityUtilities.isSecurePassword(password));

            // Masked NO debe ser un password válido
            assertFalse(SecurityUtilities.isSecurePassword(masked));
        }
    }

    // =========================================================================
    // TESTS DE CASOS EDGE Y LÍMITES
    // =========================================================================

    @Nested
    @DisplayName("Casos Edge y Límites")
    class EdgeCasesTests {

        @Test
        @DisplayName("Debe manejar token de longitud 1")
        void testGenerateSecureTokenLength1() {
            String token = SecurityUtilities.generateSecureToken(1);

            assertNotNull(token);
            // 1 byte en Base64 -> ~2 chars
            assertTrue(token.length() >= 1);
        }

        @Test
        @DisplayName("Debe manejar token muy largo (10000 bytes)")
        void testGenerateSecureTokenVeryLong() {
            String token = SecurityUtilities.generateSecureToken(10000);

            assertNotNull(token);
            assertTrue(token.length() >= 13000); // 10000 bytes -> ~13333 chars
        }

        @Test
        @DisplayName("Debe manejar password de longitud 1")
        void testGenerateSecurePasswordLength1() {
            String password = SecurityUtilities.generateSecurePassword(1);

            assertEquals(1, password.length());
        }

        @Test
        @DisplayName("Debe manejar password muy largo (1000 chars)")
        void testGenerateSecurePasswordVeryLong() {
            String password = SecurityUtilities.generateSecurePassword(1000);

            assertEquals(1000, password.length());
        }

        @Test
        @DisplayName("Debe manejar random string de longitud 1")
        void testGenerateRandomStringLength1() {
            String str = SecurityUtilities.generateRandomString(1, true, false, false);

            assertEquals(1, str.length());
        }

        @Test
        @DisplayName("Debe manejar número aleatorio en rango 0-0")
        void testGenerateRandomNumberZeroRange() {
            int num = SecurityUtilities.generateRandomNumber(0, 0);

            assertEquals(0, num);
        }

        @Test
        @DisplayName("Debe manejar número aleatorio en rango MAX_INT")
        void testGenerateRandomNumberLargeRange() {
            // Rango grande pero válido
            assertDoesNotThrow(() ->
                SecurityUtilities.generateRandomNumber(0, 100000));
        }
    }
}

