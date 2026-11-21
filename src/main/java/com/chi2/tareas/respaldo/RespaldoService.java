package com.chi2.tareas.respaldo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.chi2.tareas.TareasProperties;
import com.chi2.tareas.proyecto.ProyectoResponse;
import com.chi2.tareas.proyecto.ProyectoService;
import com.chi2.tareas.tarea.TareaResponse;
import com.chi2.tareas.tarea.TareaService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Arma los respaldos y las exportaciones (§4.15). No escribe archivos: eso es de {@link RespaldoAutomatico}. */
@Service
public class RespaldoService {

	/** Excel en castellano espera punto y coma y lee bien el UTF-8 con BOM. */
	private static final String SEPARADOR = ";";
	private static final String BOM = "\uFEFF";
	private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final List<String> COLUMNAS = List.of("Título", "Descripción", "Plazo", "Fecha", "Prioridad", "Estado",
			"Proyecto", "Pasos hechos", "Pasos", "Repetición", "Creada", "Finalizada");

	private final TareaService tareas;
	private final ProyectoService proyectos;
	private final Clock clock;
	private final String version;
	private final ZoneId zona;

	public RespaldoService(TareaService tareas, ProyectoService proyectos, Clock clock,
			TareasProperties properties, @Value("${info.app.version:dev}") String version) {
		this.tareas = tareas;
		this.proyectos = proyectos;
		this.clock = clock;
		this.version = version;
		this.zona = properties.zona();
	}

	public Respaldo respaldo() {
		return new Respaldo(version, clock.instant(), proyectos.listar(), tareas.todas());
	}

	/** Las tareas en CSV, una por renglón, para abrir en Excel o Google Sheets. */
	public String csv() {
		Map<UUID, String> nombres = proyectos.listar().stream()
				.collect(Collectors.toMap(ProyectoResponse::id, ProyectoResponse::nombre));
		StringBuilder csv = new StringBuilder(BOM).append(String.join(SEPARADOR, COLUMNAS)).append("\r\n");
		for (TareaResponse t : tareas.todas()) {
			csv.append(renglon(t, nombres)).append("\r\n");
		}
		return csv.toString();
	}

	private String renglon(TareaResponse t, Map<UUID, String> nombres) {
		long hechos = t.pasos().stream().filter(p -> p.hecho()).count();
		return List.of(
				t.titulo(),
				Objects.toString(t.descripcion(), ""),
				t.horizonte().getEtiqueta(),
				Objects.toString(t.fechaReferencia(), ""),
				t.prioridad().getEtiqueta(),
				t.estado().getEtiqueta(),
				t.proyectoId() == null ? "" : nombres.getOrDefault(t.proyectoId(), ""),
				String.valueOf(hechos),
				String.valueOf(t.pasos().size()),
				t.repeticion().getEtiqueta(),
				fechaHora(t.creadaEn()),
				fechaHora(t.completadaEn())).stream()
				.map(RespaldoService::celda)
				.collect(Collectors.joining(SEPARADOR));
	}

	private String fechaHora(Instant instante) {
		return instante == null ? "" : FECHA_HORA.format(instante.atZone(zona));
	}

	/** Entre comillas si hace falta; las comillas internas se duplican (RFC 4180). */
	static String celda(String valor) {
		boolean necesitaComillas = valor.contains(SEPARADOR) || valor.contains("\"") || valor.contains("\n")
				|| valor.contains("\r");
		return necesitaComillas ? "\"" + valor.replace("\"", "\"\"") + "\"" : valor;
	}

	/** Nombre de archivo con la fecha local: {@code tareas-2026-11-21.json}. */
	public String nombreArchivo(String extension) {
		return "tareas-" + LocalDate.ofInstant(clock.instant(), zona) + "." + extension;
	}

}
