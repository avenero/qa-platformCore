# User Story: Motor de Reglas de Negocio — Stress Test (100 Scenarios)

**Como** configurador del sistema
**Quiero** definir reglas de negocio complejas sin código
**Para** adaptar el comportamiento del sistema sin intervención del equipo de desarrollo

## Criterios de Aceptación

**Scenario 1:** Crear regla simple de tipo comparación
Given estoy en el módulo de reglas
When creo regla "edad >= 18"
Then la regla se guarda y está activa

**Scenario 2:** Crear regla con operador AND
When creo regla "edad >= 18 AND pais == 'AR'"
Then la regla combina ambas condiciones

**Scenario 3:** Crear regla con operador OR
When creo regla "edad < 18 OR tutorId IS NOT NULL"
Then la regla valida correctamente menores con tutor

**Scenario 4:** Crear regla con NOT
When creo regla "NOT (estado == 'BLOQUEADO')"
Then la regla excluye usuarios bloqueados

**Scenario 5:** Regla con paréntesis anidados
When creo regla "(a == 1 AND b == 2) OR (c == 3 AND d == 4)"
Then la precedencia de operadores es respetada

**Scenario 6:** Regla con campo numérico entero
When creo regla "stockDisponible > 0"
Then la regla detecta productos con stock

**Scenario 7:** Regla con campo decimal
When creo regla "precio < 99.99"
Then la regla admite valores decimales correctamente

**Scenario 8:** Regla con campo de fecha comparado a hoy
When creo regla "fechaVencimiento > TODAY()"
Then la función TODAY() es evaluada dinámicamente

**Scenario 9:** Regla con BETWEEN para rangos
When creo regla "monto BETWEEN 100 AND 500"
Then se valida que el monto esté en ese rango inclusive

**Scenario 10:** Regla con IN para lista de valores
When creo regla "categoria IN ('ELECTRONICA', 'INFORMATICA', 'CELULARES')"
Then se acepta cualquier valor de la lista

**Scenario 11:** Regla con LIKE para strings
When creo regla "email LIKE '%@empresa.com'"
Then detecta emails corporativos

**Scenario 12:** Regla con IS NULL
When creo regla "telefonoSecundario IS NULL"
Then detecta registros sin teléfono secundario

**Scenario 13:** Regla con IS NOT NULL
When creo regla "avatarUrl IS NOT NULL"
Then detecta usuarios con foto de perfil

**Scenario 14:** Regla con conteo de colección
When creo regla "COUNT(ordenes) >= 5"
Then detecta clientes frecuentes

**Scenario 15:** Regla con suma de colección
When creo regla "SUM(ordenes.total) > 10000"
Then detecta clientes de alto valor

**Scenario 16:** Regla con MAX en colección
When creo regla "MAX(pagos.monto) > 5000"
Then detecta el pago máximo realizado

**Scenario 17:** Regla con MIN en colección
When creo regla "MIN(calificaciones.puntaje) >= 3"
Then verifica calidad mínima de calificaciones

**Scenario 18:** Regla con AVG en colección
When creo regla "AVG(envios.tiempoEntrega) <= 48"
Then verifica tiempo promedio de entrega en horas

**Scenario 19:** Regla con condición en subcolección anidada
When creo regla "ANY(ordenes.items.categoria == 'PREMIUM')"
Then detecta si algún item de alguna orden es premium

**Scenario 20:** Regla con ALL en subcolección
When creo regla "ALL(ordenes.items.disponible == true)"
Then verifica que todos los items estén disponibles

**Scenario 21:** Regla que referencia campo de entidad relacionada
When creo regla "cliente.nivel == 'GOLD'"
Then navega la relación para evaluar el nivel

**Scenario 22:** Regla con múltiples niveles de relación
When creo regla "cliente.empresa.pais == 'AR'"
Then navega dos niveles de relación

**Scenario 23:** Regla con función de longitud de string
When creo regla "LENGTH(descripcion) > 20"
Then valida descripción con suficiente contenido

**Scenario 24:** Regla con función UPPER
When creo regla "UPPER(estado) == 'ACTIVO'"
Then la comparación es case-insensitive via UPPER

