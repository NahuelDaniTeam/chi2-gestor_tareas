package com.chi2.tareas.tarea;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.chi2.tareas.RelojDeTest;
import com.chi2.tareas.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La API contra PostgreSQL real (Testcontainers), con el reloj fijo en el jueves 19/11/2026 al mediodía
 * de Córdoba. Cubre los flujos de §4 y que no se vean tareas de otro usuario.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, TareaIntegracionTest.Reloj.class })
class TareaIntegracionTest {

	static final Instant AHORA = Instant.parse("2026-11-19T15:00:00Z");

	@TestConfiguration(proxyBeanMethods = false)
	static class Reloj {

		@Bean
		@Primary
		RelojDeTest relojDeTest() {
			return new RelojDeTest(AHORA);
		}

	}

	@Autowired
	MockMvc mvc;

	@Autowired
	TareaRepository repository;

	@Autowired
	RelojDeTest reloj;

	@Autowired
	Clock clock;

	@BeforeEach
	void limpiar() {
		repository.deleteAll();
		reloj.fijar(AHORA);
	}

	@Test
	void elAltaRapidaUsaHoyYPrioridadMediaYApareceEnHoy() throws Exception {
		mvc.perform(post("/api/tareas").contentType(MediaType.APPLICATION_JSON).content("""
						{ "titulo": "Mandar mail al director", "horizonte": "DIA" }
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.fechaReferencia").value("2026-11-19"))
				.andExpect(jsonPath("$.prioridad").value("MEDIA"))
				.andExpect(jsonPath("$.estado").value("PENDIENTE"))
				.andExpect(jsonPath("$.ambito").value("PERSONAL"))
				.andExpect(jsonPath("$.creadaEn").value("2026-11-19T15:00:00Z"));

		mvc.perform(get("/api/tareas/hoy"))
				.andExpect(jsonPath("$[*].titulo", contains("Mandar mail al director")));
	}

	@Test
	void hoyMuestraLasDelDiaLasQueVencenYTodasLasVencidasOrdenadasPorPrioridad() throws Exception {
		crear("Hoy media", "DIA", "2026-11-19", "MEDIA");
		crear("Hoy alta", "DIA", "2026-11-19", "ALTA");
		crear("Ayer sin terminar", "DIA", "2026-11-18", "BAJA");
		String terminadaAyer = crear("Ayer terminada", "DIA", "2026-11-18", "ALTA");
		cambiarEstado(terminadaAyer, "FINALIZADA");
		crear("Semana pasada sin terminar", "SEMANA", "2026-11-10", "MEDIA");
		crear("Esta semana", "SEMANA", "2026-11-19", "ALTA");
		crear("Este mes", "MES", "2026-11-19", "ALTA");
		crear("Vence hoy", "LIMITE", "2026-11-19", "BAJA");
		crear("Sin plazo", "SIN_PLAZO", null, "ALTA");
		crear("Mañana", "DIA", "2026-11-20", "ALTA");

		mvc.perform(get("/api/tareas/hoy"))
				.andExpect(jsonPath("$[*].titulo", contains(
						"Hoy alta", "Semana pasada sin terminar", "Hoy media", "Ayer sin terminar", "Vence hoy")))
				.andExpect(jsonPath("$[*].vencida", contains(false, true, false, true, false)));
	}

	@Test
	void laSemanaIncluyeSusDiasYSuPlazoPeroNoElMes() throws Exception {
		crear("Semana baja", "SEMANA", "2026-11-17", "BAJA");
		crear("Martes", "DIA", "2026-11-17", "MEDIA");
		crear("Vence el sábado", "LIMITE", "2026-11-21", "ALTA");
		crear("Del mes", "MES", "2026-11-19", "ALTA");
		crear("Otra semana", "SEMANA", "2026-11-23", "ALTA");
		crear("Sin plazo", "SIN_PLAZO", null, "ALTA");

		mvc.perform(get("/api/tareas").param("desde", "2026-11-16").param("hasta", "2026-11-22"))
				.andExpect(jsonPath("$[*].titulo", contains("Vence el sábado", "Martes", "Semana baja")));
		mvc.perform(get("/api/tareas").param("desde", "2026-11-16").param("hasta", "2026-11-22")
						.param("prioridad", "BAJA"))
				.andExpect(jsonPath("$[*].titulo", contains("Semana baja")));
	}

	@Test
	void elMesIncluyeSusDiasSusSemanasYElMes() throws Exception {
		crear("Noviembre", "MES", "2026-11-28", "MEDIA");
		crear("Diciembre", "MES", "2026-12-02", "MEDIA");
		crear("Día 25", "DIA", "2026-11-25", "MEDIA");
		crear("Semana que cruza", "SEMANA", "2026-12-02", "MEDIA");
		crear("Semana de octubre", "SEMANA", "2026-10-28", "MEDIA");

		mvc.perform(get("/api/tareas").param("desde", "2026-11-01").param("hasta", "2026-11-30"))
				.andExpect(jsonPath("$[*].titulo", contains("Semana de octubre", "Noviembre", "Día 25", "Semana que cruza")));
	}

	@Test
	void largoPlazoTraeTodoDesdeElMesQueVieneYPorPlanificarLasSinPlazo() throws Exception {
		crear("Este mes", "MES", "2026-11-01", "MEDIA");
		crear("Diciembre", "MES", "2026-12-01", "MEDIA");
		crear("Vence en julio", "LIMITE", "2027-07-01", "MEDIA");
		crear("Aprender guitarra", "SIN_PLAZO", "2027-01-01", "MEDIA");

		mvc.perform(get("/api/tareas").param("desde", "2026-12-01"))
				.andExpect(jsonPath("$[*].titulo", contains("Diciembre", "Vence en julio")));
		mvc.perform(get("/api/tareas/sin-plazo"))
				.andExpect(jsonPath("$[*].titulo", contains("Aprender guitarra")))
				.andExpect(jsonPath("$[0].fechaReferencia").value(nullValue()));
	}

	@Test
	void planificarUnaTareaSinPlazoYReprogramarla() throws Exception {
		String id = crear("Leer paper", "SIN_PLAZO", null, "MEDIA");

		mvc.perform(patch("/api/tareas/{id}/plazo", id).contentType(MediaType.APPLICATION_JSON)
						.content("{ \"horizonte\": \"SEMANA\" }"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.horizonte").value("SEMANA"))
				.andExpect(jsonPath("$.fechaReferencia").value("2026-11-16"));
		mvc.perform(patch("/api/tareas/{id}/plazo", id).contentType(MediaType.APPLICATION_JSON)
						.content("{ \"horizonte\": \"DIA\", \"fechaReferencia\": \"2026-11-24\" }"))
				.andExpect(jsonPath("$.fechaReferencia").value("2026-11-24"));
		mvc.perform(get("/api/tareas/sin-plazo")).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void alArrastrarAFinalizadaSeRegistraCuandoYAlVolverSeBorra() throws Exception {
		String id = crear("Escribir introducción", "DIA", "2026-11-19", "ALTA");
		reloj.fijar(AHORA.plusSeconds(3600));

		cambiarEstado(id, "EN_CURSO")
				.andExpect(jsonPath("$.completadaEn").value(nullValue()));
		cambiarEstado(id, "FINALIZADA")
				.andExpect(jsonPath("$.estado").value("FINALIZADA"))
				.andExpect(jsonPath("$.completadaEn").value("2026-11-19T16:00:00Z"))
				.andExpect(jsonPath("$.actualizadaEn").value("2026-11-19T16:00:00Z"));
		cambiarEstado(id, "PENDIENTE")
				.andExpect(jsonPath("$.completadaEn").value(nullValue()));

		assertThat(repository.findById(UUID.fromString(id))).get()
				.extracting(Tarea::getEstado).isEqualTo(EstadoTarea.PENDIENTE);
	}

	@Test
	void editarCambiaTodoYLlevaLaFechaAlHorizonte() throws Exception {
		String id = crear("Leer paper", "DIA", "2026-11-19", "MEDIA");

		mvc.perform(put("/api/tareas/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
						{ "titulo": "Leer paper de Kahneman", "descripcion": "Capítulos 1 a 3",
						  "horizonte": "MES", "fechaReferencia": "2026-12-15", "prioridad": "ALTA" }
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titulo").value("Leer paper de Kahneman"))
				.andExpect(jsonPath("$.descripcion").value("Capítulos 1 a 3"))
				.andExpect(jsonPath("$.fechaReferencia").value("2026-12-01"));

		mvc.perform(put("/api/tareas/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
						{ "titulo": "Leer paper de Kahneman", "horizonte": "SIN_PLAZO", "fechaReferencia": "2026-12-15" }
						"""))
				.andExpect(jsonPath("$.fechaReferencia").value(nullValue()))
				.andExpect(jsonPath("$.descripcion").value(nullValue()));

		mvc.perform(patch("/api/tareas/{id}/prioridad", id).contentType(MediaType.APPLICATION_JSON)
						.content("{ \"prioridad\": \"BAJA\" }"))
				.andExpect(jsonPath("$.prioridad").value("BAJA"));
	}

