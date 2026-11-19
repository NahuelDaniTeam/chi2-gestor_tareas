package com.chi2.tareas.tarea;

import java.time.LocalDate;

/**
 * Lo que muestra una vista de planificación: su horizonte, desde qué fecha, un título legible
 * y los períodos vecinos para navegar. En largo plazo las fechas son {@code null}.
 */
public record Periodo(Horizonte horizonte, LocalDate fecha, String titulo, LocalDate anterior, LocalDate siguiente) {

	public boolean tieneNavegacion() {
		return anterior != null;
	}

}
