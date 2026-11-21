package com.chi2.tareas.tarea;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Página de la papelera (§4.8). */
@WebMvcTest(PapeleraPaginaController.class)
@Import(WebMvcTestConfig.class)
class PapeleraPaginaControllerTest {

	@Autowired
	MockMvc mvc;

	@Test
	void muestraLaPapeleraConLosDiasDeRetencion() throws Exception {
		mvc.perform(get("/papelera"))
				.andExpect(status().isOk())
				.andExpect(view().name("papelera"))
				.andExpect(content().string(containsString("Se pueden restaurar durante <span>30</span> días")))
				.andExpect(content().string(containsString("Versión <span>0.")));
	}

}
