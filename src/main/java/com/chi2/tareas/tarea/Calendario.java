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

	/** La vista Hoy: siempre el día actual, sin navegación a otros días. */
	public Periodo periodoHoy() {
		LocalDate hoy = hoy();
		return new Periodo(Horizonte.DIA, hoy, periodo(Horizonte.DIA, hoy).titulo(), null, null);
	}

	/** Período del horizonte que contiene a {@code fecha}, con el anterior y el siguiente. */
	public Periodo periodo(Horizonte horizonte, LocalDate fecha) {
		LocalDate inicio = horizonte.fechaReferencia(fecha);
		return switch (horizonte) {
			case DIA -> new Periodo(horizonte, inicio, capitalizar(DIA.format(inicio)),
					inicio.minusDays(1), inicio.plusDays(1));
			case SEMANA -> new Periodo(horizonte, inicio, tituloSemana(inicio),
					inicio.minusWeeks(1), inicio.plusWeeks(1));
			case MES -> new Periodo(horizonte, inicio, tituloMes(inicio),
					inicio.minusMonths(1), inicio.plusMonths(1));
			case LARGO_PLAZO -> new Periodo(horizonte, null, "Sin fecha fija", null, null);
		};
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
