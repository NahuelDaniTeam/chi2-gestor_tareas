package com.chi2.tareas.estadistica;

import com.chi2.tareas.WebMvcTestConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EstadisticasPaginaController.class)
@Import(WebMvcTestConfig.class)
class EstadisticasPaginaControllerTest {

	@Autowired
	MockMvc mvc;

	@Test
	void muestraLaPagina() throws Exception {
		mvc.perform(get("/estadisticas"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Finalizadas por semana")));
	}

}
