package com.chi2.tareas.estadistica;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.chi2.tareas.tarea.Horizonte;

/** Cuentas de las estadísticas (§4.14) a partir de los días en que se finalizaron tareas. Sin dependencias: se testea sola. */
public final class CalculoEstadisticas {

	private CalculoEstadisticas() {
	}

	/** Finalizadas por semana, de la más vieja a la actual (incluida), {@code semanas} en total. */
	public static List<SemanaCompletadas> porSemana(Collection<LocalDate> diasCompletadas, LocalDate hoy, int semanas) {
		LocalDate lunesActual = Horizonte.SEMANA.fechaReferencia(hoy);
		List<SemanaCompletadas> resultado = new ArrayList<>();
		for (int i = semanas - 1; i >= 0; i--) {
			LocalDate lunes = lunesActual.minusWeeks(i);
			LocalDate domingo = lunes.plusDays(6);
			int cantidad = (int) diasCompletadas.stream().filter(d -> !d.isBefore(lunes) && !d.isAfter(domingo)).count();
			resultado.add(new SemanaCompletadas(lunes, cantidad));
		}
		return resultado;
	}

	/**
	 * Días seguidos con al menos una tarea finalizada, contando hasta hoy. Si hoy todavía no se finalizó nada,
	 * la racha de ayer sigue en pie (el día no terminó).
	 */
	public static int racha(Collection<LocalDate> diasCompletadas, LocalDate hoy) {
		Set<LocalDate> dias = new TreeSet<>(diasCompletadas);
		LocalDate dia = dias.contains(hoy) ? hoy : hoy.minusDays(1);
		int racha = 0;
		while (dias.contains(dia)) {
			racha++;
			dia = dia.minusDays(1);
		}
		return racha;
	}

}
