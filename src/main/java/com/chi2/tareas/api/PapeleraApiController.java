package com.chi2.tareas.api;

import java.util.List;
import java.util.UUID;

import com.chi2.tareas.tarea.TareaResponse;
import com.chi2.tareas.tarea.TareaService;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/papelera} (§4.8): tareas eliminadas, para restaurarlas o borrarlas de verdad. */
@Tag(name = "Papelera")
@RestController
@RequestMapping("/api/papelera")
public class PapeleraApiController {

	private final TareaService service;

	public PapeleraApiController(TareaService service) {
		this.service = service;
	}

	/** Las eliminadas, la más reciente primero. */
	@GetMapping
	public List<TareaResponse> listar() {
		return service.papelera();
	}

	@PostMapping("/{id}/restaurar")
	public TareaResponse restaurar(@PathVariable UUID id) {
		return service.restaurar(id);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminarDefinitivamente(@PathVariable UUID id) {
		service.eliminarDefinitivamente(id);
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void vaciar() {
		service.vaciarPapelera();
	}

}
