# ============================================================================
# Utilidades Compartidas para Scripts del Framework (PowerShell)
# ============================================================================
#
# Este archivo contiene funciones compartidas utilizadas por todos los scripts
# del framework Scotia QA en Windows/PowerShell.
#
# @author Abel Venero
# @version 1.0.0
# ============================================================================

# Versión de los scripts
$Script:SCRIPT_VERSION = "1.0.0"

# ============================================================================
# COLORES Y FORMATO
# ============================================================================

# Enum para colores de consola PowerShell
$Script:Colors = @{
    Success = "Green"
    Error   = "Red"
    Warning = "Yellow"
    Info    = "Cyan"
    Banner  = "Blue"
    Default = "White"
}

# ============================================================================
# FUNCIONES DE LOGGING
# ============================================================================

<#
.SYNOPSIS
    Imprimir mensaje de éxito
.PARAMETER Message
    Mensaje a mostrar
.EXAMPLE
    Log-Success "Operación completada"
#>
function Log-Success {
    param(
        [Parameter(Mandatory=$true)]
        [string]$Message
    )

    Write-Host "✓ $Message" -ForegroundColor $Script:Colors.Success
}

<#
.SYNOPSIS
    Imprimir mensaje de error
.PARAMETER Message
    Mensaje de error a mostrar
.EXAMPLE
    Log-Error "Falló la operación"
#>
function Log-Error {
    param(
        [Parameter(Mandatory=$true)]
        [string]$Message
    )

    Write-Host "✗ $Message" -ForegroundColor $Script:Colors.Error
}

<#
.SYNOPSIS
    Imprimir mensaje de advertencia
.PARAMETER Message
    Mensaje de advertencia a mostrar
.EXAMPLE
    Log-Warning "Configuración no encontrada"
#>
function Log-Warning {
    param(
        [Parameter(Mandatory=$true)]
        [string]$Message
    )

    Write-Host "⚠️  $Message" -ForegroundColor $Script:Colors.Warning
}

<#
.SYNOPSIS
    Imprimir mensaje de información
.PARAMETER Message
    Mensaje informativo a mostrar
.EXAMPLE
    Log-Info "Procesando archivos..."
#>
function Log-Info {
    param(
        [Parameter(Mandatory=$true)]
        [string]$Message
    )

    Write-Host "ℹ️  $Message" -ForegroundColor $Script:Colors.Info
}

<#
.SYNOPSIS
    Imprimir banner/título
.PARAMETER Title
    Título del banner
.EXAMPLE
    Log-Banner "Ejecutando Tests"
#>
function Log-Banner {
    param(
        [Parameter(Mandatory=$true)]
        [string]$Title
    )

    Write-Host ""
    Write-Host "═══════════════════════════════════════════" -ForegroundColor $Script:Colors.Banner
    Write-Host "  🚀 $Title" -ForegroundColor $Script:Colors.Banner
    Write-Host "═══════════════════════════════════════════" -ForegroundColor $Script:Colors.Banner
    Write-Host ""
}

<#
.SYNOPSIS
    Imprimir separador
.EXAMPLE
    Log-Separator
#>
function Log-Separator {
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor $Script:Colors.Banner
}

# ============================================================================
# DETECCIÓN DE ENTORNO
# ============================================================================

<#
.SYNOPSIS
    Detectar el sistema operativo
.OUTPUTS
    String: "Windows", "macOS", "Linux", o "Unknown"
.EXAMPLE
    $os = Get-OperatingSystem
#>
function Get-OperatingSystem {
    if ($IsWindows -or $env:OS -like "Windows*") {
        return "Windows"
    }
    elseif ($IsMacOS) {
        return "macOS"
    }
    elseif ($IsLinux) {
        return "Linux"
    }
    else {
        return "Unknown"
    }
}

<#
.SYNOPSIS
    Detectar si estamos en Jenkins
.OUTPUTS
    Boolean: $true si es Jenkins, $false si no
.EXAMPLE
    if (Test-IsJenkins) { ... }
#>
function Test-IsJenkins {
    return ($env:JENKINS_HOME -or $env:JENKINS_URL)
}

<#
.SYNOPSIS
    Detectar si estamos en CI/CD
.OUTPUTS
    Boolean: $true si es CI/CD, $false si no
.EXAMPLE
    if (Test-IsContinuousIntegration) { ... }
#>
function Test-IsContinuousIntegration {
    return ($env:CI -or $env:CONTINUOUS_INTEGRATION -or (Test-IsJenkins))
}

# ============================================================================
# DETECCIÓN DE MÓDULO
# ============================================================================

<#
.SYNOPSIS
    Auto-detectar el nombre del módulo desde múltiples fuentes
.DESCRIPTION
    Prioridad: 1) Variable entorno, 2) gradle.properties, 3) settings.gradle, 4) Directorio
.OUTPUTS
    String: Nombre del módulo detectado
.EXAMPLE
    $moduleName = Get-ModuleName
