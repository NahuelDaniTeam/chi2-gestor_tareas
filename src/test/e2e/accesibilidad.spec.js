import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";

import { crearTarea, unico } from "./ayuda.js";

// Auditoría automática con axe (WCAG 2.1 A y AA) en todas las páginas, en tema claro y oscuro.
// Falla con problemas graves o críticos; los menores se listan en el reporte sin cortar el build.
const PAGINAS = ["/", "/semana", "/mes", "/largo-plazo", "/por-planificar", "/papelera", "/revision", "/estadisticas"];

test.beforeAll(async ({ request }) => {
	// Con contenido, para que se auditen también las filas, etiquetas y fechas
	await crearTarea(request, { titulo: unico("Tarea de muestra"), prioridad: "ALTA" });
	await crearTarea(request, { titulo: unico("Sin plazo de muestra"), horizonte: "SIN_PLAZO", fechaReferencia: null });
});

for (const tema of ["light", "dark"]) {
	for (const ruta of PAGINAS) {
		test(`${ruta} en tema ${tema === "light" ? "claro" : "oscuro"}`, async ({ page }, info) => {
			await page.emulateMedia({ colorScheme: tema });
			await page.goto(ruta);
			await page.waitForLoadState("networkidle");

			const resultado = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
			const graves = resultado.violations.filter(v => v.impact === "serious" || v.impact === "critical");
			if (resultado.violations.length) {
				await info.attach("axe", { body: JSON.stringify(resultado.violations, null, 2), contentType: "application/json" });
			}
			expect(graves.map(v => `${v.id}: ${v.help} (${v.nodes.length}) → ${v.nodes[0]?.target}`)).toEqual([]);
		});
	}
}
