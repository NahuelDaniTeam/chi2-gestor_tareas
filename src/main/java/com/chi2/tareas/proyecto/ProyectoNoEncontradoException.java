package com.chi2.tareas.proyecto;

import java.util.UUID;

/** El proyecto no existe o no es del usuario actual. */
public class ProyectoNoEncontradoException extends RuntimeException {

	public static final String MENSAJE = "No encontramos ese proyecto.";

	public ProyectoNoEncontradoException(UUID id) {
		super(MENSAJE + " (" + id + ")");
	}

}
