# 🔐 Configuración SSL para Jira/Xray

Este documento explica cómo agregar el certificado SSL de Jira al truststore del framework para resolver errores de certificado.

---

## 📋 PASO 1: Obtener el Certificado de Jira

```bash
# Descargar el certificado del servidor Jira
openssl s_client -connect jira.agile.bns:443 -showcerts </dev/null 2>/dev/null | \
  openssl x509 -outform PEM > /tmp/jira-agile-bns.crt

# Verificar que se obtuvo correctamente
cat /tmp/jira-agile-bns.crt
```

**Deberías ver algo como:**
```
-----BEGIN CERTIFICATE-----
MIIFXzCCBEegAwIBAgIQCBZ...
-----END CERTIFICATE-----
```

✅ Si ves esto, el certificado se obtuvo correctamente.

---

## 📋 PASO 2: Importar al Truststore del Framework

```bash
# Navegar al directorio SSL del framework
cd /Users/abel.venero/Documents/qa-scotia-frameworks/common/ssl

# Importar el certificado al truststore
keytool -import \
  -alias jira-agile-bns \
  -file /tmp/jira-agile-bns.crt \
  -keystore myTrustStore.jks \
  -storepass changeit \
  -noprompt

# Verificar que se importó correctamente
keytool -list -keystore myTrustStore.jks -storepass changeit
```

**Deberías ver:**
```
Keystore type: PKCS12
Keystore provider: SUN

Your keystore contains 1 entry

jira-agile-bns, Dec 30, 2025, trustedCertEntry, 
Certificate fingerprint (SHA-256): ...
```

✅ Certificado importado exitosamente.

---

## 📋 PASO 3: Verificar la Configuración en el Framework

El framework **YA ESTÁ CONFIGURADO** para usar el truststore automáticamente.

El archivo `JiraHttpClient.java` busca el truststore en estas ubicaciones (en orden):

1. `../common/ssl/myTrustStore.jks` (desde módulo)
2. `common/ssl/myTrustStore.jks` (desde raíz)
3. `ssl/myTrustStore.jks` (desde common)

**No necesitas hacer nada más en el código.**

---

## 📋 PASO 4: Republicar el Framework

```bash
cd /Users/abel.venero/Documents/qa-scotia-frameworks

# Limpiar publicaciones anteriores
rm -rf ~/.m2/repository/com/scotia/qa/common/1.0.0

# Compilar y publicar
./gradlew :common:clean :common:build :common:publishToMavenLocal
```

---

## 📋 PASO 5: Probar en un Módulo

```bash
cd /Users/abel.venero/Documents/qa-module-autos

# Limpiar caché de Gradle
./gradlew clean

# Ejecutar tests con reporting a Jira
./gradlew test
```

**En los logs deberías ver:**
```
🔐 Truststore encontrado en: .../common/ssl/myTrustStore.jks
✅ SSLContext creado con truststore: myTrustStore.jks
✅ SSL configurado con truststore personalizado del framework
```

---

## 🔍 Troubleshooting

### ❌ Error: "unable to find valid certification path"

**Causa:** El certificado no está en el truststore o el truststore no se encuentra.

**Solución:**
```bash
# Verificar que el certificado está en el truststore
keytool -list -keystore common/ssl/myTrustStore.jks -storepass changeit | grep jira

# Si no aparece, repetir PASO 2
```

---

### ❌ Error: "Truststore not found"

**Causa:** El framework no encuentra el archivo `myTrustStore.jks`.

**Solución:**
```bash
# Verificar que existe
ls -lah common/ssl/myTrustStore.jks

# Si no existe, crearlo primero:
keytool -genkeypair \
  -alias dummy \
  -keyalg RSA \
  -keystore common/ssl/myTrustStore.jks \
  -storepass changeit \
  -dname "CN=localhost" \
  -validity 365

# Luego eliminar la entrada dummy e importar el certificado de Jira
keytool -delete -alias dummy -keystore common/ssl/myTrustStore.jks -storepass changeit
```

Luego repetir PASO 2.

---

### ⚠️ Warning: "No se encontró truststore personalizado"

**Causa:** El framework no pudo cargar el truststore pero continúa con SSL por defecto.

**Impacto:** Puede fallar la conexión a Jira si el certificado no está en el cacerts del JDK.

**Solución:** Verificar las rutas en los logs y asegurarse de que el truststore existe.

---

## 🎯 Alternativa: Agregar al JDK (Global)

Si prefieres agregar el certificado al JDK en lugar del truststore del framework:

```bash
# Ubicar el cacerts del JDK
CACERTS_PATH=$JAVA_HOME/lib/security/cacerts

# Importar el certificado (requiere sudo)
sudo keytool -import \
  -alias jira-agile-bns \
  -file /tmp/jira-agile-bns.crt \
  -keystore $CACERTS_PATH \
  -storepass changeit \
  -noprompt

# Verificar
keytool -list -keystore $CACERTS_PATH -storepass changeit | grep jira
```

**✅ Ventaja:** Aplica a todos los proyectos Java en la máquina.

**❌ Desventaja:** Requiere permisos de administrador y se pierde si cambias de JDK.

---

## 📚 Más Información

- Ubicación del truststore: `common/ssl/myTrustStore.jks`
- Password del truststore: `changeit`
- Código que lo usa: `common/src/main/java/com/scotia/qa/common/reporting/jira/client/JiraHttpClient.java`

---

**✅ Configuración completada. El framework ahora puede conectarse a Jira con SSL.**

