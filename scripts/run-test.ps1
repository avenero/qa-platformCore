# ============================================================================
# Scotia QA Framework - Script de Testing Genérico (PowerShell)
# VERSION: 1.0.0
# ============================================================================
#
# Script unificado para configuración y ejecución de tests en Windows.
# Auto-detecta el módulo, configuración y ejecuta tests con Gradle.
#
# CARACTERÍSTICAS:
#   ✅ Auto-detección del módulo actual
#   ✅ Búsqueda automática de archivos .env
#   ✅ Soporte para variables de entorno (Jenkins/CI-CD)
#   ✅ Modo interactivo para configuración
#   ✅ Sin hardcodeo de nombres o rutas
#
# USO:
#   .\run-test.ps1                          # Auto-detecta y ejecuta tests
#   .\run-test.ps1 -Setup                   # Modo configuración interactiva
#   .\run-test.ps1 -Env qa                  # Usar ambiente específico
#   .\run-test.ps1 -Tags "@smoke"           # Ejecutar tags específicos
#   .\run-test.ps1 -GradleArgs "clean test --info"  # Comando personalizado
#
# JENKINS/CI-CD:
#   $env:TEST_ENV="qa"; $env:DB_URL="..."; .\run-test.ps1
#
# @author Abel Venero
# @version 1.0.0
# ============================================================================

[CmdletBinding()]
param(
    [Parameter(Mandatory=$false)]
    [switch]$Help,

    [Parameter(Mandatory=$false)]
    [switch]$Setup,

    [Parameter(Mandatory=$false)]
    [string]$Env = "",

    [Parameter(Mandatory=$false)]
    [string]$Tags = "",

    [Parameter(Mandatory=$false)]
    [string]$Module = "",

    [Parameter(Mandatory=$false)]
    [string]$EnvFile = "",

    [Parameter(Mandatory=$false)]
    [switch]$Verbose,

    [Parameter(Mandatory=$false)]
    [switch]$DryRun,

    [Parameter(Mandatory=$false, ValueFromRemainingArguments=$true)]
    [string[]]$GradleArgs
)

# Configurar error handling
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

# ============================================================================
# CARGAR UTILIDADES
# ============================================================================

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$UTILS_PATH = Join-Path $SCRIPT_DIR "utils.ps1"

if (Test-Path $UTILS_PATH) {
    . $UTILS_PATH
}
else {
    Write-Host "✗ No se encontró utils.ps1. Ejecuta sync-utils.ps1 primero." -ForegroundColor Red
    exit 1
}

# ============================================================================
# VARIABLES GLOBALES
# ============================================================================

$Script:MODULE_NAME = ""
$Script:TEST_ENV = if ($env:TEST_ENV) { $env:TEST_ENV } else { "local" }
$Script:ENV_FILE_PATH = ""
$Script:GRADLE_COMMAND_ARGS = if ($GradleArgs) { $GradleArgs -join " " } else { "clean test" }

# ============================================================================
# FUNCIONES PRINCIPALES
# ============================================================================

<#
.SYNOPSIS
    Mostrar ayuda del script
#>
function Show-Help {
    Write-Host ""
    Write-Host "Scotia QA Framework - Test Runner" -ForegroundColor Blue
    Write-Host ""
    Write-Host "USO:" -ForegroundColor Yellow
    Write-Host "    .\run-test.ps1 [OPCIONES]"
    Write-Host ""
    Write-Host "OPCIONES:" -ForegroundColor Yellow
    Write-Host "    -Help                   Mostrar esta ayuda"
    Write-Host "    -Setup                  Modo configuración interactiva"
    Write-Host "    -Env <ENV>              Usar ambiente específico (qa, uat, prod)"
    Write-Host "    -Tags <TAGS>            Ejecutar tags específicos de Cucumber"
    Write-Host "    -Module <NAME>          Especificar nombre del módulo"
    Write-Host "    -EnvFile <FILE>         Usar archivo .env específico"
    Write-Host "    -Verbose                Modo verbose (Gradle --info)"
    Write-Host "    -DryRun                 Mostrar comandos sin ejecutar"
    Write-Host ""
    Write-Host "EJEMPLOS:" -ForegroundColor Yellow
    Write-Host "    # Configuración inicial (interactiva)" -ForegroundColor Cyan
    Write-Host "    .\run-test.ps1 -Setup"
    Write-Host ""
    Write-Host "    # Ejecución simple (auto-detección)" -ForegroundColor Cyan
    Write-Host "    .\run-test.ps1"
    Write-Host ""
    Write-Host "    # Usar ambiente QA" -ForegroundColor Cyan
    Write-Host "    .\run-test.ps1 -Env qa"
    Write-Host ""
    Write-Host "    # Ejecutar solo tests con tag @smoke" -ForegroundColor Cyan
    Write-Host '    .\run-test.ps1 -Tags "@smoke"'
    Write-Host ""
    Write-Host "    # Comando Gradle personalizado" -ForegroundColor Cyan
    Write-Host "    .\run-test.ps1 -GradleArgs 'clean test --info'"
    Write-Host ""
    Write-Host "    # Desde Jenkins (usando variables de entorno)" -ForegroundColor Cyan
    Write-Host '    $env:TEST_ENV="qa"; $env:DB_URL="jdbc:..."; .\run-test.ps1'
    Write-Host ""
    Write-Host "VARIABLES DE ENTORNO SOPORTADAS:" -ForegroundColor Yellow
    Write-Host "    MODULE_NAME             Nombre del módulo"
    Write-Host "    TEST_ENV                Ambiente (qa, uat, prod)"
    Write-Host "    DB_URL                  URL de base de datos"
    Write-Host "    DB_USER                 Usuario de BD"
    Write-Host "    DB_PASS                 Password de BD"
    Write-Host "    API_BASE_URL            URL base de API"
    Write-Host "    WEB_BASE_URL            URL base de aplicación web"
    Write-Host ""
}

