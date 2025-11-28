package com.scotia.qa.common.utils.testdata.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Modelo de datos para representar un usuario de prueba obtenido de la base de datos.
 *
 * <p>Este DTO contiene la información básica de un usuario de test que puede ser
 * utilizado en escenarios de automatización.</p>
 *
 * <p><b>Uso:</b></p>
 * <pre>
 * TestUser user = userFinder.findUserWith("cuenta-activa");
 * String firstName = user.getFirstName();
 * String lastName = user.getLastName();
 * String fullName = user.getFullName(); // firstName + lastName
 * String password = user.getPassword();
 * </pre>
 *
 * @author Abel Venero
 * @version 1.0.2
 * @since 2025-11-26
 */
public class TestUser {

    private final String userId;
    private final String firstName;
    private final String lastName;
    private final String password;
    private final String email;
    private final String phone;
    private final String idUserStatus;
    private final String idDefaultEnvironment;
    private final String lastLogin;
    private final String requestedSoftToken;
    private final Map<String, Object> additionalData;

    /**
     * Constructor completo con todos los campos.
     *
     * @param userId ID único del usuario
     * @param firstName Primer nombre del usuario
     * @param lastName Apellido del usuario
     * @param password Contraseña
     * @param email Email del usuario
     * @param phone Teléfono del usuario
     * @param idUserStatus ID del estado del usuario
     * @param idDefaultEnvironment ID del ambiente por defecto
     * @param lastLogin Último login
     * @param requestedSoftToken Token solicitado
     */
    public TestUser(String userId, String firstName, String lastName, String password,
                   String email, String phone, String idUserStatus, String idDefaultEnvironment,
                   String lastLogin, String requestedSoftToken) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.idUserStatus = idUserStatus;
        this.idDefaultEnvironment = idDefaultEnvironment;
        this.lastLogin = lastLogin;
        this.requestedSoftToken = requestedSoftToken;
        this.additionalData = new HashMap<>();
    }

    /**
     * Constructor mínimo con campos básicos.
     *
     * @param userId ID del usuario
     * @param firstName Primer nombre
     * @param lastName Apellido
     * @param password Password
     */
    public TestUser(String userId, String firstName, String lastName, String password) {
        this(userId, firstName, lastName, password, null, null, null, null, null, null);
    }

    /**
     * Constructor para compatibilidad (sin los campos nuevos).
     *
     * @param userId ID del usuario
     * @param firstName Primer nombre
     * @param lastName Apellido
     * @param password Password
     * @param email Email
     * @param phone Teléfono
     */
    public TestUser(String userId, String firstName, String lastName, String password,
                   String email, String phone) {
        this(userId, firstName, lastName, password, email, phone, null, null, null, null);
    }

    // Getters

    public String getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    /**
     * Retorna el nombre completo (firstName + lastName).
     *
     * @return Nombre completo del usuario
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return firstName != null ? firstName : lastName;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getIdUserStatus() {
        return idUserStatus;
    }

    public String getIdDefaultEnvironment() {
        return idDefaultEnvironment;
    }

    public String getLastLogin() {
        return lastLogin;
    }

    public String getRequestedSoftToken() {
        return requestedSoftToken;
    }

    /**
     * Obtiene datos adicionales guardados dinámicamente.
     *
     * @param key Clave del dato
     * @return Valor del dato, o null si no existe
     */
    public Object getAdditionalData(String key) {
        return additionalData.get(key);
    }

    /**
     * Guarda dato adicional.
     *
     * @param key Clave
     * @param value Valor
     */
    public void setAdditionalData(String key, Object value) {
        this.additionalData.put(key, value);
    }

    /**
     * Obtiene todos los datos adicionales.
     *
     * @return Map con todos los datos adicionales
     */
    public Map<String, Object> getAllAdditionalData() {
        return new HashMap<>(additionalData);
    }

    @Override
    public String toString() {
        return "TestUser{" +
                "userId='" + userId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", idUserStatus='" + idUserStatus + '\'' +
                ", idDefaultEnvironment='" + idDefaultEnvironment + '\'' +
                ", lastLogin='" + lastLogin + '\'' +
                ", requestedSoftToken='" + requestedSoftToken + '\'' +
                ", additionalData=" + additionalData.keySet() +
                '}';
    }
}