#>
function Get-ModuleName {
    $moduleName = ""

    # 1. Desde variable de entorno
    if ($env:MODULE_NAME) {
        $moduleName = $env:MODULE_NAME
        Log-Info "Módulo detectado desde variable de entorno: $moduleName"
        return $moduleName
    }

    # 2. Desde gradle.properties
    if (Test-Path "gradle.properties") {
        $content = Get-Content "gradle.properties" -ErrorAction SilentlyContinue
        $line = $content | Where-Object { $_ -match "^rootProject\.name" }
        if ($line) {
            $moduleName = ($line -split '=')[1].Trim()
            if ($moduleName) {
                Log-Info "Módulo detectado desde gradle.properties: $moduleName"
                return $moduleName
            }
        }
    }

    # 3. Desde settings.gradle
    if (Test-Path "settings.gradle") {
        $content = Get-Content "settings.gradle" -ErrorAction SilentlyContinue
        $line = $content | Where-Object { $_ -match "rootProject\.name" }
        if ($line) {
            if ($line -match "['\"]([^'\"]+)['\"]") {
                $moduleName = $matches[1]
                Log-Info "Módulo detectado desde settings.gradle: $moduleName"
                return $moduleName
            }
        }
    }

    # 4. Desde directorio actual
    $moduleName = Split-Path -Leaf (Get-Location)
    Log-Info "Módulo detectado desde directorio actual: $moduleName"
    return $moduleName
}

# ============================================================================
# BÚSQUEDA DE ARCHIVOS DE CONFIGURACIÓN
# ============================================================================

<#
.SYNOPSIS
    Buscar archivo de configuración .env
.DESCRIPTION
    Prioridad: .env.local > .env.${TEST_ENV} > .env
.OUTPUTS
    String: Ruta del archivo encontrado o cadena vacía
.EXAMPLE
    $envFile = Find-EnvFile
#>
function Find-EnvFile {
    $testEnv = if ($env:TEST_ENV) { $env:TEST_ENV } else { "local" }

    # 1. Buscar .env.local (máxima prioridad)
    if (Test-Path ".env.local") {
        Log-Info "Archivo de configuración encontrado: .env.local"
        return ".env.local"
    }

    # 2. Buscar .env.${TEST_ENV}
    $envSpecific = ".env.$testEnv"
    if (Test-Path $envSpecific) {
        Log-Info "Archivo de configuración encontrado: $envSpecific"
        return $envSpecific
    }

    # 3. Buscar .env genérico
    if (Test-Path ".env") {
        Log-Info "Archivo de configuración encontrado: .env"
        return ".env"
    }

    # No se encontró archivo
    Log-Warning "No se encontró archivo de configuración .env"
    return ""
}

# ============================================================================
# CARGA DE VARIABLES DE ENTORNO
# ============================================================================

<#
.SYNOPSIS
    Cargar variables desde archivo .env
.PARAMETER EnvFile
    Ruta al archivo .env
.EXAMPLE
    Import-EnvFile ".env.local"
