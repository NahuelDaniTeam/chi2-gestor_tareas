package com.chi2.tareas.tarea;

import java.time.LocalDate;

/**
 * Lo que muestra una vista: desde qué fecha, un título legible, los períodos vecinos para navegar
 * y el rango de fechas que abarca.
 *
 * @param fecha fecha de referencia de la vista (la que toma el alta rápida); {@code null} en Por planificar
 * @param rango {@code null} en Por planificar, que no tiene fechas
 */
public record Periodo(LocalDate fecha, String titulo, LocalDate anterior, LocalDate siguiente, Rango rango) {

	public boolean tieneNavegacion() {
		return anterior != null;
	}

}
