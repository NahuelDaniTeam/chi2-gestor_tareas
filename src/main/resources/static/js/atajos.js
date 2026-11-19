/**
 * Atajos del alta rápida (docs/01-especificacion.md §4.2): "!a" cambia la prioridad y "@s" el horizonte,
 * escritos en cualquier parte del texto. Módulo sin DOM, testeado en src/test/js.
 */

export const ATAJOS_PRIORIDAD = Object.freeze({
	a: "ALTA", alta: "ALTA",
	m: "MEDIA", media: "MEDIA",
	b: "BAJA", baja: "BAJA"
});

export const ATAJOS_HORIZONTE = Object.freeze({
	d: "DIA", dia: "DIA", "día": "DIA", hoy: "DIA",
	s: "SEMANA", semana: "SEMANA",
	m: "MES", mes: "MES",
	l: "LARGO_PLAZO", largo: "LARGO_PLAZO"
});

const ATAJO = /(^|\s)([!@])(\p{L}+)(?=\s|$)/gu;

/**
 * Separa los atajos del título. Un atajo que no existe ("!urgente") queda como texto.
 * Si se repite, gana el último.
 *
 * @param {string} texto lo que escribió el usuario
 * @param {{prioridad: string, horizonte: string}} base valores elegidos en los selectores
 * @returns {{titulo: string, prioridad: string, horizonte: string, conAtajos: boolean}}
 */
export function interpretar(texto, base) {
	let { prioridad, horizonte } = base;
	let conAtajos = false;
	const titulo = texto.replace(ATAJO, (todo, espacio, signo, palabra) => {
		const clave = palabra.toLowerCase();
		const tabla = signo === "!" ? ATAJOS_PRIORIDAD : ATAJOS_HORIZONTE;
		if (!tabla[clave]) return todo;
		if (signo === "!") prioridad = tabla[clave]; else horizonte = tabla[clave];
		conAtajos = true;
		return espacio;
	}).replace(/\s+/g, " ").trim();
	return { titulo, prioridad, horizonte, conAtajos };
}
