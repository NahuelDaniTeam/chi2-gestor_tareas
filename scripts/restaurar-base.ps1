# Restaura un respaldo hecho con respaldar-base.ps1. REEMPLAZA todo lo que hay en la base.
# Uso:   powershell -ExecutionPolicy Bypass -File scripts\restaurar-base.ps1 -Archivo ~\CHI2-Tareas\respaldos\base-2026-11-21_1015.sql
# Cerrá la app antes (si no, puede seguir mostrando datos viejos hasta recargar).
param([Parameter(Mandatory = $true)][string]$Archivo)
$ErrorActionPreference = 'Stop'
if (-not (Test-Path $Archivo)) { throw "No existe el archivo $Archivo" }

$contenedor = docker ps --filter 'label=com.docker.compose.service=postgres' --filter 'label=com.docker.compose.project=chi2-gestor_tareas' --format '{{.Names}}' | Select-Object -First 1
if (-not $contenedor) { throw 'No encontré el contenedor de Postgres. ¿Está abierto Docker Desktop?' }

$respuesta = Read-Host "Esto reemplaza TODAS las tareas actuales por las de $Archivo. Escribí SI para seguir"
if ($respuesta -ne 'SI') { Write-Output 'Cancelado.'; exit 1 }

Get-Content -Raw -Encoding utf8 $Archivo | docker exec -i $contenedor psql -U chi2 -d chi2_tareas -v ON_ERROR_STOP=1 | Out-Null
Write-Output 'Base restaurada.'
