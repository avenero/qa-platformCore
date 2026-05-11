package com.qa.common.utils.data;
import com.qa.common.utils.security.SecurityUtilities;

import com.qa.common.api.logging.TestLogger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generador de datos de prueba para el framework de QA.
 *
 * <p>Consolida toda la generación aleatoria de datos que antes estaba en {@link DataUtilities}:</p>
 * <ul>
 *   <li>Datos personales: RUT, email, teléfono, nombre</li>
 *   <li>Valores aleatorios: strings, números, booleanos, montos</li>
 *   <li>Fechas y timestamps: generación, parseo, aritmética, comparación</li>
 *   <li>Códigos y textos Lorem Ipsum</li>
 * </ul>
 *
 * <p><b>Seguridad:</b> usa {@link ThreadLocalRandom} para operaciones no criptográficas
 * y delega a {@link SecurityUtilities} para strings aleatorios seguros.</p>
 *
 * @author Abel Venero
 * @since 2.1.0
 */
public final class DataGenerator {

    /** Upper bound for Chilean RUT number generation (exclusive). */
    private static final int RUT_UPPER_BOUND = 20_000_000;

    /** Lower offset added to the random RUT number. */
    private static final int RUT_LOWER_OFFSET = 5_000_000;

    /** Length of randomly generated email usernames and secure strings. */
    private static final int RANDOM_STRING_LENGTH = 8;

    /** Maximum valid age for birth-date generation. */
    private static final int MAX_VALID_AGE = 150;

    /** RUT check-digit multiplier boundary that triggers a reset to 2. */
    private static final int RUT_MULTIPLIER_MAX = 7;

    /** Modulus used in RUT check-digit calculation. */
    private static final int RUT_CHECK_MODULUS = 11;

    private DataGenerator() {
        throw new UnsupportedOperationException("DataGenerator es una clase de utilidad");
    }

    // =========================================================================
    // DATOS PERSONALES Y DE IDENTIDAD
    // =========================================================================

    /**
     * Genera un RUT chileno aleatorio con formato {@code XX.XXX.XXX-Y}.
     *
     * @return RUT chileno válido con dígito verificador correcto
     */
    public static String generateRandomRut() {
        int number = SecurityUtilities.getSecureRandomInstance().nextInt(RUT_UPPER_BOUND) + RUT_LOWER_OFFSET;
        int dv = calculateRutCheckDigit(number);
        String dvStr = (dv == 10) ? "K" : String.valueOf(dv);
        return formatRut(number + "-" + dvStr);
    }

    /**
     * Genera un email aleatorio usando dominios de prueba seguros (RFC 2606).
     *
     * <p>Dominios usados: {@code example.com}, {@code test.com}, {@code testmail.org} —
     * reservados para testing y nunca usados en producción real.
     *
     * @return dirección de email aleatoria válida
     */
    public static String generateRandomEmail() {
        // RFC 2606: dominios reservados para testing — NUNCA usar dominios reales de clientes
        String[] domains = {"example.com", "test.com", "testmail.org", "qa-test.net"};
        String username = SecurityUtilities.generateRandomString(RANDOM_STRING_LENGTH, true, true, false).toLowerCase();
        String domain = domains[SecurityUtilities.getSecureRandomInstance().nextInt(domains.length)];
        return username + "@" + domain;
    }

    /**
     * Genera un número de teléfono celular chileno aleatorio.
     *
     * @return teléfono con formato {@code +569XXXXXXXX}
     */
    public static String generateRandomPhone() {
        StringBuilder phone = new StringBuilder("+569");
        for (int i = 0; i < RANDOM_STRING_LENGTH; i++) {
            phone.append(SecurityUtilities.getSecureRandomInstance().nextInt(10));
        }
        return phone.toString();
    }

    /**
     * Genera un nombre completo aleatorio.
     *
     * @return nombre y apellido aleatorios
     */
    public static String generateRandomName() {
        String[] firstNames = {"Juan", "María", "Pedro", "Ana", "Carlos", "Sofía", "Diego", "Valentina"};
        String[] lastNames  = {"González", "Rodríguez", "Pérez", "López", "Martínez", "García", "Hernández", "Muñoz"};
        int size = SecurityUtilities.getSecureRandomInstance().nextInt(firstNames.length);
        int lastSize = SecurityUtilities.getSecureRandomInstance().nextInt(lastNames.length);
        return firstNames[size] + " " + lastNames[lastSize];
    }

    // =========================================================================
    // STRINGS Y VALORES ALEATORIOS
    // =========================================================================

    /**
     * Genera un string alfanumérico aleatorio.
     *
     * @param length longitud del string
     * @return string alfanumérico aleatorio
     */
    public static String generateRandomAlphanumeric(int length) {
        return SecurityUtilities.generateRandomString(length, true, true, false);
    }

    /**
     * Genera un string numérico aleatorio.
     *
     * @param length longitud del string
     * @return string compuesto solo de dígitos
     */
    public static String generateRandomNumeric(int length) {
        return SecurityUtilities.generateRandomString(length, false, true, false);
    }

