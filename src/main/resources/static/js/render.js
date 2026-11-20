/**
 * Dibuja las tareas como lista, tablero o calendario (semana y mes). No guarda nada: avisa por las acciones.
 * Lo que se puede soltar lleva data-soltar-estado (tablero) o data-soltar-plazo + data-soltar-fecha (calendario).
 */

import { el, icono, leerPreferencia, guardarPreferencia } from "./dom.js";
import {
	ESTADOS, PRIORIDADES, agrupar, diasDeSemana, etiquetaFecha, semanasDelMes, separarFinalizadas, textoDiaSemana
} from "./formato.js";

const MAX_POR_CELDA = 3;

/**
 * @param estado {tareas, modo, cargando, error}
 * @param contexto {vista, fechaVista, hoy}
 * @param acciones {abrir(t), cambiarEstado(t, e), mover(t, plazo, fecha)}
 */
export function dibujar(estado, contexto, acciones) {
	const zonas = {
		lista: document.getElementById("lista"),
		tablero: document.getElementById("tablero"),
		calendario: document.getElementById("calendario")
	};
	document.querySelectorAll("[data-modo]").forEach(b =>
		b.setAttribute("aria-pressed", String(b.dataset.modo === estado.modo)));
	document.getElementById("cargando").hidden = !estado.cargando;
	document.getElementById("problema").hidden = !estado.error;

	const listo = !estado.cargando && !estado.error;
	// El calendario se muestra aunque esté vacío: es el lugar donde se planifica arrastrando.
	const vacio = listo && !estado.tareas.length && estado.modo !== "calendario";
	document.getElementById("vacio").hidden = !vacio;
	for (const [modo, zona] of Object.entries(zonas)) {
		if (zona) zona.hidden = !listo || vacio || estado.modo !== modo;
	}
	if (!listo || vacio) return;

	if (estado.modo === "lista") dibujarLista(zonas.lista, estado.tareas, contexto, acciones);
	else if (estado.modo === "tablero") dibujarTablero(estado.tareas, contexto, acciones);
	else if (contexto.vista === "SEMANA") dibujarSemana(zonas.calendario, estado.tareas, contexto, acciones);
	else dibujarMes(zonas.calendario, estado.tareas, contexto, acciones);
}

// ---------- Lista ----------

function dibujarLista(contenedor, tareas, contexto, acciones) {
	const { abiertas, finalizadas } = separarFinalizadas(tareas);
	const bloques = [];
	const agrupadaPorDia = contexto.vista === "SEMANA";
	for (const grupo of agrupar(abiertas, contexto.vista)) {
		if (grupo.titulo) bloques.push(titulo(grupo.titulo, grupo.tareas.length));
		bloques.push(filas(grupo.tareas, { ...contexto, mostrarDia: !agrupadaPorDia }, acciones));
	}
	if (!abiertas.length) {
		bloques.push(el("p", "rounded-xl border border-linea bg-superficie px-5 py-6 text-tenue", "Terminaste todo lo de esta vista."));
	}
	if (finalizadas.length) bloques.push(bloqueFinalizadas(finalizadas, contexto, acciones));
	contenedor.replaceChildren(...bloques);
}

function titulo(texto, cantidad) {
	return el("h2", "mt-8 mb-2.5 flex items-baseline gap-2 text-sm font-medium first:mt-0",
		texto, el("span", "text-xs tabular-nums text-tenue", String(cantidad)));
}

function filas(tareas, contexto, acciones) {
	return el("ul", "divide-y divide-linea overflow-hidden rounded-xl border border-linea bg-superficie",
		...tareas.map(t => fila(t, contexto, acciones)));
}

function bloqueFinalizadas(tareas, contexto, acciones) {
	const detalles = el("details", "group/fin mt-8");
	detalles.open = leerPreferencia("chi2-tareas-finalizadas", "cerradas") === "abiertas";
	detalles.addEventListener("toggle", () =>
		guardarPreferencia("chi2-tareas-finalizadas", detalles.open ? "abiertas" : "cerradas"));
	detalles.append(
		el("summary", "mb-3 flex cursor-pointer list-none items-center gap-2 text-sm font-medium text-tenue hover:text-tinta",
			icono("abajo", "size-4 -rotate-90 transition group-open/fin:rotate-0"),
			"Finalizadas", el("span", "text-xs tabular-nums text-tenue/70", String(tareas.length))),
		filas(tareas, contexto, acciones));
	return detalles;
}

