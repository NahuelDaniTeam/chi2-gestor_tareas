package com.chi2.tareas.tarea;

import java.util.UUID;

/** El paso no existe en esa tarea. */
public class PasoNoEncontradoException extends RuntimeException {

	public static final String MENSAJE = "No encontramos ese paso.";

	public PasoNoEncontradoException(UUID id) {
		super(MENSAJE + " (" + id + ")");
	}

}
