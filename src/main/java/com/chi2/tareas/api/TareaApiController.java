package com.chi2.tareas.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.chi2.tareas.tarea.CambioEstadoRequest;
import com.chi2.tareas.tarea.CambioPrioridadRequest;
import com.chi2.tareas.tarea.EstadoTarea;
import com.chi2.tareas.tarea.Horizonte;
import com.chi2.tareas.tarea.Prioridad;
import com.chi2.tareas.tarea.TareaRequest;
import com.chi2.tareas.tarea.TareaResponse;
import com.chi2.tareas.tarea.TareaService;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /api/tareas} (docs/01-especificacion.md §8). La interfaz web consume esta misma API;
 * una app nativa futura también.
 */
@RestController
@RequestMapping("/api/tareas")
public class TareaApiController {

	private final TareaService service;

	public TareaApiController(TareaService service) {
		this.service = service;
	}

	/** Tareas de un horizonte en el período de {@code fecha} (hoy si falta), ordenadas por prioridad. */
	@GetMapping
	public List<TareaResponse> listar(@RequestParam Horizonte horizonte,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			@RequestParam(required = false) Prioridad prioridad,
			@RequestParam(required = false) EstadoTarea estado) {
		return service.listar(horizonte, fecha, prioridad, estado);
	}

	/** Vista Hoy: las del día más las vencidas sin terminar. */
	@GetMapping("/hoy")
	public List<TareaResponse> hoy(@RequestParam(required = false) Prioridad prioridad,
			@RequestParam(required = false) EstadoTarea estado) {
		return service.hoy(prioridad, estado);
	}

	@GetMapping("/{id}")
	public TareaResponse obtener(@PathVariable UUID id) {
		return service.obtener(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TareaResponse crear(@Valid @RequestBody TareaRequest tarea) {
		return service.crear(tarea);
	}

	@PutMapping("/{id}")
	public TareaResponse editar(@PathVariable UUID id, @Valid @RequestBody TareaRequest tarea) {
		return service.editar(id, tarea);
	}

	@PatchMapping("/{id}/estado")
	public TareaResponse cambiarEstado(@PathVariable UUID id, @Valid @RequestBody CambioEstadoRequest cambio) {
		return service.cambiarEstado(id, cambio.estado());
	}

	@PatchMapping("/{id}/prioridad")
	public TareaResponse cambiarPrioridad(@PathVariable UUID id, @Valid @RequestBody CambioPrioridadRequest cambio) {
		return service.cambiarPrioridad(id, cambio.prioridad());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminar(@PathVariable UUID id) {
		service.eliminar(id);
	}

}
