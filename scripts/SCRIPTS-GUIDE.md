# 🎯 Scripts del Framework - Guía Completa de Uso

## 📋 Resumen Ejecutivo

El framework Scotia QA incluye **7 scripts** que automatizan diferentes aspectos del ciclo de vida de testing:

| # | Script | Propósito | Cuándo Usar | Tipo |
|---|--------|-----------|-------------|------|
| 1 | `create-module.sh` | Crear módulos nuevos | Al iniciar proyecto | Manual |
| 2 | `run-test.sh` | Ejecutar tests | Continuamente | Manual |
| 3 | `utils.sh` | Funciones compartidas | Automático (importado) | Librería |
| 4 | `update-scripts.sh` | Actualizar scripts | Cuando hay actualizaciones | Manual |
| 5 | `analyze-results.sh` | Analizar resultados | Después de ejecutar tests | Manual |
| 6 | `pre-commit.sh` | Validar antes de commit | Antes de cada commit | Automático |
| 7 | `code-quality.sh` | Analizar calidad código | Periódicamente | Manual |

---

## 🔄 Flujo Completo de Trabajo

### 📅 **FASE 1: Setup Inicial (Una vez)**

```bash
# 1. Crear nuevo módulo
./scripts/create-module.sh banking

# 2. Configurar pre-commit hook
cd qa-module-banking
cp ../qa-scotia-frameworks/scripts/pre-commit.sh .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit

# 3. Configurar credenciales
nano .env.local

# Listo para empezar a desarrollar!
```

---

### 🔨 **FASE 2: Desarrollo Diario**

```bash
# 1. Escribir features y steps
vim src/test/resources/features/banking/login.feature

# 2. Ejecutar tests localmente
./scripts/run-test.sh

# 3. Analizar resultados
./scripts/analyze-results.sh

# 4. Si hay tests lentos, optimizar
./scripts/analyze-results.sh --top 20

# 5. Hacer commit (pre-commit se ejecuta automáticamente)
git add .
git commit -m "feat: login tests"
# ✅ Pre-commit valida todo automáticamente
```

---

### 🔍 **FASE 3: Análisis de Calidad (Semanal)**

```bash
# 1. Analizar calidad de código
./scripts/code-quality.sh

# 2. Revisar vulnerabilidades
./scripts/code-quality.sh --security

# 3. Limpiar código sin usar
./scripts/code-quality.sh --unused

# 4. Generar reporte para el equipo
./scripts/code-quality.sh --report
open build/reports/code-quality-report.html
```

---

### 🔄 **FASE 4: Actualización de Scripts (Cuando hay cambios)**

```bash
# 1. Verificar actualizaciones
./scripts/update-scripts.sh --check

# 2. Actualizar scripts
./scripts/update-scripts.sh

# 3. Probar que todo funciona
./scripts/run-test.sh
```

---

## 📖 Documentación Detallada por Script

### 1. `create-module.sh` ⭐ **NUEVO**

**Descripción:** Crea un módulo de testing completo desde cero.

**Cuándo usar:**
- Al iniciar un nuevo proyecto de pruebas
- Para crear PoCs rápidos
- Onboarding de nuevos QAs

**Uso:**
```bash
# Modo interactivo (recomendado)
./create-module.sh

# Modo directo
./create-module.sh banking
./create-module.sh banking --dest ~/projects
./create-module.sh cards --with-api
```

**Resultado:**
```
qa-module-banking/
├── scripts/           ← run-test.sh, utils.sh, update-scripts.sh
├── src/test/          ← Estructura completa
├── build.gradle       ← Configurado
├── .env.local         ← Template
└── README.md          ← Documentado
```

**Tiempo:** ~30 segundos

---

### 2. `run-test.sh` 🚀 **PRINCIPAL**

**Descripción:** Ejecuta tests del módulo con auto-configuración.

**Cuándo usar:**
- Desarrollo local
- Validar cambios
- Antes de hacer commit
- En pipelines CI/CD

**Uso:**
```bash
# Ejecución básica
./scripts/run-test.sh

# Con tags específicos
./scripts/run-test.sh --tags "@smoke"
./scripts/run-test.sh --tags "@api and @smoke"

# Ambiente específico
./scripts/run-test.sh --env qa
```

**Características:**
- ✅ Auto-detecta módulo
- ✅ Carga .env.local automáticamente
- ✅ Verifica actualizaciones de scripts
- ✅ Compatible con Jenkins

---

### 3. `utils.sh` 🛠️ **LIBRERÍA**

**Descripción:** Funciones compartidas para todos los scripts.

**Cuándo usar:** Automáticamente (se importa en otros scripts)

**Funciones principales:**
- `log_success()`, `log_error()`, `log_warning()`
- `detect_module_name()`
- `find_env_file()`
- `validate_required_vars()`
- `check_script_updates()` ← Notifica actualizaciones

