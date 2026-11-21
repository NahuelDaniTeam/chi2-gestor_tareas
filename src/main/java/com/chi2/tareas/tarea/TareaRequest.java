package com.chi2.tareas.tarea;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta y edición de una tarea ({@code POST} y {@code PUT /api/tareas}).
 *
 * @param fechaReferencia cualquier día del período: se lleva al lunes o al primero del mes según el plazo.
 *                        Si falta y el plazo la exige, se usa hoy. Sin plazo se ignora.
 * @param prioridad si falta, {@link Prioridad#POR_DEFECTO}
 * @param proyectoId opcional; tiene que ser un proyecto del usuario
 * @param repeticion si falta, no se repite
 * @param recordatorio momento del aviso, opcional
 */
public record TareaRequest(

		@NotBlank(message = "Escribí un título.")
		@Size(max = 200, message = "El título puede tener hasta 200 caracteres.")
		String titulo,

		@Size(max = 2000, message = "La descripción puede tener hasta 2000 caracteres.")
		String descripcion,

		@NotNull(message = "Elegí un plazo: SIN_PLAZO, DIA, SEMANA, MES o LIMITE.")
		Horizonte horizonte,

		LocalDate fechaReferencia,

		Prioridad prioridad,

		UUID proyectoId,

		Repeticion repeticion,

		Instant recordatorio) {

	public TareaRequest {
		titulo = titulo == null ? null : titulo.strip();
		descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.strip();
		prioridad = prioridad == null ? Prioridad.POR_DEFECTO : prioridad;
		repeticion = repeticion == null ? Repeticion.NINGUNA : repeticion;
	}

}
