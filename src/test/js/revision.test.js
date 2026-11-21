import { test } from "node:test";
import assert from "node:assert/strict";

import { convieneRevisar } from "../../main/resources/static/js/revision-aviso.js";

const LUNES = new Date(2026, 10, 23, 10, 0);
const JUEVES = new Date(2026, 10, 19, 10, 0);

test("sin nada para revisar no se sugiere", () => {
	assert.equal(convieneRevisar(null, JUEVES, 0), false);
});

test("si nunca se hizo, o hace una semana o más, se sugiere", () => {
	assert.equal(convieneRevisar(null, JUEVES, 2), true);
	assert.equal(convieneRevisar(new Date(2026, 10, 12, 9, 0).toISOString(), JUEVES, 2), true);
	assert.equal(convieneRevisar(new Date(2026, 10, 17, 9, 0).toISOString(), JUEVES, 2), false);
});

test("los lunes se sugiere salvo que ya se haya hecho ese día", () => {
	assert.equal(convieneRevisar(new Date(2026, 10, 20, 9, 0).toISOString(), LUNES, 1), true);
	assert.equal(convieneRevisar(new Date(2026, 10, 23, 8, 0).toISOString(), LUNES, 1), false);
});
