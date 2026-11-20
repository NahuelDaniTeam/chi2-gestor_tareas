package com.chi2.tareas.proyecto;

/** Ya hay un proyecto con ese nombre (sin distinguir mayúsculas). */
public class ProyectoDuplicadoException extends RuntimeException {

	public ProyectoDuplicadoException(String nombre) {
		super("Ya tenés un proyecto llamado \"" + nombre + "\".");
	}

}
