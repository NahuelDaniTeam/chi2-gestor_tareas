package com.chi2.tareas.tarea;

import java.time.Clock;

import com.chi2.tareas.TareasProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Borra de verdad las tareas que llevan más de {@code chi2.tareas.retencion-papelera} en la papelera (§4.8). */
@Service
public class RetencionPapeleraService {

	private static final Logger log = LoggerFactory.getLogger(RetencionPapeleraService.class);

	private final TareaRepository repository;
	private final TareasProperties properties;
	private final Clock clock;

	public RetencionPapeleraService(TareaRepository repository, TareasProperties properties, Clock clock) {
		this.repository = repository;
		this.properties = properties;
		this.clock = clock;
	}

	/** Cada hora: en una PC personal no se puede contar con que la app esté abierta a una hora fija. */
	@Scheduled(cron = "0 0 * * * *")
	@Transactional
	public int purgar() {
		int borradas = repository.purgarEliminadasAntesDe(clock.instant().minus(properties.retencionPapelera()));
		if (borradas > 0) {
			log.info("Papelera: {} tareas borradas definitivamente", borradas);
		}
		return borradas;
	}

}
