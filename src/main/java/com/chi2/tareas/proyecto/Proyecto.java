package com.chi2.tareas.proyecto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** Proyecto o etiqueta que agrupa tareas (§4.6). Nunca sale de la capa de service. */
@Entity
public class Proyecto {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;

	@Column(nullable = false, length = 40)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private ColorProyecto color;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	protected Proyecto() {
		// para JPA
	}

	public Proyecto(String nombre, ColorProyecto color, UUID usuarioId, Instant ahora) {
		this.usuarioId = Objects.requireNonNull(usuarioId);
		this.creadoEn = ahora;
		editar(nombre, color);
	}

	public void editar(String nombre, ColorProyecto color) {
		this.nombre = Objects.requireNonNull(nombre);
		this.color = Objects.requireNonNull(color);
	}

	public UUID getId() {
		return id;
	}

	public UUID getUsuarioId() {
		return usuarioId;
	}

	public String getNombre() {
		return nombre;
	}

	public ColorProyecto getColor() {
		return color;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

}
