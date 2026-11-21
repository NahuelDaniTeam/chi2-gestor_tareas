package com.chi2.tareas.tarea;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Cada cuánto se repite una tarea (§4.9). Al finalizarla se crea la siguiente con la fecha que da
 * {@link #siguiente}. Una tarea de semana solo puede repetirse por semana o por mes; una de mes, por mes.
 */
public enum Repeticion {

	NINGUNA("No se repite"),
	DIARIA("Todos los días"),
	HABILES("Días hábiles"),
	SEMANAL("Cada semana"),
	MENSUAL("Cada mes");

	private final String etiqueta;

	Repeticion(String etiqueta) {
		this.etiqueta = etiqueta;
	}

	public String getEtiqueta() {
		return etiqueta;
	}

	public LocalDate siguiente(LocalDate fecha) {
		return switch (this) {
			case NINGUNA -> fecha;
			case DIARIA -> fecha.plusDays(1);
			case HABILES -> proximoDiaHabil(fecha);
			case SEMANAL -> fecha.plusWeeks(1);
			case MENSUAL -> fecha.plusMonths(1);
		};
	}

	public boolean admite(Horizonte horizonte) {
		return switch (horizonte) {
			case SIN_PLAZO -> this == NINGUNA;
			case DIA, LIMITE -> true;
			case SEMANA -> this == NINGUNA || this == SEMANAL || this == MENSUAL;
			case MES -> this == NINGUNA || this == MENSUAL;
		};
	}

	private static LocalDate proximoDiaHabil(LocalDate fecha) {
		LocalDate siguiente = fecha.plusDays(1);
		while (siguiente.getDayOfWeek() == DayOfWeek.SATURDAY || siguiente.getDayOfWeek() == DayOfWeek.SUNDAY) {
			siguiente = siguiente.plusDays(1);
		}
		return siguiente;
	}

}
