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
	horizonteVista: raiz.dataset.horizonte,
	fechaVista: raiz.dataset.fechaVista || null,
	fechaAlta: raiz.dataset.fechaVista || null,
	hoy: raiz.dataset.hoy,
	urlLista: raiz.dataset.urlLista
});
const TITULO = document.title;

const estado = {
	tareas: [],
	modo: leerPreferencia("chi2-tareas-modo", "lista") === "tablero" ? "tablero" : "lista",
	cargando: true,
	error: false
};

const acciones = {
	abrir: tarea => panel.abrir(tarea),
	cambiarEstado
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
	actualizarContadorHoy();
}

/** Pendientes de hoy (con vencidas) en la pestaña "Hoy" y en el título de la ventana. */
async function actualizarContadorHoy() {
	try {
		const deHoy = contexto.vista === "HOY" ? estado.tareas : await api.listar("/api/tareas/hoy");
		const pendientes = deHoy.filter(t => t.estado !== "FINALIZADA").length;
		const contador = document.querySelector("[data-contador-hoy]");
		contador.textContent = pendientes;
		contador.hidden = pendientes === 0;
		document.title = (pendientes ? `(${pendientes}) ` : "") + TITULO;
	} catch {
		// el contador es accesorio: si falla, la vista ya muestra el problema
	}
}

async function cambiarEstado(tarea, nuevo) {
	if (tarea.estado === nuevo) return;
	const anterior = tarea.estado;
	tarea.estado = nuevo; // se ve al instante; si falla, se vuelve atrás
	render();
	try {
		Object.assign(tarea, await api.cambiarEstado(tarea.id, nuevo));
		render();
		actualizarContadorHoy();
	} catch (e) {
		tarea.estado = anterior;
		render();
		avisar(e.message, { error: true });
	}
}

function cambiarModo(modo) {
	estado.modo = modo;
	guardarPreferencia("chi2-tareas-modo", modo);
	render();
}

const panel = iniciarPanel(contexto, cargar);
const alta = iniciarAlta(contexto, async nueva => {
	await cargar();
	return estado.tareas.some(t => t.id === nueva.id);
});
habilitarArrastre(id => estado.tareas.find(t => t.id === id), cambiarEstado);

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
	t: () => cambiarModo(estado.modo === "lista" ? "tablero" : "lista"),
	"?": () => ayuda.showModal(),
	ArrowLeft: () => irA("[data-periodo='anterior']"),
	ArrowRight: () => irA("[data-periodo='siguiente']"),
	1: () => irA("[data-vista='HOY']"),
	2: () => irA("[data-vista='SEMANA']"),
	3: () => irA("[data-vista='MES']"),
	4: () => irA("[data-vista='LARGO_PLAZO']")
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
