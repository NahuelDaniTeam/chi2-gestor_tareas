package com.chi2.tareas.tarea;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Rango de fechas que muestra una vista. Una tarea entra si su período se superpone con el rango
 * y no es más largo que él: en una semana entran sus días y la propia semana, pero no el mes;
 * en un mes entran sus días, sus semanas (también las que lo cruzan) y el mes.
 *
 * @param hasta {@code null} si el rango no tiene fin (largo plazo)
 */
public record Rango(LocalDate desde, LocalDate hasta) {

	public Rango {
		Objects.requireNonNull(desde);
		if (hasta != null && hasta.isBefore(desde)) {
			throw new IllegalArgumentException("El rango termina antes de empezar");
		}
	}

	public static Rango dia(LocalDate dia) {
		return new Rango(dia, dia);
	}

	public static Rango desde(LocalDate desde) {
		return new Rango(desde, null);
	}

	public boolean incluye(Tarea tarea) {
		Horizonte horizonte = tarea.getHorizonte();
		LocalDate inicio = tarea.getFechaReferencia();
		if (inicio == null) {
			return false;
		}
		LocalDate fin = horizonte.fin(inicio);
		boolean seSuperpone = (hasta == null || !inicio.isAfter(hasta)) && !fin.isBefore(desde);
		return seSuperpone && (hasta == null || horizonte.duracionEnDias(inicio) <= duracionEnDias());
	}

	public boolean incluye(LocalDate dia) {
		return !dia.isBefore(desde) && (hasta == null || !dia.isAfter(hasta));
	}

	/**
	 * Desde qué fecha de referencia buscar candidatas: una semana o un mes que empezó antes del rango
	 * todavía puede cruzarlo.
	 */
	public LocalDate buscarDesde() {
		return desde.minusDays(31);
	}

	private long duracionEnDias() {
		return ChronoUnit.DAYS.between(desde, hasta) + 1;
	}

}
