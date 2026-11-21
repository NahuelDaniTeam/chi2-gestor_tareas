package com.chi2.tareas.tarea;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Component;

/** Entre entidad y DTOs. La fecha ya viene resuelta por el service (hoy si faltaba). */
@Component
public class TareaMapper {

	public Tarea aEntidad(TareaRequest request, LocalDate fecha, UUID usuarioId, Instant ahora) {
		Tarea tarea = new Tarea(request.titulo(), request.descripcion(), request.horizonte(), fecha,
				request.prioridad(), usuarioId, ahora);
		aplicarExtras(request, tarea, ahora);
		return tarea;
	}

	public void aplicar(TareaRequest request, LocalDate fecha, Tarea tarea, Instant ahora) {
		tarea.editar(request.titulo(), request.descripcion(), request.horizonte(), fecha, request.prioridad(), ahora);
		aplicarExtras(request, tarea, ahora);
	}

	private static void aplicarExtras(TareaRequest request, Tarea tarea, Instant ahora) {
		tarea.asignarProyecto(request.proyectoId(), ahora);
		tarea.programarRepeticion(request.repeticion(), ahora);
		tarea.programarRecordatorio(request.recordatorio(), ahora);
	}

	public TareaResponse aResponse(Tarea tarea, LocalDate hoy) {
		return new TareaResponse(tarea.getId(), tarea.getTitulo(), tarea.getDescripcion(), tarea.getHorizonte(),
				tarea.getFechaReferencia(), tarea.getPrioridad(), tarea.getEstado(), tarea.getAmbito(), tarea.getProyectoId(),
				tarea.estaVencida(hoy), tarea.getCreadaEn(), tarea.getActualizadaEn(), tarea.getCompletadaEn(),
				tarea.getPasos().stream().map(p -> new PasoResponse(p.getId(), p.getTexto(), p.isHecho())).toList(),
				tarea.getEliminadaEn(), tarea.getRepeticion(), tarea.proximaRepeticion(), tarea.getRecordatorio());
	}

}
