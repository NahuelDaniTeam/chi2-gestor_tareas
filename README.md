# CHI2 · Gestor de tareas

Módulo de CHI2 para planificar tareas personales por día, semana, mes y largo plazo, con prioridades y tablero. Primero para uso personal; después, con grupos (por ejemplo, el equipo de la tesis).

**Stack:** Java 21 · Spring Boot · Thymeleaf · Tailwind CSS · PostgreSQL + Flyway · Maven. Igual que el web principal de CHI2.

## Correrlo

Requisitos: Java 21 y Docker Desktop abierto.

```bash
./mvnw spring-boot:run   # http://localhost:8082 (levanta Postgres solo)
./mvnw verify            # build + tests (Java y JavaScript)
npm run test:e2e         # navegador: Chrome, Firefox, celular y accesibilidad (Playwright + axe)
```

## Abrirlo todos los días

- **Acceso directo en el escritorio:** `powershell -ExecutionPolicy Bypass -File scripts\crear-acceso-directo.ps1` crea "Tareas CHI2". Al abrirlo, si la app no está corriendo la levanta (minimizada) y abre el navegador en Hoy.
- **Como app de escritorio:** con la página abierta en Chrome o Edge, menú ⋮ > "Instalar página como app". Queda en su propia ventana y se puede anclar a la barra de tareas (la app tiene que estar corriendo).
- Las tareas se guardan en un volumen de Docker (`tareas-datos`): sobreviven a reinicios de la PC y del contenedor.

## Uso rápido

- Escribí la tarea y Enter. Atajos dentro del texto: `!a` `!m` `!b` (prioridad), `@hoy` `@mañana` `@viernes` (día), `@s` `@m` (semana, mes) y `@p` (sin plazo). Ej.: `Entregar capítulo 2 !a @viernes`.
- `n` o `/` vuelven al campo de alta, `1`–`5` cambian de vista, `←` `→` cambian de semana o mes, `t` cambia entre calendario, lista y tablero. En los calendarios se arrastra para reprogramar. `?` muestra todos los atajos.
- Clic en una tarea abre el panel de edición; Escape lo cierra.
- Lista o tablero: selector arriba a la derecha. En el tablero, arrastrá las tarjetas entre columnas.

## Respaldos

- La app guarda sola un respaldo JSON por día en `~/CHI2-Tareas/respaldos` (los últimos 30).
- Menú ⋯ → **Exportar tareas (CSV)** o **Respaldo completo (JSON)**.
- Base completa: `powershell -ExecutionPolicy Bypass -File scripts\respaldar-base.ps1`; para volver a un respaldo, `scripts\restaurar-base.ps1 -Archivo <archivo.sql>`.

## API

Documentación navegable en http://localhost:8082/api/docs (OpenAPI en `/v3/api-docs`).

## Documentación

- [`CLAUDE.md`](CLAUDE.md): descripción del módulo, convenciones, arquitectura y forma de trabajo.
- [`PENDIENTES.md`](PENDIENTES.md): qué falta y quién lo tiene.
- [`docs/01-especificacion.md`](docs/01-especificacion.md): funcionalidad, modelo de datos y API.
- [`CHANGELOG.md`](CHANGELOG.md): qué cambió en cada versión.
- [`docs/02-integraciones.md`](docs/02-integraciones.md): integraciones hechas y las que siguen (Google Calendar, Telegram, aula virtual...).