**Scenario 25:** Regla con función TRIM
When creo regla "TRIM(nombre) != ''"
Then detecta nombres que no son solo espacios

**Scenario 26:** Regla con prioridad de evaluación
Given existen dos reglas aplicables al mismo objeto
When ambas reglas tienen diferente prioridad
Then se aplica primero la de mayor prioridad

**Scenario 27:** Regla con acción "BLOQUEAR"
When la regla se cumple para un usuario
Then la acción BLOQUEAR suspende la cuenta automáticamente

**Scenario 28:** Regla con acción "NOTIFICAR"
When la regla se cumple
Then se envía notificación al usuario y al administrador

**Scenario 29:** Regla con acción "ASIGNAR_ETIQUETA"
When la regla se cumple
Then se agrega la etiqueta configurada al objeto

**Scenario 30:** Regla con acción "REDIRIGIR"
When la regla se cumple durante el login
Then el usuario es redirigido a la URL especificada en la acción

**Scenario 31:** Regla inactiva no se evalúa
Given existe una regla con estado INACTIVO
When el motor evalúa reglas para un objeto
Then la regla inactiva es omitida completamente

**Scenario 32:** Activar regla cambia su estado
Given existe una regla INACTIVA
When el administrador la activa
Then el estado cambia a ACTIVO y se aplica en la próxima evaluación

**Scenario 33:** Desactivar regla no la elimina
When el administrador desactiva una regla ACTIVA
Then la regla pasa a INACTIVO pero el historial de activaciones se preserva

**Scenario 34:** Eliminar regla con historial
Given una regla tiene 100 evaluaciones registradas en historial
When el administrador elimina la regla
Then se le advierte que el historial será archivado
And debe confirmar la eliminación

**Scenario 35:** Regla con fecha de vigencia inicio
Given una regla con fechaInicio = mañana
When el motor evalúa hoy
Then la regla no es considerada (aún no vigente)

**Scenario 36:** Regla con fecha de vigencia fin
Given una regla con fechaFin = ayer
When el motor evalúa hoy
Then la regla no es considerada (ya expiró)

**Scenario 37:** Regla en ventana de vigencia
Given una regla con fechaInicio = ayer y fechaFin = mañana
When el motor evalúa hoy
Then la regla SÍ es evaluada (dentro de vigencia)

**Scenario 38:** Regla con expresión inválida no se guarda
When intento guardar regla con expresión "campo === valor" (triple igual)
Then recibo error de validación de sintaxis
And la regla no se persiste

**Scenario 39:** Regla con campo inexistente
When intento guardar regla "campoQueNoExiste > 5"
Then recibo advertencia "Campo no reconocido en el esquema"
And puedo guardar de todas formas si confirmo

**Scenario 40:** Regla con tipo de dato incorrecto
When intento guardar regla "nombre > 100" (comparando string con número)
Then recibo error de tipo "Incompatibilidad de tipos: String vs Number"

**Scenario 41:** Versionar regla al modificarla
Given una regla existe con versión 1
When la modifico
Then se crea versión 2 y la versión 1 queda en historial

**Scenario 42:** Revertir regla a versión anterior
Given una regla existe con versiones 1, 2 y 3
When revierto a la versión 1
Then la versión activa es una copia de la v1 con número v4
And se registra la acción de reversión en audit log

**Scenario 43:** Clonar regla
Given existe regla "Regla Base"
When la clono
Then se crea "Copia de Regla Base" en estado INACTIVO
And puedo modificarla de forma independiente

**Scenario 44:** Importar reglas desde JSON
Given tengo un archivo JSON con 10 definiciones de reglas
When importo el archivo
Then se crean 10 reglas en estado INACTIVO para revisión

**Scenario 45:** Exportar reglas a JSON
Given existen 20 reglas activas
When exporto todas las reglas
Then descargo un JSON con las 20 definiciones
And el JSON puede reimportarse sin pérdida de datos

**Scenario 46:** Probar regla en modo "dry run"
Given tengo una regla nueva sin activar
When la pruebo con datos de ejemplo
Then veo si la regla se hubiera aplicado y cuántos objetos habrían sido afectados

