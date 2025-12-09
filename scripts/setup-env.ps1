# ============================================================================
# Scotia QA Framework - Configuración de Variables de Entorno (Windows)
# VERSION: 1.0.0
# ============================================================================
#
# Script para cargar variables de entorno desde archivo .env.local
# y exportarlas al proceso actual de PowerShell para ejecución de tests.
#
# CARACTERÍSTICAS:
#   ✅ Carga automática de .env.local
#   ✅ Validación de variables críticas
#   ✅ Verificación interactiva
#   ✅ Compatible con PowerShell 5.1+
#
# USO:
#   . .\scripts\setup-env.ps1                  # Cargar en sesión actual
#
# LUEGO EJECUTAR TESTS:
#   .\gradlew test                             # Desde terminal
#   O ejecutar desde IntelliJ (variables ya están en el proceso)
#
# IMPORTANTE:
#   ⚠️  Debes usar '. .\setup-env.ps1' (con punto inicial) para que las variables
#       se exporten al proceso actual
#   ⚠️  NO ejecutar con .\setup-env.ps1 (las variables no persistirán)
#
# @author Abel Venero
# @version 1.0.0
# ============================================================================

[CmdletBinding()]
param()

# ============================================================================
# CONFIGURACIÓN
# ============================================================================

$ENV_FILE = ".env.local"
$SCRIPT_VERSION = "1.0.0"

# Variables críticas (obligatorias para DB)
$REQUIRED_DB_VARS = @(
    "DB_URL",
    "DB_USER",
    "DB_PASS"
)

# ============================================================================
# FUNCIONES DE LOGGING
# ============================================================================

function Write-Banner {
    param([string]$Message)
    Write-Host ""
    Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "  $Message" -ForegroundColor Cyan
    Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host ""
}

function Write-Separator {
    Write-Host "───────────────────────────────────────────" -ForegroundColor Cyan
}

function Write-InfoMessage {
    param([string]$Message)
    Write-Host "ℹ  " -ForegroundColor Blue -NoNewline
    Write-Host $Message
}

function Write-SuccessMessage {
    param([string]$Message)
    Write-Host "✓  " -ForegroundColor Green -NoNewline
    Write-Host $Message
}

function Write-WarningMessage {
    param([string]$Message)
    Write-Host "⚠  " -ForegroundColor Yellow -NoNewline
    Write-Host $Message
}

function Write-ErrorMessage {
    param([string]$Message)
    Write-Host "✗  " -ForegroundColor Red -NoNewline
    Write-Host $Message
}

# ============================================================================
# VALIDACIONES
# ============================================================================

function Test-EnvFileExists {
    if (-not (Test-Path $ENV_FILE)) {
        Write-ErrorMessage "Archivo $ENV_FILE no encontrado"
        Write-Host ""
        Write-InfoMessage "Crea el archivo copiando el template:"
        Write-Host "  Copy-Item config\templates\.env.local.template -Destination .env.local" -ForegroundColor Cyan
        Write-Host ""
        Write-InfoMessage "Y edita con tus valores reales"
        Write-Host ""
        return $false
    }
    return $true
}

# ============================================================================
# CARGA DE VARIABLES
# ============================================================================

