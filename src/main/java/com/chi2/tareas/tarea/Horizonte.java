package com.chi2.tareas.tarea;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Plazo de una tarea (§4.1). Los plazos se anidan: un día está dentro de una semana, una semana dentro
 * de un mes. {@link #SIN_PLAZO} es una tarea creada que falta planificar; {@link #LIMITE} es un tiempo
 * máximo ("antes del 15"), que ocupa el día del vencimiento.
 */
public enum Horizonte {

	SIN_PLAZO("Sin plazo"),
	DIA("Día"),
	SEMANA("Semana"),
	MES("Mes"),
	LIMITE("Fecha límite");

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
	 * Fecha de referencia para cualquier fecha dentro del período: el mismo día, el lunes de la semana
	 * o el primero del mes. Sin plazo no tiene fecha.
	 */
	public LocalDate fechaReferencia(LocalDate fecha) {
		if (fecha == null || this == SIN_PLAZO) {
			return null;
		}
		return switch (this) {
			case SEMANA -> fecha.with(TemporalAdjusters.previousOrSame(PRIMER_DIA_SEMANA));
			case MES -> fecha.withDayOfMonth(1);
			default -> fecha;
		};
	}

	/** Último día del período que empieza en {@code fechaReferencia}. */
	public LocalDate fin(LocalDate fechaReferencia) {
		if (fechaReferencia == null) {
			return null;
		}
		return switch (this) {
			case SEMANA -> fechaReferencia.plusDays(6);
			case MES -> fechaReferencia.with(TemporalAdjusters.lastDayOfMonth());
			default -> fechaReferencia;
		};
	}

	/** Días que dura el período (1 para un día o una fecha límite). */
	public long duracionEnDias(LocalDate fechaReferencia) {
		return ChronoUnit.DAYS.between(fechaReferencia, fin(fechaReferencia)) + 1;
	}

	public boolean exigeFecha() {
		return this != SIN_PLAZO;
	}

}
