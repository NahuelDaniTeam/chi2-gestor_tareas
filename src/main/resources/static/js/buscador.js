/**
 * Buscador (§4.11): Ctrl+K desde cualquier página. Busca tareas por título o descripción y también
 * ofrece ir a cada vista. Elegir una tarea lleva a su vista y la abre en el panel.
 */

import { api } from "./api.js";
import { normalizar } from "./atajos.js";
import { el, icono } from "./dom.js";
import { dondeQuedo, isoLocal, rutaParaAbrir } from "./formato.js";

const COMANDOS = [
	["Ir a Hoy", "/"], ["Ir a Semana", "/semana"], ["Ir a Mes", "/mes"], ["Ir a Largo plazo", "/largo-plazo"],
	["Ir a Por planificar", "/por-planificar"], ["Revisión semanal", "/revision"], ["Estadísticas", "/estadisticas"],
	["Papelera", "/papelera"]
].map(([texto, href]) => ({ texto, href }));

const dialogo = document.getElementById("buscador");
const campo = document.getElementById("buscador-campo");
const lista = document.getElementById("buscador-resultados");
let opciones = [];
let activa = 0;
let pedido = 0;
let espera;

function abrir() {
	if (dialogo.open) return;
	campo.value = "";
	mostrar([], "");
	dialogo.showModal();
	campo.focus();
}

async function buscar() {
	const texto = campo.value.trim();
	const numero = ++pedido;
	let tareas = [];
	if (texto) {
		try {
			tareas = await api.buscar(texto);
		} catch {
			tareas = [];
		}
	}
	if (numero === pedido) mostrar(tareas, texto);
}

function mostrar(tareas, texto) {
	const hoy = isoLocal();
	const buscado = normalizar(texto);
	const comandos = COMANDOS.filter(c => !buscado || normalizar(c.texto).includes(buscado));
	opciones = [
		...tareas.map(t => ({
			href: rutaParaAbrir(t, hoy),
			nodo: el("span", "flex min-w-0 flex-1 items-center gap-3",
				icono(t.estado === "FINALIZADA" ? "check" : "nota", "size-4 shrink-0 text-tenue"),
				el("span", "min-w-0 flex-1 truncate" + (t.estado === "FINALIZADA" ? " text-tenue line-through" : ""), t.titulo),
				el("span", "shrink-0 text-xs text-tenue", dondeQuedo(t, hoy)))
		})),
		...comandos.map(c => ({
			href: c.href,
			nodo: el("span", "flex items-center gap-3", icono("siguiente", "size-4 text-tenue"), c.texto)
		}))
	];
	activa = 0;
	const items = opciones.map((o, i) => {
		const li = el("li", "buscador-opcion", o.nodo);
		li.id = "buscador-opcion-" + i;
		li.setAttribute("role", "option");
		li.addEventListener("mousemove", () => marcar(i));
		li.addEventListener("click", () => ir(i));
		return li;
	});
	if (texto && !tareas.length) items.unshift(el("li", "px-3 py-2 text-sm text-tenue", `No hay tareas con "${texto}".`));
	lista.replaceChildren(...items);
	marcar(0);
}

function marcar(i) {
	if (!opciones.length) return;
	activa = (i + opciones.length) % opciones.length;
	lista.querySelectorAll("[role=option]").forEach((li, j) => li.setAttribute("aria-selected", String(j === activa)));
	campo.setAttribute("aria-activedescendant", "buscador-opcion-" + activa);
	document.getElementById("buscador-opcion-" + activa)?.scrollIntoView({ block: "nearest" });
}

function ir(i) {
	const opcion = opciones[i];
	if (opcion) location.href = opcion.href;
}

campo.addEventListener("input", () => {
	clearTimeout(espera);
	espera = setTimeout(buscar, 150);
});
campo.addEventListener("keydown", e => {
	if (e.key === "ArrowDown") { e.preventDefault(); marcar(activa + 1); }
	else if (e.key === "ArrowUp") { e.preventDefault(); marcar(activa - 1); }
	else if (e.key === "Enter") { e.preventDefault(); ir(activa); }
});
dialogo.addEventListener("click", e => { if (e.target === dialogo) dialogo.close(); });
document.querySelectorAll("[data-abrir-buscador]").forEach(b => b.addEventListener("click", abrir));
document.addEventListener("keydown", e => {
	if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
		e.preventDefault();
		abrir();
	}
});
