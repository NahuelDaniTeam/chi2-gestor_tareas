package com.chi2.tareas.tarea;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de {@code PATCH /api/tareas/{id}/estado}: lo usa el arrastre del tablero (§4.4). */
public record CambioEstadoRequest(
		@NotNull(message = "Elegí un estado: PENDIENTE, EN_CURSO o FINALIZADA.")
		EstadoTarea estado) {
}
