# 🔐 SSL Truststore

Este directorio contiene el truststore Java con certificados SSL corporativos para acceder a Artifactory.

---

## 📦 Contenido

- **`myTrustStore.jks`** - Java Keystore con certificados SSL corporativos (commitear al repo)

---

## 🎯 Propósito

Permite que Gradle descargue dependencias desde Artifactory corporativo sin errores SSL:
```
PKIX path building failed: unable to find valid certification path to requested target
```

---

## 🚀 Uso

### El truststore se usa automáticamente si está configurado en `gradle.properties`:

```properties
systemProp.javax.net.ssl.trustStore=common/ssl/myTrustStore.jks
systemProp.javax.net.ssl.trustStorePassword=changeit
systemProp.javax.net.ssl.trustStoreType=JKS
```

---

## 🔧 Crear/Actualizar el Truststore

Ver guía completa: **[../config/CERTIFICADO-SSL-ARTIFACTORY.md](../config/CERTIFICADO-SSL-ARTIFACTORY.md)**

### Resumen rápido:

```bash
# 1. Copiar truststore default de Java
cp $JAVA_HOME/lib/security/cacerts myTrustStore.jks

# 2. Importar certificado corporativo
keytool -import \
  -alias artifactory-bns \
  -file anthos.chl.bns.crt \
  -keystore myTrustStore.jks \
  -storepass changeit \
  -noprompt
```

---

## ✅ Ventajas de Tener el Truststore en el Proyecto

- ✅ **Cross-platform**: Funciona en Windows, macOS, Linux automáticamente
- ✅ **Portable**: Se distribuye con el proyecto (Git)
- ✅ **No requiere permisos admin**: No modifica Java del sistema
- ✅ **CI/CD friendly**: Jenkins/GitLab lo usa sin configuración extra
- ✅ **Una sola vez**: No hay que repetir en cada máquina del equipo

---

## 🔍 Verificar Certificados

```bash
# Listar todos los certificados
keytool -list \
  -keystore myTrustStore.jks \
  -storepass changeit

# Buscar certificado específico
keytool -list \
  -keystore myTrustStore.jks \
  -storepass changeit \
  | grep artifactory-bns
```

---

## ⚠️ Importante

- ✅ **SÍ commitear** `myTrustStore.jks` al repositorio (es seguro, no contiene secrets)
- ❌ **NO commitear** `*.crt` (certificados en formato texto, solo para referencia)

---

## 📚 Más Información

- **Guía completa de certificados SSL**: [../config/CERTIFICADO-SSL-ARTIFACTORY.md](../config/CERTIFICADO-SSL-ARTIFACTORY.md)
- **Troubleshooting SSL**: Ver sección "Troubleshooting" en la guía completa

---

**Última actualización**: Diciembre 5, 2025  
**Password del truststore**: `changeit` (default de Java)

