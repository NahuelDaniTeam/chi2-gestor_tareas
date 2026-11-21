import { expect, test } from "@playwright/test";

import { arrastrar, crearTarea, esperarVista, hoy, obtenerTarea, sumarDias, unico } from "./ayuda.js";

test.describe("alta rápida", () => {
	test("Enter guarda con los atajos y el campo queda listo para otra", async ({ page, request }) => {
		const titulo = unico("Comprar pan");
		await page.goto("/");
		await esperarVista(page);
		const campo = page.getByRole("textbox", { name: "Nueva tarea" });
		await expect(campo).toBeFocused();

		await campo.fill(`${titulo} !a`);
		await expect(page.locator("#alta-prioridad")).toHaveValue("ALTA");
		await campo.press("Enter");

		const fila = page.locator("#lista li", { hasText: titulo });
		await expect(fila).toBeVisible();
		await expect(fila).toContainText("Alta");
		await expect(campo).toHaveValue("");
		await expect(campo).toBeFocused();
		const [guardada] = await (await request.get(`/api/tareas/buscar?q=${encodeURIComponent(titulo)}`)).json();
		expect(guardada).toMatchObject({ titulo, prioridad: "ALTA", horizonte: "DIA", fechaReferencia: hoy() });
	});

	test("una tarea para otra vista avisa dónde quedó y lleva a verla", async ({ page }) => {
		const titulo = unico("Entregar TP");
		await page.goto("/");
		await esperarVista(page);
		await page.getByRole("textbox", { name: "Nueva tarea" }).fill(`${titulo} @s`);
		await page.keyboard.press("Enter");

		const aviso = page.getByRole("status");
		await expect(aviso).toContainText("Guardada en la semana");
		await aviso.getByRole("link", { name: "Ver" }).click();
		await expect(page).toHaveURL(/\/semana\?fecha=/);
		await expect(page.locator(".franja", { hasText: titulo })).toBeVisible();
	});
});

test.describe("tablero y calendario", () => {
	test("arrastrar una tarjeta a otra columna cambia el estado", async ({ page, request }) => {
		const tarea = await crearTarea(request, { titulo: unico("Leer paper") });
		await page.goto("/");
		await esperarVista(page);
		await page.getByRole("button", { name: /Tablero/ }).click();

		await arrastrar(page, page.locator(".tarjeta", { hasText: tarea.titulo }), page.locator('[data-soltar-estado="EN_CURSO"]'));

		await expect(page.locator('[data-soltar-estado="EN_CURSO"] .tarjeta', { hasText: tarea.titulo })).toBeVisible();
		await expect.poll(async () => (await obtenerTarea(request, tarea.id)).estado).toBe("EN_CURSO");
		await page.getByRole("button", { name: /Lista/ }).click(); // deja la preferencia como estaba
	});

	test("arrastrar en el calendario de la semana reprograma el día", async ({ page, request }) => {
		const lunes = sumarDias(hoy(), -((new Date(`${hoy()}T12:00:00`).getDay() + 6) % 7));
		const tarea = await crearTarea(request, { titulo: unico("Ordenar apuntes"), fechaReferencia: lunes });
		const miercoles = sumarDias(lunes, 2);
		await page.goto(`/semana?fecha=${lunes}`);
		await esperarVista(page);
		await page.getByRole("button", { name: /Calendario/ }).click();

		await arrastrar(page, page.locator(".pastilla", { hasText: tarea.titulo }),
			page.locator(`[data-soltar-plazo="DIA"][data-soltar-fecha="${miercoles}"]`));

		await expect.poll(async () => (await obtenerTarea(request, tarea.id)).fechaReferencia).toBe(miercoles);
	});
});

test.describe("panel de edición", () => {
	test("checklist, papelera y deshacer", async ({ page, request }) => {
		const tarea = await crearTarea(request, { titulo: unico("Preparar exposición") });
		await page.goto("/");
		await esperarVista(page);
		await page.locator("#lista li", { hasText: tarea.titulo }).getByRole("button", { name: tarea.titulo }).click();

		const panel = page.getByRole("dialog", { name: "Editar tarea" });
		await expect(panel).toBeVisible();
		await panel.getByPlaceholder("+ Agregar un paso y Enter").fill("Armar diapositivas");
		await page.keyboard.press("Enter");
		await expect(panel.locator("#p-pasos-progreso")).toHaveText("0 de 1");

		await panel.getByRole("button", { name: "Mandar a la papelera" }).click();
		await expect(panel).toBeHidden();
		await expect(page.locator("#lista li", { hasText: tarea.titulo })).toHaveCount(0);

		await page.getByRole("status").getByRole("button", { name: "Deshacer" }).click();
		await expect(page.locator("#lista li", { hasText: tarea.titulo })).toBeVisible();
		await expect(page.locator("#lista li", { hasText: tarea.titulo })).toContainText("0/1");
	});
});

test("el buscador encuentra sin tildes y abre la tarea", async ({ page, request }) => {
	const titulo = unico("Revisar capítulo");
	await crearTarea(request, { titulo, horizonte: "SIN_PLAZO", fechaReferencia: null });
	await page.goto("/");
	await esperarVista(page);

	await page.keyboard.press("Control+k");
	await page.getByRole("combobox", { name: "Buscar tareas o ir a una vista" }).fill(titulo.replace("capítulo", "capitulo"));
	await expect(page.getByRole("option", { name: new RegExp(titulo.split(" ").at(-1)) })).toBeVisible();
	await page.keyboard.press("Enter");

	await expect(page).toHaveURL(/\/por-planificar/);
	await expect(page.getByRole("dialog", { name: "Editar tarea" }).locator("#p-titulo")).toHaveValue(titulo);
});
