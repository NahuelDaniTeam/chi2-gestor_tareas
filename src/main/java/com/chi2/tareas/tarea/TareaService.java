package com.chi2.tareas.tarea;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.chi2.tareas.proyecto.ProyectoNoEncontradoException;
import com.chi2.tareas.proyecto.ProyectoService;
import com.chi2.tareas.usuario.UsuarioActual;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Única entrada de la lógica de tareas, para la web y la API. En la etapa 1 todas las tareas
 * son personales del {@link UsuarioActual}.
 */
@Service
@Transactional(readOnly = true)
public class TareaService {

	private final TareaRepository repository;
	private final TareaMapper mapper;
	private final UsuarioActual usuario;
	private final Calendario calendario;
	private final ProyectoService proyectos;
	private final Clock clock;

	public TareaService(TareaRepository repository, TareaMapper mapper, UsuarioActual usuario, Calendario calendario,
			ProyectoService proyectos, Clock clock) {
		this.proyectos = proyectos;
		this.repository = repository;
		this.mapper = mapper;
		this.usuario = usuario;
		this.calendario = calendario;
		this.clock = clock;
	}

	/**
	 * Tareas planificadas dentro de un rango (§4.3): las de sus días, sus semanas y sus meses,
	 * según {@link Rango#incluye}.
	 */
	public List<TareaResponse> listar(Rango rango, FiltroTareas filtro) {
		List<Tarea> candidatas = rango.hasta() == null
				? repository.findByUsuarioIdAndAmbitoAndFechaReferenciaGreaterThanEqual(usuario.id(), Ambito.PERSONAL,
						rango.buscarDesde())
				: repository.findByUsuarioIdAndAmbitoAndFechaReferenciaBetween(usuario.id(), Ambito.PERSONAL,
						rango.buscarDesde(), rango.hasta());
		return responder(candidatas.stream().filter(rango::incluye), filtro);
	}

	/** Tareas creadas que todavía no tienen plazo ("Por planificar"). */
	public List<TareaResponse> sinPlazo(FiltroTareas filtro) {
		return responder(repository.findByUsuarioIdAndAmbitoAndHorizonte(usuario.id(), Ambito.PERSONAL,
				Horizonte.SIN_PLAZO).stream(), filtro);
	}

	/**
	 * Vista Hoy (§4.3): las del día y las que vencen hoy, más todas las vencidas sin terminar,
	 * de cualquier plazo.
	 */
	public List<TareaResponse> hoy(FiltroTareas filtro) {
		LocalDate hoy = calendario.hoy();
		Rango deHoy = Rango.dia(hoy);
		Stream<Tarea> delDia = repository.findByUsuarioIdAndAmbitoAndFechaReferenciaBetween(usuario.id(),
				Ambito.PERSONAL, hoy, hoy).stream().filter(deHoy::incluye);
		Stream<Tarea> vencidas = repository.findByUsuarioIdAndAmbitoAndEstadoNotAndFechaReferenciaBefore(usuario.id(),
				Ambito.PERSONAL, EstadoTarea.FINALIZADA, hoy).stream().filter(t -> t.estaVencida(hoy));
		return responder(Stream.concat(delDia, vencidas), filtro);
	}

	public TareaResponse obtener(UUID id) {
		return mapper.aResponse(buscar(id), calendario.hoy());
	}

	@Transactional
	public TareaResponse crear(TareaRequest request) {
		verificarProyecto(request.proyectoId());
		Tarea tarea = mapper.aEntidad(request, fechaSegun(request.horizonte(), request.fechaReferencia()),
				usuario.id(), clock.instant());
		return mapper.aResponse(repository.save(tarea), calendario.hoy());
	}

	@Transactional
	public TareaResponse editar(UUID id, TareaRequest request) {
		Tarea tarea = buscar(id);
		verificarProyecto(request.proyectoId());
		mapper.aplicar(request, fechaSegun(request.horizonte(), request.fechaReferencia()), tarea, clock.instant());
		return mapper.aResponse(tarea, calendario.hoy());
	}