    /**
     * Genera un UUID v4 aleatorio.
     *
     * @return UUID como String
     */
    public static String generateRandomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Genera un número entero aleatorio en el rango [min, max].
     *
     * @param min valor mínimo (inclusivo)
     * @param max valor máximo (inclusivo)
     * @return número aleatorio en el rango
     */
    public static int generateRandomNumber(int min, int max) {
        return SecurityUtilities.generateRandomNumber(min, max);
    }

    /**
     * Genera un número entero aleatorio en el rango [min, max].
     * Alias de {@link #generateRandomNumber(int, int)}.
     *
     * @param min valor mínimo (inclusivo)
     * @param max valor máximo (inclusivo)
     * @return número aleatorio en el rango
     * @throws IllegalArgumentException si min &gt; max
     */
    public static int generateNumberInRange(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min no puede ser mayor que max");
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * Genera un boolean aleatorio.
     *
     * @return {@code true} o {@code false} aleatoriamente
     */
    public static boolean generateRandomBoolean() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    /**
     * Genera un monto monetario aleatorio con decimales.
     *
     * @param min      monto mínimo
     * @param max      monto máximo
     * @param decimals número de decimales
     * @return monto aleatorio redondeado a los decimales especificados
     */
    public static double generateRandomAmount(double min, double max, int decimals) {
        if (min > max) {
            throw new IllegalArgumentException("min no puede ser mayor que max");
        }
        double amount = min + (max - min) * ThreadLocalRandom.current().nextDouble();
        double multiplier = Math.pow(10, decimals);
        return Math.round(amount * multiplier) / multiplier;
    }

    /**
     * Genera una edad aleatoria dentro de un rango.
     *
     * @param min edad mínima
     * @param max edad máxima
     * @return edad aleatoria
     */
    public static int generateAgeInRange(int min, int max) {
        return generateNumberInRange(min, max);
    }

    // =========================================================================
    // TEXTOS Y CÓDIGOS
    // =========================================================================

    /**
     * Genera texto Lorem Ipsum con la longitud especificada.
     *
     * @param length longitud deseada
     * @return texto Lorem Ipsum
     */
    public static String generateLoremIpsum(int length) {
        String lorem = "Lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor "
                     + "incididunt ut labore et dolore magna aliqua ";
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length) {
            sb.append(lorem);
        }
        return sb.substring(0, length);
    }

    /**
     * Genera un código con prefijo y sufijo numérico aleatorio.
     *
     * @param prefix       prefijo del código (ej: {@code "TXN-"})
     * @param suffixLength longitud del sufijo numérico
     * @return código generado (ej: {@code "TXN-0492"})
     */
    public static String generateCode(String prefix, int suffixLength) {
        StringBuilder code = new StringBuilder(prefix != null ? prefix : "");
        for (int i = 0; i < suffixLength; i++) {
            code.append(ThreadLocalRandom.current().nextInt(10));
        }
        return code.toString();
    }

    // =========================================================================
    // FECHAS Y TIMESTAMPS
    // =========================================================================

    /**
     * Obtiene el timestamp actual en formato ISO 8601 ({@code yyyy-MM-dd'T'HH:mm:ss'Z'}).
     *
     * @return timestamp actual como String ISO 8601
     */
    public static String getCurrentTimestamp() {
        return Instant.now().toString();
    }

