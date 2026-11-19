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
export function avisar(mensaje, { error = false } = {}) {
	const aviso = document.getElementById("aviso");
	aviso.replaceChildren(icono(error ? "alerta" : "check", "size-4 shrink-0"), el("span", null, mensaje));
	aviso.dataset.tipo = error ? "error" : "ok";
	aviso.hidden = false;
	clearTimeout(temporizadorAviso);
	temporizadorAviso = setTimeout(() => { aviso.hidden = true; }, 3200);
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