**Uso en scripts personalizados:**
```bash
#!/bin/bash
source ./scripts/utils.sh

log_info "Iniciando mi script"
MODULE=$(detect_module_name)
log_success "Módulo detectado: $MODULE"
```

---

### 4. `update-scripts.sh` 🔄 **SINCRONIZACIÓN**

**Descripción:** Actualiza scripts desde el framework.

**Cuándo usar:**
- Cuando `run-test.sh` avisa de actualizaciones
- Periódicamente (mensual)
- Después de actualizar framework

**Uso:**
```bash
# Actualizar (con confirmación)
./scripts/update-scripts.sh

# Solo verificar
./scripts/update-scripts.sh --check

# Actualizar sin preguntar
./scripts/update-scripts.sh --force
```

**Características:**
- ✅ Detecta framework automáticamente
- ✅ Muestra diferencias (diff) antes de actualizar
- ✅ Crea backups automáticos
- ✅ 3 estrategias de búsqueda del framework

---

### 5. `analyze-results.sh` 📊 **NUEVO**

**Descripción:** Analiza resultados de tests y genera métricas.

**Cuándo usar:**
- Después de ejecutar suite completa
- Para identificar tests lentos
- Para detectar tests flaky
- Para reportes al equipo

**Uso:**
```bash
# Análisis completo
./scripts/analyze-results.sh

# Top 20 tests más lentos
./scripts/analyze-results.sh --top 20

# Generar reporte HTML
./scripts/analyze-results.sh --output html
open build/reports/test-analysis.html

# Solo tests flaky
./scripts/analyze-results.sh --flaky
```

**Salida:**
```
📊 Resumen de Ejecución
────────────────────────────────────────
  ✓ Passed:   150 (93.8%)
  ✗ Failed:   5 (3.1%)
  ⊘ Skipped:  5 (3.1%)
  ──────────────────
  Total:      160 tests
  Duración:   5.2m

🐌 Tests Más Lentos (Top 10)
  45.3s  loginWithValidCredentials      LoginSteps
  32.1s  complexSearchScenario          SearchSteps
  ...
```

---

### 6. `pre-commit.sh` 🛡️ **NUEVO - AUTOMÁTICO**

**Descripción:** Validaciones automáticas antes de hacer commit.

**Cuándo usar:** Automáticamente en cada commit (una vez instalado)

**Instalación:**
```bash
# En el módulo
cp ../qa-scotia-frameworks/scripts/pre-commit.sh .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

**Validaciones:**
1. ✅ Smoke tests (@smoke)
2. ✅ Formato de código (Spotless)
3. ✅ Credenciales expuestas
4. ✅ Sintaxis Gherkin
5. ✅ Versiones SNAPSHOT
6. ✅ Archivos .env

**Flujo:**
```bash
$ git commit -m "feat: nueva feature"

🚀 Pre-Commit Hook - Scotia QA Framework

🚫 Verificando Archivos de Configuración
✓ No hay archivos .env staged

🔐 Detectando Credenciales Expuestas
✓ No se detectaron credenciales expuestas

📦 Verificando Versiones SNAPSHOT
✓ No hay versiones SNAPSHOT

📝 Validando Sintaxis Gherkin
✓ Sintaxis Gherkin correcta

💅 Validando Formato de Código
✓ Formato de código correcto

🧪 Ejecutando Smoke Tests
✓ Smoke tests pasaron

✅ Todas las validaciones pasaron
✓ Procediendo con el commit...
```

**Si falla:**
```bash
❌ Algunas validaciones fallaron

Opciones:
  1. Corrige los problemas y vuelve a intentar
  2. Usa --no-verify para saltar validaciones (NO recomendado)
```

**Saltar validaciones (EMERGENCIA):**
```bash
git commit --no-verify -m "fix: hotfix crítico"
```

---

### 7. `code-quality.sh` 🔍 **NUEVO**

**Descripción:** Analiza calidad del código y detecta vulnerabilidades.

**Cuándo usar:**
- Semanalmente (mantenimiento)
- Antes de releases importantes
- Cuando se agrega mucho código nuevo
- Para auditorías de seguridad

**Uso:**
```bash
# Análisis completo
./scripts/code-quality.sh

# Solo vulnerabilidades de seguridad
./scripts/code-quality.sh --security

# Solo código sin usar
./scripts/code-quality.sh --unused

# Solo comentarios problemáticos
./scripts/code-quality.sh --comments

# Generar reporte HTML
./scripts/code-quality.sh --report
open build/reports/code-quality-report.html
```

**Detecciones:**

**🔒 Seguridad:**
- SQL Injection
- XSS (Cross-Site Scripting)
- Credenciales hardcodeadas
- Random inseguro
- Criptografía débil (DES, MD5, SHA1)
- Path Traversal

**🗑️ Código sin Usar:**
- Imports no utilizados
- Variables locales sin usar

**💬 Comentarios:**
- TODOs/FIXMEs
- Código comentado
- System.out.println (debugging)

**📊 Complejidad:**
- Métodos > 50 líneas
- Complejidad ciclomática alta

**⚠️ Malas Prácticas:**
- Catch vacíos
- Magic numbers
- Strings hardcodeados largos

**Salida:**
```
🔍 Analizador de Calidad de Código

