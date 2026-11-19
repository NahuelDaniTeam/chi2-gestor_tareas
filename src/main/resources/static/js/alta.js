/**
 * Alta rápida (docs/01-especificacion.md §4.2): foco al abrir, Enter guarda, el campo queda vacío y enfocado.
 * Los selectores muestran con qué valores se va a guardar; los atajos del texto los cambian en vivo.
 */

import { api } from "./api.js";
import { interpretar } from "./atajos.js";
import { avisar } from "./dom.js";
import { HORIZONTES } from "./formato.js";

/**
 * @param contexto {horizonteVista, fechaAlta, hoy}
 * @param alGuardar callback(tareaNueva) después de guardar
 */
export function iniciarAlta(contexto, alGuardar) {
	const form = document.getElementById("alta");
	const titulo = document.getElementById("alta-titulo");
	const horizonte = document.getElementById("alta-horizonte");
	const prioridad = document.getElementById("alta-prioridad");
	const boton = document.getElementById("alta-guardar");

	// Lo que eligió el usuario en los selectores; los atajos lo pisan solo para esta tarea.
	const base = { horizonte: horizonte.value, prioridad: prioridad.value };
	horizonte.addEventListener("change", () => { base.horizonte = horizonte.value; actualizar(); });
	prioridad.addEventListener("change", () => { base.prioridad = prioridad.value; actualizar(); });

	function actualizar() {
		const lectura = interpretar(titulo.value, base);
		horizonte.value = lectura.horizonte;
		prioridad.value = lectura.prioridad;
		horizonte.closest(".chip").classList.toggle("detectado", lectura.horizonte !== base.horizonte);
		prioridad.closest(".chip").classList.toggle("detectado", lectura.prioridad !== base.prioridad);
		prioridad.closest(".chip").dataset.prioridad = lectura.prioridad;
		boton.disabled = !lectura.titulo;
		return lectura;
	}

	/** En la vista de su mismo horizonte, la tarea va al período que se está viendo; si no, a hoy. */
	function fechaPara(h) {
		if (h === "LARGO_PLAZO") return null;
		return h === contexto.horizonteVista && contexto.fechaAlta ? contexto.fechaAlta : contexto.hoy;
	}

	let guardando = false;
	form.addEventListener("submit", async e => {
		e.preventDefault();
		const lectura = actualizar();
		if (!lectura.titulo || guardando) return;
		guardando = true;
		try {
			const nueva = await api.crear({
				titulo: lectura.titulo,
				horizonte: lectura.horizonte,
				prioridad: lectura.prioridad,
				fechaReferencia: fechaPara(lectura.horizonte)
			});
			titulo.value = "";
			actualizar();
			const visible = await alGuardar(nueva);
			if (!visible) avisar(`Guardada en ${HORIZONTES[nueva.horizonte]}.`);
		} catch (error) {
			avisar(error.primerMensaje ?? error.message, { error: true });
		} finally {
			guardando = false;
			titulo.focus();
		}
	});

	titulo.addEventListener("input", actualizar);
	// Enter en los selectores también guarda
	[horizonte, prioridad].forEach(s => s.addEventListener("keydown", e => {
		if (e.key === "Enter") { e.preventDefault(); form.requestSubmit(); }
	}));
	actualizar();

	return { enfocar: () => titulo.focus() };
}
