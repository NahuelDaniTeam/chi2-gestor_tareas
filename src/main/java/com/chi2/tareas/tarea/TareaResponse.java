package com.chi2.tareas.tarea;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Tarea tal como la ven la web y la API.
 *
 * @param vencida sin terminar y con su período o fecha límite ya pasado
 * @param pasos checklist, en orden
 * @param eliminadaEn solo en la papelera
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
		UUID proyectoId,
		boolean vencida,
		Instant creadaEn,
		Instant actualizadaEn,
		Instant completadaEn,
		List<PasoResponse> pasos,
		Instant eliminadaEn) {
}
