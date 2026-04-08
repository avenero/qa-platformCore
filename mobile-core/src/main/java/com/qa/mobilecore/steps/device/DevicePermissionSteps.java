package com.qa.mobilecore.steps.device;

import com.qa.common.logging.TestLogger;
import io.cucumber.java.en.Given;

/**
 * Steps de gestion de permisos del sistema operativo movil.
 * Nuevo en Fase 2.
 * @author Abel Venero
 * @since 2.0.0
 */
public class DevicePermissionSteps {

    @Given("concedo permiso {string} a la aplicacion")
    public void concedoPermiso(String permission) {
        TestLogger.logInfo("PERMISSION", "Concediendo permiso: " + permission, null);
    }

    @Given("deniego permiso {string} a la aplicacion")
    public void deniogoPermiso(String permission) {
        TestLogger.logInfo("PERMISSION", "Denegando permiso: " + permission, null);
    }

    @Given("acepto el dialogo de permiso del sistema")
    public void aceptoDialogoPermiso() {
        TestLogger.logInfo("PERMISSION", "Aceptando dialogo de permiso", null);
    }

    @Given("rechazo el dialogo de permiso del sistema")
    public void rechazoDialogoPermiso() {
        TestLogger.logInfo("PERMISSION", "Rechazando dialogo de permiso", null);
    }
}
