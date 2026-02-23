# Pipeline Automatico de Actualizacion de Drivers en Artifactory

**Fecha:** 23 de Febrero 2026  
**Objetivo:** Mantener drivers sincronizados con ultimas versiones estables  
**Frecuencia:** Semanal (Lunes 2AM) o manual  
**Archivo Pipeline:** `pipeline-update-drivers.jenkins`

---

## ESTRATEGIA COMPLETA

### Problema Actual:
- Chrome se actualiza automaticamente: 143 → 145
- ChromeDriver en Artifactory: 143 (desactualizado)
- Tests fallan: "ChromeDriver only supports Chrome 143"

### Solucion:
Pipeline automatico que:
1. Detecta ultima version Stable de Chrome/Firefox desde APIs oficiales
2. Descarga drivers para Mac ARM64, Windows x64, Linux x64
3. Sube a Artifactory sobrescribiendo version anterior
4. Notifica al equipo de QA
5. Tests usan automaticamente la nueva version

---

## REQUISITOS PREVIOS (Verificar con DevOps)

### 1. Permisos en Artifactory

**Necesitas:**
- Usuario: s2994840 (o crear qa-automation-uploader)
- Repositorio: libs-release-thirdparty
- Path: /external/qa-drivers/*
- Permiso: **WRITE/DEPLOY**

**Verificar permisos actuales:**
```bash
echo "test" > test.txt
curl -u s2994840:TU_TOKEN -X PUT \
  "https://artifactory.cl-devops-infra.chl.bns/artifactory/libs-release-thirdparty/external/qa-drivers/test.txt" \
  -T test.txt
```

**Resultado esperado:**
- HTTP 201 Created → Tienes permisos WRITE
- HTTP 401 Unauthorized → Token invalido
- HTTP 403 Forbidden → Sin permisos WRITE (solicitar a DevOps)

### 2. Credenciales en Jenkins

**Crear credential:**
- Jenkins → Manage Jenkins → Credentials
- Add Credentials:
  - Kind: Secret text
  - Scope: Global
  - Secret: (token generado en Artifactory)
  - ID: `artifactory-write-token`

**Generar token en Artifactory:**
1. Login → User Menu → Edit Profile
2. Generate API Key
3. Scope: libs-release-thirdparty (WRITE)
4. Copiar token y guardarlo en Jenkins

### 3. Acceso a Internet

**URLs requeridas:**
```bash
# Verificar acceso (ejecutar en Jenkins agent)
curl -I https://googlechromelabs.github.io/chrome-for-testing/
curl -I https://github.com/mozilla/geckodriver/releases
curl -I https://storage.googleapis.com/chrome-for-testing-public/
```

**Si bloqueadas → Solicitar whitelist a Infra/Seguridad**

### 4. Herramientas en Jenkins Agent

**Verificar instalacion:**
```bash
curl --version      # Descargar archivos
jq --version        # Parsear JSON
unzip -v            # Descomprimir drivers
```

**Si falta → Instalar:**
```bash
sudo apt-get install curl jq unzip  # Linux
brew install curl jq unzip          # Mac
```

---

## BLOQUEANTES IDENTIFICADOS

| # | Bloqueante | Probabilidad | Tiempo | Accion |
|---|------------|--------------|--------|--------|
| 1 | Sin permisos WRITE Artifactory | 🔴 90% | 1-3 dias | Ticket DevOps |
| 2 | Firewall bloquea Google CDN | 🟡 40% | 1 dia | Whitelist URLs |
| 3 | Jenkins sin herramientas | 🟢 10% | 1 hora | Instalar en slave |
| 4 | Token expira frecuentemente | 🟢 5% | N/A | Service Account |
| 5 | Politica prohibe auto-descargas | 🟡 30% | Reunion | Argumentar beneficios |

---

## ESTIMACION DE TIEMPO

**Mejor escenario (con permisos):**
- Implementacion: 30 min
- Primera prueba: 15 min
- Ajustes: 15 min
- **Total: 1 hora**

**Escenario real (con bloqueantes):**
- Solicitar permisos Artifactory: 1-3 dias (espera)
- Whitelist Google CDN: 1 dia (si aplica)
- Configurar credenciales Jenkins: 30 min
- Implementacion: 30 min
- Pruebas: 30 min
- **Total: 2-4 dias** (mayoria es espera de aprobaciones)

---

## IMPLEMENTACION

### Paso 1: Crear Job en Jenkins

```
Jenkins Dashboard → New Item
  Name: update-qa-drivers-artifactory
  Type: Pipeline
  → OK
```

### Paso 2: Configurar Pipeline

**Opcion A: Desde SCM (Recomendado)**
```
Pipeline section:
  Definition: Pipeline script from SCM
  SCM: Git
  Repository URL: https://bitbucket.agile.bns/scm/qaauy/qa-scotia-frameworks.git
  Branch: develop
  Script Path: web-core/pipeline-update-drivers.jenkins
```

**Opcion B: Script directo**
```
Definition: Pipeline script
  → Copiar contenido de pipeline-update-drivers.jenkins
```

### Paso 3: Configurar Trigger

```
Build Triggers:
  ☑ Build periodically
  Schedule: H 2 * * 1  (Lunes 2AM)
```

### Paso 4: Primera Ejecucion (PRUEBA)

```
1. Ir a: update-qa-drivers-artifactory
2. Click "Build with Parameters"
3. Configurar:
   ☑ UPDATE_CHROME = true
   ☑ UPDATE_FIREFOX = true
   ☑ DRY_RUN = true  (NO sube a Artifactory - solo prueba)
4. Click "Build"
5. Ver logs y verificar descargas
6. Si OK → Ejecutar sin DRY_RUN
```

---

## ESTRUCTURA RESULTANTE EN ARTIFACTORY

```
https://artifactory.cl-devops-infra.chl.bns/artifactory/libs-release-thirdparty/
└── external/
    └── qa-drivers/
        ├── chromedriver-mac/chromedriver        (ultima version Stable)
        ├── chromedriver-win/chromedriver.exe
        ├── chromedriver-linux/chromedriver
        ├── geckodriver-mac/geckodriver
        ├── geckodriver-win/geckodriver.exe
        └── geckodriver-linux/geckodriver
```

**Estrategia:** SOBRESCRIBIR siempre (simple)
- Framework NO necesita cambios
- NO hay historial de versiones en Artifactory
- Historial se mantiene en Jenkins build logs

---

## VENTAJAS

- Zero cambios en modulos de test (transparente)
- Zero cambios en framework (usa misma URL)
- Zero mantenimiento manual de versiones
- Notificaciones automaticas al equipo
- Historial en Jenkins de versiones subidas
- Rollback facil (re-ejecutar pipeline con version anterior)
- Soporta Mac, Windows, Linux automaticamente

---

## TICKET PARA DEVOPS

```
Subject: Permisos WRITE en Artifactory para automatizacion QA

Solicito permisos de DEPLOY/WRITE en:
  - Repositorio: libs-release-thirdparty
  - Path: /external/qa-drivers/*
  - Usuario: s2994840 (o crear qa-automation-uploader)
  
Proposito: 
  Pipeline automatico para mantener WebDrivers actualizados
  Ejecucion: Semanal (lunes 2AM) via cron job
  
Beneficios:
  - Evita errores de version incompatible en tests automatizados
  - Reduce tickets de soporte ("driver no funciona")
  - Zero mantenimiento manual
  - Drivers siempre sincronizados con navegadores
  
URLs que necesitan whitelist (si firewall corporativo aplica):
  - https://googlechromelabs.github.io
  - https://storage.googleapis.com/chrome-for-testing-public/
  - https://github.com/mozilla/geckodriver/releases
  - https://api.github.com/repos/mozilla/geckodriver/

Impacto: 
  - BAJO - Solo lectura de APIs publicas y escritura a 1 path especifico
  - Frecuencia: 1 vez por semana
  - Tamano: ~30MB por driver (~200MB total por actualizacion)
  - Ventana: Lunes 2AM (sin impacto en horario laboral)

Contacto: Abel Venero - abel.venero@scotia.com
```

---

## MANTENIMIENTO FUTURO

**Pipeline es auto-suficiente:**
- Ejecuta automaticamente lunes 2AM
- Detecta ultima version Stable
- Actualiza Artifactory
- Notifica equipo via email

**Intervencion manual solo si:**
- Renovacion de credenciales (anual)
- Cambio de estructura Artifactory
- Agregar Edge (no incluido por defecto)

---

## PLAN DE ACCION RECOMENDADO

### HOY (sin esperar aprobaciones):
1. Verificar acceso Google CDN (5 min)
2. Solicitar permisos DevOps (enviar ticket arriba)
3. Configurar job Jenkins con DRY_RUN=true

### Cuando tengas permisos (1-3 dias):
1. Generar token en Artifactory UI
2. Guardar credential en Jenkins
3. Ejecutar pipeline con DRY_RUN=true (ver logs)
4. Si OK → Ejecutar sin DRY_RUN
5. Validar descarga en modulos
6. Activar cron automatico

### Proximo Sprint (mejora continua):
- Agregar Edge support
- Agregar metricas (tiempo descarga, tamano archivos)
- Dashboard Grafana de versiones activas

---

## ALTERNATIVAS ANALIZADAS (Descartadas)

1. **Capability ignorar version:** NO existe en Selenium
2. **Selenium Manager auto-descarga:** Requiere Selenium 4.6+ e internet en runtime
3. **WebDriverManager library (Boni Garcia):** Migracion futura (reemplaza codigo custom)
4. **Downgrade Chrome browser:** NO recomendado (vulnerabilidades seguridad)
5. **Deteccion dinamica version instalada:** Complejo, requiere cambios framework

**Conclusion:** Pipeline automatico es la mejor opcion (simple, efectiva, escalable)
