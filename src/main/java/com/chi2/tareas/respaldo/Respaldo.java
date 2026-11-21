package com.chi2.tareas.respaldo;

import java.time.Instant;
import java.util.List;

import com.chi2.tareas.proyecto.ProyectoResponse;
import com.chi2.tareas.tarea.TareaResponse;

/** Todo lo del usuario en un solo documento JSON (§4.15): proyectos y tareas, con sus pasos. */
public record Respaldo(String version, Instant generadoEn, List<ProyectoResponse> proyectos, List<TareaResponse> tareas) {
}
