/*
 * Gestor de tareas CHI2: punto de entrada de la página de cada vista.
 * Todo pasa por la API REST (/api/tareas); la página no guarda tareas por su cuenta.
 */

import { api } from "./api.js";
import { iniciarAlta } from "./alta.js";
import { avisar, guardarPreferencia, leerPreferencia } from "./dom.js";
import { isoLocal, resumen } from "./formato.js";
import { iniciarPanel } from "./panel.js";
import { dibujar, habilitarArrastre } from "./render.js";

const raiz = document.getElementById("app");
const contexto = Object.freeze({
	vista: raiz.dataset.vista,
	fechaVista: raiz.dataset.fechaVista || null,
	hoy: raiz.dataset.hoy,
	urlLista: raiz.dataset.urlLista,
	conCalendario: raiz.dataset.conCalendario === "true"
});
const TITULO = document.title;

// Semana y Mes recuerdan su modo aparte: ahí el calendario es lo natural.
const CLAVE_MODO = contexto.conCalendario ? "chi2-tareas-modo-calendario" : "chi2-tareas-modo";
const MODOS = contexto.conCalendario ? ["calendario", "lista", "tablero"] : ["lista", "tablero"];
const pantallaChica = window.matchMedia("(max-width: 767px)").matches;

const estado = {
	tareas: [],
	modo: modoInicial(),
	cargando: true,
	error: false
};

function modoInicial() {
	const guardado = leerPreferencia(CLAVE_MODO, MODOS[0]);
	const modo = MODOS.includes(guardado) ? guardado : MODOS[0];
	// En el celular la grilla no entra: se arranca en lista.
	return modo === "calendario" && pantallaChica ? "lista" : modo;
}

const acciones = {
	abrir: tarea => panel.abrir(tarea),
	cambiarEstado,
	mover
};

function render() {
	dibujar(estado, contexto, acciones);
	document.getElementById("resumen").textContent = estado.cargando || estado.error ? "" : resumen(estado.tareas);
}

async function cargar() {
	try {
		estado.tareas = await api.listar(contexto.urlLista);
		estado.error = false;
	} catch {
		estado.error = true;
	}
	estado.cargando = false;
	render();
	actualizarContadores();
}

/** Pendientes de hoy y tareas por planificar, en la navegación; los de hoy también en el título. */
async function actualizarContadores() {
	try {
		const [deHoy, sinPlazo] = await Promise.all([
			contexto.vista === "HOY" ? estado.tareas : api.listar("/api/tareas/hoy"),
			contexto.vista === "POR_PLANIFICAR" ? estado.tareas : api.listar("/api/tareas/sin-plazo")
		]);
		const abiertas = lista => lista.filter(t => t.estado !== "FINALIZADA").length;
		mostrarContador("[data-contador-hoy]", abiertas(deHoy));
		mostrarContador("[data-contador-sin-plazo]", abiertas(sinPlazo));
		const pendientesHoy = abiertas(deHoy);
		document.title = (pendientesHoy ? `(${pendientesHoy}) ` : "") + TITULO;
	} catch {
		// los contadores son accesorios: si fallan, la vista ya muestra el problema
	}
}

function mostrarContador(selector, cantidad) {
	const contador = document.querySelector(selector);
	if (!contador) return;
	contador.textContent = cantidad;
	contador.hidden = cantidad === 0;
}

async function cambiarEstado(tarea, nuevo) {
	if (tarea.estado === nuevo) return;
	const anterior = tarea.estado;
	tarea.estado = nuevo; // se ve al instante; si falla, se vuelve atrás
	render();
	try {
		Object.assign(tarea, await api.cambiarEstado(tarea.id, nuevo));
		render();
		actualizarContadores();
	} catch (e) {
		tarea.estado = anterior;
		render();
		avisar(e.message, { error: true });
	}
}

/** Planificar o reprogramar (arrastre en el calendario, botones de Por planificar). */
async function mover(tarea, horizonte, fecha) {
	const anterior = { horizonte: tarea.horizonte, fechaReferencia: tarea.fechaReferencia };
	Object.assign(tarea, { horizonte, fechaReferencia: horizonte === "SIN_PLAZO" ? null : fecha });
	render();
	try {
		await api.cambiarPlazo(tarea.id, horizonte, fecha);
		await cargar();
	} catch (e) {
		Object.assign(tarea, anterior);
		render();
		avisar(e.primerMensaje ?? e.message, { error: true });
	}
}

function cambiarModo(modo) {
	if (!MODOS.includes(modo)) return;
	estado.modo = modo;
	guardarPreferencia(CLAVE_MODO, modo);
	render();
}

const panel = iniciarPanel(contexto, cargar);
const alta = iniciarAlta(contexto, async nueva => {
	await cargar();
	return estado.tareas.some(t => t.id === nueva.id);
});
habilitarArrastre(id => estado.tareas.find(t => t.id === id), { cambiarEstado, mover });

document.querySelectorAll("[data-modo]").forEach(b => b.addEventListener("click", () => cambiarModo(b.dataset.modo)));
document.getElementById("reintentar").addEventListener("click", () => {
	estado.cargando = true;
	render();
	cargar();
});

// ---------- Teclado ----------

const ayuda = document.getElementById("ayuda");
document.querySelectorAll("[data-abrir-ayuda]").forEach(b => b.addEventListener("click", () => ayuda.showModal()));
ayuda.addEventListener("click", e => { if (e.target === ayuda) ayuda.close(); });

const ATAJOS_GLOBALES = {
	n: () => alta.enfocar(),
	"/": () => alta.enfocar(),
	t: () => cambiarModo(MODOS[(MODOS.indexOf(estado.modo) + 1) % MODOS.length]),
	"?": () => ayuda.showModal(),
	ArrowLeft: () => irA("[data-periodo='anterior']"),
	ArrowRight: () => irA("[data-periodo='siguiente']"),
	1: () => irA("[data-vista='HOY']"),
	2: () => irA("[data-vista='SEMANA']"),
	3: () => irA("[data-vista='MES']"),
	4: () => irA("[data-vista='LARGO_PLAZO']"),
	5: () => irA("[data-vista='POR_PLANIFICAR']")
};

function irA(selector) {
	const enlace = document.querySelector(selector);
	if (enlace) location.href = enlace.href;
}

document.addEventListener("keydown", e => {
	if (e.key === "Escape" && panel.estaAbierto()) { panel.cerrar(); return; }
	const escribiendo = e.target.closest("input, textarea, select, [contenteditable]");
	if (escribiendo || e.ctrlKey || e.metaKey || e.altKey || panel.estaAbierto() || ayuda.open) return;
	const atajo = ATAJOS_GLOBALES[e.key];
	if (atajo) {
		e.preventDefault();
		atajo();
	}
});

// Si la ventana quedó abierta de un día para otro, al volver se recarga entera; si no, solo la lista.
document.addEventListener("visibilitychange", () => {
	if (document.visibilityState !== "visible" || panel.estaAbierto()) return;
	if (isoLocal() !== contexto.hoy) location.reload();
	else cargar();
});

render();
cargar();
alta.enfocar();