<#
.SYNOPSIS
    Modo configuración interactiva
#>
function Start-SetupMode {
    Log-Banner "Configuración Interactiva"

    Write-Host "Este asistente te ayudará a configurar las variables de entorno." -ForegroundColor Cyan
    Write-Host ""

    # Detectar módulo
    $Script:MODULE_NAME = Get-ModuleName
    Log-Info "Módulo detectado: $Script:MODULE_NAME"
    Write-Host ""

    # Preguntar por ambiente
    Write-Host "¿Qué ambiente deseas configurar?" -ForegroundColor Cyan
    Write-Host "  1) Local (desarrollo)"
    Write-Host "  2) QA"
    Write-Host "  3) UAT"
    Write-Host "  4) PROD"
    $envChoice = Read-Host "Opción [1]"

    switch ($envChoice) {
        "2" { $Script:TEST_ENV = "qa" }
        "3" { $Script:TEST_ENV = "uat" }
        "4" { $Script:TEST_ENV = "prod" }
        default { $Script:TEST_ENV = "local" }
    }

    $Script:ENV_FILE_PATH = ".env.$Script:TEST_ENV"

    Write-Host ""
    Log-Info "Configurando ambiente: $Script:TEST_ENV"
    Log-Info "Archivo: $Script:ENV_FILE_PATH"
    Write-Host ""

    # Configurar variables de BD
    Log-Separator
    Write-Host "Configuración de Base de Datos" -ForegroundColor Yellow
    Log-Separator
    Write-Host ""

    $dbUrl = Read-Host "DB URL [jdbc:oracle:thin:@//host:port/service]"
    $dbUser = Read-Host "DB User"
    $dbPassSecure = Read-Host "DB Password" -AsSecureString
    $dbPass = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($dbPassSecure)
    )
    Write-Host ""

    # Configurar variables de API (opcional)
    Write-Host ""
    Log-Separator
    Write-Host "Configuración de API (opcional)" -ForegroundColor Yellow
    Log-Separator
    Write-Host ""

    $apiUrl = Read-Host "API Base URL [Enter para omitir]"

    # Crear archivo .env
    $envContent = @"
# ============================================================================
# Configuración de Entorno - $Script:MODULE_NAME
# ============================================================================
# Ambiente: $Script:TEST_ENV
# Generado: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
# ============================================================================

# ====================================================================
# AMBIENTE
# ====================================================================
TEST_ENV=$Script:TEST_ENV

# ====================================================================
# BASE DE DATOS
# ====================================================================
DB_URL=$dbUrl
DB_USER=$dbUser
DB_PASS=$dbPass
DB_DRIVER=oracle.jdbc.OracleDriver

"@

    # Agregar API si se configuró
    if ($apiUrl) {
        $envContent += @"

# ====================================================================
# API
# ====================================================================
API_BASE_URL=$apiUrl

"@
    }

    # Agregar instrucciones
    $envContent += @"

# ====================================================================
# INSTRUCCIONES
# ====================================================================
# 1. NO commitear este archivo (debe estar en .gitignore)
# 2. Para usar manualmente: Get-Content $Script:ENV_FILE_PATH | ForEach-Object { ... }
# 3. Para tests: .\run-test.ps1
# ====================================================================
"@

    Set-Content -Path $Script:ENV_FILE_PATH -Value $envContent -Encoding UTF8

    Write-Host ""
    Log-Success "Archivo $Script:ENV_FILE_PATH creado exitosamente"
    Write-Host ""

    # Preguntar si ejecutar tests ahora
    $runNow = Read-Host "¿Deseas ejecutar los tests ahora? (s/N)"

    if ($runNow -match "^[Ss]$") {
        Write-Host ""
        Log-Info "Ejecutando tests..."
        Write-Host ""
        return $true  # Continuar con ejecución
    }
    else {
        Write-Host ""
        Log-Info "Para ejecutar tests más tarde, usa: .\run-test.ps1"
        exit 0
    }
}

