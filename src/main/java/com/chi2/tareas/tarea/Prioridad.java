package com.chi2.tareas.tarea;

/** Prioridad de una tarea (§4.5). El orden de declaración es el orden en las vistas: alta primero. */
public enum Prioridad {

	ALTA("Alta"),
	MEDIA("Media"),
	BAJA("Baja");

	public static final Prioridad POR_DEFECTO = MEDIA;

	private final String etiqueta;

	Prioridad(String etiqueta) {
		this.etiqueta = etiqueta;
	}

	public String getEtiqueta() {
		return etiqueta;
	}

}
