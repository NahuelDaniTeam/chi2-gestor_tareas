package com.chi2.tareas.tarea;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Horizonte de planificación de una tarea (§4.1). Cada uno sabe cuál es su fecha de referencia
 * y hasta cuándo dura el período que representa.
 */
public enum Horizonte {

	DIA("Día"),
	SEMANA("Semana"),
	MES("Mes"),
	LARGO_PLAZO("Largo plazo");

	/** La semana arranca el lunes (propuesta, PENDIENTES.md 3). */
	public static final DayOfWeek PRIMER_DIA_SEMANA = DayOfWeek.MONDAY;

	private final String etiqueta;

	Horizonte(String etiqueta) {
		this.etiqueta = etiqueta;
	}

	public String getEtiqueta() {
		return etiqueta;
	}

	/**
	 * Fecha de referencia para cualquier fecha dentro del período: el mismo día, el primer día
	 * de la semana o el primer día del mes. En largo plazo es la fecha límite, opcional.
	 */
	public LocalDate fechaReferencia(LocalDate fecha) {
		if (fecha == null) {
			return null;
		}
		return switch (this) {
			case DIA, LARGO_PLAZO -> fecha;
			case SEMANA -> fecha.with(TemporalAdjusters.previousOrSame(PRIMER_DIA_SEMANA));
			case MES -> fecha.withDayOfMonth(1);
		};
	}

	/** Último día del período que empieza en {@code fechaReferencia}. */
	public LocalDate fin(LocalDate fechaReferencia) {
		if (fechaReferencia == null) {
			return null;
		}
		return switch (this) {
			case DIA, LARGO_PLAZO -> fechaReferencia;
			case SEMANA -> fechaReferencia.plusDays(6);
			case MES -> fechaReferencia.with(TemporalAdjusters.lastDayOfMonth());
		};
	}

	public boolean exigeFecha() {
		return this != LARGO_PLAZO;
	}

}
