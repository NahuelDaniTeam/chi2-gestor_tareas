package com.chi2.tareas.api;

import java.util.List;
import java.util.UUID;

import com.chi2.tareas.proyecto.ProyectoRequest;
import com.chi2.tareas.proyecto.ProyectoResponse;
import com.chi2.tareas.proyecto.ProyectoService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/proyectos} (§4.6 y §8). */
@RestController
@RequestMapping("/api/proyectos")
public class ProyectoApiController {

	private final ProyectoService service;

	public ProyectoApiController(ProyectoService service) {
		this.service = service;
	}

	@GetMapping
	public List<ProyectoResponse> listar() {
		return service.listar();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProyectoResponse crear(@Valid @RequestBody ProyectoRequest proyecto) {
		return service.crear(proyecto);
	}

	@PutMapping("/{id}")
	public ProyectoResponse editar(@PathVariable UUID id, @Valid @RequestBody ProyectoRequest proyecto) {
		return service.editar(id, proyecto);
	}

	/** Sus tareas quedan sin proyecto. */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminar(@PathVariable UUID id) {
		service.eliminar(id);
	}

}
