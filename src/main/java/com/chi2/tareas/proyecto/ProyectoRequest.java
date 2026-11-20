package com.chi2.tareas.proyecto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alta y edición de un proyecto ({@code POST} y {@code PUT /api/proyectos}).
 *
 * @param color si falta, uno distinto a los ya usados (va rotando)
 */
public record ProyectoRequest(

		@NotBlank(message = "Escribí un nombre.")
		@Size(max = 40, message = "El nombre puede tener hasta 40 caracteres.")
		String nombre,

		ColorProyecto color) {

	public ProyectoRequest {
		nombre = nombre == null ? null : nombre.strip().replaceAll("\s+", " ");
	}

}