function fila(t, contexto, acciones) {
	const cuerpo = el("button", "min-w-0 flex-1 text-left outline-none",
		tituloTarea(t, "leading-snug"), meta(t, contexto, { conEstado: true }));
	cuerpo.type = "button";
	cuerpo.addEventListener("click", () => acciones.abrir(t));

	const li = el("li", "fila group flex items-start gap-3.5 px-4 py-3 sm:px-5",
		casilla(t, acciones), cuerpo,
		contexto.vista === "POR_PLANIFICAR" ? botonesPlanificar(t, contexto, acciones)
			: el("span", "mt-0.5 text-tenue/0 transition group-hover:text-tenue/60", icono("siguiente", "size-4")));
	li.dataset.id = t.id;
	hacerArrastrable(li, t);
	return li;
}

/** En Por planificar, cada tarea se planifica con un clic. */
function botonesPlanificar(t, { hoy }, acciones) {
	const opciones = [["Hoy", "DIA"], ["Esta semana", "SEMANA"], ["Este mes", "MES"]];
	return el("span", "flex shrink-0 items-center gap-1 opacity-100 transition sm:opacity-0 sm:group-focus-within:opacity-100 sm:group-hover:opacity-100",
		...opciones.map(([texto, plazo]) => {
			const b = el("button", "boton-mini", texto);
			b.type = "button";
			b.addEventListener("click", () => acciones.mover(t, plazo, hoy));
			return b;
		}));
}

function casilla(t, acciones) {
	const finalizada = t.estado === "FINALIZADA";
	const b = el("button", "casilla mt-0.5", icono("check", "size-3"));
	b.type = "button";
	b.setAttribute("role", "checkbox");
	b.setAttribute("aria-checked", String(finalizada));
	b.setAttribute("aria-label", (finalizada ? "Reabrir: " : "Finalizar: ") + t.titulo);
	b.dataset.prioridad = t.prioridad;
	b.addEventListener("click", e => {
		e.stopPropagation();
		acciones.cambiarEstado(t, finalizada ? "PENDIENTE" : "FINALIZADA");
	});
	return b;
}

// ---------- Tablero ----------

function dibujarTablero(tareas, contexto, acciones) {
	for (const estado of Object.keys(ESTADOS)) {
		const columna = document.querySelector(`.columna[data-soltar-estado="${estado}"]`);
		const deEstado = tareas.filter(t => t.estado === estado);
		columna.replaceChildren(...(deEstado.length
			? deEstado.map(t => tarjeta(t, contexto, acciones))
			: [el("p", "columna-vacia", "Sin tareas")]));
		document.querySelector(`[data-contador="${estado}"]`).textContent = deEstado.length;
	}
}

function tarjeta(t, contexto, acciones) {
	const b = el("button", "tarjeta",
		tituloTarea(t, "font-medium leading-snug"), meta(t, contexto, { conEstado: false }));
	b.type = "button";
	b.dataset.prioridad = t.prioridad;
	b.setAttribute("aria-label", `${t.titulo}. Abrir para editar.`);
	b.addEventListener("click", () => acciones.abrir(t));
	hacerArrastrable(b, t);
	return b;
}

// ---------- Calendario ----------

function dibujarSemana(contenedor, tareas, contexto, acciones) {
	const lunes = contexto.fechaVista;
	const delPeriodo = tareas.filter(t => t.horizonte === "SEMANA");
	const columnas = diasDeSemana(lunes).map(dia => {
		const delDia = tareas.filter(t => t.horizonte !== "SEMANA" && t.fechaReferencia === dia);
		const cabecera = el("div", "dia-cabecera" + (dia === contexto.hoy ? " es-hoy" : ""),
			el("span", "text-tenue", textoDiaSemana(dia)), el("span", "numero-dia", String(Number(dia.slice(8)))));
		const zona = el("div", "zona-dia", ...delDia.map(t => pastilla(t, contexto, acciones)));
		marcarDestino(zona, "DIA", dia);
		return el("div", "dia-semana", cabecera, zona);
	});
	contenedor.replaceChildren(
		franja("Durante la semana", delPeriodo, "SEMANA", lunes, contexto, acciones, true),
		el("div", "grilla-semana", ...columnas));
}

