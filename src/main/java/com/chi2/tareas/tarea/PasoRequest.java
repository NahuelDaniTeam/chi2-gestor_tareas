package com.chi2.tareas.tarea;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/tareas/{id}/pasos}. */
public record PasoRequest(
		@NotBlank(message = "Escribí el paso.")
		@Size(max = 200, message = "Un paso puede tener hasta 200 caracteres.")
		String texto) {

	public PasoRequest {
		texto = texto == null ? null : texto.strip();
	}

}
