# User Story: Gestión Completa de Usuarios

**Como** administrador del sistema
**Quiero** gestionar el ciclo de vida completo de los usuarios
**Para** mantener la seguridad y el control de acceso de la plataforma

## Criterios de Aceptación

### Registro

**Scenario:** Registro exitoso con datos válidos
Given el formulario de registro está vacío
When ingreso email "nuevo@ejemplo.com", contraseña "Seguro123!" y nombre "Juan García"
Then el usuario es creado con estado "PENDIENTE_VERIFICACION"
And se envía un email de verificación a "nuevo@ejemplo.com"

**Scenario:** Registro con email duplicado
Given existe un usuario con email "existente@ejemplo.com"
When intento registrar otro usuario con el mismo email
Then recibo error 409 "El email ya está registrado"
And no se crea ningún usuario adicional

**Scenario:** Registro con contraseña débil
Given el formulario de registro está en pantalla
When ingreso contraseña "1234"
Then veo mensaje "La contraseña debe tener al menos 8 caracteres, una mayúscula y un número"
And el botón de registro permanece deshabilitado

**Scenario:** Registro con email con formato inválido
When ingreso email "no-es-un-email"
Then veo error de validación "Formato de email inválido"

**Scenario:** Registro con nombre que contiene solo espacios
When ingreso nombre "     "
Then veo error "El nombre no puede estar vacío"

### Autenticación

**Scenario:** Login exitoso
Given existe usuario activo con email "usuario@ejemplo.com" y contraseña "Clave123!"
When ingreso las credenciales correctas
Then recibo un token JWT válido con expiración de 8 horas
And el campo "lastLogin" del usuario se actualiza

**Scenario:** Login con contraseña incorrecta
Given existe usuario con email "usuario@ejemplo.com"
When ingreso contraseña incorrecta 1 vez
Then recibo error 401 "Credenciales inválidas"
And el campo "failedAttempts" incrementa en 1

**Scenario:** Bloqueo tras 5 intentos fallidos
Given existe usuario con 4 intentos fallidos registrados
When ingreso contraseña incorrecta una vez más
Then la cuenta queda bloqueada por 15 minutos
And recibo error 423 "Cuenta bloqueada temporalmente"
And se envía email de alerta de seguridad al usuario

**Scenario:** Autobloqueo no aplica a cuenta de administrador
Given soy administrador con email "admin@ejemplo.com"
When ingreso credenciales incorrectas 10 veces
Then la cuenta NO se bloquea automáticamente
But se genera alerta en el log de seguridad

**Scenario:** Login con cuenta desactivada
Given existe usuario con estado "DESACTIVADO"
When intento hacer login
Then recibo error 403 "Cuenta desactivada, contacte al soporte"

### Recuperación de Contraseña

**Scenario:** Solicitar reset de contraseña
Given existe usuario con email "usuario@ejemplo.com"
When solicito reset de contraseña para ese email
Then recibo email con enlace válido por 30 minutos
And el enlace contiene un token único de un solo uso

**Scenario:** Reset con token expirado
Given tengo un token de reset con más de 30 minutos de antigüedad
When intento usarlo para cambiar contraseña
Then recibo error "El enlace ha expirado, solicite uno nuevo"

**Scenario:** Reset exitoso
Given tengo un token de reset válido
When ingreso nueva contraseña "NuevaClave456!"
Then la contraseña se actualiza
And el token queda invalidado
And todas las sesiones activas del usuario son cerradas

### Perfil de Usuario

**Scenario:** Actualizar nombre
Given estoy autenticado
When actualizo mi nombre a "María López"
Then el perfil muestra el nombre actualizado
And los demás campos permanecen sin cambios

**Scenario:** No se puede actualizar el email desde perfil
Given estoy autenticado
When intento cambiar el email en el formulario de perfil
Then el campo email está deshabilitado
And veo mensaje "Para cambiar el email contacte al administrador"

### Gestión por Administrador

**Scenario:** Admin desactiva cuenta de usuario
Given soy administrador autenticado
And existe usuario activo con id "usr-123"
When desactivo la cuenta del usuario
Then el estado del usuario cambia a "DESACTIVADO"
And todas sus sesiones activas son invalidadas inmediatamente

**Scenario:** Admin reactiva cuenta
Given existe usuario con estado "DESACTIVADO"
When el administrador reactiva la cuenta
Then el estado cambia a "ACTIVO"
And el usuario puede volver a hacer login

**Scenario:** Admin lista usuarios con paginación
Given existen 150 usuarios en el sistema
When consulto la lista de usuarios con page=1 y limit=20
Then recibo exactamente 20 usuarios
And el campo "totalElements" es 150
And el campo "totalPages" es 8
