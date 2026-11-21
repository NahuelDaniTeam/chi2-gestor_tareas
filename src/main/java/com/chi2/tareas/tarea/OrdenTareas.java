package com.chi2.tareas.tarea;

import java.util.Comparator;

/** Orden de todas las vistas (§4.3): prioridad (alta primero), fecha (sin fecha al final) y antigüedad. */
public final class OrdenTareas {

	public static final Comparator<Tarea> POR_PRIORIDAD = Comparator
			.comparing(Tarea::getPrioridad)
			.thenComparing(Tarea::getFechaReferencia, Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(Tarea::getCreadaEn);

	private OrdenTareas() {
	}

}
