import { expect, test } from "@playwright/test";

import { esperarVista, unico } from "./ayuda.js";

// Vista de celular (proyecto "celular" de playwright.config.js)
const PAGINAS = ["/", "/semana", "/mes", "/por-planificar", "/revision", "/estadisticas", "/papelera"];

for (const ruta of PAGINAS) {
	test(`${ruta} entra en el ancho del celular`, async ({ page }) => {
		await page.goto(ruta);
		await page.waitForLoadState("networkidle");
		const sobra = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
		expect(sobra, "no tiene que haber scroll horizontal").toBeLessThanOrEqual(1);
	});
}

test("en el celular Semana arranca en lista y se puede cargar una tarea", async ({ page }) => {
	await page.goto("/semana");
	await esperarVista(page);
	await expect(page.getByRole("button", { name: /Lista/ })).toHaveAttribute("aria-pressed", "true");

	const titulo = unico("Desde el celular");
	await page.getByRole("textbox", { name: "Nueva tarea" }).fill(titulo);
	await page.keyboard.press("Enter");
	await expect(page.locator("#lista li", { hasText: titulo })).toBeVisible();
});
