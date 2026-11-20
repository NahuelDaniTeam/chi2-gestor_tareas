# CHI2 · Gestor de tareas

Módulo de CHI2 para planificar tareas personales por día, semana, mes y largo plazo, con prioridades y tablero. Se abre desde la computadora sin pasos extra. Primero es de uso personal; después suma grupos con tareas compartidas (por ejemplo, el equipo de la tesis).

**Estado:** MVP (etapa 1, uso personal) implementado. Falta confirmar las propuestas de `PENDIENTES.md`.
**Última actualización:** 2026-11-19

**Documento de referencia:** `docs/01-especificacion.md` (funcionalidad, modelo de datos, API y ajustes hechos al programar). Todo lo que se construya tiene que respetarlo; si este archivo y ese documento se contradicen, manda el documento y se corrige este archivo.

**Relación con el resto de CHI2:** es un repositorio aparte del web principal (`workspaces/chi2/general` en Orca), con el mismo stack, la misma marca y las mismas reglas de trabajo. Si el web cambia una convención general (paleta, tipografías, reglas de arquitectura), se copia acá.

---

## 1. Identidad

- **Nombre del módulo:** Gestor de tareas (`gestion-tareas` dentro de CHI2).
- **Etapas:** 1, uso personal sin login · 2, grupos con roles `ADMIN` y `MIEMBRO` · futura, producto a medida para vender.
- **Tono visual:** sobrio y editorial, igual que el web principal. Fondo claro, sin elementos genéricos de plantilla.
- **Paleta y tipografías:** heredadas del web principal (provisorias allá): fondo marfil `#f7f4ee`, tinta `#191714`, acento terracota `#b5532c`, títulos en Instrument Serif y texto en Inter. Viven en el bloque `@theme` de `src/main/frontend/main.css` y en el `<link>` de `fragments/layout.html` (`PENDIENTES.md` 6).
- **Logo:** el isotipo χ² de CHI2 + "CHI2" + el rótulo "Tareas" (fragmentos `isotipo` y `logo` de `fragments/layout.html`).

## 2. Qué hace (resumen de `docs/01-especificacion.md`)

- **Tareas** con título, descripción, plazo anidado (`SIN_PLAZO`, `DIA`, `SEMANA`, `MES`, `LIMITE`; campo `horizonte`), fecha de referencia, prioridad (`ALTA`, `MEDIA`, `BAJA`) y estado (`PENDIENTE`, `EN_CURSO`, `FINALIZADA`).
- **Vistas:** Hoy (pantalla inicial, con las vencidas), Semana y Mes (calendario, lista o tablero), Largo plazo y Por planificar. Cada vista incluye lo que cae dentro de ella (`Rango`).
- **Alta rápida** con foco al abrir, Enter para guardar y atajos `!a` `@s`.
- **Tablero** con arrastre entre columnas y **panel lateral** de edición; todo se guarda al instante.

## 3. Uso diario

- Acceso directo en el escritorio (`scripts/crear-acceso-directo.ps1` → `scripts/abrir-tareas.cmd`), que levanta la app si no está corriendo y abre el navegador.
- Puerto **8082** (el web principal usa 8080 y peluquería 8081).

## 4. Stack técnico

- **Backend:** Java 21 con Spring Boot
- **Plantillas:** Thymeleaf (solo el marco de cada vista; las tareas las trae el navegador de la API)
- **Interacción:** JavaScript propio en módulos ES, sin librerías ni build (`static/js/`): alta rápida, lista, tablero con arrastre nativo de HTML5 y panel de edición. Tests de la lógica sin DOM con `node --test`
- **Estilos:** Tailwind CSS
- **Base de datos:** PostgreSQL, con migraciones Flyway
- **Pruebas:** JUnit 5 y Testcontainers
- **Build:** Maven
- **Diseño previo:** Figma, antes de codificar pantallas definitivas (`PENDIENTES.md` 5)

**Decisión de arquitectura:** el backend es API REST desde el inicio (`/api/tareas`); la web consume esa misma API, y una app nativa futura también.

### Cómo correr el proyecto

Requisitos: Java 21, Docker Desktop **abierto** (la base local y los tests usan Postgres en Docker). Node no hace falta: Maven lo descarga solo.

