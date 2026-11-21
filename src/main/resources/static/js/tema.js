/** Tema claro u oscuro (§4.13): "sistema" sigue la preferencia del sistema operativo. Se recuerda en este navegador. */

import { guardarPreferencia, leerPreferencia } from "./dom.js";

const CLAVE = "chi2-tareas-tema";
const botones = document.querySelectorAll("[data-tema]");

function aplicar(tema) {
	if (tema === "claro" || tema === "oscuro") document.documentElement.dataset.tema = tema;
	else delete document.documentElement.dataset.tema;
	botones.forEach(b => b.setAttribute("aria-pressed", String(b.dataset.tema === tema)));
	const oscuro = tema === "oscuro" || (tema === "sistema" && matchMedia("(prefers-color-scheme: dark)").matches);
	document.querySelector('meta[name="theme-color"]')?.setAttribute("content", oscuro ? "#161412" : "#f7f4ee");
}

botones.forEach(b => b.addEventListener("click", () => {
	guardarPreferencia(CLAVE, b.dataset.tema);
	aplicar(b.dataset.tema);
}));

aplicar(leerPreferencia(CLAVE, "sistema"));
