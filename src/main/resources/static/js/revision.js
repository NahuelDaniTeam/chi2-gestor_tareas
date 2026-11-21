/**
 * Revisión semanal (§4.12). Cada tarea se resuelve con un clic y sale de la lista; el aviso ofrece deshacer.
 * Cuándo fue la última revisión se recuerda en este navegador.
 */

import { api } from "./api.js";
import { registrar } from "./deshacer.js";
import { avisar, el, guardarPreferencia, leerPreferencia } from "./dom.js";
import { etiquetaFecha, momento } from "./formato.js";
import { CLAVE_REVISION } from "./revision-aviso.js";

const raiz = document.getElementById("revision");
const { hoy, manana, proximaDesde, proximaHasta } = raiz.dataset;
const contexto = { vista: "REVISION", fechaVista: null, hoy };

const OPCIONES_VENCIDAS = [
	["Hoy", "DIA", hoy], ["Mañana", "DIA", manana], ["Esta semana", "SEMANA", hoy],
	["Próxima semana", "SEMANA", proximaDesde], ["Sin plazo", "SIN_PLAZO", null]
];
const OPCIONES_SIN_PLAZO = [
	["Hoy", "DIA", hoy], ["Esta semana", "SEMANA", hoy], ["Próxima semana", "SEMANA", proximaDesde], ["Este mes", "MES", hoy]
];

async function cargar() {
	const [deHoy, sinPlazo, proxima] = await Promise.all([
		api.listar("/api/tareas/hoy"),
		api.listar("/api/tareas/sin-plazo"),
		api.listar(`/api/tareas?desde=${proximaDesde}&hasta=${proximaHasta}`)
	]);
	llenar("vencidas", deHoy.filter(t => t.vencida), t => acciones(t, OPCIONES_VENCIDAS, true), "Nada vencido. Bien ahí.");
	llenar("sin-plazo", sinPlazo.filter(t => t.estado !== "FINALIZADA"), t => acciones(t, OPCIONES_SIN_PLAZO, false), "Todo tiene plazo.");
	llenar("proxima", proxima.filter(t => t.estado !== "FINALIZADA"), () => null, "Todavía no hay nada planificado para esa semana.");
}

function llenar(id, tareas, accionesDe, vacio) {
	document.querySelector(`[data-cuenta="${id}"]`)?.replaceChildren(tareas.length ? String(tareas.length) : "");
	const filas = tareas.map(t => {
		const fecha = etiquetaFecha(t, contexto);
		return el("li", "fila-revision",
			el("span", "min-w-0 flex-1",
				el("span", "block break-words", t.titulo),
				fecha ? el("span", "mt-0.5 block text-xs text-tenue", fecha) : null),
			accionesDe(t));
	});
	document.getElementById(id).replaceChildren(...(filas.length ? filas : [el("li", "px-5 py-4 text-sm text-tenue", vacio)]));
}

function acciones(t, opciones, conCierre) {
	const botones = opciones.map(([texto, plazo, fecha]) => boton(texto, async () => {
		const anterior = { horizonte: t.horizonte, fechaReferencia: t.fechaReferencia };
		await api.cambiarPlazo(t.id, plazo, fecha);
		hecho(`"${t.titulo}" reprogramada`, () => api.cambiarPlazo(t.id, anterior.horizonte, anterior.fechaReferencia));
	}));
	if (conCierre) {
		botones.push(boton("Finalizar", async () => {
			await api.cambiarEstado(t.id, "FINALIZADA");
			hecho(`"${t.titulo}" finalizada`, () => api.cambiarEstado(t.id, "PENDIENTE"));
		}));
		botones.push(boton("Papelera", async () => {
			await api.eliminar(t.id);
			hecho(`"${t.titulo}" enviada a la papelera`, () => api.restaurar(t.id));
		}));
	}
	return el("span", "flex flex-wrap gap-1", ...botones);
}

function boton(texto, alHacer) {
	const b = el("button", "boton-mini", texto);
	b.type = "button";
	b.addEventListener("click", async () => {
		try {
			await alHacer();
		} catch (e) {
			avisar(e.primerMensaje ?? e.message, { error: true });
		}
	});
	return b;
}

async function hecho(descripcion, revertir) {
	registrar(descripcion, async () => {
		await revertir();
		await cargar();
	}, { avisarAhora: true });
	await cargar();
}

function mostrarUltima() {
	const ultima = leerPreferencia(CLAVE_REVISION, null);
	document.getElementById("ultima-revision").textContent = ultima
		? `Última revisión: ${momento(ultima)}.` : "Todavía no hiciste ninguna revisión en este navegador.";
}

document.getElementById("terminar").addEventListener("click", () => {
	guardarPreferencia(CLAVE_REVISION, new Date().toISOString());
	mostrarUltima();
	avisar("Revisión terminada. Buena semana.");
});

mostrarUltima();
cargar().catch(e => avisar(e.message, { error: true }));
