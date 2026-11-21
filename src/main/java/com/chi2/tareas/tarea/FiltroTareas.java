package com.chi2.tareas.tarea;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Filtros opcionales de los listados: cada uno, si viene, deja solo las tareas que coinciden.
 *
 * @param proyecto id del proyecto; las tareas sin proyecto no pasan
 */
public record FiltroTareas(Prioridad prioridad, EstadoTarea estado, UUID proyecto) {

	public static final FiltroTareas NINGUNO = new FiltroTareas(null, null, null);

	Predicate<Tarea> comoPredicado() {
		return t -> (prioridad == null || prioridad == t.getPrioridad())
				&& (estado == null || estado == t.getEstado())
				&& (proyecto == null || proyecto.equals(t.getProyectoId()));
	}

}