| Qué | Comando |
|---|---|
| Levantar la app (http://localhost:8082) | `./mvnw spring-boot:run` — arranca Postgres solo vía `compose.yaml` y lo deja corriendo al cerrar |
| Build completo + todos los tests | `./mvnw verify` |
| Recompilar CSS mientras se editan plantillas o el JS | `npm run watch:css` (en otra terminal, con la app corriendo) |

### Estructura y convenciones

```
docs/                          especificaciones (NN-tema.md)
PENDIENTES.md                  hoja de pendientes del equipo (§10)
compose.yaml                   Postgres para desarrollo (volumen tareas-datos)
scripts/                       abrir la app y crear el acceso directo en Windows
src/main/java/com/chi2/tareas/
├── GestorTareasApplication.java
├── ConfiguracionGeneral.java  Clock (UTC)
├── TareasProperties.java      chi2.tareas.* (zona horaria, usuario local)
├── usuario/                   UsuarioActual: quién usa la app (etapa 1: UsuarioLocal, sin login)
├── tarea/                     tareas: dominio, plazos (Rango), calendario, service, repository, páginas (docs/01)
├── proyecto/                  proyectos o etiquetas de las tareas (§4.6)
└── api/                       @RestController bajo /api/... y su manejo de errores
src/main/resources/
├── templates/                 vistas Thymeleaf (fragments/layout.html = piezas compartidas)
├── static/                    favicon, íconos, manifest, img/iconos.svg (sprite) y js/ (módulos)
└── db/migration/              migraciones Flyway
src/main/frontend/main.css     entrada de Tailwind (escanea plantillas y JS)
src/test/java/...              tests, mismo paquete que la clase que prueban
src/test/js/                   tests de los módulos de static/js sin DOM (node --test)
```

- Una funcionalidad nueva va en su propio paquete con sus capas (ver "Arquitectura en capas"); su `@RestController` va en `api/`. La etapa 2 suma el paquete `grupo/`.
- `templates/fragments/layout.html` tiene las piezas compartidas: `head(titulo)`, `icono(nombre, clase)`, `isotipo(clase)`, `logo` y `encabezado`.
- `static/js/`: `app.js` es la entrada; `api.js` habla con `/api/tareas`; `render.js`, `alta.js` y `panel.js` manejan cada parte de la pantalla; `atajos.js` y `formato.js` son lógica pura, sin DOM, y son los que se testean. Las clases de Tailwind se escriben completas (Tailwind las encuentra escaneando los archivos). Nada de `innerHTML` con datos del usuario: siempre `textContent` (helper `el()` de `dom.js`).
- Íconos: sprite `static/img/iconos.svg`, desde Thymeleaf con el fragmento `icono(nombre, clase)` y desde JS con `icono()` de `dom.js`. No usar caracteres sueltos (✕, ←) como íconos.
- `src/main/frontend/main.css` — entrada de Tailwind v4; los tokens de marca van en el bloque `@theme`. El CSS compilado (`static/css/main.css`) se genera en el build y **no se commitea**.
- `src/main/resources/db/migration/` — migraciones Flyway `V<n>__descripcion.sql`. **Nunca editar una migración ya mergeada a `main`**; crear una nueva. Si los dos crean la misma versión `V<n>` en ramas distintas, quien mergea segundo renumera la suya.
- El esquema lo define solo Flyway (`ddl-auto=validate`).
- La app corre en **UTC** (fechas guardadas en UTC). Qué día es "hoy" se decide con la zona `chi2.tareas.zona` (Argentina) a través de `Calendario`; nunca con `LocalDate.now()`.
- Etapa 1 sin login: el dueño de las tareas sale de `UsuarioActual` (hoy `UsuarioLocal`, con `chi2.tareas.usuario-local`). Toda consulta filtra por usuario y ámbito; una tarea ajena responde 404, igual que una inexistente.
- Todo endpoint o página nueva lleva su test (`@WebMvcTest` para controladores; `@SpringBootTest` + Testcontainers cuando toca la base).

### Arquitectura en capas (regla para todo el código)

El código se organiza **por funcionalidad** (`com.chi2.tareas.<funcionalidad>`, ej: `tarea`, y `grupo` en la etapa 2) y, dentro de cada una, **en capas**:

| Capa | Clase | Responsabilidad | No hace |
|---|---|---|---|
| Controller | `XxxController` (páginas Thymeleaf) · `api/XxxApiController` (REST, bajo `/api/...`) | Recibe el pedido HTTP, valida el DTO de entrada (`@Valid`), llama al service y devuelve la vista o el DTO de salida. | Lógica de negocio, acceso a repositorios, manejo de entidades. |
| Service | `XxxService` | Lógica de negocio, reglas y transacciones (`@Transactional`). Recibe y devuelve DTOs o tipos de dominio. | Conocer HTTP (`HttpServletRequest`, `Model`, códigos de estado). |
| Repository | `XxxRepository` (Spring Data JPA) | Acceso a datos. Consultas derivadas o `@Query`. | Lógica de negocio. |
| Entity | `Xxx` (`@Entity`) | Modelo persistido, con su estado e invariantes. | Salir de la capa de service: **nunca** se expone en una vista ni en la API. |
| DTO | `XxxForm` / `XxxRequest` (entrada) · `XxxResponse` (salida) | `record` de Java con Bean Validation. Es el contrato con la vista o la API. | Lógica. |
| Mapper | `XxxMapper` | Convierte entre entidad y DTO, a mano (sin librerías de mapeo). | Lógica de negocio. |

- **Las dependencias van en una sola dirección:** controller → service → repository → entity. Nunca al revés ni salteando capas (un controller no usa un repository).
- Lo que se conecta con el exterior (mail, servicios externos, reloj) va detrás de una **interfaz** propia del dominio (ej: `UsuarioActual`), con su implementación aparte (ej: `UsuarioLocal`).
- Errores: excepciones de dominio propias (ej: `TareaNoEncontradaException`), traducidas a HTTP en un `@RestControllerAdvice` con `ProblemDetail`, no con `try/catch` en cada controller.
- Configuración en records `@ConfigurationProperties`, no `@Value` sueltos.
- Si el contenido es fijo y no hay base (como `CatalogoProyectos`), la clase cumple el rol de repository y se respeta el mismo flujo.

### Principios de diseño

**SOLID**, aplicado así en este proyecto:

- **S (responsabilidad única):** una clase, un motivo para cambiar. Ej: `TareaService` aplica las reglas de las tareas; qué día es hoy y cómo se llama cada período lo resuelve `Calendario`.
- **O (abierto/cerrado):** se extiende sumando implementaciones, no editando las existentes. Ej: el login de la etapa 2 será una nueva implementación de `UsuarioActual`.
- **L (sustitución de Liskov):** toda implementación de una interfaz respeta su contrato; los tests de la interfaz pasan con cualquier implementación.
- **I (segregación de interfaces):** interfaces chicas y específicas, no "servicios dios".
- **D (inversión de dependencias):** las capas de arriba dependen de abstracciones. **Inyección por constructor** con campos `final`; nada de `@Autowired` en campos. Inyectar `java.time.Clock` en vez de llamar a `Instant.now()`, para poder testear fechas.

**Además:**

- **KISS y YAGNI:** la solución más simple que cumpla la especificación. No crear interfaces "por las dudas": solo en los límites con lo externo o cuando hay más de una implementación real.
- **DRY:** una regla de negocio vive en un solo lugar (ej: el formulario web y la API usan el mismo service).
- **Nombres del dominio en español** (`Tarea`, `cambiarEstado`, `fechaReferencia`), consistentes con los documentos de `docs/`.
- **Métodos cortos y con nombre que diga qué hacen.** Si un comentario explica *qué* hace un bloque, ese bloque es un método.
- **Sin lógica en las plantillas:** Thymeleaf solo muestra lo que le prepara el controller.
- **Tests por capa:** `@WebMvcTest` con el service mockeado para controllers; tests unitarios para services y mappers; `@DataJpaTest` o `@SpringBootTest` + Testcontainers para repositories y flujos completos.

## 5. Decisiones tomadas

- El gestor de tareas es un módulo de CHI2, en un repositorio separado del web principal.
- Primero se usa en forma personal, después con grupos.
- Horizontes: día, semana, mes y largo plazo. Prioridades: alta, media y baja. Estados: pendiente, en curso y finalizada.
- En grupos, las tareas de grupo las ven todos los miembros; las personales, solo su dueño.
- Backend API REST desde el inicio.
- Mismo stack, marca y forma de trabajo que el web principal.

## 6. Pendientes (`PENDIENTES.md`)

Los pendientes del equipo se registran **solo** en `PENDIENTES.md`, en la raíz del repo. No se duplican acá ni en otros documentos; esos documentos marcan `[PENDIENTE]` en el lugar y la tarea se anota en la hoja.

**Reglas para Claude:**

1. **Identificar con quién hablás:** usá `git config user.name`. `NahuelLuna` es Nahuel y `DanielPatolsky` es Daniel.
2. **Al arrancar cada sesión**, antes de responder lo primero que te pidan, leé `PENDIENTES.md` (la versión de `origin/main` si la rama local está atrasada) y mostrale al usuario en una lista corta:
   - lo que tiene pendiente en su sección (**"Para Nahuel"** o **"Para Daniel"**) y en **"Para los dos"**;
   - cuántos pendientes tiene el compañero, sin el detalle salvo que lo pida.
3. **Al terminar una tarea**, si cumple un pendiente, movelo a **Hechos** con la fecha. Si surge algo nuevo que tiene que hacer el compañero o el usuario, cargalo en la sección correcta con el formato de la hoja (contexto, qué entregar, dónde). **Escribilo para alguien que no estuvo en la conversación**: incluí lo ya decidido y lo que se sabe hasta ahora. Avisale al usuario qué agregaste o cerraste.
4. **Cargar y cerrar pendientes** se hace en la misma rama y PR del trabajo que los genera o resuelve; no en un commit directo a `main`.
5. Si el usuario pide "dejale un pendiente a Daniel" (o a Nahuel), se agrega en la sección del otro con `cargado por <quien>, <fecha>`.

---

## 7. Equipo y forma de trabajo

Trabajamos dos personas en paralelo, cada una con su propio Claude. Estas reglas aplican
a cualquier sesión de Claude en este repo, sea de quien sea (Nahuel o Daniel).

### Ramas

- `main` es la rama estable. **Nunca se commitea ni se pushea directo a `main`.**
- Cada uno trabaja en su rama: `<usuario-github>/<tema>` (ej: `NahuelLuna/login`).
- Ramas cortas y enfocadas en una sola cosa. Mejor varias PRs chicas que una gigante.
- Todo entra a `main` por Pull Request (squash merge). Borrar la rama después del merge.

### Mantenerse sincronizado

- Antes de arrancar una tarea y antes de abrir/actualizar una PR:
  `git fetch origin && git rebase origin/main`.
- Integrar seguido (al menos una vez por sesión) para que los conflictos sean chicos.
- En la rama propia se puede reescribir historia, pero pushear siempre con
  `git push --force-with-lease`, nunca `--force` a secas. Jamás force-push a `main`.

### Resolución de conflictos (regla para Claude)

Cuando un rebase, merge o pull genera conflictos, **Claude los resuelve solo, sin frenar
a preguntar**, siguiendo este proceso:

1. Entender la intención de cada lado antes de tocar nada: leer los commits involucrados
   (`git log origin/main..HEAD`, `git log HEAD..origin/main`, `git show <sha>`) y el código
   alrededor del conflicto.
2. Resolver preservando **las dos** funcionalidades. Nunca descartar en silencio el trabajo
   del compañero (ni el propio) eligiendo "ours"/"theirs" de un archivo entero.
3. Archivos generados (lockfiles, builds, snapshots): no mergear a mano; regenerarlos con
   la herramienta correspondiente después de resolver el resto.
4. Verificar después de resolver: que compile/levante y que pasen los tests (cuando existan).
5. Al terminar, contarle al usuario en un resumen corto qué conflictos hubo y cómo se
   resolvió cada uno.

**Única excepción — preguntar antes de resolver:** si los dos lados cambian el mismo
comportamiento de formas contradictorias (no se pueden conservar ambos), Claude explica
las dos opciones y le pregunta al usuario cuál va.

### Commits

- Commits chicos y descriptivos, en español o inglés pero consistentes dentro de una PR.
- Formato sugerido: `tipo: descripción` (`feat`, `fix`, `refactor`, `docs`, `chore`, `test`).

### Worktrees (Orca)

- Trabajamos con git worktrees: el stack de `git stash` es compartido entre todos los
  worktrees. No usar `git stash` / `git stash pop` a secas; preferir un commit WIP temporal.
