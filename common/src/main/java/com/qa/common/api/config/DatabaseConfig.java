package com.qa.common.api.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Configuración Database/JDBC del Core (TASK-K03M-F6.c).
 *
 * <p>Prefijo: {@code db} (override explícito — el default derivaría
 * {@code "Database"} → {@code "database"}, pero las keys legacy son {@code db.*}).
 *
 * <p>Para conexiones multi-DB con keys dinámicas tipo {@code oracle.db.url},
 * usar {@code ConfigLoader.getRaw("oracle.db.url")} desde {@code DbConnectorFactory}.
 * Este record cubre la conexión single/default ({@code db.*}).
 *
 * @param url           URL JDBC (ej. {@code jdbc:postgresql://host:5432/db}).
 * @param username      Usuario de conexión. Vacío permitido (Windows integrated auth).
 * @param password      Password. Vacío permitido. {@code @NotNull} no {@code @NotBlank}.
 * @param driver        Nombre fully-qualified del driver JDBC. Vacío = autodetectar por URL.
 * @param type          Tipo lógico: {@code oracle | sqlserver | postgresql | mysql | h2 | ""}.
 * @param poolSizeMax   Tamaño máximo del pool HikariCP (1..100).
 *
 * @since TASK-K03M-F6
 */
public record DatabaseConfig(
        @NotNull String url,
        @NotNull String username,
        @NotNull String password,
        @NotNull String driver,
        @NotNull String type,
        @Min(1) @Max(100) int poolSizeMax
) implements TypedConfig {

    public static DatabaseConfig defaults() {
        return new DatabaseConfig("", "", "", "", "", 10);
    }

    @Override
    public String configPrefix() {
        return "db";
    }
}
