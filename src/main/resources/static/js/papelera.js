/** Papelera (§4.8): restaurar, borrar de verdad una tarea o vaciarla entera. */

import { api } from "./api.js";
import { avisar, el, icono } from "./dom.js";
import { momento } from "./formato.js";

const lista = document.getElementById("papelera");
const vacia = document.getElementById("papelera-vacia");
const vaciar = document.getElementById("vaciar");

async function cargar() {
	let tareas;
	try {
		tareas = await api.papelera();
	} catch (e) {
		avisar(e.message, { error: true });
		return;
	}
	vacia.hidden = tareas.length > 0;
	lista.hidden = vaciar.hidden = tareas.length === 0;
	lista.replaceChildren(...tareas.map(fila));
}

function fila(t) {
	const restaurar = el("button", "boton-secundario", icono("deshacer", "size-4"), "Restaurar");
	const borrar = el("button", "boton-icono", icono("papelera", "size-4"));
	restaurar.type = borrar.type = "button";
	borrar.title = "Eliminar definitivamente";
	borrar.setAttribute("aria-label", "Eliminar definitivamente: " + t.titulo);
	const acciones = el("span", "flex shrink-0 items-center gap-1", restaurar, borrar);
	const li = el("li", "flex flex-wrap items-center justify-between gap-3 px-5 py-3.5",
		el("span", "min-w-0",
			el("span", "block break-words", t.titulo),
			el("span", "mt-0.5 block text-xs text-tenue", "Eliminada el " + momento(t.eliminadaEn))),
		acciones);

	restaurar.addEventListener("click", async () => {
		try {
			await api.restaurar(t.id);
			avisar("Tarea restaurada.");
			cargar();
		} catch (e) {
			avisar(e.message, { error: true });
		}
	});
	borrar.addEventListener("click", () => confirmar(acciones, "¿Borrarla para siempre?", async () => {
		await api.eliminarDefinitivamente(t.id);
		cargar();
	}));
	return li;
}

/** Confirmación en el lugar: lo que se borra de la papelera no se puede recuperar. */
function confirmar(contenedor, pregunta, alConfirmar) {
	const original = [...contenedor.childNodes];
	const si = el("button", "boton-peligro", "Borrar");
	const no = el("button", "boton-secundario", "Cancelar");
	si.type = no.type = "button";
	contenedor.replaceChildren(el("span", "mr-1 text-sm", pregunta), no, si);
	no.addEventListener("click", () => contenedor.replaceChildren(...original));
	si.addEventListener("click", async () => {
		try {
			await alConfirmar();
		} catch (e) {
			avisar(e.message, { error: true });
			contenedor.replaceChildren(...original);
		}
	});
	no.focus();
}

vaciar.addEventListener("click", () => confirmar(vaciar.parentElement,
	"Se borran todas para siempre. ¿Seguro?", async () => {
		await api.vaciarPapelera();
		location.reload();
	}));

cargar();
