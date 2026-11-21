/** Ayudas compartidas por los tests de punta a punta. */

/** Un título que no choca con el de otro test (comparten la base). */
export function unico(texto) {
	return `${texto} ${Math.random().toString(36).slice(2, 7)}`;
}

/** Hoy en la zona de la app (la misma timezoneId que usa Playwright). */
export function hoy() {
	return new Date().toLocaleDateString("sv", { timeZone: "America/Argentina/Cordoba" });
}

export function sumarDias(iso, dias) {
	const [a, m, d] = iso.split("-").map(Number);
	const fecha = new Date(Date.UTC(a, m - 1, d + dias));
	return fecha.toISOString().slice(0, 10);
}

/** Crea una tarea por la API (más rápido que por la pantalla cuando no es lo que se prueba). */
export async function crearTarea(request, datos) {
	const respuesta = await request.post("/api/tareas", { data: { horizonte: "DIA", fechaReferencia: hoy(), ...datos } });
	if (!respuesta.ok()) throw new Error(`No se pudo crear la tarea: ${respuesta.status()} ${await respuesta.text()}`);
	return respuesta.json();
}

export async function obtenerTarea(request, id) {
	return (await request.get(`/api/tareas/${id}`)).json();
}

/** Espera a que la vista termine de cargar (sale el esqueleto). */
export async function esperarVista(page) {
	await page.locator("#cargando").waitFor({ state: "hidden" });
}

/**
 * Arrastra como una persona: aprieta, se mueve en pasos y suelta. El dragTo de Playwright salta de un punto al otro
 * y Chrome a veces no llega a iniciar el arrastre nativo con elementos chicos.
 */
export async function arrastrar(page, origen, destino) {
	await origen.scrollIntoViewIfNeeded();
	const o = await origen.boundingBox();
	const d = await destino.boundingBox();
	await page.mouse.move(o.x + o.width / 2, o.y + o.height / 2);
	await page.mouse.down();
	await page.mouse.move(o.x + o.width / 2 + 20, o.y + o.height / 2 + 5, { steps: 5 });
	await page.mouse.move(d.x + d.width / 2, d.y + Math.min(40, d.height / 2), { steps: 10 });
	await page.mouse.up();
}
