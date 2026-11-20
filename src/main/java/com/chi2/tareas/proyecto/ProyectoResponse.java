package com.chi2.tareas.proyecto;

import java.util.UUID;

/** Proyecto tal como lo ven la web y la API; {@code hex} es el color listo para pintar. */
public record ProyectoResponse(UUID id, String nombre, ColorProyecto color, String hex) {

	static ProyectoResponse de(Proyecto proyecto) {
		return new ProyectoResponse(proyecto.getId(), proyecto.getNombre(), proyecto.getColor(),
				proyecto.getColor().getHex());
	}

}
