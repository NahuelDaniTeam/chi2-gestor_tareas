package com.chi2.tareas.estadistica;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.chi2.tareas.TareasProperties;
import com.chi2.tareas.proyecto.ProyectoService;
import com.chi2.tareas.tarea.Ambito;
import com.chi2.tareas.tarea.Calendario;
import com.chi2.tareas.tarea.EstadoTarea;
import com.chi2.tareas.tarea.Tarea;
import com.chi2.tareas.tarea.TareaRepository;
import com.chi2.tareas.usuario.UsuarioActual;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Estadísticas (§4.14): cuánto se termina por semana, la racha, lo pendiente y el avance por proyecto. */
@Service
@Transactional(readOnly = true)
public class EstadisticasService {

	private static final int SEMANAS = 8;
	/** Lo necesario para la racha y las 8 semanas, con margen. */
	private static final Duration HISTORIA = Duration.ofDays(400);

	private final TareaRepository tareas;
	private final ProyectoService proyectos;
	private final UsuarioActual usuario;
	private final Calendario calendario;
	private final TareasProperties properties;
	private final Clock clock;

	public EstadisticasService(TareaRepository tareas, ProyectoService proyectos, UsuarioActual usuario,
			Calendario calendario, TareasProperties properties, Clock clock) {
		this.tareas = tareas;
		this.proyectos = proyectos;
		this.usuario = usuario;
		this.calendario = calendario;
		this.properties = properties;
		this.clock = clock;
	}

	public EstadisticasResponse calcular() {
		LocalDate hoy = calendario.hoy();
		List<Tarea> completadas = tareas.findByUsuarioIdAndAmbitoAndCompletadaEnGreaterThanEqual(usuario.id(),
				Ambito.PERSONAL, clock.instant().minus(HISTORIA));
		List<Tarea> abiertas = tareas.findByUsuarioIdAndAmbitoAndEstadoNot(usuario.id(), Ambito.PERSONAL,
				EstadoTarea.FINALIZADA);
		List<LocalDate> dias = completadas.stream().map(this::diaDeCompletada).toList();
		List<SemanaCompletadas> porSemana = CalculoEstadisticas.porSemana(dias, hoy, SEMANAS);
		return new EstadisticasResponse(
				porSemana.getLast().cantidad(),
				porSemana.get(porSemana.size() - 2).cantidad(),
				CalculoEstadisticas.racha(dias, hoy),
				abiertas.size(),
				(int) abiertas.stream().filter(t -> t.estaVencida(hoy)).count(),
				porSemana,
				porProyecto(completadas, abiertas, hoy));
	}

	private LocalDate diaDeCompletada(Tarea tarea) {
		return LocalDate.ofInstant(tarea.getCompletadaEn(), properties.zona());
	}

	/** Un renglón por proyecto con actividad, más "Sin proyecto" si corresponde; los más activos primero. */
	private List<ProyectoEstadistica> porProyecto(List<Tarea> completadas, List<Tarea> abiertas, LocalDate hoy) {
		LocalDate hace30 = hoy.minusDays(30);
		List<ProyectoEstadistica> filas = new ArrayList<>();
		proyectos.listar().forEach(p -> filas.add(fila(p.id(), p.nombre(), p.hex(), completadas, abiertas, hace30)));
		filas.add(fila(null, "Sin proyecto", null, completadas, abiertas, hace30));
		return filas.stream()
				.filter(f -> f.completadas30Dias() + f.pendientes() > 0)
				.sorted(Comparator.comparingInt((ProyectoEstadistica f) -> f.completadas30Dias() + f.pendientes()).reversed())
				.toList();
	}

	private ProyectoEstadistica fila(UUID id, String nombre, String hex, List<Tarea> completadas, List<Tarea> abiertas,
			LocalDate desde) {
		int hechas = (int) completadas.stream()
				.filter(t -> Objects.equals(t.getProyectoId(), id) && !diaDeCompletada(t).isBefore(desde)).count();
		int pendientes = (int) abiertas.stream().filter(t -> Objects.equals(t.getProyectoId(), id)).count();
		return new ProyectoEstadistica(id, nombre, hex, hechas, pendientes);
	}

}
