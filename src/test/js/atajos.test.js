import { test } from "node:test";
import assert from "node:assert/strict";

import { buscarPorNombre, interpretar, normalizar } from "../../main/resources/static/js/atajos.js";

// Jueves 19/11/2026, en la vista Hoy
const BASE = Object.freeze({ prioridad: "MEDIA", horizonte: "DIA", fecha: "2026-11-19", hoy: "2026-11-19" });

test("sin atajos devuelve el texto y los valores elegidos", () => {
	assert.deepEqual(interpretar("  Llamar al director  ", BASE),
		{ titulo: "Llamar al director", prioridad: "MEDIA", horizonte: "DIA", fecha: "2026-11-19", proyecto: null, conAtajos: false });
});

test("prioridad y plazo salen del título", () => {
	assert.deepEqual(interpretar("Entregar capítulo 2 !a @s", BASE),
		{ titulo: "Entregar capítulo 2", prioridad: "ALTA", horizonte: "SEMANA", fecha: "2026-11-19", proyecto: null, conAtajos: true });
});

test("@p deja la tarea por planificar, sin fecha", () => {
	const r = interpretar("Aprender guitarra @p", BASE);
	assert.equal(r.horizonte, "SIN_PLAZO");
	assert.equal(r.fecha, null);
	assert.equal(interpretar("Algo @largo", BASE).horizonte, "SIN_PLAZO");
});

test("días concretos: mañana, pasado y días de la semana", () => {
	assert.deepEqual([interpretar("x @mañana", BASE).fecha, interpretar("x @manana", BASE).fecha], ["2026-11-20", "2026-11-20"]);
	assert.equal(interpretar("x @pasado", BASE).fecha, "2026-11-21");
	assert.equal(interpretar("x @lunes", BASE).fecha, "2026-11-23");
	assert.equal(interpretar("x @vie", BASE).fecha, "2026-11-20");
	assert.equal(interpretar("x @jueves", BASE).fecha, "2026-11-19", "el mismo día de la semana es hoy");
	assert.equal(interpretar("x @sábado", BASE).horizonte, "DIA");
});

test("en la vista de un período, @s o @m toman ese período", () => {
	const semanaQueViene = { ...BASE, horizonte: "SEMANA", fecha: "2026-11-23" };
	assert.equal(interpretar("x @s", semanaQueViene).fecha, "2026-11-23");
	assert.equal(interpretar("x @m", semanaQueViene).fecha, "2026-11-19");
});

test("funcionan en cualquier posición y sin importar mayúsculas", () => {
	const r = interpretar("!B Comprar @MES yerba", BASE);
	assert.equal(r.titulo, "Comprar yerba");
	assert.equal(r.prioridad, "BAJA");
	assert.equal(r.horizonte, "MES");
});

test("un atajo que no existe o pegado a una palabra queda como texto", () => {
	assert.equal(interpretar("Escribir a hola@mes.com !urgente", BASE).titulo, "Escribir a hola@mes.com !urgente");
	assert.equal(interpretar("Escribir a hola@mes.com !urgente", BASE).conAtajos, false);
});

test("#proyecto sale del título; guion bajo es espacio", () => {
	const r = interpretar("Escribir introducción #Redes_de_Datos !a", BASE);
	assert.equal(r.titulo, "Escribir introducción");
	assert.equal(r.proyecto, "Redes de Datos");
	assert.equal(r.prioridad, "ALTA");
	assert.equal(interpretar("Tema #2 del parcial", BASE).proyecto, "2");
	assert.equal(interpretar("Sin proyecto", BASE).proyecto, null);
	assert.equal(interpretar("Precio#1", BASE).titulo, "Precio#1", "pegado a una palabra no es atajo");
});

test("los proyectos se buscan sin importar tildes, mayúsculas ni separadores", () => {
	const proyectos = [{ id: "1", nombre: "Tesis" }, { id: "2", nombre: "Redes de Datos" }, { id: "3", nombre: "Facultad" }];
	assert.equal(buscarPorNombre(proyectos, "tesis").id, "1");
	assert.equal(buscarPorNombre(proyectos, "redes de datos").id, "2");
	assert.equal(buscarPorNombre(proyectos, "REDES-DATOS"), undefined);
	assert.equal(buscarPorNombre(proyectos, "facultád").id, "3");
	assert.equal(normalizar("  Árbol_de-Ideas "), "arboldeideas");
});

test("si se repite, gana el último", () => {
	assert.equal(interpretar("Algo !a !b", BASE).prioridad, "BAJA");
});
