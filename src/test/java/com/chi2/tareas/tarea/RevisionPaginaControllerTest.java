package com.chi2.tareas.tarea;

import com.chi2.tareas.WebMvcTestConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revisión semanal (§4.12). El reloj está fijo en el jueves 19/11/2026. */
@WebMvcTest(RevisionPaginaController.class)
@Import(WebMvcTestConfig.class)
class RevisionPaginaControllerTest {

	@Autowired
	MockMvc mvc;

	@Test
	void muestraLosTresPasosYLaSemanaQueViene() throws Exception {
		mvc.perform(get("/revision"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("urlProximaSemana", "/semana?fecha=2026-11-23"))
				.andExpect(content().string(allOf(
						containsString("Lo que quedó pendiente"),
						containsString("Por planificar"),
						containsString("Del 23 al 29 de noviembre"),
						containsString("data-manana=\"2026-11-20\""))));
	}

}
