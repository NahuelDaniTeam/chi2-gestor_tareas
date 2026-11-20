/**
 * Panel lateral de edición (§4.4). Todo se guarda al instante: selectores al cambiar, textos al salir del campo.
 * Eliminar pide confirmación dentro del mismo panel.
 */

import { api } from "./api.js";
import { atraparFoco, avisar, icono } from "./dom.js";
import { momento } from "./formato.js";

const ETIQUETA_FECHA = {
	DIA: "Día",
	SEMANA: "Semana",
	MES: "Mes",
	LIMITE: "Vence el"
};

const AYUDA_FECHA = {
	SIN_PLAZO: "Queda en Por planificar hasta que le pongas un plazo.",
	DIA: "",
	SEMANA: "Cualquier día: se guarda la semana que lo contiene.",
	MES: "Cualquier día: se guarda el mes que lo contiene.",
	LIMITE: "Se muestra en ese día y en Hoy cuando vence."
};

/**
 * @param contexto {hoy}
 * @param alCerrar callback() para recargar la vista
 */
export function iniciarPanel(contexto, alCerrar) {
	const panel = document.getElementById("panel");
	const fondo = document.getElementById("panel-fondo");
	const form = document.getElementById("panel-form");
	const estadoGuardado = document.getElementById("panel-guardado");
	const error = document.getElementById("p-error");
	const eliminar = document.getElementById("p-eliminar");
	const confirmar = document.getElementById("p-confirmar");
	const campo = nombre => form.elements[nombre];

	let tarea = null;
	let focoPrevio = null;
	let ultimoGuardado = Promise.resolve();
	let temporizador;

	function abrir(t) {
		tarea = t;
		focoPrevio = document.activeElement;
		campo("titulo").value = t.titulo;
		campo("descripcion").value = t.descripcion || "";
		campo("estado").value = t.estado;
		campo("prioridad").value = t.prioridad;
		campo("horizonte").value = t.horizonte;
		campo("fechaReferencia").value = t.fechaReferencia || "";
		ajustarFecha();
		mostrarMeta();
		mostrarError(null);
		indicar("");
		mostrarConfirmacion(false);
		panel.hidden = fondo.hidden = false;
		document.body.classList.add("overflow-hidden");
		requestAnimationFrame(() => panel.classList.add("abierto"));
		ajustarAlto(campo("titulo"));
		campo("titulo").focus();
	}

	function cerrar() {
		if (panel.hidden) return;
		// Salir del campo dispara su guardado; la vista se recarga cuando termina.
		if (panel.contains(document.activeElement)) document.activeElement.blur();
		panel.classList.remove("abierto");
		panel.hidden = fondo.hidden = true;
		document.body.classList.remove("overflow-hidden");
		tarea = null;
		ultimoGuardado.then(alCerrar);
		if (focoPrevio && document.body.contains(focoPrevio)) focoPrevio.focus();
	}

	function ajustarFecha() {
		const horizonte = campo("horizonte").value;
		const sinPlazo = horizonte === "SIN_PLAZO";
		document.getElementById("p-fecha-bloque").hidden = sinPlazo;
		document.getElementById("p-fecha-label").textContent = ETIQUETA_FECHA[horizonte] ?? "Fecha";
		document.getElementById("p-fecha-ayuda").textContent = AYUDA_FECHA[horizonte];
		if (!sinPlazo && !campo("fechaReferencia").value) campo("fechaReferencia").value = tarea?.fechaReferencia || contexto.hoy;
	}

	function mostrarMeta() {
		const partes = [`Creada ${momento(tarea.creadaEn)}`];
		if (tarea.actualizadaEn !== tarea.creadaEn) partes.push(`modificada ${momento(tarea.actualizadaEn)}`);
		if (tarea.completadaEn) partes.push(`finalizada ${momento(tarea.completadaEn)}`);
		document.getElementById("p-meta").textContent = partes.join(" · ");
	}

	function mostrarError(mensaje) {
		error.hidden = !mensaje;
		error.textContent = mensaje || "";
	}

	function indicar(texto, conCheck = false) {
		clearTimeout(temporizador);
		estadoGuardado.replaceChildren(...(conCheck ? [icono("check", "size-3.5")] : []), texto);
		if (conCheck) temporizador = setTimeout(() => estadoGuardado.replaceChildren(), 2000);
	}

	function cuerpoEdicion() {
		const horizonte = campo("horizonte").value;
		let fecha = campo("fechaReferencia").value || null;
		if (!fecha && horizonte !== "SIN_PLAZO") fecha = tarea.fechaReferencia || contexto.hoy;
		return {
			titulo: campo("titulo").value,
			descripcion: campo("descripcion").value,
			horizonte,
			fechaReferencia: fecha,
			prioridad: campo("prioridad").value
		};
	}

	async function guardar(cambiado) {
		if (!tarea) return;
		const t = tarea;
		if (cambiado === "titulo" && !campo("titulo").value.trim()) {
			campo("titulo").value = t.titulo; // no se deja una tarea sin título
			return;
		}
		indicar("Guardando…");
		try {
			const cuerpo = cuerpoEdicion();
			const actualizada = cambiado === "estado"
				? await api.cambiarEstado(t.id, campo("estado").value)
				: cambiado === "prioridad"
					? await api.cambiarPrioridad(t.id, cuerpo.prioridad)
					: cambiado === "horizonte" || cambiado === "fechaReferencia"
						? await api.cambiarPlazo(t.id, cuerpo.horizonte, cuerpo.fechaReferencia)
						: await api.editar(t.id, cuerpo);
			Object.assign(t, actualizada);
			if (tarea === t) {
				campo("fechaReferencia").value = t.fechaReferencia || "";
				mostrarMeta();
				mostrarError(null);
				indicar("Guardado", true);
			}
		} catch (e) {
			indicar("");
			mostrarError(e.primerMensaje ?? e.message);
		}
	}

	function mostrarConfirmacion(visible) {
		eliminar.hidden = visible;
		confirmar.hidden = !visible;
		if (visible) document.getElementById("p-confirmar-no").focus();
	}

	function ajustarAlto(textarea) {
		textarea.style.height = "auto";
		textarea.style.height = textarea.scrollHeight + "px";
	}

	// Selectores, opciones y fecha: al cambiar. Textos: al salir del campo (evento change).
	for (const nombre of ["estado", "prioridad", "horizonte", "fechaReferencia", "titulo", "descripcion"]) {
		const elementos = form.querySelectorAll(`[name="${nombre}"]`);
		elementos.forEach(e => e.addEventListener("change", () => {
			if (nombre === "horizonte") ajustarFecha();
			ultimoGuardado = guardar(nombre);
		}));
	}
	campo("titulo").addEventListener("input", e => ajustarAlto(e.target));
	campo("titulo").addEventListener("keydown", e => {
		if (e.key === "Enter") { e.preventDefault(); e.target.blur(); }
	});
	form.addEventListener("submit", e => e.preventDefault());

	eliminar.addEventListener("click", () => mostrarConfirmacion(true));
	document.getElementById("p-confirmar-no").addEventListener("click", () => {
		mostrarConfirmacion(false);
		eliminar.focus();
	});
	document.getElementById("p-confirmar-si").addEventListener("click", async () => {
		try {
			await api.eliminar(tarea.id);
			avisar("Tarea eliminada.");
			cerrar();
		} catch (e) {
			mostrarError(e.message);
		}
	});

	fondo.addEventListener("click", cerrar);
	panel.querySelector("[data-cerrar]").addEventListener("click", cerrar);
	panel.addEventListener("keydown", e => atraparFoco(panel, e));

	return { abrir, cerrar, estaAbierto: () => !panel.hidden };
}

