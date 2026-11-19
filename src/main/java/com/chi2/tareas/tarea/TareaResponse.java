package com.chi2.tareas.tarea;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Tarea tal como la ven la web y la API.
 *
 * @param vencida sin terminar y con su período o fecha límite ya pasado
 */
public record TareaResponse(
		UUID id,
		String titulo,
		String descripcion,
		Horizonte horizonte,
		LocalDate fechaReferencia,
		Prioridad prioridad,
		EstadoTarea estado,
		Ambito ambito,
		boolean vencida,
		Instant creadaEn,
		Instant actualizadaEn,
		Instant completadaEn) {
}
