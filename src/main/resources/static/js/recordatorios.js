/**
 * Recordatorios (§4.10): mientras haya alguna página de la app abierta, cada 30 segundos se fija si llegó
 * la hora de alguno. Avisa con una notificación del sistema (si se dio permiso) o con un aviso en la página.
 */

import { api } from "./api.js";
import { avisar } from "./dom.js";
import { dondeQuedo, isoLocal, rutaParaAbrir } from "./formato.js";

const CADA = 30_000;

async function revisar() {
	let llegados;
	try {
		llegados = await api.recordatorios();
	} catch {
		return; // sin conexión: se reintenta en la próxima vuelta
	}
	for (const tarea of llegados) {
		mostrar(tarea);
		try {
			await api.recordatorioAvisado(tarea.id);
		} catch {
			// si no se pudo marcar, vuelve a avisar en la próxima vuelta
		}
	}
}

function mostrar(tarea) {
	const hoy = isoLocal();
	const ruta = rutaParaAbrir(tarea, hoy);
	if ("Notification" in window && Notification.permission === "granted") {
		const notificacion = new Notification(tarea.titulo, {
			body: "Recordatorio · " + dondeQuedo(tarea, hoy),
			icon: "/icono-512.png",
			tag: tarea.id
		});
		notificacion.onclick = () => {
			window.focus();
			location.href = ruta;
		};
	}
	avisar(`Recordatorio: ${tarea.titulo}`, { enlace: { texto: "Ver", href: ruta } });
}

setInterval(revisar, CADA);
document.addEventListener("visibilitychange", () => { if (document.visibilityState === "visible") revisar(); });
revisar();