**Scenario 47:** Ver historial de evaluaciones de una regla
Given la regla ha sido evaluada 500 veces en el último mes
When consulto el historial
Then veo las últimas 50 evaluaciones con detalle
And un resumen: N de N aplicaciones, N positivas, N negativas

**Scenario 48:** Regla de throttle: no aplicar más de N veces al día por objeto
Given la regla tiene configurado throttle de 3 aplicaciones por día por usuario
When la regla aplica 3 veces al mismo usuario en el mismo día
Then la 4ta evaluación retorna "throttled" y no ejecuta la acción

**Scenario 49:** Regla con dependencia en otra regla
Given la regla A requiere que la regla B sea verdadera antes de evaluarse
When la regla B es falsa
Then la regla A no se evalúa (short-circuit)

**Scenario 50:** Bucle circular entre reglas detectado
Given la regla A depende de B y B depende de A
When intento guardar esta dependencia
Then recibo error "Dependencia circular detectada: A → B → A"

**Scenario 51:** Performance: 1000 objetos evaluados en menos de 2 segundos
Given existen 50 reglas activas y 1000 objetos a evaluar
When ejecuto la evaluación masiva
Then todos los objetos son procesados en menos de 2 segundos

**Scenario 52:** Concurrencia: 20 evaluaciones simultáneas sin inconsistencias
Given 20 usuarios modifican reglas al mismo tiempo
When el motor evalúa en paralelo
Then no hay condiciones de carrera ni resultados inconsistentes

**Scenario 53:** Regla con campo de tipo lista (array contains)
When creo regla "roles CONTAINS 'ADMIN'"
Then detecta usuarios con rol administrador en su lista de roles

**Scenario 54:** Regla con campo de tipo lista (array contains all)
When creo regla "permisos CONTAINS_ALL ['READ', 'WRITE']"
Then detecta usuarios con ambos permisos

**Scenario 55:** Regla con campo de tipo lista (array contains any)
When creo regla "categorias CONTAINS_ANY ['PREMIUM', 'VIP', 'GOLD']"
Then detecta objetos con al menos una categoría especial

**Scenario 56:** Regla con campo de tipo lista (array size)
When creo regla "SIZE(etiquetas) > 5"
Then detecta objetos con más de 5 etiquetas

**Scenario 57:** Regla que evalúa diferencia de fechas
When creo regla "DAYS_SINCE(fechaCreacion) > 365"
Then detecta cuentas con más de un año de antigüedad

**Scenario 58:** Regla que evalúa día de la semana
When creo regla "DAY_OF_WEEK(fechaCompra) IN (6, 7)"
Then detecta compras realizadas en fin de semana

**Scenario 59:** Regla que evalúa hora del día
When creo regla "HOUR(fechaTransaccion) BETWEEN 22 AND 6"
Then detecta transacciones nocturnas de alto riesgo

**Scenario 60:** Regla con operador MATCHES (regex)
When creo regla "telefono MATCHES '^\\+54[0-9]{10}$'"
Then valida el formato de teléfono argentino

**Scenario 61:** Excluir objetos de evaluación por lista de IDs
Given la regla tiene una lista de exclusión con 5 IDs
When el motor evalúa esos 5 objetos
Then son omitidos sin evaluar la condición

**Scenario 62:** Regla solo aplica a subconjunto de objetos (filtro previo)
Given la regla tiene un pre-filtro "tipo == 'EMPRESA'"
When el motor evalúa usuarios de tipo 'PERSONA'
Then esos usuarios no son procesados por esta regla

**Scenario 63:** Múltiples acciones en una misma regla
When creo regla con acciones: NOTIFICAR + ASIGNAR_ETIQUETA + INCREMENTAR_CONTADOR
Then al cumplirse la condición se ejecutan las 3 acciones en orden

**Scenario 64:** Fallo en una acción no cancela las demás
Given una regla tiene 3 acciones y la segunda falla
When el motor ejecuta las acciones
Then la primera y tercera acciones se ejecutan
And el fallo de la segunda queda registrado en el log de errores

