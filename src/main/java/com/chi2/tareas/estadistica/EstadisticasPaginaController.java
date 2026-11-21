package com.chi2.tareas.estadistica;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Página de estadísticas (§4.14). Los números los trae el navegador desde {@code /api/estadisticas}. */
@Controller
public class EstadisticasPaginaController {

	@GetMapping("/estadisticas")
	public String estadisticas() {
		return "estadisticas";
	}

}
