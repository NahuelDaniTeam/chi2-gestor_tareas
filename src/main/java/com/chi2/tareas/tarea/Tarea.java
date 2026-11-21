package com.chi2.tareas.tarea;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLRestriction;

/**
 * Tarea del gestor (§4.1 y §7). Nunca sale de la capa de service.
 * Las que están en la papelera no aparecen en ninguna consulta JPA: la papelera usa consultas nativas.
 */
@Entity
@SQLRestriction("eliminada_en is null")
public class Tarea {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 200)
	private String titulo;

	@Column(length = 2000)
	private String descripcion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Horizonte horizonte;

	@Column(name = "fecha_referencia")
	private LocalDate fechaReferencia;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 5)
	private Prioridad prioridad;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private EstadoTarea estado = EstadoTarea.PENDIENTE;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 8)
	private Ambito ambito = Ambito.PERSONAL;

	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;

	@Column(name = "proyecto_id")
	private UUID proyectoId;

	/** Etapa 2. */
	@Column(name = "grupo_id")
	private UUID grupoId;

	/** Etapa 2, opcional. */
	@Column(name = "asignado_a")
	private UUID asignadoA;

	@Column(name = "creada_en", nullable = false)
	private Instant creadaEn;

	@Column(name = "actualizada_en", nullable = false)
	private Instant actualizadaEn;

	@Column(name = "completada_en")
	private Instant completadaEn;

	@Column(name = "eliminada_en")
	private Instant eliminadaEn;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 8)
	private Repeticion repeticion = Repeticion.NINGUNA;

	@Column(name = "siguiente_creada", nullable = false)
	private boolean siguienteCreada;

	@Column
	private Instant recordatorio;

	@Column(name = "recordatorio_avisado", nullable = false)
	private boolean recordatorioAvisado;

	@OneToMany(mappedBy = "tarea", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("orden")
	@BatchSize(size = 100)
	private List<Paso> pasos = new ArrayList<>();

	protected Tarea() {
		// para JPA
	}

	/** Tarea personal nueva, en estado {@link EstadoTarea#PENDIENTE}. */
	public Tarea(String titulo, String descripcion, Horizonte horizonte, LocalDate fecha, Prioridad prioridad,
			UUID usuarioId, Instant ahora) {
		this.usuarioId = Objects.requireNonNull(usuarioId);
		this.creadaEn = ahora;
		editar(titulo, descripcion, horizonte, fecha, prioridad, ahora);
	}

	/** Cambia los datos editables, plazo incluido (ver {@link #cambiarPlazo}). */
	public void editar(String titulo, String descripcion, Horizonte horizonte, LocalDate fecha, Prioridad prioridad,
			Instant ahora) {
		this.titulo = Objects.requireNonNull(titulo);
		this.descripcion = descripcion;
		this.prioridad = Objects.requireNonNull(prioridad);
		cambiarPlazo(horizonte, fecha, ahora);
	}

	/**
	 * Planifica la tarea. La fecha se lleva a la referencia del plazo (lunes de la semana, primero del mes);
	 * sin plazo se descarta.
	 *
	 * @throws IllegalArgumentException si el plazo exige fecha y no vino
	 */
	public void cambiarPlazo(Horizonte horizonte, LocalDate fecha, Instant ahora) {
		Objects.requireNonNull(horizonte);
		if (horizonte.exigeFecha() && fecha == null) {
			throw new IllegalArgumentException("El plazo " + horizonte + " necesita una fecha");
		}
		this.horizonte = horizonte;
		this.fechaReferencia = horizonte.fechaReferencia(fecha);
		if (!repeticion.admite(horizonte)) {
			this.repeticion = Repeticion.NINGUNA;
		}
		this.actualizadaEn = ahora;
	}

	// ---------- Repetición (§4.9) ----------

	/** @throws IllegalArgumentException si el plazo no admite esa repetición (por ejemplo, sin plazo) */
	public void programarRepeticion(Repeticion nueva, Instant ahora) {
		Objects.requireNonNull(nueva);
		if (!nueva.admite(horizonte)) {
			throw new IllegalArgumentException(horizonte == Horizonte.SIN_PLAZO
					? "Para repetir una tarea, primero ponele un plazo."
					: "Una tarea de " + horizonte.getEtiqueta().toLowerCase() + " no puede repetirse "
							+ nueva.getEtiqueta().toLowerCase() + ".");
		}
		this.repeticion = nueva;
		this.actualizadaEn = ahora;
	}

	/** Fecha de la próxima, si se repite. */
	public LocalDate proximaRepeticion() {
		return repeticion == Repeticion.NINGUNA ? null : repeticion.siguiente(fechaReferencia);
	}

	/**
	 * La copia para la próxima vez: mismo título, descripción, prioridad, proyecto, repetición y pasos
	 * (sin tildar). Se crea una sola vez, al finalizar; si se reabre y se vuelve a finalizar, no se duplica.
	 */
	public Optional<Tarea> crearSiguiente(Instant ahora) {
		if (repeticion == Repeticion.NINGUNA || siguienteCreada || estado != EstadoTarea.FINALIZADA) {
			return Optional.empty();
		}
		siguienteCreada = true;
		LocalDate proxima = proximaRepeticion();
		Tarea siguiente = new Tarea(titulo, descripcion, horizonte, proxima, prioridad, usuarioId, ahora);
		siguiente.proyectoId = proyectoId;
		siguiente.repeticion = repeticion;
		pasos.forEach(p -> siguiente.agregarPaso(p.getTexto(), ahora));
		if (recordatorio != null) {
			siguiente.recordatorio = recordatorio.plus(Duration.between(
					fechaReferencia.atStartOfDay(), proxima.atStartOfDay()));
		}
		return Optional.of(siguiente);
	}

	// ---------- Recordatorio (§4.10) ----------

	/** Cambiar la hora vuelve a habilitar el aviso. */
	public void programarRecordatorio(Instant momento, Instant ahora) {
		if (!Objects.equals(momento, recordatorio)) {
			this.recordatorio = momento;
			this.recordatorioAvisado = false;
			this.actualizadaEn = ahora;
		}
	}

	public void marcarRecordatorioAvisado() {
		this.recordatorioAvisado = true;
	}

	/** Al pasar a finalizada registra cuándo; al volver a otro estado, lo borra. */
	public void cambiarEstado(EstadoTarea nuevo, Instant ahora) {
		Objects.requireNonNull(nuevo);
		if (nuevo == estado) {
			return;
		}
		this.estado = nuevo;
		this.completadaEn = nuevo == EstadoTarea.FINALIZADA ? ahora : null;
		this.actualizadaEn = ahora;
	}

	/** {@code null} la deja sin proyecto. Que el proyecto exista y sea del usuario lo verifica el service. */
	public void asignarProyecto(UUID proyectoId, Instant ahora) {
		this.proyectoId = proyectoId;
		this.actualizadaEn = ahora;
	}

	public void cambiarPrioridad(Prioridad nueva, Instant ahora) {
		Objects.requireNonNull(nueva);
		if (nueva == prioridad) {
			return;
		}
		this.prioridad = nueva;
		this.actualizadaEn = ahora;
	}

	// ---------- Checklist (§4.7) ----------

	public Paso agregarPaso(String texto, Instant ahora) {
		int orden = pasos.stream().mapToInt(Paso::getOrden).max().orElse(0) + 1;
		Paso paso = new Paso(this, texto, orden);
		pasos.add(paso);
		this.actualizadaEn = ahora;
		return paso;
	}

	/** @throws PasoNoEncontradoException si el paso no es de esta tarea */
	public void editarPaso(UUID pasoId, String texto, Boolean hecho, Instant ahora) {
		buscarPaso(pasoId).editar(texto, hecho);
		this.actualizadaEn = ahora;
	}

	/** @throws PasoNoEncontradoException si el paso no es de esta tarea */
	public void eliminarPaso(UUID pasoId, Instant ahora) {
		pasos.remove(buscarPaso(pasoId));
		this.actualizadaEn = ahora;
	}

	private Paso buscarPaso(UUID pasoId) {
		return pasos.stream().filter(p -> p.getId().equals(pasoId)).findFirst()
				.orElseThrow(() -> new PasoNoEncontradoException(pasoId));
	}

	// ---------- Papelera (§4.8) ----------

	public void enviarAPapelera(Instant ahora) {
		this.eliminadaEn = ahora;
	}

	/** Sin terminar y con su período (o su fecha límite) ya pasado. */
	public boolean estaVencida(LocalDate hoy) {
		LocalDate fin = horizonte.fin(fechaReferencia);
		return estado != EstadoTarea.FINALIZADA && fin != null && fin.isBefore(hoy);
	}

	public UUID getId() {
		return id;
	}

	public String getTitulo() {
		return titulo;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public Horizonte getHorizonte() {
		return horizonte;
	}

	public LocalDate getFechaReferencia() {
		return fechaReferencia;
	}

	public Prioridad getPrioridad() {
		return prioridad;
	}

	public EstadoTarea getEstado() {
		return estado;
	}

	public Ambito getAmbito() {
		return ambito;
	}

	public UUID getUsuarioId() {
		return usuarioId;
	}

	public UUID getProyectoId() {
		return proyectoId;
	}

	public UUID getGrupoId() {
		return grupoId;
	}

	public UUID getAsignadoA() {
		return asignadoA;
	}

	public Instant getCreadaEn() {
		return creadaEn;
	}

	public Instant getActualizadaEn() {
		return actualizadaEn;
	}

	public Instant getCompletadaEn() {
		return completadaEn;
	}

	public Repeticion getRepeticion() {
		return repeticion;
	}

	public Instant getRecordatorio() {
		return recordatorio;
	}

	public boolean isRecordatorioAvisado() {
		return recordatorioAvisado;
	}

	public Instant getEliminadaEn() {
		return eliminadaEn;
	}

	public List<Paso> getPasos() {
		return List.copyOf(pasos);
	}

}