	/** Planificar o reprogramar: lo usan el arrastre entre días, la bandeja y la revisión semanal. */
	@Transactional
	public TareaResponse cambiarPlazo(UUID id, Horizonte horizonte, LocalDate fecha) {
		Tarea tarea = buscar(id);
		tarea.cambiarPlazo(horizonte, fechaSegun(horizonte, fecha), clock.instant());
		return mapper.aResponse(tarea, calendario.hoy());
	}

	@Transactional
	public TareaResponse cambiarEstado(UUID id, EstadoTarea estado) {
		Tarea tarea = buscar(id);
		tarea.cambiarEstado(estado, clock.instant());
		return mapper.aResponse(tarea, calendario.hoy());
	}

	@Transactional
	public TareaResponse cambiarPrioridad(UUID id, Prioridad prioridad) {
		Tarea tarea = buscar(id);
		tarea.cambiarPrioridad(prioridad, clock.instant());
		return mapper.aResponse(tarea, calendario.hoy());
	}

	// ---------- Checklist (§4.7) ----------

	@Transactional
	public TareaResponse agregarPaso(UUID id, String texto) {
		Tarea tarea = buscar(id);
		tarea.agregarPaso(texto, clock.instant());
		return mapper.aResponse(tarea, calendario.hoy());
	}

	@Transactional
	public TareaResponse editarPaso(UUID id, UUID pasoId, String texto, Boolean hecho) {
		Tarea tarea = buscar(id);
		tarea.editarPaso(pasoId, texto, hecho, clock.instant());
		return mapper.aResponse(tarea, calendario.hoy());
	}

	@Transactional
	public TareaResponse eliminarPaso(UUID id, UUID pasoId) {
		Tarea tarea = buscar(id);
		tarea.eliminarPaso(pasoId, clock.instant());
		return mapper.aResponse(tarea, calendario.hoy());
	}

	// ---------- Papelera (§4.8) ----------

	/** La manda a la papelera: se puede restaurar hasta que se vacíe o pasen los días de retención. */
	@Transactional
	public void eliminar(UUID id) {
		buscar(id).enviarAPapelera(clock.instant());
	}

	public List<TareaResponse> papelera() {
		LocalDate hoy = calendario.hoy();
		return repository.papelera(usuario.id()).stream().map(t -> mapper.aResponse(t, hoy)).toList();
	}

	@Transactional
	public TareaResponse restaurar(UUID id) {
		if (repository.restaurar(id, usuario.id(), clock.instant()) == 0) {
			throw new TareaNoEncontradaException(id);
		}
		return obtener(id);
	}

	@Transactional
	public void eliminarDefinitivamente(UUID id) {
		if (repository.eliminarDefinitivamente(id, usuario.id()) == 0) {
			throw new TareaNoEncontradaException(id);
		}
	}

	@Transactional
	public void vaciarPapelera() {
		repository.vaciarPapelera(usuario.id());
	}

	private Tarea buscar(UUID id) {
		return repository.findByIdAndUsuarioIdAndAmbito(id, usuario.id(), Ambito.PERSONAL)
				.orElseThrow(() -> new TareaNoEncontradaException(id));
	}

	private void verificarProyecto(UUID proyectoId) {
		if (proyectoId == null) {
			return;
		}
		try {
			proyectos.verificar(proyectoId);
		} catch (ProyectoNoEncontradoException e) {
			throw new IllegalArgumentException("El proyecto elegido no existe.");
		}
	}

	/** Con plazo, siempre hay fecha (hoy si no vino); sin plazo, nunca. */
	private LocalDate fechaSegun(Horizonte horizonte, LocalDate fecha) {
		if (!horizonte.exigeFecha()) {
			return null;
		}
		return fecha != null ? fecha : calendario.hoy();
	}

	private List<TareaResponse> responder(Stream<Tarea> tareas, FiltroTareas filtro) {
		LocalDate hoy = calendario.hoy();
		return tareas
				.filter(filtro.comoPredicado())
				.sorted(OrdenTareas.POR_PRIORIDAD)
				.map(t -> mapper.aResponse(t, hoy))
				.toList();
	}

}
