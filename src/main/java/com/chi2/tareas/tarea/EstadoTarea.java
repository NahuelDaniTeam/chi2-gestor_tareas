package com.chi2.tareas.tarea;

/** Estado de una tarea: las columnas del tablero (§4.4). */
public enum EstadoTarea {

	PENDIENTE("Pendiente"),
	EN_CURSO("En curso"),
	FINALIZADA("Finalizada");

	private final String etiqueta;

	EstadoTarea(String etiqueta) {
		this.etiqueta = etiqueta;
	}

	public String getEtiqueta() {
		return etiqueta;
	}

}
