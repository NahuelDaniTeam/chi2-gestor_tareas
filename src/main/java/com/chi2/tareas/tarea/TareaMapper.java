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
		tarea.asignarProyecto(request.proyectoId(), ahora);
		return tarea;
	}

	public void aplicar(TareaRequest request, LocalDate fecha, Tarea tarea, Instant ahora) {
		tarea.editar(request.titulo(), request.descripcion(), request.horizonte(), fecha, request.prioridad(), ahora);
		tarea.asignarProyecto(request.proyectoId(), ahora);
	}

	public TareaResponse aResponse(Tarea tarea, LocalDate hoy) {
		return new TareaResponse(tarea.getId(), tarea.getTitulo(), tarea.getDescripcion(), tarea.getHorizonte(),
				tarea.getFechaReferencia(), tarea.getPrioridad(), tarea.getEstado(), tarea.getAmbito(), tarea.getProyectoId(),
				tarea.estaVencida(hoy), tarea.getCreadaEn(), tarea.getActualizadaEn(), tarea.getCompletadaEn());
	}

}
