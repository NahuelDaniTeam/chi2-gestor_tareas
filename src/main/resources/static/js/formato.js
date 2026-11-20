/**
 * Fechas, textos y agrupaciones de las tareas. Módulo sin DOM, testeado en src/test/js.
 * Las fechas de la API son "AAAA-MM-DD" sin hora: se arman en hora local para que no corran un día.
 */

const LOCALE = "es-AR";
const DIA = new Intl.DateTimeFormat(LOCALE, { weekday: "short", day: "numeric", month: "short" });
const DIA_LARGO = new Intl.DateTimeFormat(LOCALE, { weekday: "long", day: "numeric" });
const DIA_SEMANA = new Intl.DateTimeFormat(LOCALE, { weekday: "short" });
const DIA_MES = new Intl.DateTimeFormat(LOCALE, { day: "numeric", month: "short" });
const MES = new Intl.DateTimeFormat(LOCALE, { month: "long", year: "numeric" });
const MOMENTO = new Intl.DateTimeFormat(LOCALE, { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit", hourCycle: "h23" });

export const PRIORIDADES = Object.freeze({ ALTA: "Alta", MEDIA: "Media", BAJA: "Baja" });
export const HORIZONTES = Object.freeze({
	SIN_PLAZO: "Sin plazo", DIA: "Día", SEMANA: "Semana", MES: "Mes", LIMITE: "Fecha límite"
});
export const ESTADOS = Object.freeze({ PENDIENTE: "Pendiente", EN_CURSO: "En curso", FINALIZADA: "Finalizada" });

// ---------- Fechas ISO ----------

export function aFecha(iso) {
	const [a, m, d] = iso.split("-").map(Number);
	return new Date(a, m - 1, d);
}

export function isoLocal(fecha = new Date()) {
	const dos = n => String(n).padStart(2, "0");
	return `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}`;
}

export function sumarDias(iso, dias) {
	const f = aFecha(iso);
	f.setDate(f.getDate() + dias);
	return isoLocal(f);
}

/** Lunes de la semana que contiene a {@code iso}. */
export function lunesDe(iso) {
	const desdeLunes = (aFecha(iso).getDay() + 6) % 7;
	return sumarDias(iso, -desdeLunes);
}

/** Los 7 días (lunes a domingo) de la semana que empieza en {@code lunes}. */
export function diasDeSemana(lunes) {
	return Array.from({ length: 7 }, (_, i) => sumarDias(lunes, i));
}

/** Semanas completas (de lunes a domingo) que cubren el mes que empieza en {@code primero}. */
export function semanasDelMes(primero) {
	const mes = primero.slice(0, 7);
	const semanas = [];
	for (let lunes = lunesDe(primero); lunes.slice(0, 7) <= mes; lunes = sumarDias(lunes, 7)) {
		semanas.push(diasDeSemana(lunes));
	}
	return semanas;
}

export function capitalizar(texto) {
	return texto.charAt(0).toUpperCase() + texto.slice(1);
}

/** "19 nov, 15:30": para creación, modificación y finalización. */
export function momento(instante) {
	return MOMENTO.format(new Date(instante));
}

/** "Lun 16 nov" */
export function textoDia(iso) {
	return capitalizar(DIA.format(aFecha(iso)).replace(",", ""));
}

/** "Lun" */
export function textoDiaSemana(iso) {
	return capitalizar(DIA_SEMANA.format(aFecha(iso)).replace(".", ""));
}

/** "Lunes 16" */
export function textoDiaLargo(iso) {
	return capitalizar(DIA_LARGO.format(aFecha(iso)));
}

/** "Noviembre de 2026" */
export function textoMes(iso) {
	return capitalizar(MES.format(aFecha(iso)));
}

// ---------- Textos de una tarea ----------

/** Día relativo a hoy: "hoy", "mañana" o la fecha. */
function diaRelativo(iso, hoy) {
	if (iso === hoy) return "hoy";
	if (iso === sumarDias(hoy, 1)) return "mañana";
	return null;
}

/**
 * Fecha de la tarea para mostrar, o null si es redundante con donde se la está viendo
 * (una tarea de hoy en Hoy, una de esta semana en esa semana, un día en su columna del calendario).
 *
 * @param contexto {vista, fechaVista, hoy, mostrarDia}: mostrarDia=false cuando el día ya está a la vista
 */
export function etiquetaFecha(tarea, { vista, fechaVista, hoy, mostrarDia = true }) {
	const { horizonte, fechaReferencia: f } = tarea;
	switch (horizonte) {
		case "DIA": {
			if (!mostrarDia || (vista === "HOY" && f === hoy)) return null;
			const relativo = diaRelativo(f, hoy);
			return relativo ? capitalizar(relativo) : textoDia(f);
		}
		case "LIMITE": {
			const relativo = diaRelativo(f, hoy);
			if (relativo) return "Vence " + relativo;
			if (!mostrarDia) return "Vence";
			return "Vence el " + DIA_MES.format(aFecha(f)) + (f.slice(0, 4) === hoy.slice(0, 4) ? "" : " de " + f.slice(0, 4));
		}
		case "SEMANA":
			return vista === "SEMANA" && f === fechaVista ? null : "Semana del " + DIA_MES.format(aFecha(f));
		case "MES":
			return vista === "MES" && f === fechaVista ? null : textoMes(f);
		default:
			return null;
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

/** Abiertas en el orden que manda la API (prioridad) y finalizadas aparte, al final. */
export function separarFinalizadas(tareas) {
	return {
		abiertas: tareas.filter(t => t.estado !== "FINALIZADA"),
		finalizadas: tareas.filter(t => t.estado === "FINALIZADA")
	};
}

/**
 * Grupos de la lista según la vista, en orden de fecha. Dentro de cada grupo se respeta el orden de la API.
 * - Semana: "Durante la semana" y un grupo por día.
 * - Mes: "Durante el mes" y un grupo por semana (con sus días adentro).
 * - Largo plazo: un grupo por mes.
 * - Hoy y Por planificar: un solo grupo sin título.
 *
 * @returns {{clave: string, titulo: string|null, tareas: object[]}[]}
 */
export function agrupar(tareas, vista) {
	const claveDe = {
		SEMANA: t => t.horizonte === "SEMANA" ? "0" : t.fechaReferencia,
		MES: t => t.horizonte === "MES" ? "0" : lunesDe(t.fechaReferencia),
		LARGO_PLAZO: t => t.fechaReferencia.slice(0, 7)
	}[vista];
	if (!claveDe) return [{ clave: "todas", titulo: null, tareas }];

	const tituloDe = {
		SEMANA: c => c === "0" ? "Durante la semana" : textoDiaLargo(c),
		MES: c => c === "0" ? "Durante el mes" : "Semana del " + DIA_MES.format(aFecha(c)),
		LARGO_PLAZO: c => textoMes(c + "-01")
	}[vista];
	const grupos = new Map();
	for (const t of tareas) {
		const clave = claveDe(t);
		if (!grupos.has(clave)) grupos.set(clave, []);
		grupos.get(clave).push(t);
	}
	return [...grupos.keys()].sort().map(clave => ({ clave, titulo: tituloDe(clave), tareas: grupos.get(clave) }));
}

// ---------- Dónde quedó una tarea ----------

/** Vista donde se ve una tarea (para el enlace "Ver" después de crearla en otra vista). */
export function rutaDeTarea({ horizonte, fechaReferencia }, hoy) {
	switch (horizonte) {
		case "SIN_PLAZO": return "/por-planificar";
		case "SEMANA": return `/semana?fecha=${fechaReferencia}`;
		case "MES": return `/mes?fecha=${fechaReferencia}`;
		default: return fechaReferencia === hoy ? "/" : `/semana?fecha=${fechaReferencia}`;
	}
}

/** "Hoy", "Por planificar", "la semana del 16 nov", "Noviembre de 2026", "Mar 24 nov". */
export function dondeQuedo({ horizonte, fechaReferencia }, hoy) {
	switch (horizonte) {
		case "SIN_PLAZO": return "Por planificar";
		case "SEMANA": return "la semana del " + DIA_MES.format(aFecha(fechaReferencia));
		case "MES": return textoMes(fechaReferencia);
		default: return fechaReferencia === hoy ? "Hoy" : textoDia(fechaReferencia);
	}
}