🔒 Análisis de Vulnerabilidades de Seguridad
────────────────────────────────────────────────────────────
⚠️  SQL Injection: 2 ocurrencia(s)
  📄 UserRepository.java:45
  📄 OrderService.java:128

✓ No se detectaron otras vulnerabilidades

Issues encontrados:
────────────────────────────────────────
  🔒 Seguridad:     2
  📊 Calidad:       5
  💅 Estilo:        12
  ──────────────────
  Total:          19
```

---

## 🎯 Orden de Ejecución Recomendado

### **Desarrollo Local (Diario)**

```
1. run-test.sh              ← Ejecutar tests
2. analyze-results.sh       ← Analizar resultados
3. [git add + commit]       ← pre-commit.sh se ejecuta automáticamente
```

### **Mantenimiento (Semanal)**

```
1. code-quality.sh          ← Detectar problemas
2. [Corregir issues]
3. run-test.sh              ← Validar cambios
4. [git commit]             ← pre-commit valida
```

### **Actualización (Cuando hay cambios)**

```
1. update-scripts.sh --check    ← Verificar actualizaciones
2. update-scripts.sh            ← Actualizar
3. run-test.sh                  ← Probar
```

### **Setup Nuevo Módulo (Una vez)**

```
1. create-module.sh         ← Crear módulo
2. [Configurar .env.local]
3. cp pre-commit.sh .git/hooks/  ← Instalar hook
4. run-test.sh              ← Probar
```

---

## 📈 Métricas de Valor

| Script | Tiempo Ahorrado | Beneficio Principal |
|--------|----------------|---------------------|
| `create-module.sh` | ~30 minutos → 30 segundos | Estandarización |
| `run-test.sh` | Manual → Automático | Eficiencia |
| `update-scripts.sh` | Manual → Notificación | Sincronización |
| `analyze-results.sh` | ~15 minutos análisis | Insights |
| `pre-commit.sh` | Previene problemas | Calidad preventiva |
| `code-quality.sh` | Detecta vulnerabilidades | Seguridad |

**Total:** ~1 hora de trabajo manual → ~5 minutos automático

---

## 🚀 Integración con Jenkins

Ejemplo de Jenkinsfile usando los scripts:

```groovy
pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                // Clonar framework + módulo
                dir('framework') {
                    git 'https://github.com/scotia/qa-scotia-frameworks.git'
                }
                dir('module') {
                    git 'https://github.com/scotia/qa-module-banking.git'
                }
            }
        }
        
        stage('Code Quality') {
            steps {
                dir('module') {
                    sh '../framework/scripts/code-quality.sh --security'
                }
            }
        }
        
        stage('Tests') {
            steps {
                dir('module') {
                    sh '../framework/scripts/run-test.sh'
                }
            }
        }
        
        stage('Analysis') {
            steps {
                dir('module') {
                    sh '../framework/scripts/analyze-results.sh --output html'
                    publishHTML([reportName: 'Test Analysis', reportDir: 'build/reports'])
                }
            }
        }
    }
}
```

---

## ✅ Checklist de Uso

### Para Nuevos QAs:

- [ ] Ejecutar `create-module.sh` para crear módulo
- [ ] Configurar `.env.local` con credenciales
- [ ] Instalar `pre-commit.sh` como hook
- [ ] Ejecutar primer test con `run-test.sh`
- [ ] Leer documentación completa

### Para QAs Existentes:

- [ ] Copiar `update-scripts.sh` a tu módulo
- [ ] Ejecutar `./scripts/update-scripts.sh`
- [ ] Instalar `pre-commit.sh` como hook
- [ ] Probar `analyze-results.sh` en tu último build
- [ ] Ejecutar `code-quality.sh` en tu código

### Para Leads:

- [ ] Configurar `pre-commit.sh` como obligatorio
- [ ] Integrar `code-quality.sh` en pipelines
- [ ] Revisar reportes de `analyze-results.sh` semanalmente
- [ ] Asegurar que todos usen `create-module.sh`

---

## 🎓 Recursos Adicionales

- **Documentación completa:** `scripts/README.md`
- **Guía de framework:** `documentacion/FRAMEWORK-GUIDE.md`
- **Quick Start:** `documentacion/QUICK-START.md`
- **Troubleshooting:** Consultar logs de cada script

---

**Versión:** 1.0.0  
**Fecha:** 28 de Noviembre de 2025  
**Autor:** Abel Venero  
**Framework:** Scotia QA Framework

