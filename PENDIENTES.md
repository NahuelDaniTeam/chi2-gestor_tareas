# Pendientes · CHI2 Gestor de tareas

Hoja compartida de tareas pendientes entre Nahuel y Daniel para este módulo. Claude la lee al arrancar cada sesión y le avisa a cada uno qué tiene pendiente (ver `CLAUDE.md` §6).

**Cómo se escribe un pendiente:** tiene que poder entenderlo alguien que no estuvo en la charla donde surgió. Cada ítem lleva:
- **Contexto:** por qué existe y qué ya está decidido.
- **Qué entregar:** concreto y verificable.
- **Dónde:** el archivo o la sección donde va el resultado.
- La línea `cargado por <quien>, <AAAA-MM-DD>`.

Al completar un ítem se mueve a **Hechos** con la fecha. Los hechos de más de un mes se pueden borrar.

**Documento de referencia:** `docs/01-especificacion.md` (funcionalidad del gestor).

---

## Para Daniel

_(nada por ahora)_

## Para Nahuel

### 1. Confirmar el enfoque para el arrastre
- **Contexto:** el tablero necesita arrastrar tarjetas entre columnas (`docs/01-especificacion.md` §4.4). Se implementó con **arrastrar y soltar nativo de HTML5 y JavaScript propio** (`src/main/resources/static/js/tareas.js`), sin librerías. Limitación conocida: en celulares y tablets el arrastre nativo no funciona; ahí se cambia el estado desde la casilla de la lista o desde el panel lateral. Si hace falta arrastre táctil, la alternativa es una librería liviana como SortableJS.
- **Qué entregar:** "queda así" o "pasar a <librería>", después de probarlo un par de días.
- **Dónde:** `docs/01-especificacion.md` §8.
- cargado por Nahuel, 2026-11-19

### 2. Confirmar los atajos de teclado del alta rápida
- **Contexto:** la especificación pedía cambiar horizonte y prioridad sin el mouse (§4.2). Se implementó como texto dentro del mismo campo: `!a` `!m` `!b` para la prioridad y `@d` `@s` `@m` `@l` para el horizonte (ejemplo: `Entregar capítulo 2 !a @s`). Se descartaron combinaciones con Alt o Ctrl porque chocan con atajos del navegador (Alt+D, Ctrl+D). Fuera del campo, `n` o `/` lo enfocan.
- **Qué entregar:** "quedan así" o los atajos que prefieras.
- **Dónde:** `docs/01-especificacion.md` §4.2 y `tareas.js` (constantes `ATAJOS_PRIORIDAD` y `ATAJOS_HORIZONTE`).
- cargado por Nahuel, 2026-11-19

### 3. Confirmar que la semana arranca el lunes
- **Contexto:** se implementó con la semana de lunes a domingo, lo habitual en Argentina. Cambiarlo es tocar una constante, pero las tareas de semana ya guardadas quedan con su lunes como referencia y habría que migrarlas.
- **Qué entregar:** "lunes" o "domingo". Mejor decidirlo antes de cargar muchas tareas de semana.
- **Dónde:** `docs/01-especificacion.md` §4.1 y `Horizonte.PRIMER_DIA_SEMANA`.
- cargado por Nahuel, 2026-11-19

### 4. Definir si la aplicación se inicia sola al prender la computadora
- **Contexto:** hoy se abre con `scripts/abrir-tareas.cmd` (o el acceso directo que crea `scripts/crear-acceso-directo.ps1`): si la app no está corriendo, la levanta y abre el navegador. Necesita Docker Desktop abierto, porque la base es Postgres en Docker. Para que arranque sola alcanzaría con poner ese acceso directo en la carpeta de Inicio de Windows (`shell:startup`) y que Docker Desktop también inicie con Windows.
- **Qué entregar:** sí o no. Si es sí, Claude agrega la opción al script.
- **Dónde:** `README.md` y `scripts/`.
- cargado por Nahuel, 2026-11-19

### 7. Probar el arrastre y el alta rápida en dos navegadores
- **Contexto:** la especificación pide una prueba manual en al menos dos navegadores (§9). Claude la probó en Chrome.
- **Qué entregar:** probar en Edge o Firefox: alta con Enter y atajos, arrastre entre columnas, edición en el panel y eliminación. Anotar lo que falle.
- **Dónde:** contárselo a Claude o anotarlo acá.
- cargado por Nahuel, 2026-11-19

## Para los dos

### 5. Bocetar en Figma la pantalla Hoy y el tablero
- **Contexto:** la regla de CHI2 es diseñar en Figma antes de codificar las pantallas definitivas. El MVP ya está hecho con el estilo del web principal (marfil, tinta, acento terracota, Instrument Serif e Inter), así que el boceto puede partir de capturas de lo que hay.
- **Qué entregar:** el link al archivo de Figma con Hoy (lista) y el tablero, en escritorio.
- **Dónde:** pegar el link en `CLAUDE.md` §4.
- cargado por Nahuel, 2026-11-19

### 6. Heredar la paleta y la tipografía definitivas de CHI2
- **Contexto:** este módulo usa los mismos tokens que el web principal, que todavía son provisorios (pendiente 10 del `PENDIENTES.md` del web). Cuando se cierren allá, hay que copiarlos acá.
- **Qué entregar:** copiar el bloque `@theme` de `src/main/frontend/main.css` del web y el `<link>` de fuentes de `fragments/layout.html`.
- **Dónde:** `src/main/frontend/main.css` y `src/main/resources/templates/fragments/layout.html` de este repo.
- cargado por Nahuel, 2026-11-19

### 8. Definir cuándo se suma el login para grupos
- **Contexto:** la etapa 1 no tiene login: todas las tareas son de un usuario fijo (`chi2.tareas.usuario-local`), detrás de la interfaz `UsuarioActual`. La etapa 2 (grupos, por ejemplo el equipo de la tesis) necesita usuarios reales. Hay que decidir cuándo y cómo: login propio con Spring Security, o con Google.
- **Qué entregar:** el momento (por ejemplo, "después de usarlo un mes") y el tipo de login.
- **Dónde:** `docs/01-especificacion.md` §5.
- cargado por Nahuel, 2026-11-19

### 9. Definir si en la etapa 2 se asignan tareas de grupo a un miembro
- **Contexto:** la tabla ya tiene la columna `asignado_a` (vacía). Asignar sirve para repartir trabajo en la tesis, pero suma un filtro más ("asignadas a mí").
- **Qué entregar:** sí o no para la etapa 2.
- **Dónde:** `docs/01-especificacion.md` §5.
- cargado por Nahuel, 2026-11-19

---

## Hechos

- [x] MVP del gestor (etapa 1): tareas con horizonte, prioridad y estado; vistas Hoy, Semana, Mes y Largo plazo; lista y tablero con arrastre; alta rápida con atajos; panel de edición; API REST completa; 57 tests (unitarios, controladores e integración con Postgres). Ajustes respecto de la especificación en `docs/01-especificacion.md` §12. · 2026-11-19
