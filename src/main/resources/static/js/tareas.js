/*
 * Gestor de tareas CHI2: vistas, alta rápida, tablero con arrastre y panel de edición.
 * Todo pasa por la API REST (/api/tareas); esta página no guarda nada por su cuenta.
 * Ver docs/01-especificacion.md §3 y §4.
 */
(() => {
	"use strict";

	const app = document.getElementById("app");
	const VISTA = app.dataset.vista;
	const HORIZONTE_VISTA = app.dataset.horizonte;
	const URL_LISTA = app.dataset.urlLista;
	const FECHA_ALTA = app.dataset.fechaAlta || null;
	const HOY = app.dataset.hoy;

	const ESTADOS = { PENDIENTE: "Pendiente", EN_CURSO: "En curso", FINALIZADA: "Finalizada" };
	const PRIORIDADES = { ALTA: "Alta", MEDIA: "Media", BAJA: "Baja" };
	const HORIZONTES = { DIA: "Día", SEMANA: "Semana", MES: "Mes", LARGO_PLAZO: "Largo plazo" };

	/* Atajos del alta rápida (§4.2, propuesta en PENDIENTES.md 2): "!a" prioridad, "@s" horizonte. */
	const ATAJOS_PRIORIDAD = { a: "ALTA", alta: "ALTA", m: "MEDIA", media: "MEDIA", b: "BAJA", baja: "BAJA" };
	const ATAJOS_HORIZONTE = {
		d: "DIA", dia: "DIA", día: "DIA", hoy: "DIA",
		s: "SEMANA", semana: "SEMANA",
		m: "MES", mes: "MES",
		l: "LARGO_PLAZO", largo: "LARGO_PLAZO"
	};

	let tareas = [];
	let modo = leerModo();

	// ---------- API ----------

	async function api(metodo, url, cuerpo) {
		const respuesta = await fetch(url, {
			method: metodo,
			headers: cuerpo ? { "Content-Type": "application/json" } : {},
			body: cuerpo ? JSON.stringify(cuerpo) : undefined
		});
		if (respuesta.status === 204) return null;
		const datos = await respuesta.json().catch(() => null);
		if (!respuesta.ok) {
			const error = new Error(datos?.detail || "No se pudo guardar. Probá de nuevo.");
			error.errores = datos?.errores || {};
			throw error;
		}
		return datos;
	}

	async function cargar() {
		try {
			tareas = await api("GET", URL_LISTA);
			render();
		} catch (e) {
			avisar("No pudimos traer las tareas. ¿Está corriendo la aplicación?");
		}
	}

	// ---------- Fechas ----------

	const fmtDia = new Intl.DateTimeFormat("es-AR", { weekday: "short", day: "numeric", month: "short" });
	const fmtDiaAnio = new Intl.DateTimeFormat("es-AR", { day: "numeric", month: "short", year: "numeric" });
	const fmtMes = new Intl.DateTimeFormat("es-AR", { month: "long", year: "numeric" });
	const fmtMomento = new Intl.DateTimeFormat("es-AR", { dateStyle: "medium", timeStyle: "short" });

	/** "2026-11-19" → Date local, sin corrimiento por zona horaria. */
	function aFecha(iso) {
		const [a, m, d] = iso.split("-").map(Number);
		return new Date(a, m - 1, d);
	}

	function hoyLocal() {
		const d = new Date();
		return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
	}

	function textoFecha(t) {
		if (!t.fechaReferencia) return "Sin fecha";
		const f = aFecha(t.fechaReferencia);
		switch (t.horizonte) {
			case "DIA": return t.fechaReferencia === HOY ? "Hoy" : fmtDia.format(f);
			case "SEMANA": return "Semana del " + fmtDia.format(f);
			case "MES": return capitalizar(fmtMes.format(f));
			default: return "Límite: " + fmtDiaAnio.format(f);
		}
	}

	function capitalizar(texto) {
		return texto.charAt(0).toUpperCase() + texto.slice(1);
	}

	// ---------- Render ----------

	function render() {
		document.querySelectorAll("[data-modo]").forEach(b =>
			b.setAttribute("aria-current", b.dataset.modo === modo ? "page" : "false"));
		const hayTareas = tareas.length > 0;
		document.getElementById("vacio").hidden = hayTareas;
		document.getElementById("lista").hidden = !hayTareas || modo !== "lista";
		document.getElementById("tablero").hidden = !hayTareas || modo !== "tablero";
		document.getElementById("resumen").textContent = resumen();
		if (modo === "lista") renderLista(); else renderTablero();
	}

	function resumen() {
		const abiertas = tareas.filter(t => t.estado !== "FINALIZADA").length;
		const vencidas = tareas.filter(t => t.vencida).length;
		let texto = `${abiertas} sin terminar de ${tareas.length}`;
		if (vencidas) texto += ` · ${vencidas} vencida${vencidas > 1 ? "s" : ""}`;
		return tareas.length ? texto : "";
	}

	function renderLista() {
		const lista = document.getElementById("lista");
		lista.replaceChildren();
		if (VISTA === "LARGO_PLAZO") {
			// Largo plazo: agrupadas por prioridad (§4.3)
			for (const p of Object.keys(PRIORIDADES)) {
				const grupo = tareas.filter(t => t.prioridad === p);
				if (!grupo.length) continue;
				const h = el("h2", "mt-8 mb-2 flex items-center gap-2 text-xs font-medium tracking-[0.2em] text-tenue uppercase first:mt-0");
				h.append(el("span", `punto punto-${p}`), "Prioridad " + PRIORIDADES[p].toLowerCase());
				lista.append(h, ul(grupo));
			}
		} else {
			lista.append(ul(tareas));
		}
	}

	function ul(grupo) {
		const lista = el("ul", "divide-y divide-linea rounded-2xl border border-linea bg-superficie");
		grupo.forEach(t => lista.append(filaLista(t)));
		return lista;
	}

	function filaLista(t) {
		const li = el("li", "flex items-start gap-4 px-4 py-3.5 sm:px-5");
		const check = document.createElement("input");
		check.type = "checkbox";
		check.checked = t.estado === "FINALIZADA";
		check.className = "mt-1 size-4 shrink-0 cursor-pointer accent-[#191714]";
		check.setAttribute("aria-label", "Finalizada: " + t.titulo);
		check.addEventListener("change", () => cambiarEstado(t, check.checked ? "FINALIZADA" : "PENDIENTE"));

		const cuerpo = el("button", "min-w-0 flex-1 text-left");
		cuerpo.type = "button";
		cuerpo.addEventListener("click", () => abrirPanel(t));
		const titulo = el("span", "block break-words" + (t.estado === "FINALIZADA" ? " text-tenue line-through decoration-tenue/50" : ""));
		titulo.textContent = t.titulo;
		cuerpo.append(titulo, meta(t, true));
		li.append(check, cuerpo);
		return li;
	}

	function renderTablero() {
		for (const estado of Object.keys(ESTADOS)) {
			const columna = document.querySelector(`.columna[data-estado="${estado}"]`);
			const deEstado = tareas.filter(t => t.estado === estado);
			columna.replaceChildren(...deEstado.map(tarjeta));
			document.querySelector(`[data-contador="${estado}"]`).textContent = deEstado.length;
		}
	}

	function tarjeta(t) {
		const boton = el("button", `tarjeta tarjeta-${t.prioridad}`);
		boton.type = "button";
		boton.draggable = true;
		boton.dataset.id = t.id;
		const titulo = el("span", "block break-words font-medium" + (t.estado === "FINALIZADA" ? " text-tenue line-through decoration-tenue/50" : ""));
		titulo.textContent = t.titulo;
		boton.append(titulo, meta(t, false));
		boton.addEventListener("click", () => abrirPanel(t));
		boton.addEventListener("dragstart", e => {
			e.dataTransfer.setData("text/plain", t.id);
			e.dataTransfer.effectAllowed = "move";
			boton.classList.add("arrastrando");
		});
		boton.addEventListener("dragend", () => boton.classList.remove("arrastrando"));
		return boton;
	}

	function meta(t, conEstado) {
		const fila = el("span", "mt-1.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-tenue");
		const prioridad = el("span", "inline-flex items-center gap-1.5" + (t.prioridad === "ALTA" ? " text-acento" : ""));
		prioridad.append(el("span", `punto punto-${t.prioridad}`), PRIORIDADES[t.prioridad]);
		fila.append(prioridad, texto(textoFecha(t)));
		if (t.horizonte !== HORIZONTE_VISTA) fila.append(texto(HORIZONTES[t.horizonte]));
		if (conEstado && t.estado === "EN_CURSO") fila.append(texto("En curso"));
		if (t.vencida) {
			const vencida = el("span", "rounded-full bg-acento/10 px-2 py-0.5 font-medium text-acento");
			vencida.textContent = "Vencida";
			fila.append(vencida);
		}
		return fila;
	}

	function el(tag, clases) {
		const e = document.createElement(tag);
		if (clases) e.className = clases;
		return e;
	}

	function texto(contenido) {
		const s = el("span");
		s.textContent = contenido;
		return s;
	}

	// ---------- Cambios de estado (tablero y lista) ----------

	async function cambiarEstado(t, estado) {
		if (t.estado === estado) return;
		const anterior = t.estado;
		t.estado = estado; // optimista: se ve al instante
		render();
		try {
			reemplazar(await api("PATCH", `/api/tareas/${t.id}/estado`, { estado }));
		} catch (e) {
			t.estado = anterior;
			render();
			avisar(e.message);
		}
	}

	function reemplazar(actualizada) {
		const i = tareas.findIndex(t => t.id === actualizada.id);
		if (i >= 0) tareas[i] = actualizada;
		render();
	}

	document.querySelectorAll(".columna").forEach(columna => {
		columna.addEventListener("dragover", e => {
			e.preventDefault();
			e.dataTransfer.dropEffect = "move";
			columna.classList.add("destino");
		});
		columna.addEventListener("dragleave", e => {
			if (!columna.contains(e.relatedTarget)) columna.classList.remove("destino");
		});
		columna.addEventListener("drop", e => {
			e.preventDefault();
			columna.classList.remove("destino");
			const t = tareas.find(x => x.id === e.dataTransfer.getData("text/plain"));
			if (t) cambiarEstado(t, columna.dataset.estado);
		});
	});

	// ---------- Modo lista / tablero ----------

	function leerModo() {
		try {
			return localStorage.getItem("chi2-tareas-modo") === "tablero" ? "tablero" : "lista";
		} catch (e) {
			return "lista";
		}
	}

	document.querySelectorAll("[data-modo]").forEach(b => b.addEventListener("click", () => {
		modo = b.dataset.modo;
		try { localStorage.setItem("chi2-tareas-modo", modo); } catch (e) { /* sin almacenamiento: no pasa nada */ }
		render();
	}));

	// ---------- Alta rápida ----------

	const alta = document.getElementById("alta");
	const altaTitulo = document.getElementById("alta-titulo");
	const altaHorizonte = document.getElementById("alta-horizonte");
	const altaPrioridad = document.getElementById("alta-prioridad");
	const vistaPrevia = document.getElementById("alta-vista-previa");

	/** Separa los atajos ("!a", "@s") del título. */
	function interpretar(texto) {
		let prioridad = altaPrioridad.value;
		let horizonte = altaHorizonte.value;
		const titulo = texto.replace(/(^|\s)([!@])([\p{L}]+)(?=\s|$)/gu, (todo, espacio, signo, palabra) => {
			const clave = palabra.toLowerCase();
			if (signo === "!" && ATAJOS_PRIORIDAD[clave]) { prioridad = ATAJOS_PRIORIDAD[clave]; return espacio; }
			if (signo === "@" && ATAJOS_HORIZONTE[clave]) { horizonte = ATAJOS_HORIZONTE[clave]; return espacio; }
			return todo;
		}).replace(/\s+/g, " ").trim();
		return { titulo, prioridad, horizonte };
	}

	/** En la vista de su mismo horizonte, la tarea va al período que se está viendo; si no, a hoy. */
	function fechaPara(horizonte) {
		if (horizonte === "LARGO_PLAZO") return null;
		return horizonte === HORIZONTE_VISTA && FECHA_ALTA ? FECHA_ALTA : HOY;
	}

	altaTitulo.addEventListener("input", () => {
		const { prioridad, horizonte } = interpretar(altaTitulo.value);
		const cambio = prioridad !== altaPrioridad.value || horizonte !== altaHorizonte.value;
		vistaPrevia.textContent = cambio ? `Se guarda en ${HORIZONTES[horizonte]}, prioridad ${PRIORIDADES[prioridad].toLowerCase()}` : "";
	});

	alta.addEventListener("submit", async e => {
		e.preventDefault();
		const { titulo, prioridad, horizonte } = interpretar(altaTitulo.value);
		if (!titulo) return;
		try {
			const nueva = await api("POST", "/api/tareas", {
				titulo, horizonte, prioridad, fechaReferencia: fechaPara(horizonte)
			});
			altaTitulo.value = "";
			vistaPrevia.textContent = "";
			await cargar();
			if (!tareas.some(t => t.id === nueva.id)) avisar(`Guardada en ${HORIZONTES[nueva.horizonte]}.`);
		} catch (error) {
			avisar(error.errores?.titulo || error.message);
		}
		altaTitulo.focus();
	});

	// Enter dentro de los selectores también guarda
	[altaHorizonte, altaPrioridad].forEach(s => s.addEventListener("keydown", e => {
		if (e.key === "Enter") { e.preventDefault(); alta.requestSubmit(); }
	}));

	// ---------- Panel lateral de edición ----------

	const panel = document.getElementById("panel");
	const panelFondo = document.getElementById("panel-fondo");
	const form = document.getElementById("panel-form");
	const campo = nombre => form.elements[nombre];
	let enEdicion = null;
	let foco = null;
	let ultimoGuardado = Promise.resolve();

	function abrirPanel(t) {
		enEdicion = t;
		foco = document.activeElement;
		campo("titulo").value = t.titulo;
		campo("descripcion").value = t.descripcion || "";
		campo("estado").value = t.estado;
		campo("prioridad").value = t.prioridad;
		campo("horizonte").value = t.horizonte;
		campo("fechaReferencia").value = t.fechaReferencia || "";
		ajustarFecha();
		mostrarMeta(t);
		mostrarError(null);
		document.getElementById("panel-guardado").textContent = "";
		document.getElementById("p-eliminar").hidden = false;
		document.getElementById("p-confirmar").hidden = true;
		panel.hidden = panelFondo.hidden = false;
		campo("titulo").focus();
	}

	function cerrarPanel() {
		if (panel.hidden) return;
		// Salir del campo dispara su guardado antes de cerrar; la lista se recarga cuando termina.
		if (panel.contains(document.activeElement)) document.activeElement.blur();
		panel.hidden = panelFondo.hidden = true;
		enEdicion = null;
		ultimoGuardado.then(cargar);
		(foco && document.body.contains(foco) ? foco : altaTitulo).focus();
	}

	function ajustarFecha() {
		const horizonte = campo("horizonte").value;
		const etiquetas = { DIA: "Fecha", SEMANA: "Cualquier día de la semana", MES: "Cualquier día del mes", LARGO_PLAZO: "Fecha límite (opcional)" };
		document.getElementById("p-fecha-label").textContent = etiquetas[horizonte];
		campo("fechaReferencia").required = horizonte !== "LARGO_PLAZO";
	}

	function mostrarMeta(t) {
		const partes = ["Creada el " + fmtMomento.format(new Date(t.creadaEn)),
			"modificada el " + fmtMomento.format(new Date(t.actualizadaEn))];
		if (t.completadaEn) partes.push("finalizada el " + fmtMomento.format(new Date(t.completadaEn)));
		document.getElementById("p-meta").textContent = partes.join(" · ") + ".";
	}

	function mostrarError(mensaje) {
		const p = document.getElementById("p-error");
		p.hidden = !mensaje;
		p.textContent = mensaje || "";
	}

	async function guardarPanel(cambiado) {
		if (!enEdicion) return;
		const t = enEdicion;
		try {
			let actualizada;
			if (cambiado === "estado") {
				actualizada = await api("PATCH", `/api/tareas/${t.id}/estado`, { estado: campo("estado").value });
			} else {
				const horizonte = campo("horizonte").value;
				let fecha = campo("fechaReferencia").value || null;
				if (!fecha && horizonte !== "LARGO_PLAZO") fecha = fechaPara(horizonte);
				actualizada = await api("PUT", `/api/tareas/${t.id}`, {
					titulo: campo("titulo").value,
					descripcion: campo("descripcion").value,
					horizonte,
					fechaReferencia: fecha,
					prioridad: campo("prioridad").value
				});
				campo("fechaReferencia").value = actualizada.fechaReferencia || "";
			}
			Object.assign(t, actualizada);
			mostrarMeta(t);
			mostrarError(null);
			document.getElementById("panel-guardado").textContent = "Guardado";
		} catch (e) {
			mostrarError(Object.values(e.errores || {})[0] || e.message);
		}
	}

	// Los selectores y la fecha se guardan al cambiar; los textos, al salir del campo.
	["estado", "prioridad", "horizonte", "fechaReferencia", "titulo", "descripcion"].forEach(nombre =>
		campo(nombre).addEventListener("change", () => {
			if (nombre === "horizonte") ajustarFecha();
			ultimoGuardado = guardarPanel(nombre);
		}));
	campo("titulo").addEventListener("keydown", e => {
		if (e.key === "Enter") { e.preventDefault(); campo("titulo").blur(); }
	});
	form.addEventListener("submit", e => e.preventDefault());

	document.getElementById("p-eliminar").addEventListener("click", () => {
		document.getElementById("p-eliminar").hidden = true;
		document.getElementById("p-confirmar").hidden = false;
		document.getElementById("p-confirmar-no").focus();
	});
	document.getElementById("p-confirmar-no").addEventListener("click", () => {
		document.getElementById("p-eliminar").hidden = false;
		document.getElementById("p-confirmar").hidden = true;
	});
	document.getElementById("p-confirmar-si").addEventListener("click", async () => {
		try {
			await api("DELETE", `/api/tareas/${enEdicion.id}`);
			avisar("Tarea eliminada.");
			cerrarPanel();
		} catch (e) {
			mostrarError(e.message);
		}
	});

	panelFondo.addEventListener("click", cerrarPanel);
	panel.querySelector("[data-cerrar]").addEventListener("click", cerrarPanel);

	// ---------- Teclado y recarga ----------

	document.addEventListener("keydown", e => {
		if (e.key === "Escape") { cerrarPanel(); return; }
		const escribiendo = e.target.closest("input, textarea, select, [contenteditable]");
		if (!escribiendo && (e.key === "n" || e.key === "/") && panel.hidden) {
			e.preventDefault();
			altaTitulo.focus();
		}
	});

	// Si la ventana quedó abierta de un día para otro, al volver se recarga entera; si no, solo la lista.
	document.addEventListener("visibilitychange", () => {
		if (document.visibilityState !== "visible" || !panel.hidden) return;
		if (VISTA === "HOY" && hoyLocal() !== HOY) location.reload(); else cargar();
	});

	// ---------- Avisos ----------

	let temporizador;
	function avisar(mensaje) {
		const aviso = document.getElementById("aviso");
		aviso.textContent = mensaje;
		aviso.hidden = false;
		clearTimeout(temporizador);
		temporizador = setTimeout(() => { aviso.hidden = true; }, 3500);
	}

	cargar();
	altaTitulo.focus();
})();
