package com.chi2.tareas.tarea;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta y edición de una tarea ({@code POST} y {@code PUT /api/tareas}).
 *
 * @param fechaReferencia cualquier día del período: se lleva al lunes o al primero del mes según el horizonte.
 *                        Si falta en día, semana o mes, se usa hoy. En largo plazo es la fecha límite, opcional.
 * @param prioridad si falta, {@link Prioridad#POR_DEFECTO}
 */
public record TareaRequest(

		@NotBlank(message = "Escribí un título.")
		@Size(max = 200, message = "El título puede tener hasta 200 caracteres.")
		String titulo,

		@Size(max = 2000, message = "La descripción puede tener hasta 2000 caracteres.")
		String descripcion,

		@NotNull(message = "Elegí un horizonte: DIA, SEMANA, MES o LARGO_PLAZO.")
		Horizonte horizonte,

		LocalDate fechaReferencia,

		Prioridad prioridad) {

	public TareaRequest {
		titulo = titulo == null ? null : titulo.strip();
		descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.strip();
		prioridad = prioridad == null ? Prioridad.POR_DEFECTO : prioridad;
	}

}
