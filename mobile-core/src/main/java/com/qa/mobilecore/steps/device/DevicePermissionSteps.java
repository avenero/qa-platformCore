package com.qa.mobilecore.steps.device;

import com.qa.common.api.logging.TestLogger;
import com.qa.common.internal.runtime.ExecutionContext;
import com.qa.mobilecore.helper.MobileHelper;
import io.cucumber.java.en.Given;

/**
 * Steps de gestión de permisos del sistema operativo mobile.
 *
 * <p>Fase BDD: GIVEN. Concede o deniega permisos antes de la interacción con la app.
 * Para Android usa {@code pm grant/revoke} via ADB.
 * Para iOS los permisos se gestionan via simctl o configuración de la sesión.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class DevicePermissionSteps {

    // =========================================================================
    // Permisos genéricos
    // =========================================================================

    @Given("concedo permiso {string} a la aplicacion")
    public void concedoPermiso(String permission) {
        String resolved = ctx().variables().resolve(permission);
        mobile().grantPermission(resolved);
        TestLogger.logInfo("PERMISSION", "Permiso concedido: " + resolved, null);
    }

    @Given("deniego permiso {string} a la aplicacion")
    public void denigoPermiso(String permission) {
        String resolved = ctx().variables().resolve(permission);
        mobile().denyPermission(resolved);
        TestLogger.logInfo("PERMISSION", "Permiso denegado: " + resolved, null);
    }

    // =========================================================================
    // Diálogos del sistema
    // =========================================================================

    @Given("acepto el dialogo de permiso del sistema")
    public void aceptoDialogoPermiso() {
        mobile().acceptSystemDialog();
        TestLogger.logInfo("PERMISSION", "Dialogo de permiso aceptado", null);
    }

    @Given("rechazo el dialogo de permiso del sistema")
    public void rechazoDialogoPermiso() {
        mobile().dismissSystemDialog();
        TestLogger.logInfo("PERMISSION", "Dialogo de permiso rechazado", null);
    }

    // =========================================================================
    // Permisos específicos (aliases semánticos para mejor legibilidad BDD)
    // =========================================================================

    @Given("concedo el permiso de ubicacion")
    public void concedoPermisoUbicacion() {
        concedoPermiso("android.permission.ACCESS_FINE_LOCATION");
    }

    @Given("deniego el permiso de ubicacion")
    public void denigoPermisoUbicacion() {
        denigoPermiso("android.permission.ACCESS_FINE_LOCATION");
    }

    @Given("concedo el permiso de camara")
    public void concedoPermisoCamara() {
        concedoPermiso("android.permission.CAMERA");
    }

    @Given("deniego el permiso de camara")
    public void denigoPermisoCamara() {
        denigoPermiso("android.permission.CAMERA");
    }

    @Given("concedo el permiso de notificaciones")
    public void concedoPermisoNotificaciones() {
        concedoPermiso("android.permission.POST_NOTIFICATIONS");
    }

    @Given("deniego el permiso de notificaciones")
    public void denigoPermisoNotificaciones() {
        denigoPermiso("android.permission.POST_NOTIFICATIONS");
    }

    @Given("concedo el permiso de microfono")
    public void concedoPermisoMicrofono() {
        concedoPermiso("android.permission.RECORD_AUDIO");
    }

    @Given("concedo el permiso de almacenamiento")
    public void concedoPermisoAlmacenamiento() {
        concedoPermiso("android.permission.READ_EXTERNAL_STORAGE");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ExecutionContext ctx() {
        return ExecutionContext.requireCurrent();
    }

    private MobileHelper mobile() {
        return ctx().service(MobileHelper.class);
    }
}
