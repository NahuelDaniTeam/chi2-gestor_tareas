package com.chi2.tareas.tarea;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
	private final Clock clock;

	public TareaService(TareaRepository repository, TareaMapper mapper, UsuarioActual usuario, Calendario calendario,
			Clock clock) {
		this.repository = repository;
		this.mapper = mapper;
		this.usuario = usuario;
		this.calendario = calendario;
		this.clock = clock;
	}

	/**
	 * Tareas planificadas dentro de un rango (§4.3): las de sus días, sus semanas y sus meses,
	 * según {@link Rango#incluye}. {@code prioridad} y {@code estado} filtran si vienen.
	 */
	public List<TareaResponse> listar(Rango rango, Prioridad prioridad, EstadoTarea estado) {
		List<Tarea> candidatas = rango.hasta() == null
				? repository.findByUsuarioIdAndAmbitoAndFechaReferenciaGreaterThanEqual(usuario.id(), Ambito.PERSONAL,
						rango.buscarDesde())
				: repository.findByUsuarioIdAndAmbitoAndFechaReferenciaBetween(usuario.id(), Ambito.PERSONAL,
						rango.buscarDesde(), rango.hasta());
		return responder(candidatas.stream().filter(rango::incluye), prioridad, estado);
	}

	/** Tareas creadas que todavía no tienen plazo ("Por planificar"). */
	public List<TareaResponse> sinPlazo(Prioridad prioridad, EstadoTarea estado) {
		return responder(repository.findByUsuarioIdAndAmbitoAndHorizonte(usuario.id(), Ambito.PERSONAL,
				Horizonte.SIN_PLAZO).stream(), prioridad, estado);
	}

	/**
	 * Vista Hoy (§4.3): las del día y las que vencen hoy, más todas las vencidas sin terminar,
	 * de cualquier plazo.
	 */
	public List<TareaResponse> hoy(Prioridad prioridad, EstadoTarea estado) {
		LocalDate hoy = calendario.hoy();
		Rango deHoy = Rango.dia(hoy);
		Stream<Tarea> delDia = repository.findByUsuarioIdAndAmbitoAndFechaReferenciaBetween(usuario.id(),
				Ambito.PERSONAL, hoy, hoy).stream().filter(deHoy::incluye);
		Stream<Tarea> vencidas = repository.findByUsuarioIdAndAmbitoAndEstadoNotAndFechaReferenciaBefore(usuario.id(),
				Ambito.PERSONAL, EstadoTarea.FINALIZADA, hoy).stream().filter(t -> t.estaVencida(hoy));
		return responder(Stream.concat(delDia, vencidas), prioridad, estado);
	}

	public TareaResponse obtener(UUID id) {
		return mapper.aResponse(buscar(id), calendario.hoy());
	}

	@Transactional
	public TareaResponse crear(TareaRequest request) {
		Tarea tarea = mapper.aEntidad(request, fechaSegun(request.horizonte(), request.fechaReferencia()),
				usuario.id(), clock.instant());
		return mapper.aResponse(repository.save(tarea), calendario.hoy());
	}

	@Transactional
	public TareaResponse editar(UUID id, TareaRequest request) {
		Tarea tarea = buscar(id);
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

	@Transactional
	public void eliminar(UUID id) {
		repository.delete(buscar(id));
	}

	private Tarea buscar(UUID id) {
		return repository.findByIdAndUsuarioIdAndAmbito(id, usuario.id(), Ambito.PERSONAL)
				.orElseThrow(() -> new TareaNoEncontradaException(id));
	}

	/** Con plazo, siempre hay fecha (hoy si no vino); sin plazo, nunca. */
	private LocalDate fechaSegun(Horizonte horizonte, LocalDate fecha) {
		if (!horizonte.exigeFecha()) {
			return null;
		}
		return fecha != null ? fecha : calendario.hoy();
	}

	private List<TareaResponse> responder(Stream<Tarea> tareas, Prioridad prioridad, EstadoTarea estado) {
		LocalDate hoy = calendario.hoy();
		return tareas
				.filter(siViene(prioridad, Tarea::getPrioridad))
				.filter(siViene(estado, Tarea::getEstado))
				.sorted(OrdenTareas.POR_PRIORIDAD)
				.map(t -> mapper.aResponse(t, hoy))
				.toList();
	}

	private static <T> Predicate<Tarea> siViene(T valor, Function<Tarea, T> campo) {
		return t -> valor == null || valor == campo.apply(t);
	}

}
