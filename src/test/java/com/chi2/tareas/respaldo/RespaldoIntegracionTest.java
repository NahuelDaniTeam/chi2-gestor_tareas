package com.chi2.tareas.respaldo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.chi2.tareas.TestcontainersConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exportar y respaldo automático (§4.15) contra PostgreSQL real. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RespaldoIntegracionTest {

	@TempDir
	static Path carpeta;

	@DynamicPropertySource
	static void carpetaTemporal(DynamicPropertyRegistry registry) {
		registry.add("chi2.respaldos.carpeta", () -> carpeta.toString());
		registry.add("chi2.respaldos.conservar", () -> "2");
	}

	@Autowired
	MockMvc mvc;

	@Autowired
	RespaldoAutomatico automatico;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void exportaTodoEnJsonYLasTareasEnCsv() throws Exception {
		jdbc.update("delete from tarea");
		mvc.perform(post("/api/tareas").contentType(MediaType.APPLICATION_JSON).content("""
						{ "titulo": "Entregar TP; versión \\"final\\"", "horizonte": "SIN_PLAZO", "prioridad": "ALTA" }
						"""))
				.andExpect(status().isCreated());

		mvc.perform(get("/api/exportar/json"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", startsWith("attachment; filename=\"tareas-")))
				.andExpect(jsonPath("$.tareas[0].titulo").value("Entregar TP; versión \"final\""))
				.andExpect(jsonPath("$.version").exists());

		mvc.perform(get("/api/exportar/csv"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", startsWith("text/csv")))
				.andExpect(content().string(containsString("Título;Descripción;Plazo")))
				.andExpect(content().string(containsString("\"Entregar TP; versión \"\"final\"\"\";;Sin plazo;;Alta;Pendiente")));
	}

	@Test
	void elRespaldoAutomaticoGuardaUnoPorDiaYConservaLosUltimos() throws IOException {
		Files.writeString(carpeta.resolve("tareas-2020-01-01.json"), "{}");
		Files.writeString(carpeta.resolve("tareas-2020-01-02.json"), "{}");
		Files.writeString(carpeta.resolve("notas.txt"), "no se toca");

		Path hoy = automatico.respaldarHoy();
		automatico.respaldarHoy();

		assertThat(Files.readString(hoy)).contains("\"tareas\"");
		try (Stream<Path> archivos = Files.list(carpeta)) {
			assertThat(archivos.map(p -> p.getFileName().toString()).sorted())
					.containsExactly("notas.txt", "tareas-2020-01-02.json", hoy.getFileName().toString());
		}
	}

}
