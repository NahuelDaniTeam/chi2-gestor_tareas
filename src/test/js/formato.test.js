import { test } from "node:test";
import assert from "node:assert/strict";

import {
	aDatetimeLocal, agrupar, desdeDatetimeLocal, diasDeSemana, dondeQuedo, etiquetaFecha, lunesDe, resumen, rutaDeTarea,
	rutaParaAbrir, semanasDelMes, separarFinalizadas
} from "../../main/resources/static/js/formato.js";

const HOY = "2026-11-19"; // jueves
const EN_HOY = { vista: "HOY", fechaVista: HOY, hoy: HOY };
const EN_SEMANA = { vista: "SEMANA", fechaVista: "2026-11-16", hoy: HOY };
const EN_MES = { vista: "MES", fechaVista: "2026-11-01", hoy: HOY };

const tarea = (horizonte, fechaReferencia, extra = {}) => ({ horizonte, fechaReferencia, ...extra });

test("calendario: lunes de una fecha, días de la semana y semanas de un mes", () => {
	assert.equal(lunesDe("2026-11-19"), "2026-11-16");
	assert.equal(lunesDe("2026-11-16"), "2026-11-16");
	assert.equal(lunesDe("2026-11-22"), "2026-11-16");
	assert.deepEqual(diasDeSemana("2026-11-30"), ["2026-11-30", "2026-12-01", "2026-12-02", "2026-12-03", "2026-12-04", "2026-12-05", "2026-12-06"]);

	const noviembre = semanasDelMes("2026-11-01");
	assert.equal(noviembre.length, 6, "noviembre 2026 empieza domingo: 6 semanas");
	assert.equal(noviembre[0][0], "2026-10-26");
	assert.equal(noviembre.at(-1)[0], "2026-11-30");
	assert.equal(semanasDelMes("2027-02-01").length, 4, "febrero 2027 empieza lunes y tiene 28 días");
});

test("no repite la fecha que ya dice la vista", () => {
	assert.equal(etiquetaFecha(tarea("DIA", HOY), EN_HOY), null);
	assert.equal(etiquetaFecha(tarea("SEMANA", "2026-11-16"), EN_SEMANA), null);
	assert.equal(etiquetaFecha(tarea("MES", "2026-11-01"), EN_MES), null);
	assert.equal(etiquetaFecha(tarea("DIA", "2026-11-17"), { ...EN_SEMANA, mostrarDia: false }), null);
	assert.equal(etiquetaFecha(tarea("SIN_PLAZO", null), EN_HOY), null);
});

test("días relativos y fechas", () => {
	assert.equal(etiquetaFecha(tarea("DIA", "2026-11-17"), EN_HOY), "Mar 17 nov");
	assert.equal(etiquetaFecha(tarea("DIA", "2026-11-20"), EN_SEMANA), "Mañana");
	assert.equal(etiquetaFecha(tarea("SEMANA", "2026-11-09"), EN_HOY), "Semana del 9 nov");
	assert.equal(etiquetaFecha(tarea("MES", "2026-10-01"), EN_HOY), "Octubre de 2026");
});

test("la fecha límite siempre avisa que vence", () => {
	assert.equal(etiquetaFecha(tarea("LIMITE", HOY), EN_HOY), "Vence hoy");
	assert.equal(etiquetaFecha(tarea("LIMITE", "2026-11-20"), EN_HOY), "Vence mañana");
	assert.equal(etiquetaFecha(tarea("LIMITE", "2026-12-20"), EN_MES), "Vence el 20 dic");
	assert.equal(etiquetaFecha(tarea("LIMITE", "2027-03-01"), EN_MES), "Vence el 1 mar de 2027");
	assert.equal(etiquetaFecha(tarea("LIMITE", "2026-11-24"), { ...EN_SEMANA, mostrarDia: false }), "Vence");
});

