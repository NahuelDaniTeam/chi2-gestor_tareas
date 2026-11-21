/**
 * Cuándo sugerir la revisión semanal (§4.12): si hay algo para revisar y no se hizo en los últimos 7 días,
 * o si es lunes y todavía no se hizo hoy. Lógica sin DOM, testeada en src/test/js.
 */

export const CLAVE_REVISION = "chi2-tareas-revision";

const SIETE_DIAS = 7 * 24 * 60 * 60 * 1000;

/**
 * @param ultima instante ISO de la última revisión, o null
 * @param ahora Date
 * @param pendientes cuántas vencidas más cuántas sin plazo hay
 */
export function convieneRevisar(ultima, ahora, pendientes) {
	if (pendientes === 0) return false;
	if (!ultima) return true;
	const desde = ahora - new Date(ultima);
	const esLunes = ahora.getDay() === 1;
	const hechaHoy = new Date(ultima).toDateString() === ahora.toDateString();
	return desde >= SIETE_DIAS || (esLunes && !hechaHoy);
}
