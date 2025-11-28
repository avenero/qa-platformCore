# 🚀 QA Scotia Automation Framework

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Gradle](https://img.shields.io/badge/Gradle-8.14-blue.svg)](https://gradle.org/)
[![Selenium](https://img.shields.io/badge/Selenium-4.27.0-green.svg)](https://www.selenium.dev/)
[![Cucumber](https://img.shields.io/badge/Cucumber-7.18.0-brightgreen.svg)](https://cucumber.io/)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)](LICENSE)

> Framework de automatización de pruebas modular, robusto y extensible para API, Web y Mobile testing con arquitectura por capas y BDD.

---

## 📑 Índice

- [🎯 Visión General](#-visión-general)
- [✨ Características Principales](#-características-principales)
- [🏗️ Arquitectura](#️-arquitectura)
- [🚀 Quick Start](#-quick-start)
- [📦 Módulos del Framework](#-módulos-del-framework)
- [🔧 Compatibilidad de Versiones](#-compatibilidad-de-versiones)
- [📚 Documentación](#-documentación)
- [🤝 Contribución](#-contribución)
- [📄 Licencia](#-licencia)

---

## 🎯 Visión General

El **QA Scotia Automation Framework** es un framework de testing enterprise-grade diseñado para automatización de pruebas en múltiples plataformas (API REST, Web UI, Mobile) usando Behavior-Driven Development (BDD) con Cucumber.

### 🎪 ¿Por qué este Framework?

- ✅ **Arquitectura Multicapa**: Separación clara entre `common`, `api-core`, `web-core` y `mobile-core`
- ✅ **Sin Spring Boot**: Más rápido, liviano y con menos dependencias
- ✅ **Genérico y Reutilizable**: No contiene lógica de negocio específica
- ✅ **Type-Safe**: Validaciones en tiempo de compilación
- ✅ **BDD Nativo**: Cucumber con Gherkin en español e inglés
- ✅ **Seguro**: Sanitización automática de datos sensibles en logs
- ✅ **ScenarioContext**: Compartir datos entre capas (API → Web → Mobile)
- ✅ **Extensible**: Fácil agregar nuevos capabilities

### 🎭 ¿Para Quién es este Framework?

- **QA Engineers** que automatizan pruebas
- **Developers** que practican TDD/BDD
- **Tech Leads** que diseñan estrategias de testing
- **Arquitectos** que buscan un framework robusto y mantenible

---

## ✨ Características Principales

### 🔌 Multi-Protocolo
- **REST APIs** - Testing completo de servicios REST
- **Web UI** - Selenium WebDriver con Page Object Model
- **Mobile** - Appium para iOS y Android

### 🥒 BDD con Cucumber
- Steps predefinidos para escenarios comunes
- Gherkin en español e inglés
- Hooks automáticos para setup/teardown
- Reports nativos de Cucumber

### 🔗 ScenarioContext - Compartir Datos Entre Capas
```gherkin
# API obtiene token
Given ejecuto login API y guardo token como "authToken"

# Web usa el token del API
When navego a dashboard usando token "{authToken}"

# Mobile valida data del Web
Then verifico en mobile que el usuario sea "{username}"
```

### 📊 Logging Inteligente
- Sistema de logging multinivel (DEBUG, INFO, WARN, ERROR)
- Contexto automático por módulo y test
- Masking de datos sensibles (passwords, tokens)
- Logs en consola (con colores) y archivo

### ⏱️ Waits Inteligentes
- Esperas explícitas configurables
- Polling automático hasta que elementos aparezcan
- Sin hardcode de tiempos
- Configuración centralizada

### 🎨 Page Object Model Mejorado
- BasePage con métodos útiles
- Component Pattern para elementos reutilizables
- Factory methods para crear componentes
- Localizadores por módulo (no en el framework)

---

## 🏗️ Arquitectura

### Diagrama de Capas

```
┌─────────────────────────────────────────────────────────────────┐
│                    MÓDULOS CONSUMIDORES                         │
│              (Proyectos Específicos de Negocio)                 │
│   qa-autos • qa-banking • qa-mobile-app • qa-integration        │
└─────────────────────────────────────────────────────────────────┘
                              ↓ dependen de
┌─────────────────────────────────────────────────────────────────┐
│              FRAMEWORKS ESPECIALIZADOS (CORE)                   │
├─────────────────┬──────────────────┬──────────────────────────┤
│  📱 mobile-core │   🌐 api-core    │      💻 web-core          │
│                 │                  │                           │
│ • Appium        │ • REST Testing   │ • Selenium WebDriver      │
│ • Device Mgmt   │ • HTTP Client    │ • Page Object Model       │
│ • Native Apps   │ • Validations    │ • Cross-browser           │
└─────────────────┴──────────────────┴──────────────────────────┘
                              ↓ extienden
┌─────────────────────────────────────────────────────────────────┐
│                      🔧 COMMON (BASE)                           │
│  • Interfaces & Contracts    • Logging System                  │
│  • HTTP Client Base          • ScenarioContext                 │
│  • Cucumber Hooks            • Security Utils                  │
│  • Database Support          • Data Utilities                  │
└─────────────────────────────────────────────────────────────────┘
```

### Principios Arquitectónicos

1. **Separación de Responsabilidades**: Cada capa tiene una responsabilidad clara
2. **Dependency Inversion**: Capas superiores dependen de abstracciones, no implementaciones
3. **Open/Closed**: Abierto a extensión, cerrado a modificación
4. **DRY**: No repetir código entre módulos
5. **Configuración sobre Hardcode**: Timeouts, URLs, etc. en archivos de configuración

---

## 🚀 Quick Start

### Prerrequisitos

- ☕ **Java 21** o superior
- 🐘 **Gradle 8.14** (incluido via wrapper)
- 🌐 **Git**
- 💻 **IDE** (IntelliJ IDEA recomendado)

### Instalación

#### 1️⃣ Clonar el Repositorio

```bash
git clone https://github.com/scotia-qa/qa-scotia-frameworks.git
cd qa-scotia-frameworks
```

#### 2️⃣ Compilar y Publicar Localmente

```bash
# Compilar todo el framework
./gradlew build

# Publicar a Maven Local (~/.m2/repository)
./gradlew publishToMavenLocal
```

#### 3️⃣ Crear tu Primer Módulo de Pruebas

```bash
# Crear estructura de proyecto
mkdir -p qa-mi-proyecto/src/test/{java,resources}
cd qa-mi-proyecto
```

**`build.gradle`:**
```groovy
plugins {
    id 'java'
}

group = 'com.scotia.qa'
version = '1.0.0'

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Usar solo las capas que necesites
    implementation 'com.scotia.qa:common:1.0.2'
    implementation 'com.scotia.qa:api-core:1.0.2'
    implementation 'com.scotia.qa:web-core:1.0.2'
    // implementation 'com.scotia.qa:mobile-core:1.0.2'
}
```

#### 4️⃣ Crear tu Primera Feature

**`src/test/resources/features/login.feature`:**
```gherkin
# language: es
Característica: Login en aplicación Banking

  Escenario: Login exitoso con credenciales válidas
    Dado que navego a la URL "https://banking.example.com/login"
    Cuando ingreso el texto "usuario123" en el elemento "username"
    Y ingreso el texto "password123" en el elemento "password"
    Y presiono el botón "loginButton"
    Entonces verifico si existe el elemento "welcomeMessage"
    Y verifico que el texto en "welcomeMessage" contenga el texto "Bienvenido"
```

#### 5️⃣ Ejecutar Tests

```bash
./gradlew test
```

---

## 📦 Módulos del Framework

### 🔧 [common](./common/README.md)
**Capa base** con componentes compartidos por todos los frameworks.

**Incluye:**
- Interfaces y contratos
- HTTP Client base (Unirest)
- Sistema de logging
- ScenarioContext para compartir datos
- Cucumber Hooks
- Utilidades de seguridad

**Versión:** 1.0.2

---

### 🌐 [api-core](./api-core/README.md)
**Framework especializado** para testing de APIs REST.

**Incluye:**
- Steps predefinidos para APIs REST
- HTTP Client especializado
- Validaciones de JSON/XML
- Soporte para OAuth, JWT
- Steps para compartir datos con Web/Mobile

**Versión:** 1.0.2

**Ejemplo de uso:**
```gherkin
Given el host "https://api.banking.com" mas el contexto "/auth/login"
And agrego el header "Content-Type" con valor "application/json"
And agrego el request
  """
  {"username": "test", "password": "pass123"}
  """
When ejecuto la consulta con el metodo "POST"
Then valido que el codigo de respuesta del servicio sea 200
And obtengo el campo "token" del objeto "data" y lo guardo como "authToken"
```

---

### 💻 [web-core](./web-core/README.md)
**Framework especializado** para testing de aplicaciones Web UI.

**Incluye:**
- Steps predefinidos para interacciones Web
- WebDriver management (Chrome, Firefox, Edge, Safari)
- Page Object Model + Component Pattern
- Waits inteligentes
- Screenshot automático en fallos
- Soporte para Selenium Grid

**Versión:** 1.0.2

**Ejemplo de uso:**
```gherkin
Given actualizo URL en el navegador "https://banking.example.com"
When ingreso el texto "{authToken}" en el elemento "tokenField"
And presiono el botón "submitButton"
Then verifico si existe el elemento "dashboard"
```

---

### 📱 [mobile-core](./mobile-core/README.md)
**Framework especializado** para testing de aplicaciones Mobile (iOS/Android).

**Incluye:**
- Steps predefinidos para Mobile
- Appium integration
- Device management
- Gestures support (swipe, tap, long-press)
- Screenshot support

**Versión:** 1.0.2

---

## 🔧 Compatibilidad de Versiones

### Matriz de Tecnologías

| Tecnología | Versión | Estado | Notas |
|------------|---------|--------|-------|
| **Java** | 21 LTS | ✅ Requerido | Versión mínima soportada |
| **Gradle** | 8.14 | ✅ Incluido | Via wrapper (gradlew) |
| **Selenium WebDriver** | 4.27.0 | ✅ Última | Para web-core |
| **Appium Java Client** | 8.6.0 | ✅ Última | Para mobile-core |
| **Cucumber** | 7.18.0 | ✅ Última | BDD Framework |
| **JUnit Platform** | 1.10.0 | ✅ Última | Test Runner |
| **Unirest** | 4.4.4 | ✅ Última | HTTP Client |
| **Jackson** | 2.15.2 | ✅ Última | JSON Processing |
| **Logback** | 1.5.13 | ✅ Última | Logging |
| **AssertJ** | 3.24.2 | ✅ Última | Assertions |
| **WebDriverManager** | 5.6.2 | ✅ Última | Driver management |

### Navegadores Soportados (web-core)

| Navegador | Versión Mínima | Driver | Estado |
|-----------|----------------|--------|--------|
| **Chrome** | 120+ | ChromeDriver | ✅ Completo |
| **Firefox** | 115+ | GeckoDriver | ✅ Completo |
| **Edge** | 120+ | EdgeDriver | ✅ Completo |
| **Safari** | 17+ | SafariDriver | ✅ Completo |

### Plataformas Mobile Soportadas (mobile-core)

| Plataforma | Versión Mínima | Estado |
|------------|----------------|--------|
| **Android** | 8.0 (API 26) | ✅ Soportado |
| **iOS** | 14.0 | ✅ Soportado |

---

## 📚 Documentación

### 📘 Documentos Principales (Consolidados)

| Documento | Descripción | Audiencia |
|-----------|-------------|-----------|
| **[FRAMEWORK-GUIDE.md](documentacion/FRAMEWORK-GUIDE.md)** | 📖 Guía completa consolidada: arquitectura, capas, stack tecnológico, troubleshooting, contribución | Todos |
| **[QUICK-START.md](documentacion/QUICK-START.md)** | 🚀 Guía paso a paso para configurar y ejecutar tu primer test | Nuevos usuarios |

### 📖 Documentación por Capa

| Documento | Descripción |
|-----------|-------------|
| **[common/README.md](./common/README.md)** | 🔧 Capa base: Config, ScenarioContext, DB, HTTP, Logging, Test Data Finder |
| **[api-core/README.md](./api-core/README.md)** | 🌐 API Testing: +40 steps de Cucumber, validaciones, auth |
| **[web-core/README.md](./web-core/README.md)** | 💻 Web Testing: +50 steps, WebDriver, estrategia Module-First |
| **[mobile-core/README.md](./mobile-core/README.md)** | 📱 Mobile Testing: +45 steps, Appium, Android/iOS |
| **[scripts/README.md](./scripts/README.md)** | 📜 Scripts de automatización, CI/CD, Jenkins |

> **Nota:** Toda la documentación sobre troubleshooting, contribución y guías adicionales ha sido consolidada en **FRAMEWORK-GUIDE.md**

---

## 🤝 Contribución

¿Quieres contribuir al framework? ¡Excelente! 

1. Lee la [Guía de Contribución en FRAMEWORK-GUIDE.md](./documentacion/FRAMEWORK-GUIDE.md#-contribución)
2. Crea un branch desde `develop`
3. Haz tus cambios siguiendo las convenciones
4. Crea un Pull Request

### Convenciones

- ✅ Código en **inglés**
- ✅ Comentarios y documentación en **español**
- ✅ Steps de Cucumber en **español**
- ✅ Commits siguiendo [Conventional Commits](https://www.conventionalcommits.org/)

---

## 👥 Equipo

**Desarrollado por:** QA Team - Scotia Bank  
**Autor Principal:** Abel Venero  
**Mantenedores:** QA Engineering Team

---

## 📄 Licencia

Copyright © 2025 Scotia Bank. Todos los derechos reservados.

Este framework es propiedad de Scotia Bank y su uso está restringido a proyectos autorizados de la organización.

---

## 🌟 Estado del Proyecto

| Capa | Estado | Cobertura Tests | Build |
|------|--------|-----------------|-------|
| **common** | ✅ Estable | 85% | ![Build](https://img.shields.io/badge/build-passing-brightgreen) |
| **api-core** | ✅ Estable | 80% | ![Build](https://img.shields.io/badge/build-passing-brightgreen) |
| **web-core** | ✅ Estable | 90% | ![Build](https://img.shields.io/badge/build-passing-brightgreen) |
| **mobile-core** | ⚠️ Beta | 70% | ![Build](https://img.shields.io/badge/build-passing-brightgreen) |

---

## 📞 Soporte

¿Necesitas ayuda?

- 📧 Email: qa-team@scotiabank.com
- 💬 Slack: #qa-automation
- 📝 Issues: [GitHub Issues](https://github.com/scotia-qa/qa-scotia-frameworks/issues)

---

<div align="center">

**[⬆ Volver arriba](#-qa-scotia-automation-framework)**

Hecho con ❤️ por el QA Team de Scotia Bank

</div>