<#
.SYNOPSIS
    Ejecutar tests con Gradle
#>
function Invoke-Tests {
    $gradleCmd = Get-GradleCommand
    $gradleProps = Get-GradleProperties

    Log-Separator
    Write-Host "Comando a ejecutar:" -ForegroundColor Cyan
    Write-Host ""

    # Construir comando completo
    $fullCommand = "$gradleCmd $Script:GRADLE_COMMAND_ARGS $($gradleProps -join ' ')"

    # Mostrar comando (ocultando valores sensibles)
    $safeCommand = $fullCommand -replace '-DDB_PASS=[^ ]*', '-DDB_PASS=***HIDDEN***'
    $safeCommand = $safeCommand -replace '-DAPI_TOKEN=[^ ]*', '-DAPI_TOKEN=***HIDDEN***'

    Write-Host $safeCommand -ForegroundColor Yellow
    Write-Host ""
    Log-Separator
    Write-Host ""

    # Ejecutar o simular (dry-run)
    if ($DryRun) {
        Log-Warning "Modo DRY-RUN: No se ejecutará el comando"
        return
    }

    Log-Success "🚀 Ejecutando tests..."
    Write-Host ""

    # Ejecutar comando
    try {
        # Separar comando base y argumentos
        $cmdParts = $fullCommand -split ' ', 2
        $executable = $cmdParts[0]
        $arguments = if ($cmdParts.Length -gt 1) { $cmdParts[1] } else { "" }

        # Ejecutar con manejo de salida
        if ($arguments) {
            & $executable $arguments.Split(' ')
        }
        else {
            & $executable
        }

        $exitCode = $LASTEXITCODE

        Write-Host ""
        Log-Separator

        if ($exitCode -eq 0) {
            Log-Success "Tests ejecutados exitosamente"
        }
        else {
            Log-Error "Tests fallaron con código: $exitCode"
            exit $exitCode
        }
    }
    catch {
        Write-Host ""
        Log-Separator
        Log-Error "Error ejecutando tests: $_"
        exit 1
    }
}

# ============================================================================
# FLUJO PRINCIPAL
# ============================================================================

function Main {
    # Mostrar ayuda si se solicitó
    if ($Help) {
        Show-Help
        exit 0
    }

    # Banner inicial
    Log-Banner "Scotia QA Framework - Test Runner"

    # Mostrar información del sistema
    $osInfo = Get-OperatingSystem
    Log-Info "Sistema: $osInfo"

    if (Test-IsContinuousIntegration) {
        Log-Info "Entorno: CI/CD"
    }
    else {
        Log-Info "Entorno: Local"
    }
    Write-Host ""

    # Procesar parámetros
    if ($Env) {
        $Script:TEST_ENV = $Env
    }

    if ($Tags) {
        $Script:GRADLE_COMMAND_ARGS += " -Dcucumber.filter.tags=`"$Tags`""
    }

    if ($Module) {
        $Script:MODULE_NAME = $Module
    }

    if ($Verbose) {
        $Script:GRADLE_COMMAND_ARGS += " --info"
    }

    # Modo setup interactivo
    if ($Setup) {
        $continueExecution = Start-SetupMode
        if (-not $continueExecution) {
            exit 0
        }
    }

    # Auto-detectar módulo si no se especificó
    if (-not $Script:MODULE_NAME) {
        $Script:MODULE_NAME = Get-ModuleName
    }

    Log-Info "Módulo: $Script:MODULE_NAME"
    Log-Info "Ambiente: $Script:TEST_ENV"
    Write-Host ""

    # Verificar dependencias
    if (-not (Test-FrameworkDependencies)) {
        exit 1
    }

    Write-Host ""

    # Buscar archivo de configuración si no se especificó
    if (-not $EnvFile) {
        $Script:ENV_FILE_PATH = Find-EnvFile
    }
    else {
        $Script:ENV_FILE_PATH = $EnvFile
    }

    # Cargar archivo de configuración si existe
    if ($Script:ENV_FILE_PATH -and (Test-Path $Script:ENV_FILE_PATH)) {
        Import-EnvFile -EnvFile $Script:ENV_FILE_PATH
        Write-Host ""
    }
    elseif (-not (Test-IsContinuousIntegration)) {
        Log-Warning "No se encontró archivo de configuración"
        Log-Info "Usa -Setup para configuración interactiva"
        Log-Info "O configura variables de entorno manualmente"
        Write-Host ""
    }

    # Ejecutar tests
    Invoke-Tests
}

# ============================================================================
# EJECUCIÓN
# ============================================================================

try {
    Main
}
catch {
    Write-Host ""
    Log-Error "Error inesperado: $_"
    exit 1
}