#>
function Import-EnvFile {
    param(
        [Parameter(Mandatory=$true)]
        [string]$EnvFile
    )

    if (-not (Test-Path $EnvFile)) {
        Log-Error "Archivo no encontrado: $EnvFile"
        return $false
    }

    Log-Info "Cargando variables desde: $EnvFile"

    Get-Content $EnvFile -ErrorAction SilentlyContinue | ForEach-Object {
        $line = $_.Trim()

        # Ignorar líneas vacías y comentarios
        if ($line -and -not $line.StartsWith("#")) {
            # Dividir en nombre=valor
            $parts = $line -split '=', 2
            if ($parts.Count -eq 2) {
                $name = $parts[0].Trim()
                $value = $parts[1].Trim()

                # Remover comillas si existen
                $value = $value -replace '^["\']|["\']$', ''

                # Establecer variable de entorno
                [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
            }
        }
    }

    Log-Success "Variables cargadas exitosamente"
    return $true
}

# ============================================================================
# VALIDACIÓN DE VARIABLES
# ============================================================================

<#
.SYNOPSIS
    Validar que variables requeridas estén definidas
.PARAMETER Variables
    Array de nombres de variables a validar
.OUTPUTS
    Boolean: $true si todas están definidas, $false si falta alguna
.EXAMPLE
    $valid = Test-RequiredVariables @("DB_URL", "DB_USER", "DB_PASS")
#>
function Test-RequiredVariables {
    param(
        [Parameter(Mandatory=$true)]
        [string[]]$Variables
    )

    $missingVars = @()
    $allValid = $true

    foreach ($var in $Variables) {
        $value = [System.Environment]::GetEnvironmentVariable($var)

        if (-not $value) {
            $missingVars += $var
            $allValid = $false
            Log-Error "$var no está configurada"
        }
        else {
            Log-Success "$var configurada"
        }
    }

    if (-not $allValid) {
        Write-Host ""
        Log-Error "Faltan variables requeridas: $($missingVars -join ', ')"
        return $false
    }

    return $true
}

# ============================================================================
# CONSTRUCCIÓN DE COMANDOS GRADLE
# ============================================================================

<#
.SYNOPSIS
    Construir argumentos de Gradle desde variables de entorno
.OUTPUTS
    Array: Argumentos -D para Gradle
.EXAMPLE
    $gradleProps = Get-GradleProperties
#>
function Get-GradleProperties {
    $props = @()

    # Lista de variables estándar del framework
    $vars = @(
        "DB_URL",
        "DB_USER",
        "DB_PASS",
        "DB_DRIVER",
        "TEST_ENV",
        "API_BASE_URL",
        "API_TOKEN",
        "WEB_BASE_URL",
        "APP_PATH",
        "BROWSER",
        "HEADLESS",
        "PLATFORM"
    )

    # Construir propiedades para cada variable definida
    foreach ($var in $vars) {
        $value = [System.Environment]::GetEnvironmentVariable($var)
        if ($value) {
            $props += "-D$var=$value"
        }
    }

    return $props
}

# ============================================================================
# UTILIDADES DE ARCHIVOS
# ============================================================================

<#
.SYNOPSIS
    Verificar si existe Gradle Wrapper
.OUTPUTS
    Boolean: $true si existe, $false si no
.EXAMPLE
    if (Test-HasGradleWrapper) { ... }
#>
function Test-HasGradleWrapper {
    return (Test-Path "gradlew.bat") -or (Test-Path "gradlew")
}

<#
.SYNOPSIS
    Obtener comando Gradle apropiado (wrapper o instalación global)
.OUTPUTS
    String: Comando de Gradle a usar
.EXAMPLE
    $gradleCmd = Get-GradleCommand
#>
function Get-GradleCommand {
    if (Test-HasGradleWrapper) {
        # En Windows, usar .bat
        if (Test-Path "gradlew.bat") {
            return ".\gradlew.bat"
        }
        return ".\gradlew"
    }
    else {
        return "gradle"
    }
}

# ============================================================================
# VALIDACIÓN DE DEPENDENCIAS
# ============================================================================

<#
.SYNOPSIS
    Verificar que un comando existe
.PARAMETER Command
    Comando a verificar
.PARAMETER Name
    Nombre descriptivo del comando
.OUTPUTS
    Boolean: $true si existe, $false si no
.EXAMPLE
    Test-Command "java" "Java JDK"
#>
function Test-Command {
    param(
        [Parameter(Mandatory=$true)]
        [string]$Command,

        [Parameter(Mandatory=$true)]
        [string]$Name
    )

    $found = Get-Command $Command -ErrorAction SilentlyContinue

    if (-not $found) {
        Log-Error "$Name no está instalado (comando: $Command)"
        return $false
    }

    Log-Success "$Name encontrado"
    return $true
}

<#
.SYNOPSIS
    Verificar dependencias del framework
.OUTPUTS
    Boolean: $true si todas las dependencias están, $false si falta alguna
.EXAMPLE
    if (Test-FrameworkDependencies) { ... }
#>
function Test-FrameworkDependencies {
    $allOk = $true

    Log-Info "Verificando dependencias..."
    Write-Host ""

    if (-not (Test-Command "java" "Java JDK")) {
        $allOk = $false
    }

    if (-not (Test-HasGradleWrapper) -and -not (Test-Command "gradle" "Gradle")) {
        $allOk = $false
    }

    Write-Host ""

    if (-not $allOk) {
        Log-Error "Faltan dependencias requeridas"
        return $false
    }

    Log-Success "Todas las dependencias están instaladas"
    return $true
}

# ============================================================================
# FUNCIONES DE AYUDA
# ============================================================================

<#
.SYNOPSIS
    Mostrar versión de Java
.OUTPUTS
    String: Versión de Java
.EXAMPLE
    Get-JavaVersion
#>
function Get-JavaVersion {
    $javaVersion = & java -version 2>&1 | Select-Object -First 1
    return $javaVersion
}

<#
.SYNOPSIS
    Mostrar versión de Gradle
.OUTPUTS
    String: Versión de Gradle
.EXAMPLE
    Get-GradleVersion
#>
function Get-GradleVersion {
    $gradleCmd = Get-GradleCommand
    $gradleVersion = & $gradleCmd --version 2>&1 | Where-Object { $_ -match "Gradle" } | Select-Object -First 1
    return $gradleVersion
}

# ============================================================================
# MANEJO DE ERRORES
# ============================================================================

<#
.SYNOPSIS
    Configurar $ErrorActionPreference para el script
.EXAMPLE
    Set-StrictMode
#>
function Set-StrictMode {
    $ErrorActionPreference = "Stop"
    $ProgressPreference = "SilentlyContinue"
}

# ============================================================================
# EXPORTAR FUNCIONES (PowerShell las exporta automáticamente)
# ============================================================================

# En PowerShell, todas las funciones definidas en un módulo/script
# están disponibles automáticamente cuando se importa con `. .\utils.ps1`

Write-Verbose "utils.ps1 v$Script:SCRIPT_VERSION cargado correctamente"

