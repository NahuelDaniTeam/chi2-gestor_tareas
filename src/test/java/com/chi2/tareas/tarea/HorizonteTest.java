package com.chi2.tareas.tarea;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Cálculo de la fecha de referencia y del fin del período por horizonte (§4.1, §9). */
class HorizonteTest {

	@ParameterizedTest(name = "{0} con {1} → {2}")
	@CsvSource({
			"DIA,         2026-11-19, 2026-11-19",
			"SEMANA,      2026-11-19, 2026-11-16",   // jueves → lunes
			"SEMANA,      2026-11-16, 2026-11-16",   // el lunes ya es la referencia
			"SEMANA,      2026-11-22, 2026-11-16",   // domingo → lunes anterior
			"SEMANA,      2026-12-02, 2026-11-30",   // cruza de mes
			"SEMANA,      2027-01-01, 2026-12-28",   // cruza de año
			"MES,         2026-11-19, 2026-11-01",
			"MES,         2026-02-28, 2026-02-01",
			"LARGO_PLAZO, 2027-03-15, 2027-03-15"
	})
	void laFechaDeReferenciaDependeDelHorizonte(Horizonte horizonte, LocalDate fecha, LocalDate esperada) {
		assertThat(horizonte.fechaReferencia(fecha)).isEqualTo(esperada);
	}

	@Test
	void largoPlazoPuedeNoTenerFecha() {
		assertThat(Horizonte.LARGO_PLAZO.fechaReferencia(null)).isNull();
		assertThat(Horizonte.LARGO_PLAZO.exigeFecha()).isFalse();
		assertThat(Horizonte.DIA.exigeFecha()).isTrue();
	}

	@ParameterizedTest(name = "{0} desde {1} termina {2}")
	@CsvSource({
			"DIA,    2026-11-19, 2026-11-19",
			"SEMANA, 2026-11-16, 2026-11-22",
			"MES,    2026-02-01, 2026-02-28",
			"MES,    2028-02-01, 2028-02-29"
	})
	void elFinDelPeriodo(Horizonte horizonte, LocalDate referencia, LocalDate fin) {
		assertThat(horizonte.fin(referencia)).isEqualTo(fin);
	}

}
