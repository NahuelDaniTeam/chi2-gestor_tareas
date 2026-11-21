# CHI2 · Gestor de tareas personal

Aplicación de uso diario para planificar tareas personales por día, semana, mes y largo plazo, con prioridades. Se abre desde la computadora sin pasos extra. Primero funciona para uso personal y después permite crear grupos (por ejemplo, el equipo de la tesis de la facultad) con tareas compartidas.

Este documento describe la **funcionalidad**. El stack técnico y la estructura del repositorio se toman del proyecto local de CHI2 en Orca (el web principal): si hay diferencias con la sección 8, prevalece el repositorio.

**Estado:** MVP (etapa 1) implementado. Los ajustes hechos al programarlo están en la §12.
**Última actualización:** 2026-11-19
**Referencias:** `CLAUDE.md` · `PENDIENTES.md`

---

## 1. Identidad

- **Nombre del módulo:** Gestor de tareas (módulo `gestion-tareas` dentro de CHI2).
- **Etapa 1:** uso personal, un solo usuario.
- **Etapa 2:** grupos de usuarios con tareas compartidas (equipo de la tesis).
- **Etapa futura:** producto a medida para vender, si la base se adapta.
- **Tono visual:** sobrio y editorial, igual que el web principal. Fondo claro, tipografía clara, sin elementos genéricos de plantilla. Paleta, tipografías y logo: **heredados del web principal** (provisorios allá, `PENDIENTES.md` 6).

## 2. Objetivo

Que el uso diario sea simple: abrir la aplicación, ver qué hay para hoy, cargar tareas en segundos y tener una vista clara de la semana, el mes y lo que queda a largo plazo.

## 3. Uso diario

- La aplicación se abre desde un acceso directo en el escritorio o la barra de tareas, en el navegador, sin login en la etapa personal (`scripts/abrir-tareas.cmd`, ver `README.md`). También se puede "instalar" desde Chrome o Edge como app de escritorio.
- Al abrir, la pantalla principal muestra **Hoy**: tareas del día ordenadas por prioridad.
- Carga de una tarea en menos de cinco segundos: el foco queda en el campo de alta al abrir.
- Todo lo que se mueve (estado, prioridad, fecha) se guarda al instante, sin botón de guardar.
- Funciona con la computadora encendida y, si se puede, se inicia sola al prender el equipo (opcional, `PENDIENTES.md` 4).

## 4. Funcionalidad del MVP (etapa 1)

### 4.1 Tareas y plazos

- **Título** (obligatorio, hasta 200 caracteres), **descripción** (opcional, hasta 2000).
- **Plazo** (campo `horizonte`), que se anida como un calendario: un día está dentro de una semana, una semana dentro de un mes y los meses forman el largo plazo.
  - `SIN_PLAZO`: creada, falta planificarla. Vive en **Por planificar**.
  - `DIA`: "lo hago ese día".
  - `SEMANA` y `MES`: "en algún momento de esa semana o ese mes".
  - `LIMITE`: tiempo máximo, "tiene que estar antes del 15". Ocupa el día del vencimiento y se rotula "Vence…".
- **Fecha de referencia:** el día, el lunes de la semana o el primero del mes; sin plazo, ninguna. Se puede mandar cualquier día del período y el sistema la lleva a la referencia.
- **Prioridad:** `ALTA`, `MEDIA` o `BAJA`. Por defecto, `MEDIA`.
- **Estado:** `PENDIENTE`, `EN_CURSO`, `FINALIZADA`.
- Registro de creación, última modificación y fecha de finalización.
- Una tarea está **vencida** si no está finalizada y su período (o su fecha límite) ya terminó. Sin plazo nunca vence.

### 4.2 Alta rápida

