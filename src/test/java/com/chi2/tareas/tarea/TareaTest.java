package com.chi2.tareas.tarea;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Transiciones de estado, {@code completada_en} y vencimiento (§4.1, §9). */
class TareaTest {

	static final UUID USUARIO = UUID.randomUUID();
	static final Instant CREADA = Instant.parse("2026-11-19T12:00:00Z");
	static final Instant DESPUES = Instant.parse("2026-11-19T15:30:00Z");
	static final LocalDate HOY = LocalDate.parse("2026-11-19");

	@Test
	void unaTareaNuevaEsPersonalYEstaPendiente() {
		Tarea tarea = tarea(Horizonte.DIA, HOY);

		assertThat(tarea.getEstado()).isEqualTo(EstadoTarea.PENDIENTE);
		assertThat(tarea.getAmbito()).isEqualTo(Ambito.PERSONAL);
		assertThat(tarea.getCreadaEn()).isEqualTo(CREADA);
		assertThat(tarea.getActualizadaEn()).isEqualTo(CREADA);
		assertThat(tarea.getCompletadaEn()).isNull();
	}

	@Test
	void laFechaSeLlevaALaReferenciaDelHorizonte() {
		assertThat(tarea(Horizonte.SEMANA, HOY).getFechaReferencia()).isEqualTo("2026-11-16");
		assertThat(tarea(Horizonte.MES, HOY).getFechaReferencia()).isEqualTo("2026-11-01");
	}

	@Test
	void diaSemanaYMesExigenFecha() {
		assertThatThrownBy(() -> tarea(Horizonte.SEMANA, null)).isInstanceOf(IllegalArgumentException.class);
		assertThat(tarea(Horizonte.LARGO_PLAZO, null).getFechaReferencia()).isNull();
	}

	@Test
	void alFinalizarseRegistraCuandoYAlReabrirseSeBorra() {
		Tarea tarea = tarea(Horizonte.DIA, HOY);

		tarea.cambiarEstado(EstadoTarea.EN_CURSO, CREADA);
		assertThat(tarea.getCompletadaEn()).isNull();

		tarea.cambiarEstado(EstadoTarea.FINALIZADA, DESPUES);
		assertThat(tarea.getCompletadaEn()).isEqualTo(DESPUES);
		assertThat(tarea.getActualizadaEn()).isEqualTo(DESPUES);

		tarea.cambiarEstado(EstadoTarea.PENDIENTE, DESPUES.plusSeconds(60));
		assertThat(tarea.getCompletadaEn()).isNull();
	}

	@Test
	void volverAlMismoEstadoNoCambiaNada() {
		Tarea tarea = tarea(Horizonte.DIA, HOY);
		tarea.cambiarEstado(EstadoTarea.FINALIZADA, CREADA);

		tarea.cambiarEstado(EstadoTarea.FINALIZADA, DESPUES);

		assertThat(tarea.getCompletadaEn()).isEqualTo(CREADA);
		assertThat(tarea.getActualizadaEn()).isEqualTo(CREADA);
	}

	@Test
	void cambiarLaPrioridadActualizaLaModificacion() {
		Tarea tarea = tarea(Horizonte.DIA, HOY);

		tarea.cambiarPrioridad(Prioridad.ALTA, DESPUES);

		assertThat(tarea.getPrioridad()).isEqualTo(Prioridad.ALTA);
		assertThat(tarea.getActualizadaEn()).isEqualTo(DESPUES);
	}

	@Test
	void estaVencidaSiTerminoSuPeriodoSinFinalizarse() {
		assertThat(tarea(Horizonte.DIA, HOY.minusDays(1)).estaVencida(HOY)).isTrue();
		assertThat(tarea(Horizonte.DIA, HOY).estaVencida(HOY)).isFalse();
		// la semana del 16 sigue en curso el 19
		assertThat(tarea(Horizonte.SEMANA, HOY.minusDays(3)).estaVencida(HOY)).isFalse();
		assertThat(tarea(Horizonte.SEMANA, HOY.minusWeeks(1)).estaVencida(HOY)).isTrue();
		assertThat(tarea(Horizonte.LARGO_PLAZO, null).estaVencida(HOY)).isFalse();
		assertThat(tarea(Horizonte.LARGO_PLAZO, HOY.minusDays(1)).estaVencida(HOY)).isTrue();

		Tarea finalizada = tarea(Horizonte.DIA, HOY.minusDays(1));
		finalizada.cambiarEstado(EstadoTarea.FINALIZADA, DESPUES);
		assertThat(finalizada.estaVencida(HOY)).isFalse();
	}

	static Tarea tarea(Horizonte horizonte, LocalDate fecha) {
		return new Tarea("Leer capítulo 3", null, horizonte, fecha, Prioridad.MEDIA, USUARIO, CREADA);
	}

}