    /**
     * Obtiene la fecha/hora actual en el formato especificado.
     *
     * @param format formato de fecha (ej: {@code "yyyy-MM-dd"}, {@code "dd/MM/yyyy HH:mm:ss"})
     * @return fecha actual formateada
     */
    public static String getCurrentTimestamp(String format) {
        if (format == null || format.trim().isEmpty()) {
            return getCurrentTimestamp();
        }
        try {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern(format));
        } catch (Exception e) {
            TestLogger.logError("DATA_GENERATOR", "Error formateando fecha: " + e.getMessage(), null);
            return getCurrentTimestamp();
        }
    }

    /**
     * Parsea una fecha desde un string con formato específico.
     *
     * @param dateString string con la fecha
     * @param format     formato del string (ej: {@code "yyyy-MM-dd"})
     * @return {@link LocalDate} parseado
     * @throws IllegalArgumentException si el formato o la fecha son inválidos
     */
    public static LocalDate parseDate(String dateString, String format) {
        if (dateString == null || format == null) {
            throw new IllegalArgumentException("dateString y format no pueden ser null");
        }
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(format));
        } catch (Exception e) {
            throw new IllegalArgumentException(
                String.format("Error parseando fecha '%s' con formato '%s': %s",
                    dateString, format, e.getMessage()), e);
        }
    }

    /**
     * Formatea una fecha a un string con el formato especificado.
     *
     * @param date   fecha a formatear
     * @param format formato deseado
     * @return fecha formateada como String
     */
    public static String formatDate(LocalDate date, String format) {
        if (date == null || format == null) {
            throw new IllegalArgumentException("date y format no pueden ser null");
        }
        try {
            return date.format(DateTimeFormatter.ofPattern(format));
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Error formateando fecha con formato '" + format + "': " + e.getMessage(), e);
        }
    }

    /**
     * Agrega días a una fecha.
     *
     * @param date fecha base
     * @param days días a agregar (negativo para restar)
     * @return nueva fecha
     */
    public static LocalDate addDaysToDate(LocalDate date, int days) {
        if (date == null) {
            throw new IllegalArgumentException("date no puede ser null");
        }
        return date.plusDays(days);
    }

    /**
     * Agrega meses a una fecha.
     *
     * @param date   fecha base
     * @param months meses a agregar (negativo para restar)
     * @return nueva fecha
     */
    public static LocalDate addMonthsToDate(LocalDate date, int months) {
        if (date == null) {
            throw new IllegalArgumentException("date no puede ser null");
        }
        return date.plusMonths(months);
    }

    /**
     * Agrega años a una fecha.
     *
     * @param date  fecha base
     * @param years años a agregar (negativo para restar)
     * @return nueva fecha
     */
    public static LocalDate addYearsToDate(LocalDate date, int years) {
        if (date == null) {
            throw new IllegalArgumentException("date no puede ser null");
        }
        return date.plusYears(years);
    }

    /**
     * Calcula la diferencia en días entre dos fechas.
     *
     * @param date1 fecha inicial
     * @param date2 fecha final
     * @return número de días (puede ser negativo)
     */
    public static long getDaysBetween(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("Las fechas no pueden ser null");
        }
        return ChronoUnit.DAYS.between(date1, date2);
    }

    /**
     * Calcula la diferencia en meses entre dos fechas.
     *
     * @param date1 fecha inicial
     * @param date2 fecha final
     * @return número de meses (puede ser negativo)
     */
    public static long getMonthsBetween(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("Las fechas no pueden ser null");
        }
        return ChronoUnit.MONTHS.between(date1, date2);
    }

    /**
     * Genera una fecha de nacimiento para una edad exacta.
     *
     * @param age edad en años (0–150)
     * @return fecha de nacimiento
     */
    public static LocalDate generateBirthDateForAge(int age) {
        if (age < 0 || age > MAX_VALID_AGE) {
            throw new IllegalArgumentException("Edad debe estar entre 0 y " + MAX_VALID_AGE);
        }
        return LocalDate.now().minusYears(age);
    }

    /**
     * Genera una fecha de nacimiento aleatoria para un rango de edad.
     *
     * @param minAge edad mínima
     * @param maxAge edad máxima
     * @return fecha de nacimiento aleatoria en el rango
     */
    public static LocalDate generateBirthDateForAgeRange(int minAge, int maxAge) {
        if (minAge < 0 || maxAge < minAge || maxAge > MAX_VALID_AGE) {
            throw new IllegalArgumentException(
                "Rango inválido: minAge >= 0, maxAge >= minAge, maxAge <= " + MAX_VALID_AGE);
        }
        return generateBirthDateForAge(generateRandomNumber(minAge, maxAge));
    }

    /**
     * Genera una fecha aleatoria en los últimos N días.
     *
     * @param days número de días hacia atrás (&gt;= 0)
     * @return fecha aleatoria en el rango
     */
    public static LocalDate generateDateInLastDays(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("days debe ser positivo");
        }
        return LocalDate.now().minusDays(generateRandomNumber(0, days));
    }

    /**
     * Genera una fecha aleatoria en los próximos N días.
     *
     * @param days número de días hacia adelante (&gt;= 0)
     * @return fecha aleatoria en el rango
     */
    public static LocalDate generateDateInNextDays(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("days debe ser positivo");
        }
        return LocalDate.now().plusDays(generateRandomNumber(0, days));
    }

    /**
     * Verifica si una fecha está en el pasado.
     *
     * @param date fecha a verificar
     * @return {@code true} si está en el pasado
     */
    public static boolean isDateInPast(LocalDate date) {
        return date != null && date.isBefore(LocalDate.now());
    }

    /**
     * Verifica si una fecha está en el futuro.
     *
     * @param date fecha a verificar
     * @return {@code true} si está en el futuro
     */
    public static boolean isDateInFuture(LocalDate date) {
        return date != null && date.isAfter(LocalDate.now());
    }

    // =========================================================================
    // MÉTODOS PRIVADOS
    // =========================================================================

    /**
     * Calcula el dígito verificador de un RUT chileno.
     */
    private static int calculateRutCheckDigit(int rut) {
        int sum = 0;
        int multiplier = 2;
        while (rut > 0) {
            sum += (rut % 10) * multiplier;
            rut /= 10;
            multiplier = (multiplier == RUT_MULTIPLIER_MAX) ? 2 : multiplier + 1;
        }
        int remainder = sum % RUT_CHECK_MODULUS;
        return (remainder == 0) ? 0 : (remainder == 1) ? 10 : RUT_CHECK_MODULUS - remainder;
    }

    /**
     * Formatea el RUT con puntos y guión ({@code XX.XXX.XXX-Y}).
     */
    private static String formatRut(String rut) {
        String[] parts = rut.split("-");
        StringBuilder formatted = new StringBuilder(parts[0]);
        for (int i = formatted.length() - 3; i > 0; i -= 3) {
            formatted.insert(i, ".");
        }
        return formatted + "-" + parts[1];
    }
}

