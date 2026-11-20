package com.chi2.tareas.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.chi2.tareas.WebMvcTestConfig;
import com.chi2.tareas.tarea.Ambito;
import com.chi2.tareas.tarea.EstadoTarea;
import com.chi2.tareas.tarea.Horizonte;
import com.chi2.tareas.tarea.Prioridad;
import com.chi2.tareas.tarea.Rango;
import com.chi2.tareas.tarea.TareaNoEncontradaException;
import com.chi2.tareas.tarea.TareaRequest;
import com.chi2.tareas.tarea.TareaResponse;
import com.chi2.tareas.tarea.TareaService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code /api/tareas} (§8), con el service simulado. */
@WebMvcTest(TareaApiController.class)
@Import(WebMvcTestConfig.class)
class TareaApiControllerTest {

	static final UUID ID = UUID.fromString("7d0f7a0e-5a3b-4f43-9a52-2a2f3d1c0b11");

	@Autowired
	MockMvc mvc;

	@MockitoBean
	TareaService service;

	@Test
	void listaPorRango() throws Exception {
		Rango semana = new Rango(LocalDate.parse("2026-11-16"), LocalDate.parse("2026-11-22"));
		when(service.listar(semana, Prioridad.ALTA, null)).thenReturn(List.of(respuesta(EstadoTarea.PENDIENTE)));

		mvc.perform(get("/api/tareas").param("desde", "2026-11-16").param("hasta", "2026-11-22")
						.param("prioridad", "ALTA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(ID.toString()))
				.andExpect(jsonPath("$[0].fechaReferencia").value("2026-11-16"))
				.andExpect(jsonPath("$[0].prioridad").value("ALTA"));
	}

	@Test
	void unRangoSinFinEsLargoPlazo() throws Exception {
		mvc.perform(get("/api/tareas").param("desde", "2026-12-01")).andExpect(status().isOk());

		verify(service).listar(Rango.desde(LocalDate.parse("2026-12-01")), null, null);
	}

	@Test
	void sinDesdeOConUnRangoAlReves400() throws Exception {
		mvc.perform(get("/api/tareas"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Falta o no es válido el parámetro 'desde'."));
		mvc.perform(get("/api/tareas").param("desde", "2026-11-20").param("hasta", "2026-11-19"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(service);
	}

	@Test
	void sinPlazoDevuelveLasPorPlanificar() throws Exception {
		when(service.sinPlazo(null, null)).thenReturn(List.of());

		mvc.perform(get("/api/tareas/sin-plazo")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void planificarConPatch() throws Exception {
		when(service.cambiarPlazo(ID, Horizonte.DIA, LocalDate.parse("2026-11-24"))).thenReturn(respuesta(EstadoTarea.PENDIENTE));

		mvc.perform(patch("/api/tareas/{id}/plazo", ID).contentType(MediaType.APPLICATION_JSON)
						.content("{ \"horizonte\": \"DIA\", \"fechaReferencia\": \"2026-11-24\" }"))
				.andExpect(status().isOk());
		mvc.perform(patch("/api/tareas/{id}/plazo", ID).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.horizonte").exists());
	}

	@Test
	void hoyDevuelveLasDelDia() throws Exception {
		when(service.hoy(null, null)).thenReturn(List.of(respuesta(EstadoTarea.PENDIENTE)));

		mvc.perform(get("/api/tareas/hoy"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void crearResponde201ConPrioridadMediaPorDefecto() throws Exception {
		when(service.crear(any())).thenReturn(respuesta(EstadoTarea.PENDIENTE));

		mvc.perform(post("/api/tareas").contentType(MediaType.APPLICATION_JSON).content("""
						{ "titulo": "  Leer capítulo 3  ", "horizonte": "DIA" }
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(ID.toString()));

		verify(service).crear(argThat((TareaRequest r) -> r.titulo().equals("Leer capítulo 3")
				&& r.prioridad() == Prioridad.MEDIA && r.fechaReferencia() == null && r.descripcion() == null));
	}

	@Test
	void crearSinTituloNiHorizonteResponde400ConLosErrores() throws Exception {
		mvc.perform(post("/api/tareas").contentType(MediaType.APPLICATION_JSON).content("""
						{ "titulo": "   " }
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.titulo").value("Escribí un título."))
				.andExpect(jsonPath("$.errores.horizonte").exists());

		verifyNoInteractions(service);
	}

	@Test
	void unTituloDeMasDe200CaracteresNoSeAcepta() throws Exception {
		String largo = "x".repeat(201);
		mvc.perform(post("/api/tareas").contentType(MediaType.APPLICATION_JSON)
						.content("{ \"titulo\": \"" + largo + "\", \"horizonte\": \"DIA\" }"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.titulo").value("El título puede tener hasta 200 caracteres."));
	}

	@Test
	void unaPrioridadQueNoExisteResponde400() throws Exception {
		mvc.perform(post("/api/tareas").contentType(MediaType.APPLICATION_JSON).content("""
						{ "titulo": "Algo", "horizonte": "DIA", "prioridad": "URGENTE" }
						"""))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(service);
	}

	@Test
	void editarConPut() throws Exception {
		when(service.editar(eq(ID), any())).thenReturn(respuesta(EstadoTarea.PENDIENTE));

		mvc.perform(put("/api/tareas/{id}", ID).contentType(MediaType.APPLICATION_JSON).content("""
						{ "titulo": "Leer capítulo 4", "horizonte": "SEMANA", "fechaReferencia": "2026-11-19", "prioridad": "ALTA" }
						"""))
				.andExpect(status().isOk());

		verify(service).editar(eq(ID), argThat(r -> r.horizonte() == Horizonte.SEMANA
				&& r.fechaReferencia().equals(LocalDate.parse("2026-11-19"))));
	}

	@Test
	void cambiarEstadoConPatch() throws Exception {
		when(service.cambiarEstado(ID, EstadoTarea.FINALIZADA)).thenReturn(respuesta(EstadoTarea.FINALIZADA));

		mvc.perform(patch("/api/tareas/{id}/estado", ID).contentType(MediaType.APPLICATION_JSON)
						.content("{ \"estado\": \"FINALIZADA\" }"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("FINALIZADA"));
	}

	@Test
	void cambiarEstadoSinEstadoResponde400() throws Exception {
		mvc.perform(patch("/api/tareas/{id}/estado", ID).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.estado").exists());
	}

	@Test
	void cambiarPrioridadConPatch() throws Exception {
		when(service.cambiarPrioridad(ID, Prioridad.BAJA)).thenReturn(respuesta(EstadoTarea.PENDIENTE));

		mvc.perform(patch("/api/tareas/{id}/prioridad", ID).contentType(MediaType.APPLICATION_JSON)
						.content("{ \"prioridad\": \"BAJA\" }"))
				.andExpect(status().isOk());

		verify(service).cambiarPrioridad(ID, Prioridad.BAJA);
	}

	@Test
	void eliminarResponde204() throws Exception {
		mvc.perform(delete("/api/tareas/{id}", ID))
				.andExpect(status().isNoContent());

		verify(service).eliminar(ID);
	}

	@Test
	void unaTareaQueNoExisteResponde404() throws Exception {
		doThrow(new TareaNoEncontradaException(ID)).when(service).eliminar(ID);

		mvc.perform(delete("/api/tareas/{id}", ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value(TareaNoEncontradaException.MENSAJE));
	}

	@Test
	void unIdQueNoEsUuidResponde400() throws Exception {
		mvc.perform(get("/api/tareas/{id}", "123"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(service);
	}

	static TareaResponse respuesta(EstadoTarea estado) {
		Instant ahora = WebMvcTestConfig.AHORA;
		return new TareaResponse(ID, "Leer capítulo 3", null, Horizonte.SEMANA, LocalDate.parse("2026-11-16"),
				Prioridad.ALTA, estado, Ambito.PERSONAL, false, ahora, ahora,
				estado == EstadoTarea.FINALIZADA ? ahora : null);
	}

}
