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
		return mostrar(Vista.HOY, calendario.hoyComoPeriodo(), model);
	}

	@GetMapping("/semana")
	public String semana(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			Model model) {
		return mostrar(Vista.SEMANA, calendario.semana(fechaOHoy(fecha)), model);
	}

	@GetMapping("/mes")
	public String mes(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			Model model) {
		return mostrar(Vista.MES, calendario.mes(fechaOHoy(fecha)), model);
	}

	@GetMapping("/largo-plazo")
	public String largoPlazo(Model model) {
		return mostrar(Vista.LARGO_PLAZO, calendario.largoPlazo(), model);
	}

	@GetMapping("/por-planificar")
	public String porPlanificar(Model model) {
		return mostrar(Vista.POR_PLANIFICAR, calendario.porPlanificar(), model);
	}

	private String mostrar(Vista vista, Periodo periodo, Model model) {
		LocalDate hoy = calendario.hoy();
		boolean navega = periodo.tieneNavegacion();
		boolean esElActual = navega && periodo.rango().desde().compareTo(hoy) <= 0
				&& periodo.rango().hasta().compareTo(hoy) >= 0;
		model.addAttribute("vista", vista);
		model.addAttribute("periodo", periodo);
		model.addAttribute("urlLista", vista.urlLista(periodo));
		model.addAttribute("urlAnterior", navega ? vista.rutaPara(periodo.anterior()) : null);
		model.addAttribute("urlSiguiente", navega ? vista.rutaPara(periodo.siguiente()) : null);
		model.addAttribute("urlActual", navega && !esElActual ? vista.getRuta() : null);
		model.addAttribute("hoy", hoy);
		// El alta toma hoy si cae dentro de lo que se está viendo; si no, el inicio del período.
		boolean hoyEnLaVista = periodo.rango() != null && periodo.rango().incluye(hoy);
		model.addAttribute("fechaAlta", hoyEnLaVista || periodo.fecha() == null ? hoy : periodo.fecha());
		model.addAttribute("horizontes", Horizonte.values());
		model.addAttribute("prioridades", Prioridad.values());
		model.addAttribute("estados", EstadoTarea.values());
		return "tareas";
	}

	private LocalDate fechaOHoy(LocalDate fecha) {
		return fecha != null ? fecha : calendario.hoy();
	}

}
