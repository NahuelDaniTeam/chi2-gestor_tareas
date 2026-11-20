package com.chi2.tareas.tarea;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

import org.springframework.stereotype.Component;

/** Qué día es hoy en la zona del usuario, y los períodos que muestran las vistas (§4.3). */
@Component
public class Calendario {

	private static final Locale ES = Locale.of("es", "AR");
	private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES);
	private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("d 'de' MMMM", ES);

	private final Clock clock;
	private final TareasProperties properties;

	public Calendario(Clock clock, TareasProperties properties) {
		this.clock = clock;
		this.properties = properties;
	}

	public LocalDate hoy() {
		return LocalDate.ofInstant(clock.instant(), properties.zona());
	}

	/** Siempre el día actual, sin navegación a otros días. */
	public Periodo hoyComoPeriodo() {
		LocalDate hoy = hoy();
		return new Periodo(hoy, capitalizar(DIA.format(hoy)), null, null, Rango.dia(hoy));
	}

	/** La semana (lunes a domingo) que contiene a {@code fecha}. */
	public Periodo semana(LocalDate fecha) {
		LocalDate lunes = Horizonte.SEMANA.fechaReferencia(fecha);
		return new Periodo(lunes, tituloSemana(lunes), lunes.minusWeeks(1), lunes.plusWeeks(1),
				new Rango(lunes, Horizonte.SEMANA.fin(lunes)));
	}

	/** El mes que contiene a {@code fecha}. */
	public Periodo mes(LocalDate fecha) {
		LocalDate primero = Horizonte.MES.fechaReferencia(fecha);
		return new Periodo(primero, tituloMes(primero), primero.minusMonths(1), primero.plusMonths(1),
				new Rango(primero, Horizonte.MES.fin(primero)));
	}

	/** Todo lo planificado desde el mes que viene en adelante, agrupado por mes. */
	public Periodo largoPlazo() {
		LocalDate proximoMes = Horizonte.MES.fechaReferencia(hoy()).plusMonths(1);
		return new Periodo(proximoMes, "Próximos meses", null, null, Rango.desde(proximoMes));
	}

	/** Tareas sin plazo: no tiene fechas. */
	public Periodo porPlanificar() {
		return new Periodo(null, "Sin plazo todavía", null, null, null);
	}

	/** "Del 16 al 22 de noviembre" o, si cruza de mes, "Del 30 de noviembre al 6 de diciembre". */
	static String tituloSemana(LocalDate lunes) {
		LocalDate domingo = Horizonte.SEMANA.fin(lunes);
		String desde = lunes.getMonth() == domingo.getMonth()
				? String.valueOf(lunes.getDayOfMonth())
				: DIA_MES.format(lunes);
		return "Del " + desde + " al " + DIA_MES.format(domingo);
	}

	static String tituloMes(LocalDate primero) {
		return capitalizar(primero.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, ES)) + " " + primero.getYear();
	}

	private static String capitalizar(String texto) {
		return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
	}

}
