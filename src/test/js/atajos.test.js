import { test } from "node:test";
import assert from "node:assert/strict";

import { interpretar } from "../../main/resources/static/js/atajos.js";

const BASE = Object.freeze({ prioridad: "MEDIA", horizonte: "DIA" });

test("sin atajos devuelve el texto y los valores elegidos", () => {
	assert.deepEqual(interpretar("  Llamar al director  ", BASE),
		{ titulo: "Llamar al director", prioridad: "MEDIA", horizonte: "DIA", conAtajos: false });
});

test("los atajos cambian prioridad y horizonte y salen del título", () => {
	assert.deepEqual(interpretar("Entregar capítulo 2 !a @s", BASE),
		{ titulo: "Entregar capítulo 2", prioridad: "ALTA", horizonte: "SEMANA", conAtajos: true });
});

test("funcionan en cualquier posición, en palabra completa y sin importar mayúsculas", () => {
	const r = interpretar("!B Comprar @MES yerba", BASE);
	assert.equal(r.titulo, "Comprar yerba");
	assert.equal(r.prioridad, "BAJA");
	assert.equal(r.horizonte, "MES");
});

test("acepta las formas largas", () => {
	const r = interpretar("Tesis @largo !alta", BASE);
	assert.equal(r.horizonte, "LARGO_PLAZO");
	assert.equal(r.prioridad, "ALTA");
	assert.equal(interpretar("Algo @día", BASE).horizonte, "DIA");
});

test("un atajo que no existe o pegado a una palabra queda como texto", () => {
	assert.deepEqual(interpretar("Escribir a hola@mes.com !urgente", BASE),
		{ titulo: "Escribir a hola@mes.com !urgente", prioridad: "MEDIA", horizonte: "DIA", conAtajos: false });
});

test("si se repite, gana el último", () => {
	assert.equal(interpretar("Algo !a !b", BASE).prioridad, "BAJA");
});

test("solo atajos deja el título vacío", () => {
	assert.equal(interpretar("!a @s", BASE).titulo, "");
});
