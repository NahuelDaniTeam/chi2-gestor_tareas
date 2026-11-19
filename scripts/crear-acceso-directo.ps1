# Crea "Tareas CHI2" en el escritorio, que ejecuta scripts\abrir-tareas.cmd.
# Uso: powershell -ExecutionPolicy Bypass -File scripts\crear-acceso-directo.ps1
# Para fijarlo en la barra de tareas: clic derecho sobre el acceso directo > Mostrar más opciones > Anclar a la barra de tareas.
$raiz = Split-Path -Parent $PSScriptRoot
$destino = Join-Path ([Environment]::GetFolderPath('Desktop')) 'Tareas CHI2.lnk'

$shell = New-Object -ComObject WScript.Shell
$acceso = $shell.CreateShortcut($destino)
$acceso.TargetPath = Join-Path $raiz 'scripts\abrir-tareas.cmd'
$acceso.WorkingDirectory = $raiz
$acceso.WindowStyle = 7  # minimizada
$acceso.Description = 'Gestor de tareas CHI2'
$acceso.Save()

Write-Output "Acceso directo creado: $destino"