function dibujarMes(contenedor, tareas, contexto, acciones) {
	const primero = contexto.fechaVista;
	const mes = primero.slice(0, 7);
	const cabeceras = diasDeSemana(semanasDelMes(primero)[0][0]).map(d => el("div", "mes-cabecera", textoDiaSemana(d)));
	const filasSemana = semanasDelMes(primero).map(dias => {
		const deSemana = tareas.filter(t => t.horizonte === "SEMANA" && t.fechaReferencia === dias[0]);
		const celdas = dias.map(dia => celdaMes(dia, mes, tareas, contexto, acciones));
		return el("div", "semana-mes",
			franja("Semana del " + Number(dias[0].slice(8)), deSemana, "SEMANA", dias[0], contexto, acciones, false),
			el("div", "grid grid-cols-7", ...celdas));
	});
	contenedor.replaceChildren(
		franja("Durante el mes", tareas.filter(t => t.horizonte === "MES"), "MES", primero, contexto, acciones, true),
		el("div", "grilla-mes", el("div", "grid grid-cols-7", ...cabeceras), ...filasSemana));
}

function celdaMes(dia, mes, tareas, contexto, acciones) {
	const delDia = tareas.filter(t => (t.horizonte === "DIA" || t.horizonte === "LIMITE") && t.fechaReferencia === dia);
	const visibles = delDia.slice(0, MAX_POR_CELDA).map(t => pastilla(t, contexto, acciones));
	const resto = delDia.length - visibles.length;
	const enlaceDia = el("a", "numero-dia", String(Number(dia.slice(8))));
	enlaceDia.href = `/semana?fecha=${dia}`;
	enlaceDia.title = "Ver la semana";
	const celda = el("div", "celda-mes" + (dia.slice(0, 7) !== mes ? " fuera-de-mes" : "") + (dia === contexto.hoy ? " es-hoy" : ""),
		enlaceDia, ...visibles,
		resto > 0 ? el("a", "mas-tareas", `+${resto} más`) : null);
	if (resto > 0) celda.lastChild.href = `/semana?fecha=${dia}`;
	marcarDestino(celda, "DIA", dia);
	return celda;
}

/** Banda de tareas de un período más largo que un día ("Durante la semana", "Semana del 16", "Durante el mes"). */
function franja(texto, tareas, plazo, fecha, contexto, acciones, siempreVisible) {
	const zona = el("div", "franja" + (tareas.length || siempreVisible ? "" : " franja-vacia"),
		el("span", "franja-titulo", texto),
		...tareas.map(t => pastilla(t, contexto, acciones)),
		!tareas.length ? el("span", "franja-ayuda", "Arrastrá acá lo que no tiene día") : null);
	marcarDestino(zona, plazo, fecha);
	return zona;
}

/** Tarea compacta para el calendario. */
function pastilla(t, contexto, acciones) {
	const finalizada = t.estado === "FINALIZADA";
	const b = el("button", "pastilla" + (finalizada ? " finalizada" : "") + (t.vencida ? " vencida" : ""),
		t.horizonte === "LIMITE" ? icono("bandera", "size-3 shrink-0 text-acento") : el("span", `punto punto-${t.prioridad}`),
		el("span", "truncate", t.titulo));
	b.type = "button";
	b.title = t.titulo + (t.horizonte === "LIMITE" ? " (fecha límite)" : "");
	const proyecto = contexto.proyectoDe(t.proyectoId);
	if (proyecto) b.style.boxShadow = `inset 2px 0 0 ${proyecto.hex}`;
	b.addEventListener("click", () => acciones.abrir(t));
	hacerArrastrable(b, t);
	return b;
}

// ---------- Arrastrar y soltar ----------

function hacerArrastrable(elemento, t) {
	elemento.draggable = true;
	elemento.dataset.arrastrable = t.id;
}

function marcarDestino(zona, plazo, fecha) {
	zona.dataset.soltarPlazo = plazo;
	zona.dataset.soltarFecha = fecha;
}

/**
 * Arrastrar y soltar con un solo juego de eventos delegados: sirve para el tablero (cambia el estado)
 * y para el calendario (cambia el plazo). Se engancha una vez; las tarjetas cambian en cada dibujo.
 */
