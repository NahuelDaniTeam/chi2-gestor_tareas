package com.chi2.tareas.api;

import com.chi2.tareas.estadistica.EstadisticasResponse;
import com.chi2.tareas.estadistica.EstadisticasService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/estadisticas} (§4.14). */
@RestController
public class EstadisticasApiController {

	private final EstadisticasService service;

	public EstadisticasApiController(EstadisticasService service) {
		this.service = service;
	}

	@GetMapping("/api/estadisticas")
	public EstadisticasResponse estadisticas() {
		return service.calcular();
	}

}
