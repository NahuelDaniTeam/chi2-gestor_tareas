package com.chi2.tareas.tarea;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plazos anidados (§4.3): qué tareas entran en el rango de cada vista. */
class RangoTest {

	static final Rango SEMANA_16 = new Rango(LocalDate.parse("2026-11-16"), LocalDate.parse("2026-11-22"));
	static final Rango NOVIEMBRE = new Rango(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30"));
	static final Rango HOY = Rango.dia(LocalDate.parse("2026-11-19"));

	@Test
	void unDiaEntraEnSuDiaSuSemanaYSuMes() {
		Tarea martes = tarea(Horizonte.DIA, "2026-11-17");

		assertThat(SEMANA_16.incluye(martes)).isTrue();
		assertThat(NOVIEMBRE.incluye(martes)).isTrue();
		assertThat(Rango.dia(LocalDate.parse("2026-11-17")).incluye(martes)).isTrue();
		assertThat(HOY.incluye(martes)).isFalse();
	}

	@Test
	void unaFechaLimiteSeComportaComoSuDia() {
		Tarea vence = tarea(Horizonte.LIMITE, "2026-11-19");

		assertThat(HOY.incluye(vence)).isTrue();
		assertThat(SEMANA_16.incluye(vence)).isTrue();
	}

	@Test
	void unaSemanaEntraEnSuSemanaYEnLosMesesQueToca() {
		Tarea semana = tarea(Horizonte.SEMANA, "2026-11-16");
		Tarea cruzaDeMes = tarea(Horizonte.SEMANA, "2026-11-30");

		assertThat(SEMANA_16.incluye(semana)).isTrue();
		assertThat(NOVIEMBRE.incluye(semana)).isTrue();
		assertThat(HOY.incluye(semana)).isFalse();
		assertThat(NOVIEMBRE.incluye(cruzaDeMes)).isTrue();
		assertThat(new Rango(LocalDate.parse("2026-12-01"), LocalDate.parse("2026-12-31")).incluye(cruzaDeMes)).isTrue();
	}

	@Test
	void unMesNoEntraEnUnaSemanaNiEnUnDia() {
		Tarea mes = tarea(Horizonte.MES, "2026-11-01");

		assertThat(NOVIEMBRE.incluye(mes)).isTrue();
		assertThat(SEMANA_16.incluye(mes)).isFalse();
		assertThat(Rango.dia(LocalDate.parse("2026-11-01")).incluye(mes)).isFalse();
	}

	@Test
	void unRangoSinFinIncluyeTodoLoQueEmpiezaDespues() {
		Rango largoPlazo = Rango.desde(LocalDate.parse("2026-12-01"));

		assertThat(largoPlazo.incluye(tarea(Horizonte.MES, "2027-03-01"))).isTrue();
		assertThat(largoPlazo.incluye(tarea(Horizonte.DIA, "2026-12-24"))).isTrue();
		assertThat(largoPlazo.incluye(tarea(Horizonte.MES, "2026-11-01"))).isFalse();
	}

	@Test
	void sinPlazoNoEntraEnNingunRango() {
		assertThat(Rango.desde(LocalDate.parse("2000-01-01")).incluye(tarea(Horizonte.SIN_PLAZO, null))).isFalse();
	}

	@Test
	void unRangoNoPuedeTerminarAntesDeEmpezar() {
		assertThatThrownBy(() -> new Rango(LocalDate.parse("2026-11-20"), LocalDate.parse("2026-11-19")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	static Tarea tarea(Horizonte horizonte, String fecha) {
		return new Tarea("x", null, horizonte, fecha == null ? null : LocalDate.parse(fecha), Prioridad.MEDIA,
				UUID.randomUUID(), Instant.parse("2026-11-19T12:00:00Z"));
	}

}
