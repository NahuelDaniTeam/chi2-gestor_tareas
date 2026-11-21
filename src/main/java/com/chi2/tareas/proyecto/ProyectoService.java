package com.chi2.tareas.proyecto;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import com.chi2.tareas.usuario.UsuarioActual;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Proyectos del usuario actual (§4.6). */
@Service
@Transactional(readOnly = true)
public class ProyectoService {

	private final ProyectoRepository repository;
	private final UsuarioActual usuario;
	private final Clock clock;

	public ProyectoService(ProyectoRepository repository, UsuarioActual usuario, Clock clock) {
		this.repository = repository;
		this.usuario = usuario;
		this.clock = clock;
	}

	public List<ProyectoResponse> listar() {
		return repository.findByUsuarioIdOrderByNombreAsc(usuario.id()).stream().map(ProyectoResponse::de).toList();
	}

	/** @throws ProyectoDuplicadoException si ya hay uno con ese nombre */
	@Transactional
	public ProyectoResponse crear(ProyectoRequest request) {
		verificarNombreLibre(request.nombre(), null);
		ColorProyecto color = request.color() != null ? request.color()
				: ColorProyecto.siguiente(repository.countByUsuarioId(usuario.id()));
		return ProyectoResponse.de(repository.save(new Proyecto(request.nombre(), color, usuario.id(), clock.instant())));
	}

	@Transactional
	public ProyectoResponse editar(UUID id, ProyectoRequest request) {
		Proyecto proyecto = buscar(id);
		verificarNombreLibre(request.nombre(), id);
		proyecto.editar(request.nombre(), request.color() != null ? request.color() : proyecto.getColor());
		return ProyectoResponse.de(proyecto);
	}

	/** Sus tareas quedan sin proyecto (la base pone {@code proyecto_id} en nulo). */
	@Transactional
	public void eliminar(UUID id) {
		repository.delete(buscar(id));
	}

	/**
	 * Para asignar un proyecto a una tarea: que exista y sea del usuario.
	 *
	 * @throws ProyectoNoEncontradoException si no
	 */
	public void verificar(UUID id) {
		buscar(id);
	}

	private Proyecto buscar(UUID id) {
		return repository.findByIdAndUsuarioId(id, usuario.id()).orElseThrow(() -> new ProyectoNoEncontradoException(id));
	}

	private void verificarNombreLibre(String nombre, UUID propio) {
		repository.findByUsuarioIdAndNombreIgnoreCase(usuario.id(), nombre)
				.filter(otro -> !otro.getId().equals(propio))
				.ifPresent(otro -> {
					throw new ProyectoDuplicadoException(otro.getNombre());
				});
	}

}
