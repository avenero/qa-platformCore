# ============================================================================
# Scotia QA Framework - Configuracion de Variables de Entorno (Windows)
# VERSION: 1.0.1
# ============================================================================
#
# Script para cargar variables de entorno desde archivo .env.local
#
# USO:
#   . .\scripts\setup-env.ps1
#
# LUEGO EJECUTAR TESTS:
#   .\gradlew test
#
# @author Abel Venero
# @version 1.0.1
# ============================================================================

$ENV_FILE = ".env.local"

# Verificar que existe .env.local
if (-not (Test-Path $ENV_FILE)) {
    Write-Host ""
    Write-Host "ERROR: Archivo $ENV_FILE no encontrado" -ForegroundColor Red
    Write-Host ""
    Write-Host "Crea el archivo copiando el template:" -ForegroundColor Yellow
    Write-Host "  Copy-Item config\templates\.env.local.template -Destination .env.local" -ForegroundColor Cyan
    Write-Host ""
    return
}

Write-Host ""
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host "  Configurar Variables de Entorno" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host ""

# Contador de variables cargadas
$count = 0

# Leer archivo linea por linea
Get-Content $ENV_FILE | ForEach-Object {
    $line = $_.Trim()

    # Ignorar lineas vacias y comentarios
    if ($line -and -not $line.StartsWith("#")) {
        # Dividir en nombre=valor
        $parts = $line -split '=', 2
        if ($parts.Count -eq 2) {
            $name = $parts[0].Trim()
            $value = $parts[1].Trim()

            # Remover comillas dobles si existen
            if ($value.StartsWith('"') -and $value.EndsWith('"')) {
                $value = $value.Substring(1, $value.Length - 2)
            }

            # Remover comillas simples si existen
            if ($value.StartsWith("'") -and $value.EndsWith("'")) {
                $value = $value.Substring(1, $value.Length - 2)
            }

            # Establecer variable de entorno
            [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
            $count++

            # Mostrar variable (ocultar passwords)
            if ($name -match "PASS|PASSWORD|TOKEN|SECRET|KEY") {
                Write-Host "  $name = ***HIDDEN***" -ForegroundColor Green
            }
            else {
                Write-Host "  $name = $value" -ForegroundColor Green
            }
        }
    }
}

Write-Host ""
Write-Host "Variables cargadas: $count" -ForegroundColor Green
Write-Host ""
Write-Host "Ahora puedes ejecutar:" -ForegroundColor Yellow
Write-Host "  .\gradlew test" -ForegroundColor Cyan
Write-Host ""

