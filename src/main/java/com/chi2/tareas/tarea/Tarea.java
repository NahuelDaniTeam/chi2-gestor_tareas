package com.chi2.tareas.tarea;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** Tarea del gestor (§4.1 y §7). Nunca sale de la capa de service. */
@Entity
public class Tarea {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 200)
	private String titulo;

	@Column(length = 2000)
	private String descripcion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
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

	/**
	 * Cambia los datos editables. La fecha se lleva a la fecha de referencia del horizonte
	 * (lunes de la semana, primero del mes).
	 *
	 * @throws IllegalArgumentException si el horizonte exige fecha y no vino
	 */
	public void editar(String titulo, String descripcion, Horizonte horizonte, LocalDate fecha, Prioridad prioridad,
			Instant ahora) {
		Objects.requireNonNull(horizonte);
		if (horizonte.exigeFecha() && fecha == null) {
			throw new IllegalArgumentException("El horizonte " + horizonte + " necesita una fecha");
		}
		this.titulo = Objects.requireNonNull(titulo);
		this.descripcion = descripcion;
		this.horizonte = horizonte;
		this.fechaReferencia = horizonte.fechaReferencia(fecha);
		this.prioridad = Objects.requireNonNull(prioridad);
		this.actualizadaEn = ahora;
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

	public void cambiarPrioridad(Prioridad nueva, Instant ahora) {
		Objects.requireNonNull(nueva);
		if (nueva == prioridad) {
			return;
		}
		this.prioridad = nueva;
		this.actualizadaEn = ahora;
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

}