- Un campo de texto, con foco al abrir la aplicación. Enter guarda; el campo queda vacío y enfocado.
- Por defecto, la tarea toma el plazo de la vista: en Hoy, el día de hoy; en Semana o Mes, ese período; en Largo plazo y Por planificar, sin plazo. Prioridad `MEDIA`.
- Debajo del campo, tres chips muestran con qué se va a guardar (plazo, fecha y prioridad) y se pueden cambiar. Cuando un atajo cambia uno, se marca en terracota.
- Atajos en el mismo texto (propuesta, `PENDIENTES.md` 2):
  - prioridad: `!a` (alta), `!m` (media), `!b` (baja);
  - día: `@hoy`, `@mañana`, `@pasado`, `@lunes` … `@domingo` (o `@lun`, `@vie`): el próximo de ese día, hoy si coincide;
  - período: `@s` (semana), `@m` (mes), `@p` (sin plazo).
  - Ejemplo: `Entregar capítulo 2 !a @viernes`.
- Si la tarea queda en otra vista, un aviso dice dónde quedó y trae un enlace "Ver".

**Atajos globales** (fuera de un campo de texto; `?` muestra la lista completa): `1` a `5` cambian de vista, `←` y `→` van al período anterior o siguiente, `t` cambia entre calendario, lista y tablero, `n` o `/` enfocan el alta, `Esc` cierra el panel.

### 4.3 Vistas de planificación

Cada vista incluye las tareas cuyo período cae dentro de ella y no es más largo que ella: la semana muestra sus días y la propia semana (no el mes); el mes, sus días, sus semanas (también las que lo cruzan) y el mes. Dentro de cada vista se ordena por prioridad, fecha y antigüedad.

- **Hoy:** las del día, las que vencen hoy y todas las vencidas sin terminar, marcadas "Vencida".
- **Semana:** calendario de lunes a domingo con una banda "Durante la semana". Navegación a la semana anterior y siguiente.
- **Mes:** calendario en grilla con una banda "Durante el mes" y, en cada semana que tenga tareas propias, una banda de semana. Cada día lleva a su semana.
- **Largo plazo:** todo lo planificado desde el mes que viene, agrupado por mes.
- **Por planificar:** las tareas sin plazo, con botones para mandarlas a hoy, esta semana o este mes.

En los calendarios se **arrastra** para reprogramar: a otro día, a la banda de la semana o a la del mes. Una fecha límite sigue siendo fecha límite al cambiarla de día.

Semana y Mes se ven como **calendario**, **lista** o **tablero**; las demás vistas, como lista o tablero. El modo elegido se recuerda en ese navegador (en el celular, el calendario arranca como lista).

En la lista, las tareas se agrupan según la vista (por día en Semana, por semana en Mes, por mes en Largo plazo) y las finalizadas van al final en un bloque plegable. La fecha de cada tarea se muestra solo cuando agrega información. La prioridad media, que es la de por defecto, no se rotula.

Las pestañas Hoy y Por planificar muestran cuántas tareas tienen pendientes; el título de la ventana, las de hoy.

### 4.4 Tablero

- Columnas por estado: `PENDIENTE`, `EN_CURSO`, `FINALIZADA`.
- Las tarjetas se arrastran entre columnas. Al soltarlas, el estado cambia y se guarda.
- El tablero respeta la vista activa.
- Cada tarjeta muestra título, prioridad (con color discreto) y fecha.
- Edición de título, descripción, estado, plazo, fecha y prioridad desde un panel lateral, que se abre al hacer clic en la tarjeta. Se guarda al instante (los textos, al salir del campo).
- Eliminación con confirmación dentro del panel.

### 4.5 Prioridades

- Tres niveles: `ALTA`, `MEDIA`, `BAJA`.
- Dentro de cada vista, las tareas de prioridad alta aparecen primero.
- Las tareas de prioridad alta se distinguen con un punto y un filete en terracota (el acento de la marca); media, con un punto gris; baja, con un punto solo de contorno.

### 4.6 Proyectos

