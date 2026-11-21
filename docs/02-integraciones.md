# Integraciones · CHI2 Gestor de tareas

Qué se conecta con qué: lo que ya está hecho, lo que viene y por qué. Claude lo revisa al terminar cada integración y **recomienda la siguiente**, con el valor que aporta, el esfuerzo y lo que hace falta que haga una persona (cuentas, credenciales, permisos).

**Última actualización:** 2026-11-21
**Referencias:** `docs/01-especificacion.md` · `PENDIENTES.md` (las tareas para personas, como crear credenciales, van ahí)

**Reglas para cualquier integración:**
- Las credenciales (claves, tokens, secretos) **nunca** van al repo: variables de entorno o archivo local ignorado por git.
- Se apagan por configuración: si falta la credencial, la app arranca igual y la integración queda desactivada.
- Lo que sale de la app (a Google u otro servicio) se documenta acá: qué datos y para qué.
- Cada una lleva sus tests, con el servicio externo simulado.

---

## Realizadas

| Integración | Qué hace | Desde |
|---|---|---|
| **API REST + OpenAPI** | Todo lo que hace la web está en `/api/...`, documentado en `/api/docs`. Es la base de cualquier integración o de una app nativa. | 0.1.0 / 0.2.0 |
| **Exportar a Excel / Google Sheets** | Menú ⋯ → *Exportar tareas (CSV)*: se abre directo en Excel; en Sheets, *Archivo → Importar*. | sin publicar |
| **Respaldo JSON** | Respaldo completo descargable y uno automático por día en `~/CHI2-Tareas/respaldos`. | sin publicar |
| **Notificaciones del sistema** | Los recordatorios usan las notificaciones de Windows desde el navegador. | 0.2.0 |

## Pendientes, en el orden recomendado

| # | Integración | Valor | Esfuerzo | Qué hace falta de una persona |
|---|---|---|---|---|
| 1 | **Calendario en formato iCalendar (.ics)** | Alto | Bajo | Nada |
| 2 | **Google Calendar (sincronización)** | Alto | Medio | Crear credenciales OAuth en Google Cloud (pasos abajo) |
| 3 | **Fechas de la facultad desde el aula virtual** | Alto | Bajo | El enlace de exportación del calendario del aula virtual |
| 4 | **Telegram: recordatorios y alta desde el celular** | Alto | Medio | Crear un bot con @BotFather (2 minutos) |
| 5 | **Resumen diario por mail** | Medio | Bajo | Una cuenta para enviar (puede ser la de CHI2 del web principal) |
| 6 | **Respaldo en Google Drive** | Medio | Medio | Las mismas credenciales de Google del punto 2 |
| 7 | **WhatsApp** | Alto | Alto | Cuenta de WhatsApp Business y la app publicada en internet |

### 1. Calendario .ics (recomendada para empezar)
- **Qué:** un archivo `.ics` (estándar que entienden todos los calendarios) con las tareas que tienen día o fecha límite, como eventos de día completo, más los recordatorios como alarmas.
- **Cómo se usa:** menú ⋯ → *Descargar calendario (.ics)* y se importa en Google Calendar (*Configuración → Importar*). Outlook y el Calendario de Apple, además, se pueden **suscribir** a `http://localhost:8082/calendario.ics` y se actualizan solos.
- **Límite:** Google no puede suscribirse mientras la app corra en la PC (sus servidores no llegan a `localhost`); para eso está el punto 2.

### 2. Google Calendar (sincronización)
- **Qué:** las tareas con día, semana, mes o fecha límite aparecen en un calendario propio, **"CHI2 Tareas"**, dentro de tu Google Calendar (no se mezcla con los demás). Si la movés o la finalizás en la app, se actualiza; si la eliminás, se borra el evento. Las de día y fecha límite van como eventos de todo el día; las de semana o mes, como eventos que abarcan el período.
- **Segunda etapa, opcional:** que lo que cargues en ese calendario desde el celular entre a la app como tarea.
- **Qué datos salen:** título, descripción, fecha y estado de las tareas con plazo. Nada más.
- **Cómo se construye:** OAuth 2.0 de Google con Spring Security (cliente OAuth), permiso mínimo `calendar.app.created` (solo puede tocar el calendario que crea la app), token guardado cifrado en la base, sincronización al guardar cada tarea y una pasada cada 15 minutos para reintentar lo que falló. Se activa desde el menú ⋯ → *Conectar Google Calendar*.
- **Qué hace falta (Nahuel):**
  1. Entrar a <https://console.cloud.google.com/> con tu cuenta de Google y crear un proyecto, por ejemplo "CHI2 Tareas".
  2. *APIs y servicios → Biblioteca* → habilitar **Google Calendar API**.
  3. *Pantalla de consentimiento de OAuth*: tipo **Externo**, en modo **Prueba**, y agregarte como usuario de prueba.
  4. *Credenciales → Crear credenciales → ID de cliente de OAuth* → tipo **Aplicación web**, con este URI de redireccionamiento autorizado: `http://localhost:8082/login/oauth2/code/google`.
  5. Guardar el **ID de cliente** y el **secreto** como variables de entorno de Windows: `CHI2_GOOGLE_CLIENTE_ID` y `CHI2_GOOGLE_CLIENTE_SECRETO`. No pegarlos en el chat ni en el repo.

### 3. Fechas de la facultad (aula virtual)
- **Qué:** el aula virtual de la UTN (Moodle) exporta su calendario como `.ics`. La app lo leería periódicamente y crearía las entregas y parciales como tareas con **fecha límite** en un proyecto "Facultad", sin duplicarlas.
- **Qué hace falta:** en el aula virtual, *Calendario → Exportar calendario → Obtener URL del calendario*, y pasarle ese enlace a Claude (es privado: va como variable de entorno).

### 4. Telegram
- **Qué:** un bot propio que manda los recordatorios al celular aunque no estés frente a la PC, y que acepta tareas por mensaje con los mismos atajos (`Pagar la luz !a @viernes`). Funciona sin publicar la app: el bot consulta a Telegram desde la PC.
- **Límite:** la PC tiene que estar prendida con la app corriendo (pendiente 4 de `PENDIENTES.md`).
- **Qué hace falta:** hablar con @BotFather en Telegram, crear el bot y guardar el token como variable de entorno.

### 5. Resumen diario por mail
- **Qué:** a la mañana, un mail con lo de hoy, lo vencido y lo que vence esta semana. El web principal ya tiene la parte de mail resuelta (Gmail SMTP y Mailpit en desarrollo) y se puede reutilizar el mismo enfoque.

### 6. Respaldo en Google Drive
- **Qué:** subir el respaldo diario a una carpeta de Drive, para que no dependa del disco de la PC. Usa las credenciales del punto 2, con el permiso mínimo `drive.file`.

### 7. WhatsApp
- **Qué:** lo mismo que Telegram, por WhatsApp, que es el canal principal de CHI2. Requiere la API oficial de WhatsApp Business (Meta), que necesita la app publicada en internet: tiene sentido después del hosting y del login (etapa 2).

---

## Descartadas o en espera

| Integración | Motivo |
|---|---|
| Google Tasks | Se pisa con la propia app; Google Calendar cubre lo que se necesita ver afuera. |
| Notion, Trello, Todoist | Son gestores de tareas: competirían con la app en lugar de complementarla. Si hiciera falta migrar datos, alcanza con el CSV/JSON. |
