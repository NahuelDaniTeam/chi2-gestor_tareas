package com.chi2.tareas.estadistica;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculoEstadisticasTest {

	static final LocalDate JUEVES = LocalDate.parse("2026-11-19");

	@Test
	void cuentaPorSemanaDeLunesADomingoHastaLaActual() {
		List<LocalDate> dias = List.of(LocalDate.parse("2026-11-16"), LocalDate.parse("2026-11-19"),
				LocalDate.parse("2026-11-15"), LocalDate.parse("2026-09-01"));

		List<SemanaCompletadas> semanas = CalculoEstadisticas.porSemana(dias, JUEVES, 3);

		assertThat(semanas).extracting(SemanaCompletadas::lunes)
				.containsExactly(LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-09"), LocalDate.parse("2026-11-16"));
		assertThat(semanas).extracting(SemanaCompletadas::cantidad).containsExactly(0, 1, 2);
	}

	@Test
	void laRachaCuentaDiasSeguidosYNoSeCortaSiHoyTodaviaNoSeTermino() {
		List<LocalDate> hastaAyer = List.of(LocalDate.parse("2026-11-18"), LocalDate.parse("2026-11-17"),
				LocalDate.parse("2026-11-17"), LocalDate.parse("2026-11-15"));
		assertThat(CalculoEstadisticas.racha(hastaAyer, JUEVES)).isEqualTo(2);

		List<LocalDate> conHoy = List.of(JUEVES, LocalDate.parse("2026-11-18"));
		assertThat(CalculoEstadisticas.racha(conHoy, JUEVES)).isEqualTo(2);

		assertThat(CalculoEstadisticas.racha(List.of(LocalDate.parse("2026-11-10")), JUEVES)).isZero();
	}

}
