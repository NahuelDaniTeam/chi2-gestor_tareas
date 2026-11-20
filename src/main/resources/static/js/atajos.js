/**
 * Atajos del alta rápida (docs/01-especificacion.md §4.2), escritos en cualquier parte del texto:
 * "!a" cambia la prioridad; "@s", "@mañana", "@viernes" o "@p" cambian el plazo; "#tesis" elige el proyecto.
 * Módulo sin DOM, testeado en src/test/js.
 */

import { sumarDias, aFecha } from "./formato.js";

export const ATAJOS_PRIORIDAD = Object.freeze({
	a: "ALTA", alta: "ALTA",
	m: "MEDIA", media: "MEDIA",
	b: "BAJA", baja: "BAJA"
});

/** Plazos sin día concreto: la fecha la pone la vista (o hoy). */
const ATAJOS_PLAZO = Object.freeze({
	s: "SEMANA", semana: "SEMANA",
	m: "MES", mes: "MES",
	p: "SIN_PLAZO", planificar: "SIN_PLAZO", sin: "SIN_PLAZO", l: "SIN_PLAZO", largo: "SIN_PLAZO"
});

/** Días concretos: cuántos días después de hoy, o el próximo día de la semana (0 = domingo). */
const ATAJOS_DIA = Object.freeze({
	d: 0, hoy: 0, mañana: 1, manana: 1, pasado: 2,
	dom: "0", domingo: "0", lun: "1", lunes: "1", mar: "2", martes: "2",
	mie: "3", "mié": "3", miercoles: "3", "miércoles": "3", jue: "4", jueves: "4",
	vie: "5", viernes: "5", sab: "6", "sáb": "6", sabado: "6", "sábado": "6"
});

const ATAJO = /(^|\s)([!@])(\p{L}+)(?=\s|$)/gu;
const PROYECTO = /(^|\s)#([\p{L}\p{N}][\p{L}\p{N}_-]*)(?=\s|$)/gu;

/** Para comparar nombres: sin mayúsculas, tildes, espacios, guiones ni guiones bajos. */
export function normalizar(texto) {
	return texto.normalize("NFD").replace(/\p{Diacritic}/gu, "").toLowerCase().replace(/[\s_-]+/g, "");
}

/** El proyecto cuyo nombre coincide con lo escrito ("#redes_de_datos" encuentra "Redes de Datos"), o undefined. */
export function buscarPorNombre(proyectos, nombre) {
	const buscado = normalizar(nombre);
	return proyectos.find(p => normalizar(p.nombre) === buscado);
}

/** Fecha del atajo de día: "+n" días desde hoy, o el próximo día de la semana (hoy si coincide). */
function fechaDeDia(valor, hoy) {
	if (typeof valor === "number") return sumarDias(hoy, valor);
	const faltan = (Number(valor) - aFecha(hoy).getDay() + 7) % 7;
	return sumarDias(hoy, faltan);
}

/**
 * Separa los atajos del título. Un atajo que no existe ("!urgente") queda como texto. Si se repite, gana el último.
 *
 * @param {string} texto lo que escribió el usuario
 * @param {{prioridad: string, horizonte: string, fecha: string|null, hoy: string}} base valores de los selectores
 * @returns {{titulo, prioridad, horizonte, fecha, proyecto, conAtajos}} fecha ISO (null sin plazo);
 *          proyecto: el nombre escrito con "#" (guiones bajos como espacios), o null
 */
export function interpretar(texto, base) {
	let { prioridad, horizonte, fecha } = base;
	let conAtajos = false;
	let proyecto = null;
	const sinProyecto = texto.replace(PROYECTO, (todo, espacio, nombre) => {
		proyecto = nombre.replace(/_/g, " ");
		conAtajos = true;
		return espacio;
	});
	const titulo = sinProyecto.replace(ATAJO, (todo, espacio, signo, palabra) => {
		const clave = palabra.toLowerCase();
		if (signo === "!" && ATAJOS_PRIORIDAD[clave]) {
			prioridad = ATAJOS_PRIORIDAD[clave];
		} else if (signo === "@" && clave in ATAJOS_DIA) {
			horizonte = "DIA";
			fecha = fechaDeDia(ATAJOS_DIA[clave], base.hoy);
		} else if (signo === "@" && ATAJOS_PLAZO[clave]) {
			const nuevo = ATAJOS_PLAZO[clave];
			fecha = nuevo === "SIN_PLAZO" ? null : (nuevo === base.horizonte && base.fecha ? base.fecha : base.hoy);
			horizonte = nuevo;
		} else {
			return todo;
		}
		conAtajos = true;
		return espacio;
	}).replace(/\s+/g, " ").trim();
	return { titulo, prioridad, horizonte, fecha, proyecto, conAtajos };
}
