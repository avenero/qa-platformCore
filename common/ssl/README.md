# SSL Truststore — qa-platformCore

Este directorio contiene el truststore Java con certificados SSL para acceso a servicios externos (Jira, APIs internas con TLS custom).

---

## Contenido

- **`myTrustStore.jks`** — Java Keystore con certificados SSL (commitear al repo)

---

## Propósito

Permite que el framework y Gradle accedan a servicios externos sin errores SSL:
```
PKIX path building failed: unable to find valid certification path to requested target
```

El truststore es cargado automáticamente por `SSLContextFactory` (`com.qa.common.ssl.SSLContextFactory`) al inicializar el framework.

---

## Uso automático

El truststore se activa si existe en el path del módulo. `SSLContextFactory` lo busca en:

1. `../common/ssl/myTrustStore.jks` (desde módulo)
2. `common/ssl/myTrustStore.jks` (desde raíz)
3. `ssl/myTrustStore.jks` (desde common)

Para forzar uso vía Gradle, agregar en `gradle.properties`:

```properties
systemProp.javax.net.ssl.trustStore=common/ssl/myTrustStore.jks
systemProp.javax.net.ssl.trustStorePassword=changeit
systemProp.javax.net.ssl.trustStoreType=JKS
```

---

## Agregar un certificado externo (ej: Jira corporativo)

### Paso 1 — Obtener el certificado

```bash
openssl s_client -connect <host>:443 -showcerts </dev/null 2>/dev/null | \
  openssl x509 -outform PEM > /tmp/servicio.crt

cat /tmp/servicio.crt   # Debe mostrar -----BEGIN CERTIFICATE----- ... -----END CERTIFICATE-----
```

### Paso 2 — Importar al truststore

```bash
cd qa-platformCore/common/ssl

keytool -import \
  -alias <nombre-alias> \
  -file /tmp/servicio.crt \
  -keystore myTrustStore.jks \
  -storepass changeit \
  -noprompt

# Verificar
keytool -list -keystore myTrustStore.jks -storepass changeit
```

### Paso 3 — Republicar el módulo common

```bash
cd qa-platformCore
./gradlew :common:clean :common:build :common:publishToMavenLocal
```

### Paso 4 — Verificar en el proyecto consumidor

Ejecutar los tests que usan el servicio con TLS. En los logs debe aparecer:
```
SSLContext creado con truststore: myTrustStore.jks
SSL configurado con truststore personalizado del framework
```

---

## Crear el truststore desde cero (si no existe)

```bash
# Copiar desde cacerts del JDK
cp $JAVA_HOME/lib/security/cacerts myTrustStore.jks

# O crear vacío (con entrada dummy temporal)
keytool -genkeypair \
  -alias dummy \
  -keyalg RSA \
  -keystore myTrustStore.jks \
  -storepass changeit \
  -dname "CN=localhost" \
  -validity 365

# Eliminar entrada dummy
keytool -delete -alias dummy -keystore myTrustStore.jks -storepass changeit
```

---

## Verificar certificados

```bash
# Listar todos
keytool -list -keystore myTrustStore.jks -storepass changeit

# Buscar uno específico
keytool -list -keystore myTrustStore.jks -storepass changeit | grep <alias>
```

---

## Alternativa global (JDK)

Si se prefiere instalar en el JDK del sistema en lugar del truststore del framework:

```bash
CACERTS_PATH=$JAVA_HOME/lib/security/cacerts
sudo keytool -import \
  -alias <nombre-alias> \
  -file /tmp/servicio.crt \
  -keystore $CACERTS_PATH \
  -storepass changeit \
  -noprompt
```

Ventaja: aplica a todos los proyectos Java.
Desventaja: requiere permisos admin, se pierde al cambiar de JDK.

---

## Notas

- `myTrustStore.jks` **SÍ debe commitearse** al repo (no contiene secrets, solo certificados públicos)
- `*.crt` (certificados en texto) **NO commitear** — son artefactos temporales de importación
- Password del truststore: `changeit` (default Java)
- Namespace del framework: `com.qa.*` — código en `qa-platformCore/common/src/main/java/com/qa/`
