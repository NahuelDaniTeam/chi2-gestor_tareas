/**
 * Estadísticas (§4.14): números principales, un gráfico de barras de una sola serie (finalizadas por semana)
 * con tooltip al pasar el mouse o el foco, su tabla equivalente y el avance por proyecto.
 */

import { api } from "./api.js";
import { avisar, el } from "./dom.js";
import { aFecha } from "./formato.js";

const DIA_MES = new Intl.DateTimeFormat("es-AR", { day: "numeric", month: "short" });
const SVG = "http://www.w3.org/2000/svg";

function svg(tag, atributos = {}) {
	const nodo = document.createElementNS(SVG, tag);
	for (const [clave, valor] of Object.entries(atributos)) nodo.setAttribute(clave, valor);
	return nodo;
}

const plural = (n, uno, varios) => `${n} ${n === 1 ? uno : varios}`;
const semanaDel = lunes => "Semana del " + DIA_MES.format(aFecha(lunes));

function datos(e) {
	const poner = (clave, texto) => { document.querySelector(`[data-dato="${clave}"]`).textContent = texto; };
	poner("semana", e.completadasEstaSemana);
	poner("racha", e.rachaDias);
	poner("pendientes", e.pendientes);
	poner("vencidas", e.vencidas);
	const diferencia = e.completadasEstaSemana - e.completadasSemanaPasada;
	poner("comparacion", diferencia === 0 ? "igual que la semana pasada"
		: `${diferencia > 0 ? "+" : "−"}${Math.abs(diferencia)} respecto de la semana pasada`);
}

/** Barras verticales desde una base en cero; la de la semana actual lleva su valor escrito. */
function grafico(semanas) {
	const contenedor = document.getElementById("grafico");
	const ancho = 640, alto = 220, izquierda = 28, abajo = 26, arriba = 18;
	const maximo = Math.max(4, ...semanas.map(s => s.cantidad));
	const escala = Math.ceil(maximo / 4) * 4;
	const columna = (ancho - izquierda) / semanas.length;
	const barra = Math.min(36, columna * 0.55);
	const y = v => alto - abajo - (v / escala) * (alto - abajo - arriba);

	const lienzo = svg("svg", { viewBox: `0 0 ${ancho} ${alto}`, class: "w-full h-auto overflow-visible" });
	for (let i = 0; i <= 4; i++) {
		const valor = (escala / 4) * i;
		lienzo.append(
			svg("line", { x1: izquierda, x2: ancho, y1: y(valor), y2: y(valor), class: i === 0 ? "eje" : "grilla" }),
			Object.assign(svg("text", { x: izquierda - 8, y: y(valor) + 4, "text-anchor": "end", class: "eje-texto" }),
				{ textContent: String(valor) }));
	}

	const tooltip = el("div", "tooltip-grafico");
	tooltip.hidden = true;
	semanas.forEach((s, i) => {
		const centro = izquierda + columna * i + columna / 2;
		const alto0 = y(0) - y(s.cantidad);
		const grupo = svg("g", { tabindex: "0", role: "img", class: "barra-grupo",
			"aria-label": `${semanaDel(s.lunes)}: ${plural(s.cantidad, "finalizada", "finalizadas")}` });
		// Área sensible: toda la columna, más grande que la barra
		grupo.append(svg("rect", { x: centro - columna / 2, y: arriba, width: columna, height: alto - abajo - arriba, class: "barra-zona" }));
		if (s.cantidad > 0) {
			// Barra con las puntas de datos redondeadas (4px) y apoyada en la base
			const r = Math.min(4, alto0);
			const x0 = centro - barra / 2, x1 = centro + barra / 2, base = y(0), tope = y(s.cantidad);
			grupo.append(svg("path", { class: "barra",
				d: `M${x0},${base} V${tope + r} Q${x0},${tope} ${x0 + r},${tope} H${x1 - r} Q${x1},${tope} ${x1},${tope + r} V${base} Z` }));
		}
		if (i === semanas.length - 1) {
			grupo.append(Object.assign(svg("text", { x: centro, y: y(s.cantidad) - 6, "text-anchor": "middle", class: "valor-texto" }),
				{ textContent: String(s.cantidad) }));
		}
		grupo.append(Object.assign(svg("text", { x: centro, y: alto - 6, "text-anchor": "middle", class: "eje-texto" }),
			{ textContent: DIA_MES.format(aFecha(s.lunes)) }));

		const mostrar = () => {
			tooltip.replaceChildren(el("span", "block text-xs text-tenue", semanaDel(s.lunes)),
				el("span", "block font-medium", plural(s.cantidad, "finalizada", "finalizadas")));
			tooltip.hidden = false;
			const caja = contenedor.getBoundingClientRect();
			tooltip.style.left = `${(centro / ancho) * caja.width}px`;
			tooltip.style.top = `${(y(s.cantidad) / alto) * caja.height}px`;
			grupo.classList.add("activa");
		};
		const ocultar = () => { tooltip.hidden = true; grupo.classList.remove("activa"); };
		grupo.addEventListener("mouseenter", mostrar);
		grupo.addEventListener("focus", mostrar);
		grupo.addEventListener("mouseleave", ocultar);
		grupo.addEventListener("blur", ocultar);
		lienzo.append(grupo);
	});
	contenedor.replaceChildren(lienzo, tooltip);

	document.getElementById("tabla-semanas").replaceChildren(...semanas.map(s =>
		el("tr", null, el("td", null, semanaDel(s.lunes)), el("td", "text-right tabular-nums", String(s.cantidad)))));
}

function proyectos(filas) {
	const cuerpo = document.getElementById("tabla-proyectos");
	if (!filas.length) {
		cuerpo.replaceChildren(el("tr", null, el("td", "text-tenue", "Todavía no hay actividad.")));
		return;
	}
	cuerpo.replaceChildren(...filas.map(f => {
		const punto = el("span", "punto" + (f.hex ? "" : " punto-proyecto"));
		if (f.hex) punto.style.backgroundColor = f.hex;
		return el("tr", null,
			el("td", null, el("span", "inline-flex items-center gap-2", punto, f.nombre)),
			el("td", "text-right tabular-nums", String(f.completadas30Dias)),
			el("td", "text-right tabular-nums", String(f.pendientes)));
	}));
}

try {
	const e = await api.estadisticas();
	datos(e);
	grafico(e.porSemana);
	proyectos(e.porProyecto);
} catch (error) {
	avisar(error.message, { error: true });
}