- Un **proyecto** (o etiqueta) agrupa tareas: "Tesis", "Facultad", "Personal". Tiene nombre (hasta 40 caracteres, único sin distinguir mayúsculas) y uno de siete colores apagados.
- Cada tarea tiene, como mucho, un proyecto.
- **Filtro** en todas las vistas, en la barra de la vista ("Todos los proyectos" o uno). Se recuerda en ese navegador. Con un proyecto filtrado, las tareas nuevas se crean en ese proyecto.
- En el alta, `#tesis` asigna el proyecto (sin importar mayúsculas ni tildes; `#redes_de_datos` encuentra "Redes de Datos"). Si no existe, se crea.
- El diálogo "Administrar proyectos…" (desde el filtro) permite crear, renombrar, cambiar el color y eliminar. Al eliminar un proyecto, sus tareas quedan sin proyecto.
- Base de la etapa 2: un grupo va a funcionar como un proyecto compartido.

### 4.7 Checklist

- Cada tarea puede tener **pasos** (hasta 200 caracteres cada uno), en el orden en que se agregan.
- En el panel: casilla para marcar, texto editable en el lugar y botón para quitar; "+ Agregar un paso" y Enter agrega otro.
- En la lista, el tablero y la tarjeta se ve el progreso ("2/5").

### 4.8 Papelera y deshacer

- **Eliminar no pregunta:** la tarea va a la papelera y un aviso ofrece **Deshacer** durante unos segundos.
- **Ctrl+Z** (fuera de un campo de texto) deshace la última acción: mandar a la papelera, finalizar o reabrir, y reprogramar (arrastre o botones de Por planificar). Se recuerdan las últimas 20 mientras la página está abierta.
- La **papelera** (menú ⋯ del encabezado) lista lo eliminado: se puede restaurar o borrar para siempre, una por una o vaciándola entera (esto sí pide confirmación, porque no se puede deshacer).
- Lo que lleva más de **30 días** en la papelera se borra solo (`chi2.tareas.retencion-papelera`; se revisa cada hora).
- Las tareas en la papelera no aparecen en ninguna vista ni en la API de tareas.

### 4.9 Tareas recurrentes

- En el panel, "Repetir": no se repite, todos los días, días hábiles (lunes a viernes), cada semana o cada mes.
- Una tarea de día o con fecha límite admite todas; una de semana, cada semana o cada mes; una de mes, cada mes; sin plazo no se repite. Si se cambia a un plazo que no admite la repetición elegida, deja de repetirse.
- Al **finalizarla** se crea la siguiente con la próxima fecha, el mismo título, descripción, prioridad, proyecto, repetición y pasos (sin tildar), y el recordatorio corrido a la nueva fecha. Se crea una sola vez: reabrirla y volver a finalizarla no la duplica.
- Un aviso dice cuándo es la próxima.

### 4.10 Recordatorios

- En el panel, "Recordarme" con día y hora. Cambiar la hora vuelve a habilitar el aviso.
- Mientras haya alguna página de la app abierta, cada 30 segundos se fija si llegó la hora de alguno: avisa con una **notificación del sistema** (el permiso se pide al programar el primero) y con un aviso en la página que lleva a la tarea.
- Las tareas finalizadas no avisan. Con la app cerrada no hay avisos (para eso haría falta el inicio automático, `PENDIENTES.md` 4).

### 4.11 Buscador

- **Ctrl+K** (o la lupa del encabezado) desde cualquier página: busca por título o descripción, sin distinguir mayúsculas ni tildes, y muestra primero las sin terminar.
- También ofrece ir a cada vista, la revisión semanal, las estadísticas y la papelera.
- Flechas para moverse, Enter para abrir: la tarea se abre en el panel de su vista.

## 5. Etapa 2: grupos (después del MVP personal)

- Un usuario puede **crear grupos** (por ejemplo, "Tesis") e invitar a otros usuarios.
- Cada miembro tiene un rol: `ADMIN` (invita, edita el grupo y elimina tareas de cualquiera) o `MIEMBRO` (crea y gestiona sus tareas de grupo).
- Una tarea pertenece a un **ámbito**: `PERSONAL` o un `GRUPO` concreto.
- Las tareas de grupo son visibles para todos los miembros del grupo.
- Las tareas personales solo las ve su dueño.
- **Filtro de visualización** en todas las vistas (Hoy, Semana, Mes, Largo plazo, tablero):
  - **Solo personales.**
  - **Solo de un grupo** (elegido en el selector).
  - **Todas:** personales más las de todos los grupos del usuario.
