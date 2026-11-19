/** Ayudas mínimas para armar DOM. El texto del usuario va siempre por textContent, nunca como HTML. */

const SVG = "http://www.w3.org/2000/svg";

export function el(tag, clases, ...hijos) {
	const e = document.createElement(tag);
	if (clases) e.className = clases;
	e.append(...hijos.filter(h => h !== null && h !== undefined && h !== false));
	return e;
}

/** Ícono del sprite /img/iconos.svg. */
export function icono(nombre, clases = "size-4") {
	const svg = document.createElementNS(SVG, "svg");
	svg.setAttribute("class", "icono " + clases);
	svg.setAttribute("aria-hidden", "true");
	const use = document.createElementNS(SVG, "use");
	use.setAttribute("href", `/img/iconos.svg#${nombre}`);
	svg.append(use);
	return svg;
}

/** Mantiene el foco del teclado dentro de un contenedor abierto (panel, diálogo). */
export function atraparFoco(contenedor, evento) {
	if (evento.key !== "Tab") return;
	const enfocables = [...contenedor.querySelectorAll(
		"button, [href], input, select, textarea, [tabindex]:not([tabindex='-1'])")]
		.filter(e => !e.disabled && e.offsetParent !== null);
	if (!enfocables.length) return;
	const primero = enfocables[0];
	const ultimo = enfocables.at(-1);
	if (evento.shiftKey && document.activeElement === primero) {
		evento.preventDefault();
		ultimo.focus();
	} else if (!evento.shiftKey && document.activeElement === ultimo) {
		evento.preventDefault();
		primero.focus();
	}
}

let temporizadorAviso;

/** Aviso breve abajo al centro. */
/**
 * Aviso breve abajo al centro. Con {@code enlace} ({texto, href}) dura más y suma un enlace,
 * por ejemplo para ir a la vista donde quedó una tarea recién creada.
 */
export function avisar(mensaje, { error = false, enlace = null } = {}) {
	const aviso = document.getElementById("aviso");
	const accion = enlace
		? el("a", "ml-2 font-medium underline decoration-fondo/40 underline-offset-4 hover:decoration-fondo", enlace.texto)
		: null;
	if (accion) accion.href = enlace.href;
	aviso.replaceChildren(icono(error ? "alerta" : "check", "size-4 shrink-0"), el("span", null, mensaje), accion);
	aviso.dataset.tipo = error ? "error" : "ok";
	aviso.hidden = false;
	clearTimeout(temporizadorAviso);
	const ocultar = () => { temporizadorAviso = setTimeout(() => { aviso.hidden = true; }, enlace ? 7000 : 3200); };
	// Mientras el mouse está encima, el aviso no se va
	aviso.onmouseenter = () => clearTimeout(temporizadorAviso);
	aviso.onmouseleave = ocultar;
	ocultar();
}

export function leerPreferencia(clave, porDefecto) {
	try {
		return localStorage.getItem(clave) ?? porDefecto;
	} catch {
		return porDefecto;
	}
}

export function guardarPreferencia(clave, valor) {
	try {
		localStorage.setItem(clave, valor);
	} catch {
		// sin almacenamiento (ventana privada): la preferencia dura hasta recargar
	}
}
