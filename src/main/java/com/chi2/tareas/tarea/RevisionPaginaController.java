package com.chi2.tareas.tarea;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Revisión semanal (§4.12): reprogramar lo vencido, planificar lo que no tiene plazo y mirar la semana que viene.
 * Las tareas las trae el navegador desde la API.
 */
@Controller
public class RevisionPaginaController {

	private final Calendario calendario;

	public RevisionPaginaController(Calendario calendario) {
		this.calendario = calendario;
	}

	@GetMapping("/revision")
	public String revision(Model model) {
		LocalDate hoy = calendario.hoy();
		Periodo proxima = calendario.semana(hoy.plusWeeks(1));
		model.addAttribute("hoy", hoy);
		model.addAttribute("manana", hoy.plusDays(1));
		model.addAttribute("proximaSemana", proxima);
		model.addAttribute("urlProximaSemana", Vista.SEMANA.rutaPara(proxima.fecha()));
		return "revision";
	}

}