export function habilitarArrastre(buscarTarea, { cambiarEstado, mover }) {
	let arrastrando = null;
	const destinoDe = e => e.target.closest?.("[data-soltar-estado], [data-soltar-plazo]");

	document.addEventListener("dragstart", e => {
		const origen = e.target.closest?.("[data-arrastrable]");
		if (!origen) return;
		arrastrando = buscarTarea(origen.dataset.arrastrable);
		e.dataTransfer.setData("text/plain", origen.dataset.arrastrable);
		e.dataTransfer.effectAllowed = "move";
		requestAnimationFrame(() => {
			origen.classList.add("arrastrando");
			document.body.classList.add("arrastrando-algo");
		});
	});
	document.addEventListener("dragend", e => {
		e.target.closest?.("[data-arrastrable]")?.classList.remove("arrastrando");
		document.body.classList.remove("arrastrando-algo");
		document.querySelectorAll(".destino").forEach(z => z.classList.remove("destino"));
		arrastrando = null;
	});
	document.addEventListener("dragover", e => {
		const destino = destinoDe(e);
		if (!destino || !arrastrando) return;
		e.preventDefault();
		e.dataTransfer.dropEffect = "move";
		document.querySelectorAll(".destino").forEach(z => z !== destino && z.classList.remove("destino"));
		destino.classList.add("destino");
	});
	document.addEventListener("drop", e => {
		const destino = destinoDe(e);
		if (!destino || !arrastrando) return;
		e.preventDefault();
		destino.classList.remove("destino");
		const tarea = arrastrando;
		if (destino.dataset.soltarEstado) {
			cambiarEstado(tarea, destino.dataset.soltarEstado);
			return;
		}
		const plazo = destino.dataset.soltarPlazo;
		const fecha = destino.dataset.soltarFecha;
		// Una fecha límite sigue siendo fecha límite al cambiarla de día.
		const nuevoPlazo = plazo === "DIA" && tarea.horizonte === "LIMITE" ? "LIMITE" : plazo;
		if (nuevoPlazo !== tarea.horizonte || fecha !== tarea.fechaReferencia) mover(tarea, nuevoPlazo, fecha);
	});
}

// ---------- Datos de cada tarea ----------

/** Título, con una marca discreta si la tarea tiene descripción. */
function tituloTarea(t, clases) {
	const finalizada = t.estado === "FINALIZADA" ? " finalizada" : "";
	const nota = t.descripcion
		? el("span", "ml-1.5 inline-flex align-[-0.125em] text-tenue/70", icono("nota", "size-3.5"), el("span", "sr-only", " (tiene descripción)"))
		: null;
	return el("span", `titulo-tarea block break-words ${clases}${finalizada}`, t.titulo, nota);
}

/** Nombre y color del proyecto, salvo que la vista ya esté filtrada por ese proyecto. */
function etiquetaProyecto(t, contexto) {
	const proyecto = contexto.proyectoDe(t.proyectoId);
	if (!proyecto || contexto.filtroProyecto() === proyecto.id) return null;
	const punto = el("span", "punto");
	punto.style.backgroundColor = proyecto.hex;
	return el("span", "inline-flex items-center gap-1.5", punto, proyecto.nombre);
}

function meta(t, contexto, { conEstado }) {
	const partes = [];
	const proyecto = etiquetaProyecto(t, contexto);
	if (proyecto) partes.push(proyecto);
	if (t.prioridad !== "MEDIA") {
		partes.push(el("span", "inline-flex items-center gap-1.5" + (t.prioridad === "ALTA" ? " font-medium text-acento" : ""),
			el("span", `punto punto-${t.prioridad}`), PRIORIDADES[t.prioridad]));
	}
	if (t.vencida) {
		partes.push(el("span", "inline-flex items-center gap-1 font-medium text-acento", icono("alerta", "size-3.5"), "Vencida"));
	}
	const fecha = etiquetaFecha(t, contexto);
	if (fecha) {
		partes.push(el("span", "inline-flex items-center gap-1" + (t.horizonte === "LIMITE" ? " text-tinta" : ""),
			icono(t.horizonte === "LIMITE" ? "bandera" : "calendario", "size-3.5"), fecha));
	}
	if (conEstado && t.estado === "EN_CURSO") partes.push(el("span", "etiqueta-estado", "En curso"));
	if (!partes.length) return null;
	return el("span", "mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-tenue", ...partes);
}

