package com.chi2.tareas.proyecto;

/**
 * Colores de proyecto: apagados, para que convivan con el acento terracota de la marca sin competir.
 * El valor hexadecimal lo usa la interfaz para el punto y la etiqueta.
 */
public enum ColorProyecto {

	TERRACOTA("#b5532c"),
	OCRE("#b8862b"),
	OLIVA("#6f7a3a"),
	PINO("#3e7462"),
	AZUL("#3f6a8f"),
	CIRUELA("#7d4f74"),
	PIZARRA("#5f6670");

	private final String hex;

	ColorProyecto(String hex) {
		this.hex = hex;
	}

	public String getHex() {
		return hex;
	}

	/** Para un proyecto nuevo sin color elegido: va rotando según cuántos hay. */
	public static ColorProyecto siguiente(long cantidadDeProyectos) {
		ColorProyecto[] colores = values();
		return colores[(int) (cantidadDeProyectos % colores.length)];
	}

}
