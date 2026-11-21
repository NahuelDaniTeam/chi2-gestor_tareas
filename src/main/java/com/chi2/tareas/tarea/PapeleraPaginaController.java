package com.chi2.tareas.tarea;

import com.chi2.tareas.TareasProperties;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Página de la papelera (§4.8). Las tareas las trae el navegador desde {@code /api/papelera}. */
@Controller
public class PapeleraPaginaController {

	private final TareasProperties properties;

	public PapeleraPaginaController(TareasProperties properties) {
		this.properties = properties;
	}

	@GetMapping("/papelera")
	public String papelera(Model model) {
		model.addAttribute("diasRetencion", properties.retencionPapelera().toDays());
		return "papelera";
	}

}
