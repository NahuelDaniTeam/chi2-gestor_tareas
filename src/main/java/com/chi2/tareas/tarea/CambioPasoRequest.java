package com.chi2.tareas.tarea;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PATCH /api/tareas/{id}/pasos/{pasoId}}: cambia lo que venga.
 *
 * @param texto si viene, no puede quedar vacío
 */
public record CambioPasoRequest(
		@Size(max = 200, message = "Un paso puede tener hasta 200 caracteres.")
		@Pattern(regexp = ".*\\S.*", message = "Escribí el paso.")
		String texto,
		Boolean hecho) {

	public CambioPasoRequest {
		texto = texto == null ? null : texto.strip();
	}

}
