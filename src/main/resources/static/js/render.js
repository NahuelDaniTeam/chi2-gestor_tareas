/** Dibuja las tareas como lista o como tablero. No guarda nada: avisa por las acciones recibidas. */

import { el, icono, leerPreferencia, guardarPreferencia } from "./dom.js";
import { ESTADOS, HORIZONTES, PRIORIDADES, etiquetaFecha, separarFinalizadas } from "./formato.js";

/**
 * @param estado {tareas, modo, cargando, error}
 * @param contexto {vista, horizonteVista, fechaVista, hoy}
 * @param acciones {abrir(tarea), cambiarEstado(tarea, estado), reintentar()}
 */
export function dibujar(estado, contexto, acciones) {
	const lista = document.getElementById("lista");
	const tablero = document.getElementById("tablero");
	const vacio = document.getElementById("vacio");
	const problema = document.getElementById("problema");

	document.querySelectorAll("[data-modo]").forEach(b =>
		b.setAttribute("aria-pressed", String(b.dataset.modo === estado.modo)));
	document.getElementById("cargando").hidden = !estado.cargando;
	problema.hidden = !estado.error;
	if (estado.cargando || estado.error) {
		lista.hidden = tablero.hidden = vacio.hidden = true;
		return;
	}

	const hay = estado.tareas.length > 0;
	vacio.hidden = hay;
	lista.hidden = !hay || estado.modo !== "lista";
	tablero.hidden = !hay || estado.modo !== "tablero";
	if (!hay) return;
	if (estado.modo === "lista") dibujarLista(lista, estado.tareas, contexto, acciones);
	else dibujarTablero(estado.tareas, contexto, acciones);
}

// ---------- Lista ----------

function dibujarLista(contenedor, tareas, contexto, acciones) {
	const { abiertas, finalizadas } = separarFinalizadas(tareas);
	const bloques = [];
	if (contexto.vista === "LARGO_PLAZO") {
		for (const prioridad of Object.keys(PRIORIDADES)) {
			const grupo = abiertas.filter(t => t.prioridad === prioridad);
			if (grupo.length) bloques.push(titulo(`Prioridad ${PRIORIDADES[prioridad].toLowerCase()}`, grupo.length), filas(grupo, contexto, acciones));
		}
	} else if (abiertas.length) {
		bloques.push(filas(abiertas, contexto, acciones));
	} else {
		bloques.push(el("p", "rounded-xl border border-linea bg-superficie px-5 py-6 text-tenue", "Terminaste todo lo de esta vista."));
	}
	if (finalizadas.length) bloques.push(bloqueFinalizadas(finalizadas, contexto, acciones));
	contenedor.replaceChildren(...bloques);
}

function titulo(texto, cantidad) {
	return el("h2", "mt-8 mb-3 flex items-baseline gap-2 text-sm font-medium text-tenue first:mt-0",
		texto, el("span", "text-xs tabular-nums text-tenue/70", String(cantidad)));
}

function filas(tareas, contexto, acciones) {
	return el("ul", "divide-y divide-linea overflow-hidden rounded-xl border border-linea bg-superficie",
		...tareas.map(t => fila(t, contexto, acciones)));
}

function bloqueFinalizadas(tareas, contexto, acciones) {
	const detalles = el("details", "group/fin mt-8");
	detalles.open = leerPreferencia("chi2-tareas-finalizadas", "cerradas") === "abiertas";
	detalles.addEventListener("toggle", () =>
		guardarPreferencia("chi2-tareas-finalizadas", detalles.open ? "abiertas" : "cerradas"));
	const resumen = el("summary", "mb-3 flex cursor-pointer list-none items-center gap-2 text-sm font-medium text-tenue hover:text-tinta",
		icono("abajo", "size-4 -rotate-90 transition group-open/fin:rotate-0"),
		"Finalizadas", el("span", "text-xs tabular-nums text-tenue/70", String(tareas.length)));
	detalles.append(resumen, filas(tareas, contexto, acciones));
	return detalles;
}

