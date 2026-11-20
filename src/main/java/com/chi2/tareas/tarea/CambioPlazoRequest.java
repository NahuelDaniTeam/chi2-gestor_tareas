package com.chi2.tareas.tarea;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de {@code PATCH /api/tareas/{id}/plazo}.
 *
 * @param fechaReferencia cualquier día del período; si falta y el plazo la exige, hoy. Sin plazo se ignora.
 */
public record CambioPlazoRequest(
		@NotNull(message = "Elegí un plazo: SIN_PLAZO, DIA, SEMANA, MES o LIMITE.")
		Horizonte horizonte,
		LocalDate fechaReferencia) {
}