test("la semana se agrupa en 'durante la semana' y cada día, en orden", () => {
	const grupos = agrupar([
		tarea("DIA", "2026-11-20", { id: "vie" }),
		tarea("SEMANA", "2026-11-16", { id: "sem" }),
		tarea("LIMITE", "2026-11-17", { id: "mar" }),
		tarea("DIA", "2026-11-20", { id: "vie2" })
	], "SEMANA");
	assert.deepEqual(grupos.map(g => g.titulo), ["Durante la semana", "Martes 17", "Viernes 20"]);
	assert.deepEqual(grupos[2].tareas.map(t => t.id), ["vie", "vie2"]);
});

test("el mes se agrupa en 'durante el mes' y por semana; el largo plazo, por mes", () => {
	const mes = agrupar([
		tarea("DIA", "2026-11-25"), tarea("MES", "2026-11-01"), tarea("SEMANA", "2026-11-16"), tarea("DIA", "2026-11-17")
	], "MES");
	assert.deepEqual(mes.map(g => [g.titulo, g.tareas.length]),
		[["Durante el mes", 1], ["Semana del 16 nov", 2], ["Semana del 23 nov", 1]]);

	const largo = agrupar([tarea("MES", "2027-01-01"), tarea("LIMITE", "2026-12-20")], "LARGO_PLAZO");
	assert.deepEqual(largo.map(g => g.titulo), ["Diciembre de 2026", "Enero de 2027"]);

	assert.deepEqual(agrupar([tarea("SIN_PLAZO", null)], "POR_PLANIFICAR").map(g => g.titulo), [null]);
});

test("el resumen cuenta pendientes, vencidas y finalizadas", () => {
	assert.equal(resumen([
		{ estado: "PENDIENTE", vencida: true }, { estado: "EN_CURSO", vencida: false }, { estado: "FINALIZADA", vencida: false }
	]), "2 pendientes · 1 vencida · 1 finalizada");
	assert.equal(resumen([]), "");
});

test("separa las finalizadas sin cambiar el orden de la API", () => {
	const a = { id: "a", estado: "PENDIENTE" };
	const b = { id: "b", estado: "FINALIZADA" };
	const c = { id: "c", estado: "EN_CURSO" };
	assert.deepEqual(separarFinalizadas([a, b, c]), { abiertas: [a, c], finalizadas: [b] });
});

test("una tarea creada en otra vista sabe dónde quedó y cómo llegar", () => {
	assert.deepEqual([rutaDeTarea(tarea("SIN_PLAZO", null), HOY), dondeQuedo(tarea("SIN_PLAZO", null), HOY)],
		["/por-planificar", "Por planificar"]);
	assert.deepEqual([rutaDeTarea(tarea("SEMANA", "2026-11-16"), HOY), dondeQuedo(tarea("SEMANA", "2026-11-16"), HOY)],
		["/semana?fecha=2026-11-16", "la semana del 16 nov"]);
	assert.deepEqual([rutaDeTarea(tarea("MES", "2026-12-01"), HOY), dondeQuedo(tarea("MES", "2026-12-01"), HOY)],
		["/mes?fecha=2026-12-01", "Diciembre de 2026"]);
	assert.deepEqual([rutaDeTarea(tarea("DIA", "2026-11-24"), HOY), dondeQuedo(tarea("DIA", "2026-11-24"), HOY)],
		["/semana?fecha=2026-11-24", "Mar 24 nov"]);
	assert.equal(rutaDeTarea(tarea("LIMITE", HOY), HOY), "/");
});

test("la ruta para abrir una tarea suma ?tarea= o &tarea= según haga falta", () => {
	assert.equal(rutaParaAbrir({ id: "x", horizonte: "DIA", fechaReferencia: HOY }, HOY), "/?tarea=x");
	assert.equal(rutaParaAbrir({ id: "x", horizonte: "SEMANA", fechaReferencia: "2026-11-16" }, HOY), "/semana?fecha=2026-11-16&tarea=x");
});

test("el recordatorio va y vuelve del input datetime-local sin perder la hora", () => {
	const instante = "2026-11-19T16:30:00.000Z";
	assert.equal(desdeDatetimeLocal(aDatetimeLocal(instante)), instante);
	assert.match(aDatetimeLocal(instante), /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/);
	assert.equal(aDatetimeLocal(null), "");
	assert.equal(desdeDatetimeLocal(""), null);
});