**Scenario 65:** Rollback cuando la acción crítica falla
Given la regla tiene una acción marcada como CRITICAL
When esa acción falla
Then las acciones anteriores se revierten (rollback)
And la condición se marca como "FAILED" en el historial

**Scenario 66:** Regla auditada: quién la creó
Given se crea una regla
Then el campo "creadoPor" registra el userId del creador con timestamp

**Scenario 67:** Regla auditada: historial de modificaciones
Given una regla fue modificada 5 veces por 3 usuarios diferentes
When consulto el audit trail
Then veo las 5 modificaciones con usuario, timestamp y diff del cambio

**Scenario 68:** Permisos: rol VIEWER solo puede ver reglas
Given soy usuario con rol VIEWER
When intento editar una regla
Then recibo error 403 "No tienes permiso para modificar reglas"

**Scenario 69:** Permisos: rol EDITOR puede crear y editar pero no eliminar
Given soy usuario con rol EDITOR
When creo y modifico reglas
Then ambas operaciones son permitidas
But si intento eliminar recibo 403

**Scenario 70:** Permisos: solo ADMIN puede eliminar reglas
Given soy usuario con rol ADMIN
When elimino una regla
Then la eliminación es exitosa

**Scenario 71:** Regla compartida entre organizaciones (multi-tenant)
Given una regla es marcada como "COMPARTIDA"
When otra organización importa la regla
Then obtiene una copia independiente que puede modificar sin afectar el original

**Scenario 72:** Aislamiento: reglas de una org no son visibles para otra
Given soy usuario de la organización "Empresa A"
When listo todas las reglas
Then solo veo las reglas de "Empresa A"
And no hay forma de ver reglas de otras organizaciones

**Scenario 73:** Regla con expresión que contiene injection
When intento guardar regla con expresión "' OR '1'='1"
Then el sistema sanitiza la entrada o rechaza con error de validación
And no se produce ninguna ejecución de SQL ni código arbitrario

**Scenario 74:** API de reglas protegida con autenticación
When consulto la API de reglas sin token de autenticación
Then recibo 401 Unauthorized

**Scenario 75:** API de reglas con token inválido
When consulto la API con token JWT manipulado
Then recibo 401 Unauthorized
And el intento queda registrado en el log de seguridad

**Scenario 76:** Regla con descripción muy larga (10.000 caracteres)
When guardo una regla con descripción de 10.000 caracteres
Then se guarda correctamente
And al listar reglas la descripción se muestra truncada a 200 chars con indicador "..."

**Scenario 77:** Nombre de regla duplicado dentro de la misma organización
When creo dos reglas con el mismo nombre en la misma organización
Then la segunda recibe advertencia "Ya existe una regla con este nombre"
And puedo continuar si confirmo (nombres duplicados permitidos con advertencia)

**Scenario 78:** Regla con tags para clasificación
When creo regla con tags ["seguridad", "fraude", "alta-prioridad"]
When filtro reglas por tag "fraude"
Then solo aparecen las reglas etiquetadas con "fraude"

**Scenario 79:** Buscar reglas por texto libre
When busco en el campo de búsqueda "email"
Then aparecen todas las reglas cuya expresión o descripción contiene "email"
And el texto coincidente aparece resaltado en los resultados

**Scenario 80:** Paginación de listado de reglas
Given existen 200 reglas en la organización
When consulto la API con page=1 y limit=25
Then recibo exactamente 25 reglas
And el header X-Total-Count indica 200

**Scenario 81:** Ordenamiento de reglas por prioridad descendente
When consulto reglas ordenadas por prioridad DESC
Then la primera regla de la lista tiene la prioridad más alta

**Scenario 82:** Ordenamiento por fecha de última modificación
When consulto reglas ordenadas por updatedAt DESC
Then veo primero las modificadas más recientemente

**Scenario 83:** Filtro por estado activo/inactivo
When filtro reglas con estado=ACTIVO
Then solo aparecen reglas en estado ACTIVO
And el total refleja solo las activas