- Opcional en esta etapa: asignar una tarea de grupo a un miembro concreto (`asignado_a`).

## 6. Fuera de alcance del MVP

- Adjuntos.
- Edición en tiempo real entre usuarios (los cambios se ven al recargar; la lista también se recarga al volver a la ventana).
- App nativa. La API REST ya queda preparada para sumarla más adelante.

## 7. Modelo de datos

Tabla `tarea` (migraciones `V1__tarea.sql` y `V2__plazos_anidados.sql`):

| Campo | Tipo | Notas |
|---|---|---|
| `id` | UUID | Clave primaria |
| `titulo` | texto, obligatorio | Máximo 200 caracteres |
| `descripcion` | texto, opcional | Máximo 2000 caracteres |
| `horizonte` | enum | Plazo: `SIN_PLAZO`, `DIA`, `SEMANA`, `MES`, `LIMITE` |
| `fecha_referencia` | fecha, opcional | Según el plazo; nula si y solo si es `SIN_PLAZO` (restricción en la base) |
| `prioridad` | enum | `ALTA`, `MEDIA`, `BAJA` |
| `estado` | enum | `PENDIENTE`, `EN_CURSO`, `FINALIZADA` |
| `ambito` | enum | `PERSONAL`, `GRUPO` |
| `usuario_id` | UUID | Creador / dueño |
| `grupo_id` | UUID, opcional | Obligatorio si `ambito = GRUPO` (restricción en la base) |
| `asignado_a` | UUID, opcional | Etapa 2 |
| `creada_en`, `actualizada_en` | timestamp | En UTC |
| `completada_en` | timestamp, opcional | Se completa al pasar a `FINALIZADA` y se borra si vuelve atrás (restricción en la base) |

Tabla `proyecto` (`V3__proyecto.sql`): `id`, `usuario_id`, `nombre`, `color`, `creado_en`. La tarea suma `proyecto_id` (clave foránea; al borrar el proyecto queda en nulo).

`V4__pasos_y_papelera.sql` suma la tabla `paso` (`tarea_id`, `texto`, `hecho`, `orden`; se borra con su tarea) y `tarea.eliminada_en` (papelera).

`V5__repeticion_recordatorio_busqueda.sql` suma `repeticion`, `siguiente_creada`, `recordatorio`, `recordatorio_avisado` y la extensión `unaccent` para el buscador.

Etapa 2 agrega las tablas `grupo` y `miembro_grupo` (grupo, usuario y rol) y las claves foráneas de `grupo_id` y `asignado_a`. El modelo queda preparado desde el MVP con `usuario_id` y `ambito`, para no rehacer la base.

Los cambios de esquema se hacen con migraciones Flyway, nunca a mano.

## 8. Stack y estructura técnica

- El mismo stack que el web principal de CHI2: Java 21, Spring Boot, Thymeleaf, Tailwind CSS, PostgreSQL con Flyway, JUnit 5 y Testcontainers, Maven (detalle en `CLAUDE.md`).
- El backend se expone como **API REST** desde el inicio. La interfaz web consume esa misma API: Thymeleaf arma el marco de cada vista y el navegador trae las tareas de `/api/tareas`.
- Arrastre de tarjetas: arrastrar y soltar nativo de HTML5 con JavaScript propio, sin librerías (propuesta, `PENDIENTES.md` 1).
- El JavaScript está en módulos ES en `static/js/` (sin build): la lógica sin DOM (`atajos.js`, `formato.js`) tiene tests con el runner de Node (`src/test/js`), que corren en `./mvnw verify`.
- Las páginas de error (404 y 500) tienen el diseño de la app (`templates/error.html`).
- Corre en el puerto **8082**, para poder tenerla abierta junto con el web principal (8080) y peluquería (8081).

