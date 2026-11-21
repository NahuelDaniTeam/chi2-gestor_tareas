package com.chi2.tareas.tarea;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tareas recurrentes (§4.9). */
class RepeticionTest {

	static final Instant AHORA = Instant.parse("2026-11-19T15:00:00Z");

	@ParameterizedTest(name = "{0} después de {1} → {2}")
	@CsvSource({
			"DIARIA,  2026-11-19, 2026-11-20",
			"HABILES, 2026-11-19, 2026-11-20", // jueves → viernes
			"HABILES, 2026-11-20, 2026-11-23", // viernes → lunes
			"HABILES, 2026-11-21, 2026-11-23", // sábado → lunes
			"SEMANAL, 2026-11-19, 2026-11-26",
			"MENSUAL, 2026-01-31, 2026-02-28"
	})
	void laSiguienteFecha(Repeticion repeticion, LocalDate fecha, LocalDate esperada) {
		assertThat(repeticion.siguiente(fecha)).isEqualTo(esperada);
	}

	@Test
	void cadaPlazoAdmiteLasRepeticionesQueTienenSentido() {
		assertThat(Repeticion.DIARIA.admite(Horizonte.DIA)).isTrue();
		assertThat(Repeticion.DIARIA.admite(Horizonte.SEMANA)).isFalse();
		assertThat(Repeticion.SEMANAL.admite(Horizonte.SEMANA)).isTrue();
		assertThat(Repeticion.SEMANAL.admite(Horizonte.MES)).isFalse();
		assertThat(Repeticion.MENSUAL.admite(Horizonte.MES)).isTrue();
		assertThat(Repeticion.SEMANAL.admite(Horizonte.SIN_PLAZO)).isFalse();
		assertThat(Repeticion.NINGUNA.admite(Horizonte.SIN_PLAZO)).isTrue();
	}

	@Test
	void alFinalizarseCreaLaSiguienteUnaSolaVezConSusPasosSinTildar() {
		Tarea tarea = tarea(Horizonte.DIA, "2026-11-19");
		tarea.programarRepeticion(Repeticion.SEMANAL, AHORA);
		tarea.agregarPaso("Juntar recibos", AHORA);
		tarea.editarPaso(tarea.getPasos().getFirst().getId(), null, true, AHORA);
		tarea.programarRecordatorio(Instant.parse("2026-11-19T12:00:00Z"), AHORA);

		assertThat(tarea.crearSiguiente(AHORA)).as("sin finalizar no hay siguiente").isEmpty();
		tarea.cambiarEstado(EstadoTarea.FINALIZADA, AHORA);
		Tarea siguiente = tarea.crearSiguiente(AHORA).orElseThrow();

		assertThat(siguiente.getFechaReferencia()).isEqualTo("2026-11-26");
		assertThat(siguiente.getEstado()).isEqualTo(EstadoTarea.PENDIENTE);
		assertThat(siguiente.getRepeticion()).isEqualTo(Repeticion.SEMANAL);
		assertThat(siguiente.getPasos()).singleElement().matches(p -> !p.isHecho() && p.getTexto().equals("Juntar recibos"));
		assertThat(siguiente.getRecordatorio()).isEqualTo("2026-11-26T12:00:00Z");

		tarea.cambiarEstado(EstadoTarea.PENDIENTE, AHORA);
		tarea.cambiarEstado(EstadoTarea.FINALIZADA, AHORA);
		assertThat(tarea.crearSiguiente(AHORA)).as("reabrir y volver a finalizar no duplica").isEmpty();
	}

	@Test
	void sinPlazoNoSeRepiteYAlQuitarElPlazoSeDejaDeRepetir() {
		Tarea sinPlazo = tarea(Horizonte.SIN_PLAZO, null);
		assertThatThrownBy(() -> sinPlazo.programarRepeticion(Repeticion.DIARIA, AHORA))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Para repetir una tarea, primero ponele un plazo.");

		Tarea diaria = tarea(Horizonte.DIA, "2026-11-19");
		diaria.programarRepeticion(Repeticion.DIARIA, AHORA);
		diaria.cambiarPlazo(Horizonte.SEMANA, LocalDate.parse("2026-11-19"), AHORA);
		assertThat(diaria.getRepeticion()).isEqualTo(Repeticion.NINGUNA);
	}

	@Test
	void cambiarLaHoraDelRecordatorioVuelveAHabilitarElAviso() {
		Tarea tarea = tarea(Horizonte.DIA, "2026-11-19");
		tarea.programarRecordatorio(Instant.parse("2026-11-19T12:00:00Z"), AHORA);
		tarea.marcarRecordatorioAvisado();

		tarea.programarRecordatorio(Instant.parse("2026-11-19T12:00:00Z"), AHORA);
		assertThat(tarea.isRecordatorioAvisado()).as("misma hora: sigue avisado").isTrue();
		tarea.programarRecordatorio(Instant.parse("2026-11-19T18:00:00Z"), AHORA);
		assertThat(tarea.isRecordatorioAvisado()).isFalse();
	}

	static Tarea tarea(Horizonte horizonte, String fecha) {
		return new Tarea("Pagar expensas", null, horizonte, fecha == null ? null : LocalDate.parse(fecha),
				Prioridad.MEDIA, UUID.randomUUID(), AHORA);
	}

}
