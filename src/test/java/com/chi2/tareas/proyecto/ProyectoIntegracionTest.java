package com.chi2.tareas.proyecto;

import com.chi2.tareas.TestcontainersConfiguration;
import com.chi2.tareas.tarea.TareaRepository;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Proyectos (§4.6) contra PostgreSQL real: alta, nombres únicos, tareas con proyecto, filtro y eliminación. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProyectoIntegracionTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	TareaRepository tareas;

	@Autowired
	ProyectoRepository proyectos;

	@BeforeEach
	void limpiar() {
		tareas.deleteAll();
		proyectos.deleteAll();
	}

	@Test
	void creaProyectosConColorRotativoYOrdenados() throws Exception {
		crear("Tesis");
		crear("Facultad");

		mvc.perform(get("/api/proyectos"))
				.andExpect(jsonPath("$[*].nombre", contains("Facultad", "Tesis")))
				.andExpect(jsonPath("$[*].color", contains("OCRE", "TERRACOTA")))
				.andExpect(jsonPath("$[1].hex").value("#b5532c"));
	}

	@Test
	void noPermiteDosConElMismoNombre() throws Exception {
		crear("Tesis");

		mvc.perform(post("/api/proyectos").contentType(MediaType.APPLICATION_JSON).content("{ \"nombre\": \"  tesis \" }"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Ya tenés un proyecto llamado \"Tesis\"."));
		mvc.perform(post("/api/proyectos").contentType(MediaType.APPLICATION_JSON).content("{ \"nombre\": \"\" }"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void lasTareasSeFiltranPorProyectoYQuedanSinProyectoAlEliminarlo() throws Exception {
		String tesis = crear("Tesis");
		crearTarea("Escribir introducción", tesis);
		crearTarea("Comprar yerba", null);

		mvc.perform(get("/api/tareas/sin-plazo").param("proyecto", tesis))
				.andExpect(jsonPath("$[*].titulo", contains("Escribir introducción")))
				.andExpect(jsonPath("$[0].proyectoId").value(tesis));

		mvc.perform(delete("/api/proyectos/{id}", tesis)).andExpect(status().isNoContent());
		mvc.perform(get("/api/tareas/sin-plazo"))
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].proyectoId").value(nullValue()));
	}

	@Test
	void renombrarYCambiarColor() throws Exception {
		String id = crear("Tesis");

		mvc.perform(put("/api/proyectos/{id}", id).contentType(MediaType.APPLICATION_JSON)
						.content("{ \"nombre\": \"Tesis final\", \"color\": \"PINO\" }"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Tesis final"))
				.andExpect(jsonPath("$.hex").value("#3e7462"));
	}

	@Test
	void unaTareaNoPuedeUsarUnProyectoQueNoExiste() throws Exception {
		mvc.perform(post("/api/tareas").contentType(MediaType.APPLICATION_JSON).content("""
						{ "titulo": "x", "horizonte": "SIN_PLAZO", "proyectoId": "7d0f7a0e-5a3b-4f43-9a52-2a2f3d1c0b11" }
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("El proyecto elegido no existe."));
	}

	private String crear(String nombre) throws Exception {
		String cuerpo = mvc.perform(post("/api/proyectos").contentType(MediaType.APPLICATION_JSON)
						.content("{ \"nombre\": \"" + nombre + "\" }"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(cuerpo, "$.id");
	}

	private void crearTarea(String titulo, String proyectoId) throws Exception {
		String proyecto = proyectoId == null ? "null" : "\"" + proyectoId + "\"";
		mvc.perform(post("/api/tareas").contentType(MediaType.APPLICATION_JSON).content("""
						{ "titulo": "%s", "horizonte": "SIN_PLAZO", "proyectoId": %s }
						""".formatted(titulo, proyecto)))
				.andExpect(status().isCreated());
	}

}
