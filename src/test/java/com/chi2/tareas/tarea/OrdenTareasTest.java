package com.chi2.tareas.tarea;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Orden de las vistas: prioridad, después fecha, después antigüedad (§4.3, §9). */
class OrdenTareasTest {

	static final UUID USUARIO = UUID.randomUUID();
	static final Instant T0 = Instant.parse("2026-11-19T12:00:00Z");

	@Test
	void altaPrimeroLuegoPorFechaYSinFechaAlFinal() {
		Tarea bajaTemprana = tarea("baja temprana", Prioridad.BAJA, "2026-11-01", T0);
		Tarea mediaSinFecha = tarea("media sin fecha", Prioridad.MEDIA, null, T0);
		Tarea mediaTardia = tarea("media tardía", Prioridad.MEDIA, "2026-12-01", T0);
		Tarea mediaTemprana = tarea("media temprana", Prioridad.MEDIA, "2026-11-20", T0);
		Tarea alta = tarea("alta", Prioridad.ALTA, "2027-06-01", T0);

		List<String> orden = List.of(bajaTemprana, mediaSinFecha, mediaTardia, mediaTemprana, alta).stream()
				.sorted(OrdenTareas.POR_PRIORIDAD).map(Tarea::getTitulo).toList();

		assertThat(orden).containsExactly("alta", "media temprana", "media tardía", "media sin fecha", "baja temprana");
	}

	@Test
	void aIgualPrioridadYFechaVaPrimeroLaMasVieja() {
		Tarea nueva = tarea("nueva", Prioridad.MEDIA, "2026-11-19", T0.plusSeconds(60));
		Tarea vieja = tarea("vieja", Prioridad.MEDIA, "2026-11-19", T0);

		assertThat(List.of(nueva, vieja).stream().sorted(OrdenTareas.POR_PRIORIDAD).map(Tarea::getTitulo))
				.containsExactly("vieja", "nueva");
	}

	static Tarea tarea(String titulo, Prioridad prioridad, String fecha, Instant creada) {
		return new Tarea(titulo, null, Horizonte.LARGO_PLAZO, fecha == null ? null : LocalDate.parse(fecha),
				prioridad, USUARIO, creada);
	}

}
