/**
 * Alta rápida (docs/01-especificacion.md §4.2): foco al abrir, Enter guarda, el campo queda vacío y enfocado.
 * Los chips muestran con qué plazo, fecha y prioridad se va a guardar; los atajos del texto los cambian en vivo.
 */

import { api } from "./api.js";
import { buscarPorNombre, interpretar } from "./atajos.js";
import { avisar, el } from "./dom.js";
import { dondeQuedo, rutaDeTarea } from "./formato.js";

/**
 * @param contexto {hoy}
 * @param proyectos módulo de proyectos (lista, crear)
 * @param alGuardar callback(tareaNueva) después de guardar; devuelve si quedó visible en la vista
 */
export function iniciarAlta(contexto, proyectos, alGuardar) {
	const form = document.getElementById("alta");
	const titulo = document.getElementById("alta-titulo");
	const plazo = document.getElementById("alta-horizonte");
	const fecha = document.getElementById("alta-fecha");
	const prioridad = document.getElementById("alta-prioridad");
	const proyecto = document.getElementById("alta-proyecto");
	const opcionNuevo = el("option");
	opcionNuevo.value = "__nuevo";
	const boton = document.getElementById("alta-guardar");
	const chip = control => control.closest(".chip");

	// Lo que eligió el usuario en los chips; los atajos lo pisan solo para esta tarea.
	const base = { horizonte: plazo.value, fecha: fecha.value || contexto.hoy, prioridad: prioridad.value, hoy: contexto.hoy };
	plazo.addEventListener("change", () => { base.horizonte = plazo.value; actualizar(); });
	fecha.addEventListener("change", () => { if (fecha.value) base.fecha = fecha.value; actualizar(); });
	prioridad.addEventListener("change", () => { base.prioridad = prioridad.value; actualizar(); });
	proyecto.addEventListener("change", () => { if (proyecto.value !== "__nuevo") base.proyecto = proyecto.value; actualizar(); });
	base.proyecto = proyecto.value;

	function actualizar() {
		const lectura = interpretar(titulo.value, base);
		plazo.value = lectura.horizonte;
		fecha.value = lectura.fecha ?? "";
		prioridad.value = lectura.prioridad;
		chip(fecha).hidden = lectura.horizonte === "SIN_PLAZO";
		chip(plazo).classList.toggle("detectado", lectura.horizonte !== base.horizonte);
		chip(fecha).classList.toggle("detectado", lectura.fecha !== base.fecha && lectura.horizonte !== "SIN_PLAZO");
		chip(prioridad).classList.toggle("detectado", lectura.prioridad !== base.prioridad);
		chip(prioridad).dataset.prioridad = lectura.prioridad;
		mostrarProyecto(lectura);
		boton.disabled = !lectura.titulo;
		return lectura;
	}

	/** "#tesis": si existe, lo elige; si no, muestra que se va a crear. */
	function mostrarProyecto(lectura) {
		opcionNuevo.remove();
		lectura.proyectoExistente = lectura.proyecto ? buscarPorNombre(proyectos.lista, lectura.proyecto) : undefined;
		if (lectura.proyecto && !lectura.proyectoExistente) {
			opcionNuevo.textContent = `Nuevo: ${lectura.proyecto}`;
			proyecto.append(opcionNuevo);
			proyecto.value = "__nuevo";
		} else {
			proyecto.value = lectura.proyectoExistente?.id ?? base.proyecto ?? "";
		}
		chip(proyecto).classList.toggle("detectado", Boolean(lectura.proyecto));
		const punto = chip(proyecto).querySelector(".punto-proyecto");
		punto.style.backgroundColor = proyectos.porId(proyecto.value)?.hex ?? "";
	}

	/** El proyecto con el que se guarda: el escrito con "#" (creándolo si hace falta) o el del chip. */
	async function proyectoPara(lectura) {
		if (lectura.proyectoExistente) return lectura.proyectoExistente.id;
		if (lectura.proyecto) {
			const nuevo = await proyectos.crear(lectura.proyecto);
			avisar(`Proyecto "${nuevo.nombre}" creado.`);
			return nuevo.id;
		}
		return base.proyecto || null;
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
				fechaReferencia: lectura.horizonte === "SIN_PLAZO" ? null : lectura.fecha,
				proyectoId: await proyectoPara(lectura)
			});
			titulo.value = "";
			actualizar();
			const visible = await alGuardar(nueva);
			if (!visible) {
				avisar(`Guardada en ${dondeQuedo(nueva, contexto.hoy)}.`,
					{ enlace: { texto: "Ver", href: rutaDeTarea(nueva, contexto.hoy) } });
			}
		} catch (error) {
			avisar(error.primerMensaje ?? error.message, { error: true });
		} finally {
			guardando = false;
			titulo.focus();
		}
	});

	titulo.addEventListener("input", actualizar);
	// Enter en los chips también guarda
	[plazo, prioridad].forEach(s => s.addEventListener("keydown", e => {
		if (e.key === "Enter") { e.preventDefault(); form.requestSubmit(); }
	}));
	actualizar();

	return { enfocar: () => titulo.focus() };
}
