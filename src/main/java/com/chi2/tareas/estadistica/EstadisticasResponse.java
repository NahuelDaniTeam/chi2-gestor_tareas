package com.chi2.tareas.estadistica;

import java.util.List;

/**
 * Estadísticas del usuario (§4.14).
 *
 * @param rachaDias días seguidos con al menos una tarea finalizada
 * @param porSemana finalizadas por semana, las últimas 8 (de la más vieja a la actual)
 */
public record EstadisticasResponse(
		int completadasEstaSemana,
		int completadasSemanaPasada,
		int rachaDias,
		int pendientes,
		int vencidas,
		List<SemanaCompletadas> porSemana,
		List<ProyectoEstadistica> porProyecto) {
}