**Scenario 84:** Filtro combinado: estado + tag + texto
When filtro con estado=ACTIVO, tag=fraude, q=monto
Then solo aparecen reglas que cumplen TODAS las condiciones del filtro

**Scenario 85:** Webhook al aplicar regla
Given la regla tiene configurado un webhook de notificación
When la regla aplica sobre un objeto
Then se hace POST al webhook con payload: {ruleId, objectId, objectType, result, timestamp}
And se espera respuesta 2xx en menos de 5 segundos

**Scenario 86:** Retry de webhook fallido
Given el webhook destino devolvió error 503
When han pasado 5 minutos
Then el sistema reintenta el webhook con backoff exponencial
And después de 3 intentos fallidos el webhook se marca como "FAILED" con alerta al administrador

**Scenario 87:** Regla con contador: incrementar campo al aplicarse
Given la regla tiene acción INCREMENTAR campo "comprasAprobadas" en 1
When la regla aplica al usuario
Then el campo incrementa atómicamente en 1
And en concurrencia alta no hay lost updates

**Scenario 88:** Expresión con función personalizada (custom function)
Given el administrador registró la función CUSTOM_RISK_SCORE()
When creo regla "CUSTOM_RISK_SCORE(usuario) > 75"
Then la función es invocada durante la evaluación

**Scenario 89:** Función personalizada con error en ejecución
Given la función personalizada lanza una excepción
When el motor evalúa la regla
Then el error es capturado y registrado
And la regla se considera NO aplicable (fail-safe)

**Scenario 90:** Evaluación lazy: cortocircuito en AND
Given condición "A AND B" donde A es evaluación costosa y B es barata
When A es falso
Then B no se evalúa (lazy AND)
And el tiempo de evaluación es menor que si se evaluaran ambas

**Scenario 91:** Evaluación lazy: cortocircuito en OR
Given condición "A OR B" donde A es verdadero
When A es evaluado como verdadero
Then B no se evalúa
And la regla aplica inmediatamente

**Scenario 92:** Caché de resultados de evaluación
Given la misma regla se evalúa 100 veces sobre el mismo objeto inmutable en 1 segundo
When el caché está habilitado
Then solo la primera evaluación ejecuta el motor
And las 99 restantes usan el resultado cacheado

**Scenario 93:** Invalidación de caché al modificar la regla
Given el resultado de una regla está en caché
When la regla es modificada
Then el caché se invalida
And la próxima evaluación recalcula desde cero

**Scenario 94:** Invalidación de caché al modificar el objeto evaluado
Given el resultado de la regla está en caché para un objeto
When ese objeto es modificado
Then el caché del objeto se invalida
And la próxima evaluación recalcula

**Scenario 95:** Métricas de evaluación expuestas en /metrics
Given el motor de reglas está en funcionamiento
When consulto el endpoint /actuator/metrics/rules.evaluation.count
Then veo el total de evaluaciones realizadas desde el inicio

**Scenario 96:** Alerta de regla que nunca aplica (dead rule)
Given una regla lleva 30 días sin aplicarse ni una vez
When el job nocturno de análisis ejecuta
Then se genera una alerta para el administrador con sugerencia de revisar/eliminar la regla

**Scenario 97:** Regla con condición ALWAYS TRUE (sin condición)
When creo regla con expresión vacía o "true"
Then la regla aplica a TODOS los objetos del tipo configurado
And se muestra advertencia "Esta regla aplica universalmente"

**Scenario 98:** Regla con condición ALWAYS FALSE (imposible)
When creo regla con expresión "1 == 2"
Then el validador detecta la condición imposible
And muestra advertencia "Esta condición nunca será verdadera"

**Scenario 99:** Límite máximo de reglas activas por organización
Given la organización tiene 999 reglas activas (límite = 1000)
When intento activar la regla número 1001
Then recibo error 422 "Límite máximo de 1000 reglas activas alcanzado"
And se sugiere desactivar reglas obsoletas

**Scenario 100:** Documentación auto-generada de reglas activas
When el administrador descarga el reporte "Reglas Activas"
Then recibe un PDF con tabla de todas las reglas activas, su descripción, prioridad, última modificación y estadísticas de aplicación del último mes
