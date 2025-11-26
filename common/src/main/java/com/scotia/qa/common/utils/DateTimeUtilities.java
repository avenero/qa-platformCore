package com.scotia.qa.common.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utilidades especializadas para manejo de fechas y tiempos sin dependencias de Spring.
 * Clase estática que proporciona funcionalidades comunes para todos los frameworks.
 *
 * @author Abel Venero
 * @since 1.0.0
 */
public class DateTimeUtilities {

    private DateTimeUtilities() {
        // Utility class - no instances
    }

    // Formateadores comunes
    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public static final DateTimeFormatter READABLE_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter READABLE_DATETIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    public static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    public static final DateTimeFormatter CHILE_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final DateTimeFormatter CHILE_DATETIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    // Zona horaria de Chile
    public static final ZoneId CHILE_ZONE = ZoneId.of("America/Santiago");

    // =================================================================================
    // OBTENCIÓN DE FECHAS Y TIEMPOS
    // =================================================================================

    /**
     * Obtiene la fecha y hora actual en zona horaria de Chile.
     */
    public static LocalDateTime getCurrentChileDateTime() {
        return LocalDateTime.now(CHILE_ZONE);
    }

    /**
     * Obtiene la fecha actual en zona horaria de Chile.
     */
    public static LocalDate getCurrentChileDate() {
        return LocalDate.now(CHILE_ZONE);
    }

    /**
     * Obtiene un timestamp único para identificación.
     */
    public static String getUniqueTimestamp() {
        return getCurrentChileDateTime().format(TIMESTAMP);
    }

    /**
     * Obtiene la fecha actual en formato ISO.
     */
    public static String getCurrentDateISO() {
        return getCurrentChileDate().format(ISO_DATE);
    }

    /**
     * Obtiene la fecha y hora actual en formato ISO.
     */
    public static String getCurrentDateTimeISO() {
        return getCurrentChileDateTime().format(ISO_DATETIME);
    }

    // =================================================================================
    // FORMATEO DE FECHAS
    // =================================================================================

    /**
     * Formatea una fecha con el patrón especificado.
     */
    public String formatDate(LocalDate date, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return date.format(formatter);
    }

    /**
     * Formatea una fecha y hora con el patrón especificado.
     */
    public String formatDateTime(LocalDateTime dateTime, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return dateTime.format(formatter);
    }

    /**
     * Formatea una fecha en formato legible chileno.
     */
    public String formatDateChile(LocalDate date) {
        return date.format(CHILE_DATE);
    }

    /**
     * Formatea una fecha y hora en formato legible chileno.
     */
    public String formatDateTimeChile(LocalDateTime dateTime) {
        return dateTime.format(CHILE_DATETIME);
    }

    // =================================================================================
    // PARSING DE FECHAS
    // =================================================================================

    /**
     * Parsea una fecha desde string con formato específico.
     */
    public static LocalDate parseDate(String dateString, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDate.parse(dateString, formatter);
    }

    /**
     * Parsea una fecha y hora desde string con formato específico.
     */
    public static LocalDateTime parseDateTime(String dateTimeString, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDateTime.parse(dateTimeString, formatter);
    }

    /**
     * Parsea una fecha en formato chileno.
     */
    public static LocalDate parseDateChile(String dateString) {
        return LocalDate.parse(dateString, CHILE_DATE);
    }

    /**
     * Parsea una fecha ISO.
     */
    public static LocalDate parseDateISO(String dateString) {
        return LocalDate.parse(dateString, ISO_DATE);
    }

    // =================================================================================
    // CÁLCULOS DE FECHAS
    // =================================================================================

    /**
     * Agrega días a una fecha.
     */
    public static LocalDate addDays(LocalDate date, int days) {
        return date.plusDays(days);
    }

    /**
     * Agrega meses a una fecha.
     */
    public static LocalDate addMonths(LocalDate date, int months) {
        return date.plusMonths(months);
    }

    /**
     * Agrega años a una fecha.
     */
    public static LocalDate addYears(LocalDate date, int years) {
        return date.plusYears(years);
    }