Endpoints del MVP:

| Método y ruta | Qué hace | Respuesta |
|---|---|---|
| `GET /api/tareas?desde=&hasta=&prioridad=&estado=` | Tareas planificadas dentro del rango, según la regla de §4.3. Sin `hasta`, el rango no tiene fin. `prioridad` y `estado` filtran. | 200 / 400 |
| `GET /api/tareas/hoy?prioridad=&estado=` | Vista Hoy (§4.3). | 200 |
| `GET /api/tareas/sin-plazo?prioridad=&estado=` | Por planificar. | 200 |
| `GET /api/tareas/{id}` | Una tarea. | 200 / 404 |
| `POST /api/tareas` | Alta. Cuerpo: `titulo`, `descripcion`, `horizonte`, `fechaReferencia`, `prioridad`. Sin fecha (salvo sin plazo), se usa hoy; sin prioridad, `MEDIA`. | 201 / 400 |
| `PATCH /api/tareas/{id}/plazo` | `{ "horizonte": "DIA", "fechaReferencia": "2026-11-24" }`: planificar o reprogramar. | 200 / 400 / 404 |
| `PUT /api/tareas/{id}` | Edición completa, mismo cuerpo que el alta. | 200 / 400 / 404 |
| `PATCH /api/tareas/{id}/estado` | `{ "estado": "FINALIZADA" }` | 200 / 400 / 404 |
| `PATCH /api/tareas/{id}/prioridad` | `{ "prioridad": "ALTA" }` | 200 / 400 / 404 |
| `DELETE /api/tareas/{id}` | Elimina. | 204 / 404 |
| `GET /api/proyectos` | Proyectos del usuario, por nombre. | 200 |
| `POST /api/proyectos` | `{ "nombre": "Tesis", "color": "PINO" }` (color opcional, rota si falta). | 201 / 400 / 409 |
| `PUT /api/proyectos/{id}` | Renombrar o cambiar el color. | 200 / 400 / 404 / 409 |
| `DELETE /api/proyectos/{id}` | Elimina; sus tareas quedan sin proyecto. | 204 / 404 |

| `POST /api/tareas/{id}/pasos` | `{ "texto": "Buscar fuentes" }`: agrega un paso. Devuelve la tarea. | 201 / 400 / 404 |
| `PATCH /api/tareas/{id}/pasos/{pasoId}` | `{ "hecho": true }` y/o `{ "texto": "..." }`. | 200 / 400 / 404 |
| `DELETE /api/tareas/{id}/pasos/{pasoId}` | Quita un paso. Devuelve la tarea. | 200 / 404 |
| `GET /api/papelera` | Tareas eliminadas, la más reciente primero. | 200 |
| `POST /api/papelera/{id}/restaurar` | Restaura. | 200 / 404 |
| `DELETE /api/papelera/{id}` | Borra para siempre. | 204 / 404 |
| `DELETE /api/papelera` | Vacía la papelera. | 204 |

| `GET /api/tareas/buscar?q=` | Hasta 30 tareas que contienen el texto. | 200 |
| `GET /api/tareas/recordatorios` | Recordatorios que ya llegaron y no se avisaron. | 200 |
| `POST /api/tareas/{id}/recordatorio/avisado` | Marca el recordatorio como avisado. | 204 / 404 |

El alta y la edición aceptan `repeticion` (`NINGUNA`, `DIARIA`, `HABILES`, `SEMANAL`, `MENSUAL`) y `recordatorio` (instante ISO); la respuesta suma `proximaRepeticion`. `DELETE /api/tareas/{id}` manda la tarea a la papelera. Los listados de tareas aceptan además `proyecto=<id>`. El alta y la edición de tareas aceptan `proyectoId`.

Los errores vuelven como `ProblemDetail`; los de validación, con un mapa `errores` por campo. Una tarea de otro usuario responde 404, igual que una inexistente.

Etapa 2 suma los endpoints de grupos y miembros y el parámetro `ambito` en los listados.

## 9. Pruebas

Implementadas (`./mvnw verify`):

