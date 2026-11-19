package com.chi2.tareas.tarea;

import com.chi2.tareas.WebMvcTestConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Páginas de las vistas (§3 y §4.3). El reloj está fijo en el jueves 19/11/2026. */
@WebMvcTest(TareaPaginaController.class)
@Import(WebMvcTestConfig.class)
class TareaPaginaControllerTest {

	@Autowired
	MockMvc mvc;

	@Test
	void alAbrirSeVeHoyConElAltaRapidaEnfocada() throws Exception {
		mvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("tareas"))
				.andExpect(model().attribute("vista", Vista.HOY))
				.andExpect(model().attribute("urlLista", "/api/tareas/hoy"))
				.andExpect(content().string(allOf(
						containsString("Jueves 19 de noviembre"),
						containsString("id=\"alta-titulo\""),
						containsString("autofocus"),
						not(containsString("Período anterior")))));
	}

	@Test
	void laSemanaMuestraElPeriodoYNavega() throws Exception {
		mvc.perform(get("/semana").param("fecha", "2026-11-26"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("urlLista", "/api/tareas?horizonte=SEMANA&fecha=2026-11-23"))
				.andExpect(model().attribute("urlAnterior", "/semana?fecha=2026-11-16"))
				.andExpect(model().attribute("urlSiguiente", "/semana?fecha=2026-11-30"))
				.andExpect(model().attribute("urlActual", "/semana"))
				.andExpect(content().string(containsString("Del 23 al 29 de noviembre")));
	}

	@Test
	void sinFechaLaSemanaEsLaActualYNoOfreceVolver() throws Exception {
		mvc.perform(get("/semana"))
				.andExpect(model().attribute("urlLista", "/api/tareas?horizonte=SEMANA&fecha=2026-11-16"))
				.andExpect(model().attribute("urlActual", (Object) null));
	}

	@Test
	void elMesMuestraSuNombre() throws Exception {
		mvc.perform(get("/mes").param("fecha", "2026-12-05"))
				.andExpect(model().attribute("urlLista", "/api/tareas?horizonte=MES&fecha=2026-12-01"))
				.andExpect(content().string(containsString("Diciembre 2026")));
	}

	@Test
	void largoPlazoNoTieneNavegacion() throws Exception {
		mvc.perform(get("/largo-plazo"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("urlLista", "/api/tareas?horizonte=LARGO_PLAZO"))
				.andExpect(model().attribute("urlAnterior", (Object) null))
				.andExpect(content().string(containsString("Sin fecha fija")));
	}

}
