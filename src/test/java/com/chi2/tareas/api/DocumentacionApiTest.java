package com.chi2.tareas.api;

import com.chi2.tareas.TestcontainersConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La documentación OpenAPI se genera y cubre la API (no las páginas). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DocumentacionApiTest {

	@Autowired
	MockMvc mvc;

	@Test
	void publicaLosEndpointsDeLaApi() throws Exception {
		mvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("CHI2 · Gestor de tareas"))
				.andExpect(jsonPath("$.paths['/api/tareas'].get").exists())
				.andExpect(jsonPath("$.paths['/api/tareas/{id}/plazo'].patch").exists())
				.andExpect(jsonPath("$.paths['/api/proyectos'].post").exists())
				.andExpect(jsonPath("$.paths['/api/estadisticas'].get").exists())
				.andExpect(jsonPath("$.paths['/semana']").doesNotExist());
	}

	@Test
	void laVersionSaleDelBuild() throws Exception {
		mvc.perform(get("/actuator/info"))
				.andExpect(jsonPath("$.app.version").value(startsWith("0.")));
	}

}