- Unitarias: cálculo de fecha de referencia y fin de período por horizonte (`HorizonteTest`), qué tareas entran en cada vista (`RangoTest`), transiciones de estado, `completada_en` y vencimiento (`TareaTest`), orden por prioridad (`OrdenTareasTest`), "hoy" en hora argentina y títulos de período (`CalendarioTest`).
- Navegador (`src/test/js`, `node --test`): atajos del alta rápida, calendario (semanas de un mes), agrupación de la lista, fechas que se muestran, resumen de la vista y separación de finalizadas.
- Controladores con el service simulado: API (`TareaApiControllerTest`) y páginas (`TareaPaginaControllerTest`).
- Integración de la API con PostgreSQL real (`TareaIntegracionTest`, Testcontainers): alta rápida, vista Hoy con vencidas, semana, mes, largo plazo, arrastre a finalizada, edición, eliminación y que no se vean tareas de otro usuario.

Pendiente:

- Etapa 2: pruebas de permisos (un miembro no ve tareas personales de otro, un `MIEMBRO` no elimina tareas ajenas).
- Prueba manual del arrastre y del alta rápida en al menos dos navegadores (`PENDIENTES.md` 7).

## 10. Decisiones tomadas

- El gestor de tareas es un módulo de CHI2, separado del web principal (repositorio propio).
- Primero se usa en forma personal, después con grupos.
- Plazos anidados: sin plazo, día, semana, mes y fecha límite. Largo plazo es una vista, no un plazo (2026-11-19).
- Prioridades: alta, media y baja.
- El tablero usa los estados pendiente, en curso y finalizada.
- En grupos, las tareas de grupo son visibles para todos los miembros; las personales, solo para su dueño.
- El backend es API REST desde el inicio.
- "Hoy" se calcula en hora de Argentina (`chi2.tareas.zona`); todo se guarda en UTC.

## 11. Pendientes

Los pendientes se registran solo en `PENDIENTES.md`. Los que salen de este documento son: arrastre (1), atajos (2), inicio de semana (3), inicio automático (4), bocetos en Figma (5), paleta y tipografía (6), prueba en dos navegadores (7), login para grupos (8) y asignación de tareas de grupo (9).

## 12. Ajustes hechos al programar el MVP

- **El alta rápida usa el horizonte de la vista activa.** En Hoy es `DIA` con fecha de hoy, como pedía la especificación; en Semana o Mes, la tarea va al período que se está mirando, y en Largo plazo, sin fecha. Así, cargar desde la vista de la semana que viene no obliga a cambiar nada.
- **Hoy incluye las vencidas de cualquier horizonte,** no solo las del día: una tarea de la semana pasada sin terminar también aparece, marcada como "Vencida". Y las de largo plazo con fecha límite hoy.
- **Lista y tablero en todas las vistas.** La especificación pedía ambas cosas; se resolvió con un selector Lista / Tablero.
- **Fecha opcional en la API:** si no viene (salvo largo plazo), se usa hoy. Simplifica el alta desde una app nativa.
- **Sin login en la etapa 1:** todas las tareas son de un usuario fijo (`chi2.tareas.usuario-local`), detrás de la interfaz `UsuarioActual` que la etapa 2 reemplaza por el usuario logueado.
- **Finalizadas al final de la lista.** La especificación ordena por prioridad; las finalizadas mezcladas con las pendientes hacían ruido, así que en la lista van en un bloque plegable al final (en el tablero ya tienen su columna).
- **Atajos globales y ayuda (`?`)**, que la especificación no pedía, para usar la app sin mouse.
- **Plazos anidados (2026-11-19, pedido de Nahuel).** El MVP tenía cuatro horizontes excluyentes y cada vista mostraba solo el suyo. Se reemplazó por plazos anidados (§4.1 y §4.3), la vista Por planificar y los calendarios de semana y mes. Las tareas `LARGO_PLAZO` existentes pasaron a `SIN_PLAZO` (sin fecha) o `LIMITE` (con fecha).