function Import-EnvFile {
    param([string]$FilePath)

    Write-InfoMessage "Cargando variables desde: $FilePath"
    Write-Host ""

    $count = 0

    Get-Content $FilePath -ErrorAction Stop | ForEach-Object {
        $line = $_.Trim()

        # Ignorar líneas vacías y comentarios
        if ($line -and -not $line.StartsWith("#")) {
            # Dividir en nombre=valor
            $parts = $line -split '=', 2
            if ($parts.Count -eq 2) {
                $name = $parts[0].Trim()
                $value = $parts[1].Trim()

                # Remover comillas si existen
                $value = $value -replace '^["\x27]|["\x27]$', ''

                # Establecer variable de entorno en el proceso actual
                [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
                $count++
            }
        }
    }

    Write-SuccessMessage "Variables cargadas: $count"
    return $true
}

# ============================================================================
# VERIFICACIÓN DE VARIABLES
# ============================================================================

function Show-LoadedVariables {
    Write-Separator
    Write-Host ""
    Write-Host "Variables en .env.local:" -ForegroundColor White
    Write-Host ""

    $varNum = 1

    Get-Content $ENV_FILE | ForEach-Object {
        $line = $_.Trim()

        if ($line -and -not $line.StartsWith("#")) {
            $parts = $line -split '=', 2
            if ($parts.Count -eq 2) {
                $name = $parts[0].Trim()
                $value = $parts[1].Trim()

                # Ocultar valores sensibles
                if ($name -match "(PASS|PASSWORD|TOKEN|SECRET|KEY)") {
                    $value = "***HIDDEN***"
                }
                else {
                    # Remover comillas para mostrar
                    $value = $value -replace '^["\x27]|["\x27]$', ''
                }

                Write-Host "   " -NoNewline
                Write-Host ("{0,2}. " -f $varNum) -ForegroundColor Cyan -NoNewline
                Write-Host ("{0,-25}" -f $name) -NoNewline -ForegroundColor White
                Write-Host " = $value"
                $varNum++
            }
        }
    }

    Write-Host ""
    Write-Separator
    Write-Host ""
}

function Test-EnvironmentVariables {
    Write-Host "Estado en el entorno actual:" -ForegroundColor White
    Write-Host ""

    $allSet = $true
    $checkedVars = @{}

    Get-Content $ENV_FILE | ForEach-Object {
        $line = $_.Trim()

        if ($line -and -not $line.StartsWith("#")) {
            $parts = $line -split '=', 2
            if ($parts.Count -eq 2) {
                $name = $parts[0].Trim()

                # Evitar duplicados
                if (-not $checkedVars.ContainsKey($name)) {
                    $checkedVars[$name] = $true

                    $value = [System.Environment]::GetEnvironmentVariable($name, "Process")

                    if ($value) {
                        Write-SuccessMessage "$name (cargada)"
                    }
                    else {
                        Write-WarningMessage "$name (vacía o no configurada)"
                        $allSet = $false
                    }
                }
            }
        }
    }

    Write-Host ""

    if ($allSet) {
        Write-SuccessMessage "Todas las variables están cargadas en el entorno actual"
    }
    else {
        Write-WarningMessage "Algunas variables están vacías"
        Write-InfoMessage "Edita $ENV_FILE y vuelve a ejecutar este script"
    }

    Write-Host ""
}

# ============================================================================
# VERIFICACIÓN ESPECÍFICA DE BD
# ============================================================================

function Test-DatabaseConfig {
    $missingVars = @()

    foreach ($var in $REQUIRED_DB_VARS) {
        $value = [System.Environment]::GetEnvironmentVariable($var, "Process")
        if (-not $value) {
            $missingVars += $var
        }
    }

    if ($missingVars.Count -gt 0) {
        Write-Separator
        Write-Host ""
        Write-WarningMessage "Configuración de Base de Datos incompleta"
        Write-Host ""
        Write-InfoMessage "Variables faltantes:"
        foreach ($var in $missingVars) {
            Write-Host "   • $var"
        }
        Write-Host ""
        Write-InfoMessage "Si vas a ejecutar tests de database (@db, @database), configura estas variables"
        Write-Host ""
        return $false
    }
    else {
        Write-SuccessMessage "Configuración de BD completa"
        return $true
    }
}

# ============================================================================
# FUNCIÓN PRINCIPAL
# ============================================================================

function Main {
    Write-Banner "🔧 Configurar Variables de Entorno"

    # Verificar que existe .env.local
    if (-not (Test-EnvFileExists)) {
        return
    }

    # Cargar variables
    try {
        if (-not (Import-EnvFile -FilePath $ENV_FILE)) {
            return
        }
    }
    catch {
        Write-ErrorMessage "Error cargando archivo: $_"
        return
    }

    Write-Host ""

    # Mostrar variables cargadas
    Show-LoadedVariables

    # Verificar estado en el entorno
    Test-EnvironmentVariables

    # Verificar configuración de BD
    Test-DatabaseConfig | Out-Null

    Write-Separator
    Write-Host ""
    Write-SuccessMessage "Variables de entorno configuradas exitosamente"
    Write-Host ""
    Write-InfoMessage "Ahora puedes ejecutar tests:"
    Write-Host ""
    Write-Host "   .\gradlew test                    " -ForegroundColor Green -NoNewline
    Write-Host "# Todos los tests"
    Write-Host "   .\gradlew test --tests '*Smoke*'  " -ForegroundColor Green -NoNewline
    Write-Host "# Tests específicos"
    Write-Host ""
    Write-InfoMessage "O ejecutar desde IntelliJ (las variables ya están disponibles en este proceso)"
    Write-Host ""

    # Instrucciones adicionales
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "📝 Notas:" -ForegroundColor White
    Write-Host "   • Las variables solo están disponibles en esta sesión de PowerShell"
    Write-Host "   • Si cierras la terminal, deberás ejecutar este script nuevamente"
    Write-Host "   • Para ejecutar en IntelliJ, abre IntelliJ desde esta misma terminal:"
    Write-Host "     " -NoNewline
    Write-Host "& 'C:\Program Files\JetBrains\IntelliJ IDEA\bin\idea64.exe'" -ForegroundColor Cyan
    Write-Host ""
}

# ============================================================================
# EJECUCIÓN
# ============================================================================

try {
    Main
}
catch {
    Write-Host ""
    Write-ErrorMessage "Error inesperado: $_"
    Write-Host ""
}

