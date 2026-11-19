package com.chi2.tareas.tarea;

import java.util.UUID;

/** La tarea no existe o no es del usuario actual (no se distingue, para no revelar tareas ajenas). */
public class TareaNoEncontradaException extends RuntimeException {

	public static final String MENSAJE = "No encontramos esa tarea.";

	public TareaNoEncontradaException(UUID id) {
		super(MENSAJE + " (" + id + ")");
	}

}
