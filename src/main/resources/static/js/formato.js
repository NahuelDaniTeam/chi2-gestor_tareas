/**
 * Textos que se muestran de cada tarea: fecha legible y resumen de la vista. Módulo sin DOM.
 * Las fechas de la API son "AAAA-MM-DD" sin hora: se arman en hora local para que no corran un día.
 */

const LOCALE = "es-AR";
const DIA = new Intl.DateTimeFormat(LOCALE, { weekday: "short", day: "numeric", month: "short" });
const DIA_MES = new Intl.DateTimeFormat(LOCALE, { day: "numeric", month: "short" });
const MES = new Intl.DateTimeFormat(LOCALE, { month: "long", year: "numeric" });
const MOMENTO = new Intl.DateTimeFormat(LOCALE, { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit", hourCycle: "h23" });

export const PRIORIDADES = Object.freeze({ ALTA: "Alta", MEDIA: "Media", BAJA: "Baja" });
export const HORIZONTES = Object.freeze({ DIA: "Día", SEMANA: "Semana", MES: "Mes", LARGO_PLAZO: "Largo plazo" });
export const ESTADOS = Object.freeze({ PENDIENTE: "Pendiente", EN_CURSO: "En curso", FINALIZADA: "Finalizada" });

export function aFecha(iso) {
	const [a, m, d] = iso.split("-").map(Number);
	return new Date(a, m - 1, d);
}

export function isoLocal(fecha = new Date()) {
	const dos = n => String(n).padStart(2, "0");
	return `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}`;
}

export function capitalizar(texto) {
	return texto.charAt(0).toUpperCase() + texto.slice(1);
}

/** "19 nov, 15:30": para creación, modificación y finalización. */
export function momento(instante) {
	return MOMENTO.format(new Date(instante));
}

/**
 * Fecha de la tarea para mostrar, o null si es redundante con la vista
 * (una tarea de hoy en Hoy, una de esta semana en la vista de esa semana).
 *
 * @param tarea {horizonte, fechaReferencia}
 * @param contexto {horizonteVista, fechaVista, hoy} fechas ISO
 */
export function etiquetaFecha(tarea, { horizonteVista, fechaVista, hoy }) {
	const { horizonte, fechaReferencia } = tarea;
	if (!fechaReferencia) return horizonte === "LARGO_PLAZO" && horizonteVista !== "LARGO_PLAZO" ? "Sin fecha" : null;
	if (horizonte === horizonteVista && fechaReferencia === fechaVista) return null;
	const fecha = aFecha(fechaReferencia);
	switch (horizonte) {
		case "DIA":
			if (fechaReferencia === hoy) return "Hoy";
			return capitalizar(DIA.format(fecha).replace(",", ""));
		case "SEMANA":
			return "Semana del " + DIA_MES.format(fecha);
		case "MES":
			return capitalizar(MES.format(fecha));
		default:
			return "Vence el " + DIA_MES.format(fecha) + (fechaReferencia.slice(0, 4) === hoy.slice(0, 4) ? "" : " de " + fechaReferencia.slice(0, 4));
	}
}

/** "3 pendientes · 1 vencida · 2 finalizadas" (vacío si no hay tareas). */
export function resumen(tareas) {
	const finalizadas = tareas.filter(t => t.estado === "FINALIZADA").length;
	const abiertas = tareas.length - finalizadas;
	const vencidas = tareas.filter(t => t.vencida).length;
	const partes = [];
	if (abiertas) partes.push(plural(abiertas, "pendiente", "pendientes"));
	if (vencidas) partes.push(plural(vencidas, "vencida", "vencidas"));
	if (finalizadas) partes.push(plural(finalizadas, "finalizada", "finalizadas"));
	return partes.join(" · ");
}

function plural(n, uno, varios) {
	return `${n} ${n === 1 ? uno : varios}`;
}

/** Vista donde se ve una tarea (para el enlace "Ver" después de crearla en otra vista). */
export function rutaDeTarea({ horizonte, fechaReferencia }) {
	switch (horizonte) {
		case "SEMANA": return `/semana?fecha=${fechaReferencia}`;
		case "MES": return `/mes?fecha=${fechaReferencia}`;
		case "LARGO_PLAZO": return "/largo-plazo";
		default: return "/";
	}
}

/** "la semana del 16 nov", "noviembre de 2026", "largo plazo": dónde quedó una tarea. */
export function dondeQuedo({ horizonte, fechaReferencia }, hoy) {
	switch (horizonte) {
		case "SEMANA": return "la semana del " + DIA_MES.format(aFecha(fechaReferencia));
		case "MES": return MES.format(aFecha(fechaReferencia));
		case "LARGO_PLAZO": return "Largo plazo";
		default: return fechaReferencia === hoy ? "Hoy" : DIA.format(aFecha(fechaReferencia)).replace(",", "");
	}
}

/** Abiertas en el orden que manda la API (prioridad) y finalizadas aparte, al final. */
export function separarFinalizadas(tareas) {
	return {
		abiertas: tareas.filter(t => t.estado !== "FINALIZADA"),
		finalizadas: tareas.filter(t => t.estado === "FINALIZADA")
	};
}
