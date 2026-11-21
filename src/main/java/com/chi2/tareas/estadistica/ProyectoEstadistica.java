package com.chi2.tareas.estadistica;

import java.util.UUID;

/** Avance de un proyecto: finalizadas en los últimos 30 días y pendientes. {@code id} nulo = sin proyecto. */
public record ProyectoEstadistica(UUID id, String nombre, String hex, int completadas30Dias, int pendientes) {
}