function fila(t, contexto, acciones) {
	const finalizada = t.estado === "FINALIZADA";
	const casilla = el("button", "casilla mt-0.5", icono("check", "size-3"));
	casilla.type = "button";
	casilla.setAttribute("role", "checkbox");
	casilla.setAttribute("aria-checked", String(finalizada));
	casilla.setAttribute("aria-label", (finalizada ? "Reabrir: " : "Finalizar: ") + t.titulo);
	casilla.dataset.prioridad = t.prioridad;
	casilla.addEventListener("click", () => acciones.cambiarEstado(t, finalizada ? "PENDIENTE" : "FINALIZADA"));

	const cuerpo = el("button", "min-w-0 flex-1 text-left outline-none",
		tituloTarea(t, "leading-snug"),
		meta(t, contexto, { conEstado: true }));
	cuerpo.type = "button";
	cuerpo.addEventListener("click", () => acciones.abrir(t));

	const li = el("li", "fila group flex items-start gap-3.5 px-4 py-3 sm:px-5", casilla, cuerpo,
		el("span", "mt-0.5 text-tenue/0 transition group-hover:text-tenue/60", icono("siguiente", "size-4")));
	li.dataset.id = t.id;
	return li;
}

// ---------- Tablero ----------

function dibujarTablero(tareas, contexto, acciones) {
	for (const estado of Object.keys(ESTADOS)) {
		const columna = document.querySelector(`.columna[data-estado="${estado}"]`);
		const deEstado = tareas.filter(t => t.estado === estado);
		columna.replaceChildren(...(deEstado.length
			? deEstado.map(t => tarjeta(t, contexto, acciones))
			: [el("p", "columna-vacia", "Sin tareas")]));
		document.querySelector(`[data-contador="${estado}"]`).textContent = deEstado.length;
	}
}

function tarjeta(t, contexto, acciones) {
	const boton = el("button", "tarjeta",
		tituloTarea(t, "font-medium leading-snug"),
		meta(t, contexto, { conEstado: false }));
	boton.type = "button";
	boton.draggable = true;
	boton.dataset.id = t.id;
	boton.dataset.prioridad = t.prioridad;
	boton.setAttribute("aria-label", `${t.titulo}. Prioridad ${PRIORIDADES[t.prioridad].toLowerCase()}. Abrir para editar.`);
	boton.addEventListener("click", () => acciones.abrir(t));
	boton.addEventListener("dragstart", e => {
		e.dataTransfer.setData("text/plain", t.id);
		e.dataTransfer.effectAllowed = "move";
		requestAnimationFrame(() => boton.classList.add("arrastrando"));
	});
	boton.addEventListener("dragend", () => boton.classList.remove("arrastrando"));
	return boton;
}

/** Arrastrar y soltar entre columnas. Se engancha una sola vez; las tarjetas cambian en cada dibujo. */
export function habilitarArrastre(buscarTarea, cambiarEstado) {
	document.querySelectorAll(".columna").forEach(columna => {
		columna.addEventListener("dragover", e => {
			e.preventDefault();
			e.dataTransfer.dropEffect = "move";
			columna.classList.add("destino");
		});
		columna.addEventListener("dragleave", e => {
			if (!columna.contains(e.relatedTarget)) columna.classList.remove("destino");
		});
		columna.addEventListener("drop", e => {
			e.preventDefault();
			columna.classList.remove("destino");
			const tarea = buscarTarea(e.dataTransfer.getData("text/plain"));
			if (tarea) cambiarEstado(tarea, columna.dataset.estado);
		});
	});
}

// ---------- Datos de cada tarea ----------

/** Título, con una marca discreta si la tarea tiene descripción. */
function tituloTarea(t, clases) {
	const finalizada = t.estado === "FINALIZADA" ? " finalizada" : "";
	const nota = t.descripcion
		? el("span", "ml-1.5 inline-flex align-[-0.125em] text-tenue/70", icono("nota", "size-3.5"), el("span", "sr-only", " (tiene descripción)"))
		: null;
	return el("span", `titulo-tarea block break-words ${clases}${finalizada}`, t.titulo, nota);
}

function meta(t, contexto, { conEstado }) {
	const partes = [];
	if (t.prioridad !== "MEDIA") {
		partes.push(el("span", "inline-flex items-center gap-1.5" + (t.prioridad === "ALTA" ? " font-medium text-acento" : ""),
			el("span", `punto punto-${t.prioridad}`), PRIORIDADES[t.prioridad]));
	}
	if (t.vencida) {
		partes.push(el("span", "inline-flex items-center gap-1 font-medium text-acento", icono("alerta", "size-3.5"), "Vencida"));
	}
	const fecha = etiquetaFecha(t, contexto);
	if (fecha) partes.push(el("span", "inline-flex items-center gap-1", icono("calendario", "size-3.5"), fecha));
	else if (t.horizonte !== contexto.horizonteVista) partes.push(el("span", null, HORIZONTES[t.horizonte]));
	if (conEstado && t.estado === "EN_CURSO") partes.push(el("span", "etiqueta-estado", "En curso"));
	if (!partes.length) return null;
	return el("span", "mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-tenue", ...partes);
}
