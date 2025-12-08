# 🔐 Configuración de Certificado SSL para Artifactory

**Problema**: Error SSL al descargar dependencias desde Artifactory corporativo  
**Solución**: Importar certificado corporativo al Java truststore  
**Fecha**: Diciembre 2025

---

## 📋 Índice

- [🎯 Problema y Síntomas](#-problema-y-síntomas)
- [✅ Solución Recomendada: Truststore en el Proyecto](#-solución-recomendada-truststore-en-el-proyecto)
- [💻 Opción 1: Windows - Importar al Java del Sistema](#-opción-1-windows---importar-al-java-del-sistema)
- [🍎 Opción 2: macOS - Importar al Java del Sistema](#-opción-2-macos---importar-al-java-del-sistema)
- [🐧 Opción 3: Linux - Importar al Java del Sistema](#-opción-3-linux---importar-al-java-del-sistema)
- [🔍 Verificación](#-verificación)
- [🐛 Troubleshooting](#-troubleshooting)

---

## 🎯 Problema y Síntomas

### Error Típico

```
> Could not GET 'https://artifactory.cldevops.chl.bns/...'
> Got SSL handshake exception during request
> PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: 
  unable to find valid certification path to requested target
```

### Causa

Java no confía en el certificado SSL del Artifactory corporativo (autofirmado o con CA corporativa).

---

## ✅ Solución Recomendada: Truststore en el Proyecto

### 🎯 Esta es la MEJOR opción porque:
- ✅ **Cross-platform**: Funciona en Windows, macOS, Linux
- ✅ **Portable**: Va con el proyecto (Git)
- ✅ **No requiere permisos admin**
- ✅ **CI/CD friendly**: Jenkins/GitLab lo usa automáticamente
- ✅ **Una sola vez**: No repetir en cada máquina

---

### Paso 1: Obtener el Certificado

#### Opción A: Descargar desde el Navegador

**Chrome/Edge**:
1. Ir a: `https://artifactory.cldevops.chl.bns`
2. Click en el **candado** 🔒 en la barra de direcciones
3. Click en **"Conexión es segura"** → **"El certificado es válido"**
4. Click en **"Detalles"** → **"Exportar"**
5. Guardar como: `anthos.chl.bns.crt` (formato: Base64 encoded X.509)

**Firefox**:
1. Ir a: `https://artifactory.cldevops.chl.bns`
2. Click en el **candado** 🔒
3. Click en **"Conexión segura"** → **"Más información"**
4. Click en **"Ver certificado"** → **"Descargar"** → **"PEM (certificado)"**
5. Guardar como: `anthos.chl.bns.crt`

#### Opción B: Descargar con OpenSSL (Terminal)

```bash
# Windows PowerShell o macOS/Linux Terminal
openssl s_client -connect artifactory.cldevops.chl.bns:443 -showcerts < /dev/null 2>/dev/null | openssl x509 -outform PEM > anthos.chl.bns.crt
```

---

### Paso 2: Crear Truststore en el Proyecto

#### Windows (PowerShell como Administrador)

```powershell
# 1. Navegar al proyecto
cd C:\Users\{TU_USUARIO}\Downloads\qa-scotia-frameworks

# 2. Ubicar JAVA_HOME
echo $env:JAVA_HOME
# Si está vacío:
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.x"

# 3. Crear directorio ssl en common (si no existe)
New-Item -ItemType Directory -Force -Path common\ssl

# 4. Copiar truststore default de Java al proyecto
Copy-Item "$env:JAVA_HOME\lib\security\cacerts" -Destination "common\ssl\myTrustStore.jks"

# 5. Importar certificado al truststore del proyecto
& "$env:JAVA_HOME\bin\keytool.exe" -import `
  -alias artifactory-bns `
  -file anthos.chl.bns.crt `
  -keystore common\ssl\myTrustStore.jks `
  -storepass changeit `
  -noprompt

# Salida esperada:
# Certificate was added to keystore
```

#### macOS/Linux (Terminal)

```bash
# 1. Navegar al proyecto
cd ~/Documents/qa-scotia-frameworks

# 2. Verificar JAVA_HOME
echo $JAVA_HOME
# Si está vacío:
export JAVA_HOME=$(/usr/libexec/java_home -v 21)  # macOS
# export JAVA_HOME=/usr/lib/jvm/java-21-openjdk    # Linux

# 3. Crear directorio ssl en common (si no existe)
mkdir -p common/ssl

# 4. Copiar truststore default de Java al proyecto
cp $JAVA_HOME/lib/security/cacerts common/ssl/myTrustStore.jks

# 5. Cambiar permisos (importante)
chmod 644 common/ssl/myTrustStore.jks

# 6. Importar certificado al truststore del proyecto
keytool -import \
  -alias artifactory-bns \
  -file anthos.chl.bns.crt \
  -keystore common/ssl/myTrustStore.jks \
  -storepass changeit \
  -noprompt

# Salida esperada:
# Certificate was added to keystore
```

---

### Paso 3: Configurar Gradle para Usar el Truststore

**Editar `gradle.properties`** (en la raíz del proyecto):

```properties
# SSL Truststore Corporativo
systemProp.javax.net.ssl.trustStore=common/ssl/myTrustStore.jks
systemProp.javax.net.ssl.trustStorePassword=changeit
systemProp.javax.net.ssl.trustStoreType=JKS

# Timeout aumentado (redes corporativas lentas)
org.gradle.daemon.idletimeout=10800000
systemProp.http.connectionTimeout=300000
systemProp.http.socketTimeout=300000
```

---

### Paso 4: Verificar Configuración

```bash
# Windows
.\gradlew.bat clean build --refresh-dependencies

# macOS/Linux
./gradlew clean build --refresh-dependencies
```

**Salida esperada**: Descarga de dependencias sin errores SSL.

---

### Paso 5: Commitear Truststore al Repositorio

```bash
# Agregar truststore al repositorio (es seguro, no contiene secrets)
git add common/ssl/myTrustStore.jks
git add gradle.properties
git commit -m "feat: agregar truststore corporativo para Artifactory SSL"

# Agregar certificado al .gitignore (opcional, solo para referencia)
echo "anthos.chl.bns.crt" >> .gitignore
```

**Ahora todos los miembros del equipo heredan la configuración** ✅

---

## 💻 Opción 1: Windows - Importar al Java del Sistema

### ⚠️ Desventaja: Hay que repetir en cada máquina Windows

### Paso 1: Verificar Java

```powershell
# Verificar versión de Java
java -version

# Encontrar JAVA_HOME
Get-Command java | Select-Object -ExpandProperty Definition
# O
echo $env:JAVA_HOME
```

### Paso 2: Importar Certificado (Como Administrador)

```powershell
# Abrir PowerShell como Administrador

# Navegar al directorio donde está el certificado
cd C:\Users\{TU_USUARIO}\Downloads

# Importar certificado
& "C:\Program Files\Java\jdk-21.0.x\bin\keytool.exe" -import `
  -alias artifactory-bns `
  -file anthos.chl.bns.crt `
  -keystore "C:\Program Files\Java\jdk-21.0.x\lib\security\cacerts" `
  -storepass changeit `
  -noprompt
```

**Salida esperada**:
```
Trust this certificate? [no]:  yes
Certificate was added to keystore
```

### Paso 3: Verificar

```powershell
# Listar certificados en el truststore
& "C:\Program Files\Java\jdk-21.0.x\bin\keytool.exe" -list `
  -keystore "C:\Program Files\Java\jdk-21.0.x\lib\security\cacerts" `
  -storepass changeit `
  | Select-String "artifactory-bns"
```

**Salida esperada**:
```
artifactory-bns, Dec 5, 2025, trustedCertEntry,
```

---

## 🍎 Opción 2: macOS - Importar al Java del Sistema

### ⚠️ Desventaja: Hay que repetir en cada Mac

### Paso 1: Verificar Java

```bash
# Verificar versión de Java
java -version

# Encontrar JAVA_HOME
/usr/libexec/java_home -v 21

# Configurar JAVA_HOME (agregar a ~/.zshrc o ~/.bash_profile)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

### Paso 2: Importar Certificado

```bash
# Navegar al directorio donde está el certificado
cd ~/Downloads

# Importar certificado (puede pedir contraseña de administrador)
sudo keytool -import \
  -alias artifactory-bns \
  -file anthos.chl.bns.crt \
  -keystore $JAVA_HOME/lib/security/cacerts \
  -storepass changeit \
  -noprompt
```

**Salida esperada**:
```
Password: [ingresar contraseña de admin]
Certificate was added to keystore
```

### Paso 3: Verificar

```bash
# Listar certificados
keytool -list \
  -keystore $JAVA_HOME/lib/security/cacerts \
  -storepass changeit \
  | grep artifactory-bns
```

**Salida esperada**:
```
artifactory-bns, Dec 5, 2025, trustedCertEntry,
```

---

## 🐧 Opción 3: Linux - Importar al Java del Sistema

### ⚠️ Desventaja: Hay que repetir en cada servidor Linux

### Paso 1: Verificar Java

```bash
# Verificar versión de Java
java -version

# Encontrar JAVA_HOME
readlink -f $(which java)
# Ejemplo: /usr/lib/jvm/java-21-openjdk-amd64/bin/java

# Configurar JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

### Paso 2: Importar Certificado

```bash
# Navegar al directorio donde está el certificado
cd ~/

# Importar certificado (requiere sudo)
sudo keytool -import \
  -alias artifactory-bns \
  -file anthos.chl.bns.crt \
  -keystore $JAVA_HOME/lib/security/cacerts \
  -storepass changeit \
  -noprompt
```

**Salida esperada**:
```
Certificate was added to keystore
```

### Paso 3: Verificar

```bash
# Listar certificados
keytool -list \
  -keystore $JAVA_HOME/lib/security/cacerts \
  -storepass changeit \
  | grep artifactory-bns
```

---

## 🔍 Verificación

### Probar Descarga de Dependencias

```bash
# Limpiar caché de Gradle
rm -rf .gradle
rm -rf ~/.gradle/caches  # macOS/Linux
Remove-Item -Recurse -Force .gradle  # Windows
Remove-Item -Recurse -Force $env:USERPROFILE\.gradle\caches  # Windows

# Compilar con logs detallados
./gradlew clean build --refresh-dependencies --info  # macOS/Linux
.\gradlew.bat clean build --refresh-dependencies --info  # Windows
```

**Buscar en los logs**:
```
> Resolving dependency com.aventstack:extentreports:5.1.1
> Downloaded https://artifactory.cldevops.chl.bns/.../extentreports-5.1.1.jar (2.3 MB)
```

✅ **Si ves descargas exitosas = Certificado funcionando**

---

## 🐛 Troubleshooting

### ❌ Error: "keytool: command not found"

**Causa**: Java no está en el PATH

**Solución Windows**:
```powershell
# Usar ruta completa
& "C:\Program Files\Java\jdk-21.0.x\bin\keytool.exe" ...
```

**Solución macOS/Linux**:
```bash
# Usar ruta completa
$JAVA_HOME/bin/keytool ...
```

---

### ❌ Error: "keytool error: java.io.IOException: Keystore was tampered with, or password was incorrect"

**Causa**: Password incorrecto

**Solución**:
- Password default de Java truststore es: `changeit`
- Si fue cambiado, contactar a IT

---

### ❌ Error: "Alias <artifactory-bns> already exists"

**Causa**: Certificado ya fue importado antes

**Solución 1**: Eliminar y reimportar
```bash
# Eliminar alias existente
keytool -delete \
  -alias artifactory-bns \
  -keystore common/ssl/myTrustStore.jks \
  -storepass changeit

# Reimportar
keytool -import \
  -alias artifactory-bns \
  -file anthos.chl.bns.crt \
  -keystore common/ssl/myTrustStore.jks \
  -storepass changeit \
  -noprompt
```

**Solución 2**: Ignorar (ya está configurado)

---

### ❌ Sigue fallando SSL después de importar

**Verificar que Gradle use el truststore**:

```bash
# Verificar que gradle.properties tenga:
cat gradle.properties | grep trustStore

# Debe mostrar:
# systemProp.javax.net.ssl.trustStore=common/ssl/myTrustStore.jks
# systemProp.javax.net.ssl.trustStorePassword=changeit
```

**Verificar que el certificado esté en el truststore**:
```bash
keytool -list \
  -keystore common/ssl/myTrustStore.jks \
  -storepass changeit \
  | grep artifactory-bns
```

---

### ❌ Error en CI/CD (Jenkins/GitLab)

**Causa**: CI/CD no tiene acceso al truststore del proyecto

**Solución**:

**Opción A**: Usar truststore del proyecto (recomendado)
```groovy
// Jenkinsfile
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh './gradlew clean build'
                // Usa automáticamente common/ssl/myTrustStore.jks
            }
        }
    }
}
```

**Opción B**: Importar certificado en el agente de CI/CD
```bash
# En el servidor Jenkins/GitLab
sudo keytool -import \
  -alias artifactory-bns \
  -file anthos.chl.bns.crt \
  -keystore $JAVA_HOME/lib/security/cacerts \
  -storepass changeit \
  -noprompt
```

---

## 📊 Comparación de Opciones

| Aspecto | Truststore en Proyecto | Java del Sistema |
|---------|------------------------|------------------|
| **Setup** | ⏱️ Una sola vez | ⏱️⏱️⏱️ En cada máquina |
| **Portabilidad** | ✅ Cross-platform | ❌ Manual en cada OS |
| **CI/CD** | ✅ Automático | ❌ Requiere setup |
| **Permisos Admin** | ✅ No requiere | ❌ Requiere |
| **Mantenimiento** | ✅ Git lo mantiene | ❌ Manual |
| **Recomendado** | ✅✅✅ SÍ | ⚠️ Solo si no puedes usar proyecto |

---

## ✅ Recomendación Final

### Para Equipos de Desarrollo (Scotia QA):
**Usar Truststore en el Proyecto** (Opción Recomendada)
- Setup una sola vez
- Todos heredan la configuración
- CI/CD funciona automáticamente

### Para Desarrolladores Individuales:
**Importar al Java del Sistema** solo si:
- No puedes modificar el proyecto
- Trabajas solo (no equipo)
- No usas CI/CD

---

## 📞 Soporte

**Si sigues teniendo problemas SSL**:
1. Verificar que `gradle.properties` tenga la configuración del truststore
2. Verificar que `common/ssl/myTrustStore.jks` exista
3. Listar certificados del truststore: `keytool -list -keystore common/ssl/myTrustStore.jks -storepass changeit`
4. Probar con `--debug` para ver logs completos: `./gradlew build --debug > gradle-debug.log 2>&1`

**Contacto**: QA Team - Scotia Bank  
**Email**: qa-automation@scotiabank.com

---

## 📚 Referencias

- **Java Keytool**: https://docs.oracle.com/en/java/javase/21/docs/specs/man/keytool.html
- **Gradle SSL Config**: https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_system_properties
- **SSL Certificates**: https://www.baeldung.com/java-ssl

---

**Última actualización**: Diciembre 5, 2025  
**Versión Framework**: 1.0.0  
**Certificado**: anthos.chl.bns (BNS Artifactory)

