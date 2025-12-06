# ============================================================================
# sync-utils.ps1 - Sincronizar Scripts desde JAR de common (PowerShell)
# ============================================================================
#
# Este script extrae utils.sh y utils.ps1 desde el JAR de common publicado
# en Maven local. Solo actualiza archivos CORE (utils.*), nunca toca archivos
# custom del módulo (run-tests.ps1, etc.)
#
# Uso:
#   .\scripts\sync-utils.ps1              # Sincronizar con última versión
#   .\scripts\sync-utils.ps1 -Version "1.0.1"  # Sincronizar con versión específica
#
# @author Abel Venero
# @version 1.0.0
# ============================================================================

[CmdletBinding()]
param(
    [Parameter(Mandatory=$false)]
    [string]$Version = "",

    [Parameter(Mandatory=$false)]
    [switch]$Help
)

# ============================================================================
# FUNCIONES DE LOGGING (Definir PRIMERO antes de cualquier configuración)
# ============================================================================

function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-ErrorMessage {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

function Write-WarningMessage {
    param([string]$Message)
    Write-Host "⚠️  $Message" -ForegroundColor Yellow
}

function Write-InfoMessage {
    param([string]$Message)
    Write-Host "ℹ️  $Message" -ForegroundColor Cyan
}

# ============================================================================
# CONFIGURACIÓN (Después de definir funciones de logging)
# ============================================================================

# Configurar error handling (ahora las funciones ya están disponibles)
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

function Write-BannerMessage {
    param([string]$Title)
    Write-Host ""
    Write-Host "═══════════════════════════════════════════" -ForegroundColor Blue
    Write-Host "  🔄 $Title" -ForegroundColor Blue
    Write-Host "═══════════════════════════════════════════" -ForegroundColor Blue
    Write-Host ""
}

# ============================================================================
# FUNCIONES PRINCIPALES
# ============================================================================

<#
.SYNOPSIS
    Buscar JAR de common en Maven local
#>
function Find-CommonJar {
    param([string]$TargetVersion)

    $mavenRepo = Join-Path $env:USERPROFILE ".m2\repository\com\scotia\qa\common"

    if (-not (Test-Path $mavenRepo)) {
        Write-ErrorMessage "Repositorio Maven local no encontrado: $mavenRepo"
        Write-InfoMessage "Ejecuta primero: .\gradlew :common:publishToMavenLocal (en el framework)"
        return $null
    }

    # Si se especificó versión, buscar esa versión específica
    if ($TargetVersion) {
        $jarPath = Join-Path $mavenRepo "$TargetVersion\common-$TargetVersion.jar"
        if (Test-Path $jarPath) {
            return $jarPath
        }
        else {
            Write-ErrorMessage "JAR no encontrado: $jarPath"
            return $null
        }
    }

    # Buscar última versión (ordenar por fecha de modificación)
    $jarFiles = Get-ChildItem -Path $mavenRepo -Filter "common-*.jar" -Recurse -File |
                Where-Object {
                    $_.Name -notlike "*-sources.jar" -and
                    $_.Name -notlike "*-javadoc.jar"
                } |
                Sort-Object LastWriteTime -Descending |
                Select-Object -First 1

    if (-not $jarFiles) {
        Write-ErrorMessage "No se encontró ningún JAR de common en Maven local"
        Write-InfoMessage "Ejecuta: .\gradlew :common:publishToMavenLocal (en el framework)"
        return $null
    }

    return $jarFiles.FullName
}

<#
.SYNOPSIS
    Extraer scripts del JAR
#>
function Extract-ScriptsFromJar {
    param([string]$JarPath)

    $tempDir = Join-Path $env:TEMP "sync-utils-$(Get-Random)"
    New-Item -ItemType Directory -Path $tempDir -Force | Out-Null

    Write-InfoMessage "Extrayendo scripts desde: $(Split-Path -Leaf $JarPath)"

    try {
        # Verificar que el JAR contiene los scripts
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $zip = [System.IO.Compression.ZipFile]::OpenRead($JarPath)

        $hasScripts = $zip.Entries | Where-Object { $_.FullName -like "META-INF/scripts/utils.*" }

        if (-not $hasScripts) {
            Write-ErrorMessage "El JAR no contiene scripts en META-INF/scripts/"
            Write-WarningMessage "Puede ser una versión antigua de common sin soporte cross-platform"
            $zip.Dispose()
            Remove-Item -Path $tempDir -Recurse -Force
            return $false
        }

        # Extraer utils.sh y utils.ps1
        foreach ($entry in $zip.Entries) {
            if ($entry.FullName -like "META-INF/scripts/utils.sh" -or
                $entry.FullName -like "META-INF/scripts/utils.ps1") {

                $destPath = Join-Path $tempDir $entry.Name
                [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $destPath, $true)
            }
        }

        $zip.Dispose()

        # Verificar extracción exitosa
        $utilsSh = Join-Path $tempDir "utils.sh"
        if (-not (Test-Path $utilsSh)) {
            Write-ErrorMessage "Falló la extracción de utils.sh"
            Remove-Item -Path $tempDir -Recurse -Force
            return $false
        }

        # Crear directorio scripts/ si no existe
        if (-not (Test-Path "scripts")) {
            New-Item -ItemType Directory -Path "scripts" -Force | Out-Null
        }

        # Copiar scripts (sobrescribir)
        Copy-Item -Path $utilsSh -Destination "scripts\utils.sh" -Force
        Write-Success "utils.sh actualizado"

        $utilsPs1 = Join-Path $tempDir "utils.ps1"
        if (Test-Path $utilsPs1) {
            Copy-Item -Path $utilsPs1 -Destination "scripts\utils.ps1" -Force
            Write-Success "utils.ps1 actualizado"
        }

        # Limpiar temporal
        Remove-Item -Path $tempDir -Recurse -Force

        return $true
    }
    catch {
        Write-ErrorMessage "Error extrayendo scripts: $_"
        if (Test-Path $tempDir) {
            Remove-Item -Path $tempDir -Recurse -Force
        }
        return $false
    }
}

<#
.SYNOPSIS
    Mostrar información del JAR
#>
function Show-JarInfo {
    param([string]$JarPath)

    $jarName = Split-Path -Leaf $JarPath
    $version = $jarName -replace 'common-(.*)\.jar', '$1'
    $jarDate = (Get-Item $JarPath).LastWriteTime.ToString("yyyy-MM-dd HH:mm:ss")

    Write-Host ""
    Write-InfoMessage "JAR encontrado: $jarName"
    Write-InfoMessage "Versión: $version"
    Write-InfoMessage "Fecha: $jarDate"
    Write-Host ""
}

<#
.SYNOPSIS
    Mostrar ayuda de uso
#>
function Show-Usage {
    Write-Host "Uso: .\sync-utils.ps1 [opciones]"
    Write-Host ""
    Write-Host "Opciones:"
    Write-Host "  -Version <VERSION>   Sincronizar con versión específica (ej: 1.0.1)"
    Write-Host "  -Help                Mostrar esta ayuda"
    Write-Host ""
    Write-Host "Ejemplos:"
    Write-Host "  .\sync-utils.ps1                  # Sincronizar con última versión"
    Write-Host "  .\sync-utils.ps1 -Version 1.0.1   # Sincronizar con versión 1.0.1"
}

# ============================================================================
# MAIN
# ============================================================================

function Main {
    if ($Help) {
        Show-Usage
        exit 0
    }

    Write-BannerMessage "Sincronizar Scripts desde common"

    # Buscar JAR
    $jarPath = Find-CommonJar -TargetVersion $Version
    if (-not $jarPath) {
        exit 1
    }

    # Mostrar información
    Show-JarInfo -JarPath $jarPath

    # Extraer scripts
    if (-not (Extract-ScriptsFromJar -JarPath $jarPath)) {
        exit 1
    }

    Write-Host ""
    Write-Success "Scripts sincronizados exitosamente"
    Write-InfoMessage "Archivos actualizados:"
    Write-Host "  • scripts\utils.sh"
    Write-Host "  • scripts\utils.ps1"
    Write-Host ""
    Write-InfoMessage "Nota: Solo se actualizan utils.* (archivos CORE)"
    Write-InfoMessage "      Los archivos custom del módulo no se tocan"
    Write-Host ""
}

# Ejecutar main
try {
    Main
}
catch {
    Write-ErrorMessage "Error inesperado: $_"
    exit 1
}

