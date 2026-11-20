package com.chi2.tareas.tarea;

import java.time.LocalDate;

/**
 * Vistas de planificación (§4.3), en el orden de la navegación. Cada una sabe su ruta, con qué plazo
 * guarda el alta rápida y de qué endpoint trae las tareas.
 */
public enum Vista {

	HOY("Hoy", "/", Horizonte.DIA, false),
	SEMANA("Semana", "/semana", Horizonte.SEMANA, true),
	MES("Mes", "/mes", Horizonte.MES, true),
	LARGO_PLAZO("Largo plazo", "/largo-plazo", Horizonte.SIN_PLAZO, false),
	POR_PLANIFICAR("Por planificar", "/por-planificar", Horizonte.SIN_PLAZO, false);

	private final String etiqueta;
	private final String ruta;
	private final Horizonte plazoAlta;
	private final boolean conCalendario;

	Vista(String etiqueta, String ruta, Horizonte plazoAlta, boolean conCalendario) {
		this.etiqueta = etiqueta;
		this.ruta = ruta;
		this.plazoAlta = plazoAlta;
		this.conCalendario = conCalendario;
	}

	/** Endpoint de la API con las tareas de esta vista para el período elegido. */
	public String urlLista(Periodo periodo) {
		return switch (this) {
			case HOY -> "/api/tareas/hoy";
			case POR_PLANIFICAR -> "/api/tareas/sin-plazo";
			default -> {
				Rango rango = periodo.rango();
				yield "/api/tareas?desde=" + rango.desde() + (rango.hasta() == null ? "" : "&hasta=" + rango.hasta());
			}
		};
	}

	/** Ruta de esta vista para otro período (navegación anterior / siguiente). */
	public String rutaPara(LocalDate fecha) {
		return fecha == null ? ruta : ruta + "?fecha=" + fecha;
	}

	public String getEtiqueta() {
		return etiqueta;
	}

	public String getRuta() {
		return ruta;
	}

	public Horizonte getPlazoAlta() {
		return plazoAlta;
	}

	public boolean isConCalendario() {
		return conCalendario;
	}

}
