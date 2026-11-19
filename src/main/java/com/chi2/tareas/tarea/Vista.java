package com.chi2.tareas.tarea;

import java.time.LocalDate;

/** Vistas de planificación (§4.3), con su ruta en la web y el horizonte que muestran. */
public enum Vista {

	HOY("Hoy", "/", Horizonte.DIA),
	SEMANA("Semana", "/semana", Horizonte.SEMANA),
	MES("Mes", "/mes", Horizonte.MES),
	LARGO_PLAZO("Largo plazo", "/largo-plazo", Horizonte.LARGO_PLAZO);

	private final String etiqueta;
	private final String ruta;
	private final Horizonte horizonte;

	Vista(String etiqueta, String ruta, Horizonte horizonte) {
		this.etiqueta = etiqueta;
		this.ruta = ruta;
		this.horizonte = horizonte;
	}

	/** Endpoint de la API que trae las tareas de esta vista para el período elegido. */
	public String urlLista(Periodo periodo) {
		return switch (this) {
			case HOY -> "/api/tareas/hoy";
			case LARGO_PLAZO -> "/api/tareas?horizonte=LARGO_PLAZO";
			case SEMANA, MES -> "/api/tareas?horizonte=" + horizonte + "&fecha=" + periodo.fecha();
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

	public Horizonte getHorizonte() {
		return horizonte;
	}

}
