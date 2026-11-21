package com.chi2.tareas.respaldo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

/**
 * Respaldo diario automático (§4.15): un JSON por día en {@code chi2.respaldos.carpeta}, conservando los últimos
 * {@code chi2.respaldos.conservar}. Se revisa al arrancar y cada hora (en una PC personal no hay hora fija segura).
 */
@Component
public class RespaldoAutomatico {

	private static final Logger log = LoggerFactory.getLogger(RespaldoAutomatico.class);
	private static final String PATRON = "tareas-*.json";

	private final RespaldoService service;
	private final RespaldosProperties properties;
	private final JsonMapper json;

	public RespaldoAutomatico(RespaldoService service, RespaldosProperties properties, JsonMapper json) {
		this.service = service;
		this.properties = properties;
		this.json = json;
	}

	@EventListener(ApplicationReadyEvent.class)
	@Scheduled(cron = "0 15 * * * *")
	public void respaldarSiHaceFalta() {
		if (!properties.automaticos()) {
			return;
		}
		try {
			respaldarHoy();
		} catch (RuntimeException e) {
			log.warn("No se pudo guardar el respaldo automático en {}: {}", properties.carpeta(), e.getMessage());
		}
	}

	/**
	 * Escribe el respaldo de hoy si todavía no existe y borra los que sobran.
	 *
	 * @return el archivo de hoy
	 */
	public Path respaldarHoy() {
		try {
			Files.createDirectories(properties.carpeta());
			Path archivo = properties.carpeta().resolve(service.nombreArchivo("json"));
			if (Files.notExists(archivo)) {
				Path temporal = Files.createTempFile(properties.carpeta(), "tareas-", ".tmp");
				json.writerWithDefaultPrettyPrinter().writeValue(temporal.toFile(), service.respaldo());
				Files.move(temporal, archivo);
				log.info("Respaldo guardado: {}", archivo);
			}
			borrarViejos();
			return archivo;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void borrarViejos() throws IOException {
		List<Path> respaldos;
		try (Stream<Path> archivos = Files.list(properties.carpeta())) {
			respaldos = archivos.filter(p -> p.getFileSystem().getPathMatcher("glob:" + PATRON).matches(p.getFileName()))
					.sorted(Comparator.comparing(Path::getFileName).reversed())
					.toList();
		}
		for (Path viejo : respaldos.stream().skip(properties.conservar()).toList()) {
			Files.deleteIfExists(viejo);
		}
	}

}
