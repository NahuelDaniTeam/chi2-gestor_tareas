# Cambios · CHI2 Gestor de tareas

Todos los cambios que se notan al usar la app o la API. Formato de [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/); versiones con [versionado semántico](https://semver.org/lang/es/).

**Cómo se publica una versión:** al mergear a `main` lo que está en "Sin publicar", se le pone número (según el cambio: arreglos → `0.2.1`, funciones nuevas → `0.3.0`), se actualiza `<version>` en `pom.xml` y se crea el tag `v0.3.0` en ese commit de `main`. La versión se ve en el menú ⋯ de la app y en `/actuator/info`.

## [Sin publicar]

### Agregado
- Documentación de la API con OpenAPI: Swagger UI en `/api/docs` y JSON en `/v3/api-docs`.
- Respaldos: uno automático por día en `~/CHI2-Tareas/respaldos` (se guardan los últimos 30), exportación a CSV y JSON desde el menú, y scripts `respaldar-base.ps1` / `restaurar-base.ps1` para la base completa.
- Integración continua en GitHub Actions: cada PR corre todos los tests, incluidos los de punta a punta.
- Tests de punta a punta con Playwright en Chrome y Firefox, con vista de celular y auditoría de accesibilidad (axe).
- La versión de la app se ve en el menú ⋯.

## [0.2.0] - 2026-11-21

### Agregado
- Plazos anidados: sin plazo (Por planificar), día, semana, mes y fecha límite. Cada vista incluye lo que cae dentro de ella.
- Calendarios de Semana y Mes con arrastre para reprogramar; Largo plazo agrupado por mes.
- Proyectos (etiquetas) con color, filtro y atajo `#nombre`.
- Checklist dentro de cada tarea.
- Papelera con restauración y borrado a los 30 días; deshacer con aviso y Ctrl+Z.
- Buscador (Ctrl+K).
- Tareas recurrentes (diaria, días hábiles, semanal, mensual).
- Recordatorios con notificación del sistema.
- Revisión semanal y estadísticas.
- Modo oscuro (sigue al sistema o se elige).

### Cambiado
- `LARGO_PLAZO` dejó de ser un plazo: las tareas existentes pasaron a "sin plazo" o "fecha límite" (migración V2).
- Eliminar ya no pide confirmación: va a la papelera y se puede deshacer.

## [0.1.0] - 2026-11-19

### Agregado
- MVP personal: tareas con horizonte, prioridad y estado; vistas Hoy, Semana, Mes y Largo plazo en lista o tablero; alta rápida con atajos; panel de edición; API REST.
- Interfaz con la marca de CHI2, íconos propios, atajos de teclado y página de error propia.

### Arreglado
- Una tarea creada para otra vista ya no "desaparece": el aviso dice dónde quedó y lleva a verla.
