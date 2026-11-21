package com.chi2.tareas.tarea;

import java.time.Clock;
import java.time.Duration;
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
import org.springframework.jdbc.core.JdbcTemplate;
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

	@Autowired
	RetencionPapeleraService retencion;

	@Autowired
	JdbcTemplate jdbc;

	@BeforeEach
	void limpiar() {
		// También las que están en la papelera, que el repositorio no ve
		jdbc.update("delete from tarea");
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
	void eliminarLaMandaALaPapeleraDondeSePuedeRestaurar() throws Exception {
		String id = crear("Borrar", "DIA", "2026-11-19", "MEDIA");

		mvc.perform(delete("/api/tareas/{id}", id)).andExpect(status().isNoContent());
		mvc.perform(get("/api/tareas/{id}", id)).andExpect(status().isNotFound());
		mvc.perform(get("/api/tareas/hoy")).andExpect(jsonPath("$.length()").value(0));
		mvc.perform(get("/api/papelera"))
				.andExpect(jsonPath("$[*].titulo", contains("Borrar")))
				.andExpect(jsonPath("$[0].eliminadaEn").value("2026-11-19T15:00:00Z"));

		mvc.perform(post("/api/papelera/{id}/restaurar", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eliminadaEn").value(nullValue()));
		mvc.perform(get("/api/tareas/hoy")).andExpect(jsonPath("$[*].titulo", contains("Borrar")));
		mvc.perform(get("/api/papelera")).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void desdeLaPapeleraSeBorraDeVerdadUnaOTodas() throws Exception {
		String una = crear("Una", "DIA", "2026-11-19", "MEDIA");
		String otra = crear("Otra", "DIA", "2026-11-19", "MEDIA");
		String sigue = crear("Sigue", "DIA", "2026-11-19", "MEDIA");
		mvc.perform(delete("/api/tareas/{id}", una));
		mvc.perform(delete("/api/tareas/{id}", otra));

		mvc.perform(delete("/api/papelera/{id}", una)).andExpect(status().isNoContent());
		mvc.perform(delete("/api/papelera/{id}", sigue)).andExpect(status().isNotFound());
		mvc.perform(get("/api/papelera")).andExpect(jsonPath("$[*].titulo", contains("Otra")));

		mvc.perform(delete("/api/papelera")).andExpect(status().isNoContent());
		mvc.perform(get("/api/papelera")).andExpect(jsonPath("$.length()").value(0));
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	void laRetencionBorraLasQueLlevanMasDeTreintaDiasEnLaPapelera() throws Exception {
		String vieja = crear("Vieja", "DIA", "2026-11-19", "MEDIA");
		mvc.perform(delete("/api/tareas/{id}", vieja));
		reloj.adelantar(Duration.ofDays(20));
		String nueva = crear("Nueva", "DIA", "2026-11-19", "MEDIA");
		mvc.perform(delete("/api/tareas/{id}", nueva));
		reloj.adelantar(Duration.ofDays(11));

		assertThat(retencion.purgar()).isEqualTo(1);
		mvc.perform(get("/api/papelera")).andExpect(jsonPath("$[*].titulo", contains("Nueva")));
	}

	@Test
	void alFinalizarUnaRecurrenteAparecePorCuentaPropiaLaSiguiente() throws Exception {
		String cuerpo = mvc.perform(post("/api/tareas").contentType(MediaType.APPLICATION_JSON).content("""
						{ "titulo": "Regar las plantas", "horizonte": "DIA", "fechaReferencia": "2026-11-19",
						  "repeticion": "HABILES" }
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.repeticion").value("HABILES"))
				.andExpect(jsonPath("$.proximaRepeticion").value("2026-11-20"))
				.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(cuerpo, "$.id");

		cambiarEstado(id, "FINALIZADA").andExpect(status().isOk());
		cambiarEstado(id, "PENDIENTE");
		cambiarEstado(id, "FINALIZADA");

		mvc.perform(get("/api/tareas").param("desde", "2026-11-20").param("hasta", "2026-11-20"))
				.andExpect(jsonPath("$[*].titulo", contains("Regar las plantas")))
				.andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
		assertThat(repository.count()).as("se crea una sola vez").isEqualTo(2);
	}

	@Test
	void unaTareaSinPlazoNoSePuedeRepetir() throws Exception {
		mvc.perform(post("/api/tareas").contentType(MediaType.APPLICATION_JSON).content("""
						{ "titulo": "x", "horizonte": "SIN_PLAZO", "repeticion": "DIARIA" }
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Para repetir una tarea, primero ponele un plazo."));
	}

	@Test
	void elBuscadorNoDistingueTildesNiMayusculasYDejaLasFinalizadasAlFinal() throws Exception {
		String vieja = crear("Leer el CAPÍTULO 3", "DIA", "2026-11-19", "MEDIA");
		cambiarEstado(vieja, "FINALIZADA");
		crear("Escribir capitulo 4", "SIN_PLAZO", null, "MEDIA");
		crear("Comprar yerba", "SIN_PLAZO", null, "MEDIA");
		String borrada = crear("Capítulo descartado", "SIN_PLAZO", null, "MEDIA");
		mvc.perform(delete("/api/tareas/{id}", borrada));

		mvc.perform(get("/api/tareas/buscar").param("q", "capítulo"))
				.andExpect(jsonPath("$[*].titulo", contains("Escribir capitulo 4", "Leer el CAPÍTULO 3")));
		mvc.perform(get("/api/tareas/buscar").param("q", "100%")).andExpect(jsonPath("$.length()").value(0));
		mvc.perform(get("/api/tareas/buscar").param("q", "  ")).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void losRecordatoriosLlegadosSeAvisanUnaVez() throws Exception {
		String cuerpo = mvc.perform(post("/api/tareas").contentType(MediaType.APPLICATION_JSON).content("""
						{ "titulo": "Llamar al dentista", "horizonte": "DIA", "fechaReferencia": "2026-11-19",
						  "recordatorio": "2026-11-19T16:00:00Z" }
						"""))
				.andExpect(jsonPath("$.recordatorio").value("2026-11-19T16:00:00Z"))
				.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(cuerpo, "$.id");

		mvc.perform(get("/api/tareas/recordatorios")).andExpect(jsonPath("$.length()").value(0));
		reloj.adelantar(Duration.ofHours(1));
		mvc.perform(get("/api/tareas/recordatorios")).andExpect(jsonPath("$[*].titulo", contains("Llamar al dentista")));

		mvc.perform(post("/api/tareas/{id}/recordatorio/avisado", id)).andExpect(status().isNoContent());
		mvc.perform(get("/api/tareas/recordatorios")).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void elChecklistSeAgregaTildaEditaYBorra() throws Exception {
		String id = crear("Capítulo 2", "SEMANA", "2026-11-19", "ALTA");

		String cuerpo = mvc.perform(post("/api/tareas/{id}/pasos", id).contentType(MediaType.APPLICATION_JSON)
						.content("{ \"texto\": \" Buscar fuentes \" }"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.pasos[*].texto", contains("Buscar fuentes")))
				.andReturn().getResponse().getContentAsString();
		String paso = JsonPath.read(cuerpo, "$.pasos[0].id");
		mvc.perform(post("/api/tareas/{id}/pasos", id).contentType(MediaType.APPLICATION_JSON)
						.content("{ \"texto\": \"Escribir borrador\" }"))
				.andExpect(jsonPath("$.pasos[*].texto", contains("Buscar fuentes", "Escribir borrador")));

		mvc.perform(patch("/api/tareas/{id}/pasos/{paso}", id, paso).contentType(MediaType.APPLICATION_JSON)
						.content("{ \"hecho\": true }"))
				.andExpect(jsonPath("$.pasos[0].hecho").value(true))
				.andExpect(jsonPath("$.pasos[0].texto").value("Buscar fuentes"));
		mvc.perform(patch("/api/tareas/{id}/pasos/{paso}", id, paso).contentType(MediaType.APPLICATION_JSON)
						.content("{ \"texto\": \"   \" }"))
				.andExpect(status().isBadRequest());

		mvc.perform(delete("/api/tareas/{id}/pasos/{paso}", id, paso))
				.andExpect(jsonPath("$.pasos[*].texto", contains("Escribir borrador")));
		mvc.perform(delete("/api/tareas/{id}/pasos/{paso}", id, paso)).andExpect(status().isNotFound());
		mvc.perform(get("/api/tareas").param("desde", "2026-11-16").param("hasta", "2026-11-22"))
				.andExpect(jsonPath("$[0].pasos.length()").value(1));
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
