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
	 * Tareas de un horizonte en el período que contiene a {@code fecha} (hoy si falta).
	 * En largo plazo, todas. {@code prioridad} y {@code estado} filtran si vienen.
	 */
	public List<TareaResponse> listar(Horizonte horizonte, LocalDate fecha, Prioridad prioridad, EstadoTarea estado) {
		List<Tarea> tareas = horizonte == Horizonte.LARGO_PLAZO
				? repository.findByUsuarioIdAndAmbitoAndHorizonte(usuario.id(), Ambito.PERSONAL, horizonte)
				: repository.findByUsuarioIdAndAmbitoAndHorizonteInAndFechaReferencia(usuario.id(), Ambito.PERSONAL,
						List.of(horizonte), horizonte.fechaReferencia(fechaOHoy(fecha)));
		return responder(tareas.stream(), prioridad, estado);
	}

	/**
	 * Vista Hoy (§4.3): las tareas del día (y las de largo plazo que vencen hoy) más todas las vencidas
	 * sin terminar, de cualquier horizonte.
	 */
	public List<TareaResponse> hoy(Prioridad prioridad, EstadoTarea estado) {
		LocalDate hoy = calendario.hoy();
		UUID id = usuario.id();
		Stream<Tarea> deHoy = repository.findByUsuarioIdAndAmbitoAndHorizonteInAndFechaReferencia(id,
				Ambito.PERSONAL, List.of(Horizonte.DIA, Horizonte.LARGO_PLAZO), hoy).stream();
		Stream<Tarea> vencidas = repository.findByUsuarioIdAndAmbitoAndEstadoNotAndFechaReferenciaBefore(id,
				Ambito.PERSONAL, EstadoTarea.FINALIZADA, hoy).stream().filter(t -> t.estaVencida(hoy));
		return responder(Stream.concat(deHoy, vencidas), prioridad, estado);
	}

	public TareaResponse obtener(UUID id) {
		return mapper.aResponse(buscar(id), calendario.hoy());
	}

	@Transactional
	public TareaResponse crear(TareaRequest request) {
		Tarea tarea = mapper.aEntidad(request, fechaSegun(request), usuario.id(), clock.instant());
		return mapper.aResponse(repository.save(tarea), calendario.hoy());
	}

	@Transactional
	public TareaResponse editar(UUID id, TareaRequest request) {
		Tarea tarea = buscar(id);
		mapper.aplicar(request, fechaSegun(request), tarea, clock.instant());
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

	/** Día, semana y mes siempre tienen fecha (hoy si no vino); largo plazo, solo si se puso una límite. */
	private LocalDate fechaSegun(TareaRequest request) {
		return request.horizonte().exigeFecha() ? fechaOHoy(request.fechaReferencia()) : request.fechaReferencia();
	}

	private LocalDate fechaOHoy(LocalDate fecha) {
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
