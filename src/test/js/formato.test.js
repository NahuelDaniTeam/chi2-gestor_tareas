import { test } from "node:test";
import assert from "node:assert/strict";

import { etiquetaFecha, resumen, separarFinalizadas } from "../../main/resources/static/js/formato.js";

const HOY = { horizonteVista: "DIA", fechaVista: "2026-11-19", hoy: "2026-11-19" };
const SEMANA = { horizonteVista: "SEMANA", fechaVista: "2026-11-16", hoy: "2026-11-19" };
const LARGO = { horizonteVista: "LARGO_PLAZO", fechaVista: null, hoy: "2026-11-19" };

const tarea = (horizonte, fechaReferencia) => ({ horizonte, fechaReferencia });

test("no repite la fecha que ya dice la vista", () => {
	assert.equal(etiquetaFecha(tarea("DIA", "2026-11-19"), HOY), null);
	assert.equal(etiquetaFecha(tarea("SEMANA", "2026-11-16"), SEMANA), null);
	assert.equal(etiquetaFecha(tarea("LARGO_PLAZO", null), LARGO), null);
});

test("una vencida en Hoy muestra su día", () => {
	assert.equal(etiquetaFecha(tarea("DIA", "2026-11-17"), HOY), "Mar 17 nov");
});

test("tareas de otros horizontes muestran su período", () => {
	assert.equal(etiquetaFecha(tarea("SEMANA", "2026-11-09"), HOY), "Semana del 9 nov");
	assert.equal(etiquetaFecha(tarea("MES", "2026-10-01"), HOY), "Octubre de 2026");
});

test("el largo plazo muestra el vencimiento, con el año solo si no es el actual", () => {
	assert.equal(etiquetaFecha(tarea("LARGO_PLAZO", "2026-12-20"), LARGO), "Vence el 20 dic");
	assert.equal(etiquetaFecha(tarea("LARGO_PLAZO", "2027-03-01"), LARGO), "Vence el 1 mar de 2027");
});

test("el resumen cuenta pendientes, vencidas y finalizadas", () => {
	const tareas = [
		{ estado: "PENDIENTE", vencida: true },
		{ estado: "EN_CURSO", vencida: false },
		{ estado: "FINALIZADA", vencida: false }
	];
	assert.equal(resumen(tareas), "2 pendientes · 1 vencida · 1 finalizada");
	assert.equal(resumen([]), "");
	assert.equal(resumen([{ estado: "FINALIZADA" }, { estado: "FINALIZADA" }]), "2 finalizadas");
});

test("separa las finalizadas sin cambiar el orden de la API", () => {
	const a = { id: "a", estado: "PENDIENTE" };
	const b = { id: "b", estado: "FINALIZADA" };
	const c = { id: "c", estado: "EN_CURSO" };
	assert.deepEqual(separarFinalizadas([a, b, c]), { abiertas: [a, c], finalizadas: [b] });
});