    /**
     * Calcula la diferencia en días entre dos fechas.
     */
    public static long daysBetween(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Calcula la diferencia en meses entre dos fechas.
     */
    public static long monthsBetween(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.MONTHS.between(startDate, endDate);
    }

    /**
     * Obtiene el primer día del mes.
     */
    public static LocalDate getFirstDayOfMonth(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    /**
     * Obtiene el último día del mes.
     */
    public static LocalDate getLastDayOfMonth(LocalDate date) {
        return date.withDayOfMonth(date.lengthOfMonth());
    }

    // =================================================================================
    // GENERACIÓN DE FECHAS ALEATORIAS
    // =================================================================================

    /**
     * Genera una fecha aleatoria entre dos fechas.
     */
    public static LocalDate generateRandomDateBetween(LocalDate startDate, LocalDate endDate) {
        long startEpochDay = startDate.toEpochDay();
        long endEpochDay = endDate.toEpochDay();
        long randomDay = ThreadLocalRandom.current().nextLong(startEpochDay, endEpochDay + 1);
        return LocalDate.ofEpochDay(randomDay);
    }

    /**
     * Genera una fecha aleatoria en el pasado (hasta 5 años atrás).
     */
    public static LocalDate generateRandomPastDate() {
        LocalDate today = getCurrentChileDate();
        LocalDate fiveYearsAgo = today.minusYears(5);
        return generateRandomDateBetween(fiveYearsAgo, today);
    }

    /**
     * Genera una fecha aleatoria en el futuro (hasta 2 años adelante).
     */
    public static LocalDate generateRandomFutureDate() {
        LocalDate today = getCurrentChileDate();
        LocalDate twoYearsLater = today.plusYears(2);
        return generateRandomDateBetween(today, twoYearsLater);
    }

    /**
     * Genera una fecha de nacimiento aleatoria para adultos (18-80 años).
     */
    public static LocalDate generateRandomAdultBirthDate() {
        LocalDate today = getCurrentChileDate();
        LocalDate minDate = today.minusYears(80);
        LocalDate maxDate = today.minusYears(18);
        return generateRandomDateBetween(minDate, maxDate);
    }

    // =================================================================================
    // VALIDACIONES DE FECHAS
    // =================================================================================

    /**
     * Verifica si una fecha está en el pasado.
     */
    public boolean isInPast(LocalDate date) {
        return date.isBefore(getCurrentChileDate());
    }

    /**
     * Verifica si una fecha está en el futuro.
     */
    public boolean isInFuture(LocalDate date) {
        return date.isAfter(getCurrentChileDate());
    }

    /**
     * Verifica si una fecha es hoy.
     */
    public boolean isToday(LocalDate date) {
        return date.equals(getCurrentChileDate());
    }

    /**
     * Verifica si una fecha está en un rango.
     */
    public boolean isDateInRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * Verifica si una persona es mayor de edad.
     */
    public boolean isAdult(LocalDate birthDate) {
        return daysBetween(birthDate, getCurrentChileDate()) >= (18 * 365);
    }

    // =================================================================================
    // UTILIDADES PARA TESTING
    // =================================================================================

    /**
     * Obtiene una fecha válida para testing (evita fines de semana y feriados básicos).
     */
    public static LocalDate getValidTestDate() {
        LocalDate date = getCurrentChileDate().plusDays(1);

        // Evitar fines de semana
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY ||
               date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }

        return date;
    }

    /**
     * Genera un rango de fechas para testing.
     */
    public static LocalDate[] generateDateRange(int daysFromNow, int rangeDays) {
        LocalDate startDate = getCurrentChileDate().plusDays(daysFromNow);
        LocalDate endDate = startDate.plusDays(rangeDays);
        return new LocalDate[]{startDate, endDate};
    }

    /**
     * Obtiene el timestamp actual en milisegundos.
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * Calcula la duración transcurrida desde un timestamp.
     */
    public Duration getDurationSince(long startTimestamp) {
        long currentTimestamp = getCurrentTimestamp();
        return Duration.ofMillis(currentTimestamp - startTimestamp);
    }
}
