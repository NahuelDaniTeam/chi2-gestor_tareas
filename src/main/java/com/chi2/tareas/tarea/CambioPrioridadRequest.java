package com.chi2.tareas.tarea;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de {@code PATCH /api/tareas/{id}/prioridad}. */
public record CambioPrioridadRequest(
		@NotNull(message = "Elegí una prioridad: ALTA, MEDIA o BAJA.")
		Prioridad prioridad) {
}
