package com.chi2.tareas.tarea;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Páginas de las vistas de planificación. Arman el marco (navegación, período, alta rápida);
 * las tareas las trae el navegador desde {@code /api/tareas}.
 */
@Controller
public class TareaPaginaController {

	private final Calendario calendario;

	public TareaPaginaController(Calendario calendario) {
		this.calendario = calendario;
	}

	/** Al abrir la aplicación se ve Hoy (§3). */
	@GetMapping("/")
	public String hoy(Model model) {
		return mostrar(Vista.HOY, calendario.periodoHoy(), model);
	}

	@GetMapping("/semana")
	public String semana(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			Model model) {
		return mostrar(Vista.SEMANA, calendario.periodo(Horizonte.SEMANA, fechaOHoy(fecha)), model);
	}

	@GetMapping("/mes")
	public String mes(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			Model model) {
		return mostrar(Vista.MES, calendario.periodo(Horizonte.MES, fechaOHoy(fecha)), model);
	}

	@GetMapping("/largo-plazo")
	public String largoPlazo(Model model) {
		return mostrar(Vista.LARGO_PLAZO, calendario.periodo(Horizonte.LARGO_PLAZO, null), model);
	}

	private String mostrar(Vista vista, Periodo periodo, Model model) {
		LocalDate hoy = calendario.hoy();
		LocalDate periodoActual = vista.getHorizonte().fechaReferencia(hoy);
		model.addAttribute("vista", vista);
		model.addAttribute("vistas", Vista.values());
		model.addAttribute("periodo", periodo);
		model.addAttribute("urlLista", vista.urlLista(periodo));
		model.addAttribute("urlAnterior", periodo.tieneNavegacion() ? vista.rutaPara(periodo.anterior()) : null);
		model.addAttribute("urlSiguiente", periodo.tieneNavegacion() ? vista.rutaPara(periodo.siguiente()) : null);
		model.addAttribute("urlActual", periodo.tieneNavegacion() && !periodo.fecha().equals(periodoActual)
				? vista.getRuta() : null);
		model.addAttribute("hoy", hoy);
		model.addAttribute("fechaAlta", periodo.fecha());
		model.addAttribute("horizontes", Horizonte.values());
		model.addAttribute("prioridades", Prioridad.values());
		model.addAttribute("estados", EstadoTarea.values());
		return "tareas";
	}

	private LocalDate fechaOHoy(LocalDate fecha) {
		return fecha != null ? fecha : calendario.hoy();
	}

}
