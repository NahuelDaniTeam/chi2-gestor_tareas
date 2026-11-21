# Respaldo completo de la base (todas las tablas) con pg_dump, desde el contenedor de Postgres de compose.yaml.
# Uso:   powershell -ExecutionPolicy Bypass -File scripts\respaldar-base.ps1
# Deja  ~\CHI2-Tareas\respaldos\base-AAAA-MM-DD_HHMM.sql  y conserva los últimos 30.
# Complementa el respaldo JSON diario que hace la app sola (docs/01-especificacion.md §4.15).
$ErrorActionPreference = 'Stop'
$carpeta = Join-Path $HOME 'CHI2-Tareas\respaldos'
New-Item -ItemType Directory -Force $carpeta | Out-Null

$contenedor = docker ps --filter 'label=com.docker.compose.service=postgres' --filter 'label=com.docker.compose.project=chi2-gestor_tareas' --format '{{.Names}}' | Select-Object -First 1
if (-not $contenedor) { throw 'No encontré el contenedor de Postgres. ¿Está abierto Docker Desktop y la app levantada al menos una vez?' }

$archivo = Join-Path $carpeta ("base-{0}.sql" -f (Get-Date -Format 'yyyy-MM-dd_HHmm'))
docker exec $contenedor pg_dump -U chi2 -d chi2_tareas --clean --if-exists --no-owner | Out-File -Encoding utf8 $archivo
Write-Output "Respaldo guardado: $archivo"

Get-ChildItem $carpeta -Filter 'base-*.sql' | Sort-Object Name -Descending | Select-Object -Skip 30 | Remove-Item
