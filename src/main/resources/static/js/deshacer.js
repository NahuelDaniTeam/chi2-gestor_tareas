/**
 * Deshacer (§4.8): cada acción que se puede revertir deja acá cómo volver atrás.
 * Ctrl+Z (fuera de un campo de texto) o el botón "Deshacer" del aviso revierten la última.
 */

import { avisar } from "./dom.js";

const MAXIMO = 20;
const pila = [];

/**
 * @param descripcion "Tarea eliminada", "Tarea movida al viernes"...
 * @param revertir async () => void
 * @param avisarAhora si true, muestra el aviso con el botón "Deshacer"
 */
export function registrar(descripcion, revertir, { avisarAhora = false } = {}) {
	pila.push({ descripcion, revertir });
	if (pila.length > MAXIMO) pila.shift();
	if (avisarAhora) avisar(descripcion + ".", { accion: { texto: "Deshacer", alHacer: deshacer } });
}

export async function deshacer() {
	const ultima = pila.pop();
	if (!ultima) {
		avisar("No hay nada para deshacer.");
		return;
	}
	try {
		await ultima.revertir();
		avisar(`Deshecho: ${ultima.descripcion.toLowerCase()}.`);
	} catch (e) {
		avisar(e.message, { error: true });
	}
}
