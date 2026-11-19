@echo off
rem Abre el gestor de tareas CHI2 en el navegador. Si la app no esta corriendo, la levanta
rem en una ventana minimizada y espera a que responda (docs/01-especificacion.md §3).
rem Requisito: Docker Desktop abierto (la base es Postgres en Docker).
setlocal
set URL=http://localhost:8082
cd /d "%~dp0.."

curl -s -o nul %URL%/actuator/health && goto abrir

echo Levantando el gestor de tareas...
start "CHI2 Tareas" /min cmd /c "mvnw.cmd -q spring-boot:run"

rem Espera hasta 3 minutos (la primera vez Maven descarga dependencias)
for /l %%i in (1,1,90) do (
	timeout /t 2 /nobreak >nul
	curl -s -o nul %URL%/actuator/health && goto abrir
)
echo No arranco. Revisa que Docker Desktop este abierto y mira la ventana "CHI2 Tareas".
pause
exit /b 1

:abrir
start "" %URL%
