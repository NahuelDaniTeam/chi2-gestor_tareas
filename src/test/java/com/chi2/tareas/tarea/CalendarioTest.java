package com.chi2.tareas.tarea;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import com.chi2.tareas.TareasProperties;
import com.chi2.tareas.RelojDeTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarioTest {

	final RelojDeTest reloj = new RelojDeTest(Instant.parse("2026-11-19T15:00:00Z"));
	final Calendario calendario = new Calendario(reloj,
			new TareasProperties(ZoneId.of("America/Argentina/Cordoba"), UUID.randomUUID(), Duration.ofDays(30)));

	@Test
	void hoyEsElDiaEnArgentinaAunqueEnUtcYaSeaManana() {
		reloj.fijar(Instant.parse("2026-11-20T02:30:00Z")); // 23:30 del 19 en Córdoba
		assertThat(calendario.hoy()).isEqualTo("2026-11-19");

		reloj.fijar(Instant.parse("2026-11-20T03:00:00Z")); // medianoche en Córdoba
		assertThat(calendario.hoy()).isEqualTo("2026-11-20");
	}

	@Test
	void hoyEsUnSoloDiaSinNavegacion() {
		Periodo hoy = calendario.hoyComoPeriodo();

		assertThat(hoy.titulo()).isEqualTo("Jueves 19 de noviembre");
		assertThat(hoy.tieneNavegacion()).isFalse();
		assertThat(hoy.rango()).isEqualTo(Rango.dia(LocalDate.parse("2026-11-19")));
	}

	@Test
	void laSemanaVaDeLunesADomingoYNavegaDeASiete() {
		Periodo semana = calendario.semana(LocalDate.parse("2026-11-19"));

		assertThat(semana.fecha()).isEqualTo("2026-11-16");
		assertThat(semana.titulo()).isEqualTo("Del 16 al 22 de noviembre");
		assertThat(semana.rango()).isEqualTo(new Rango(LocalDate.parse("2026-11-16"), LocalDate.parse("2026-11-22")));
		assertThat(semana.anterior()).isEqualTo("2026-11-09");
		assertThat(semana.siguiente()).isEqualTo("2026-11-23");
	}

	@Test
	void unaSemanaQueCruzaDeMesNombraLosDosMeses() {
		assertThat(Calendario.tituloSemana(LocalDate.parse("2026-11-30")))
				.isEqualTo("Del 30 de noviembre al 6 de diciembre");
	}

	@Test
	void elMesNavegaPorMes() {
		Periodo mes = calendario.mes(LocalDate.parse("2026-01-19"));

		assertThat(mes.titulo()).isEqualTo("Enero 2026");
		assertThat(mes.rango().hasta()).isEqualTo("2026-01-31");
		assertThat(mes.anterior()).isEqualTo("2025-12-01");
		assertThat(mes.siguiente()).isEqualTo("2026-02-01");
	}

	@Test
	void elLargoPlazoEmpiezaElMesQueVieneYNoTermina() {
		Periodo largo = calendario.largoPlazo();

		assertThat(largo.rango()).isEqualTo(Rango.desde(LocalDate.parse("2026-12-01")));
		assertThat(largo.tieneNavegacion()).isFalse();
	}

	@Test
	void porPlanificarNoTieneFechas() {
		assertThat(calendario.porPlanificar().rango()).isNull();
	}

}
