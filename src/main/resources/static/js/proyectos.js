/**
 * Proyectos (§4.6): el filtro de la barra, los selectores del alta y del panel, y el diálogo para
 * crearlos, renombrarlos, cambiarles el color o eliminarlos.
 */

import { api } from "./api.js";
import { avisar, el, guardarPreferencia, icono, leerPreferencia } from "./dom.js";

const CLAVE_FILTRO = "chi2-tareas-proyecto";
const ADMINISTRAR = "__administrar";
const COLORES = ["TERRACOTA", "OCRE", "OLIVA", "PINO", "AZUL", "CIRUELA", "PIZARRA"];

/**
 * @param alCambiar callback() cuando cambia el filtro o algún proyecto (para volver a dibujar)
 */
export async function iniciarProyectos(alCambiar) {
	let lista = [];
	let filtro = leerPreferencia(CLAVE_FILTRO, "");

	const selectFiltro = document.getElementById("filtro-proyecto");
	const selectAlta = document.getElementById("alta-proyecto");
	const selectPanel = document.getElementById("p-proyecto");
	const dialogo = document.getElementById("dialogo-proyectos");

	async function recargar() {
		try {
			lista = await api.proyectos();
		} catch {
			lista = [];
		}
		if (filtro && !lista.some(p => p.id === filtro)) filtro = "";
		pintarSelectores();
		pintarDialogo();
		alCambiar();
	}

	function opciones(select, primera, extra = []) {
		const valor = select.value;
		select.replaceChildren(el("option", null, primera), ...lista.map(p => {
			const o = el("option", null, p.nombre);
			o.value = p.id;
			return o;
		}), ...extra);
		select.firstChild.value = "";
		select.value = lista.some(p => p.id === valor) ? valor : "";
	}

	function pintarSelectores() {
		const administrar = el("option", null, "Administrar proyectos…");
		administrar.value = ADMINISTRAR;
		opciones(selectFiltro, "Todos los proyectos", [el("hr"), administrar]);
		selectFiltro.value = filtro;
		pintarPunto(selectFiltro, filtro);
		opciones(selectAlta, "Sin proyecto");
		if (!selectAlta.dataset.tocado) selectAlta.value = filtro;
		pintarPunto(selectAlta, selectAlta.value);
		opciones(selectPanel, "Sin proyecto");
	}

	/** El punto de color del chip que contiene al selector. */
	function pintarPunto(select, id) {
		const punto = select.closest(".chip")?.querySelector(".punto-proyecto");
		if (punto) punto.style.backgroundColor = porId(id)?.hex ?? "";
	}

	selectFiltro.addEventListener("change", () => {
		if (selectFiltro.value === ADMINISTRAR) {
			selectFiltro.value = filtro;
			dialogo.showModal();
			return;
		}
		filtro = selectFiltro.value;
		guardarPreferencia(CLAVE_FILTRO, filtro);
		pintarPunto(selectFiltro, filtro);
		if (!selectAlta.dataset.tocado) {
			selectAlta.value = filtro;
			selectAlta.dispatchEvent(new Event("change"));
			delete selectAlta.dataset.tocado;
		}
		alCambiar();
	});
	selectAlta.addEventListener("change", e => {
		if (e.isTrusted) selectAlta.dataset.tocado = "true";
		pintarPunto(selectAlta, selectAlta.value);
	});

	// ---------- Diálogo ----------

	function pintarDialogo() {
		const filas = lista.map(p => {
			const color = el("button", "muestra-color");
			color.type = "button";
			color.style.backgroundColor = p.hex;
			color.title = "Cambiar color";
			color.setAttribute("aria-label", `Cambiar el color de ${p.nombre}`);
			color.addEventListener("click", () => guardar(p, { color: COLORES[(COLORES.indexOf(p.color) + 1) % COLORES.length] }));

			const nombre = el("input", "min-w-0 flex-1 rounded-md bg-transparent px-2 py-1 outline-none focus:bg-superficie");
			nombre.value = p.nombre;
			nombre.maxLength = 40;
			nombre.setAttribute("aria-label", "Nombre del proyecto");
			nombre.addEventListener("change", () => nombre.value.trim() ? guardar(p, { nombre: nombre.value }) : (nombre.value = p.nombre));
			nombre.addEventListener("keydown", e => { if (e.key === "Enter") { e.preventDefault(); nombre.blur(); } });

			const borrar = el("button", "boton-icono", icono("papelera", "size-4"));
			borrar.type = "button";
			borrar.setAttribute("aria-label", `Eliminar ${p.nombre}`);
			const fila = el("li", "flex items-center gap-2 py-1.5", color, nombre, borrar);
			borrar.addEventListener("click", () => confirmarBorrado(fila, p));
			return fila;
		});
		document.getElementById("lista-proyectos").replaceChildren(...(filas.length ? filas
			: [el("li", "py-3 text-sm text-tenue", "Todavía no hay proyectos. Creá el primero abajo.")]));
	}

	function confirmarBorrado(fila, p) {
		const si = el("button", "boton-peligro", "Eliminar");
		const no = el("button", "boton-secundario", "Cancelar");
		si.type = no.type = "button";
		fila.replaceChildren(el("span", "flex-1 text-sm", `¿Eliminar "${p.nombre}"? Sus tareas quedan sin proyecto.`), no, si);
		no.addEventListener("click", pintarDialogo);
		si.addEventListener("click", async () => {
			try {
				await api.eliminarProyecto(p.id);
				await recargar();
			} catch (e) {
				avisar(e.message, { error: true });
			}
		});
		no.focus();
	}

	async function guardar(p, cambios) {
		try {
			await api.editarProyecto(p.id, { nombre: p.nombre, color: p.color, ...cambios });
			await recargar();
		} catch (e) {
			avisar(e.primerMensaje ?? e.message, { error: true });
			pintarDialogo();
		}
	}

	document.getElementById("nuevo-proyecto").addEventListener("submit", async e => {
		e.preventDefault();
		const campo = e.target.elements.nombre;
		if (!campo.value.trim()) return;
		try {
			await crear(campo.value);
			campo.value = "";
		} catch (error) {
			avisar(error.primerMensaje ?? error.message, { error: true });
		}
		campo.focus();
	});
	dialogo.addEventListener("click", e => { if (e.target === dialogo) dialogo.close(); });

	// ---------- API del módulo ----------

	function porId(id) {
		return id ? lista.find(p => p.id === id) : undefined;
	}

	async function crear(nombre) {
		const nuevo = await api.crearProyecto({ nombre });
		await recargar();
		return nuevo;
	}

	await recargar();

	return {
		get lista() { return lista; },
		get filtro() { return filtro; },
		porId,
		crear,
		/** Solo las tareas del proyecto elegido en el filtro (todas si no hay filtro). */
		filtrar: tareas => filtro ? tareas.filter(t => t.proyectoId === filtro) : tareas,
		administrar: () => dialogo.showModal()
	};
}
