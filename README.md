# CHI2 · Gestor de tareas

Módulo de CHI2 para planificar tareas personales por día, semana, mes y largo plazo, con prioridades y tablero. Primero para uso personal; después, con grupos (por ejemplo, el equipo de la tesis).

**Stack:** Java 21 · Spring Boot · Thymeleaf · Tailwind CSS · PostgreSQL + Flyway · Maven. Igual que el web principal de CHI2.

## Correrlo

Requisitos: Java 21 y Docker Desktop abierto.

```bash
./mvnw spring-boot:run   # http://localhost:8082 (levanta Postgres solo)
./mvnw verify            # build + tests
```

## Abrirlo todos los días

- **Acceso directo en el escritorio:** `powershell -ExecutionPolicy Bypass -File scripts\crear-acceso-directo.ps1` crea "Tareas CHI2". Al abrirlo, si la app no está corriendo la levanta (minimizada) y abre el navegador en Hoy.
- **Como app de escritorio:** con la página abierta en Chrome o Edge, menú ⋮ > "Instalar página como app". Queda en su propia ventana y se puede anclar a la barra de tareas (la app tiene que estar corriendo).
- Las tareas se guardan en un volumen de Docker (`tareas-datos`): sobreviven a reinicios de la PC y del contenedor.

## Uso rápido

- Escribí la tarea y Enter. Atajos dentro del texto: `!a` `!m` `!b` (prioridad) y `@d` `@s` `@m` `@l` (día, semana, mes, largo plazo). Ej.: `Entregar capítulo 2 !a @s`.
- `n` o `/` vuelven al campo de alta. Clic en una tarea abre el panel de edición; Escape lo cierra.
- Lista o tablero: selector arriba a la derecha. En el tablero, arrastrá las tarjetas entre columnas.

## Documentación

- [`CLAUDE.md`](CLAUDE.md): descripción del módulo, convenciones, arquitectura y forma de trabajo.
- [`PENDIENTES.md`](PENDIENTES.md): qué falta y quién lo tiene.
- [`docs/01-especificacion.md`](docs/01-especificacion.md): funcionalidad, modelo de datos y API.