	@Test
	void eliminar() throws Exception {
		String id = crear("Borrar", "DIA", "2026-11-19", "MEDIA");

		mvc.perform(delete("/api/tareas/{id}", id)).andExpect(status().isNoContent());
		mvc.perform(get("/api/tareas/{id}", id)).andExpect(status().isNotFound());
		assertThat(repository.count()).isZero();
	}

	@Test
	void lasTareasDeOtroUsuarioNoSeVenNiSeTocan() throws Exception {
		Tarea ajena = repository.save(new Tarea("Ajena", null, Horizonte.DIA, LocalDate.parse("2026-11-19"),
				Prioridad.ALTA, UUID.randomUUID(), clock.instant()));

		mvc.perform(get("/api/tareas/hoy")).andExpect(jsonPath("$.length()").value(0));
		mvc.perform(get("/api/tareas/{id}", ajena.getId())).andExpect(status().isNotFound());
		cambiarEstado(ajena.getId().toString(), "FINALIZADA").andExpect(status().isNotFound());
		mvc.perform(delete("/api/tareas/{id}", ajena.getId())).andExpect(status().isNotFound());

		assertThat(repository.count()).isEqualTo(1);
	}

	private String crear(String titulo, String horizonte, String fecha, String prioridad) throws Exception {
		String fechaJson = fecha == null ? "null" : "\"" + fecha + "\"";
		String cuerpo = mvc.perform(post("/api/tareas").contentType(MediaType.APPLICATION_JSON).content("""
						{ "titulo": "%s", "horizonte": "%s", "fechaReferencia": %s, "prioridad": "%s" }
						""".formatted(titulo, horizonte, fechaJson, prioridad)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(cuerpo, "$.id");
	}

	private org.springframework.test.web.servlet.ResultActions cambiarEstado(String id, String estado)
			throws Exception {
		return mvc.perform(patch("/api/tareas/{id}/estado", id).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"estado\": \"" + estado + "\" }"));
	}

}
