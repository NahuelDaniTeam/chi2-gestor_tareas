package com.chi2.tareas.tarea;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** Paso del checklist de una tarea (§4.7). Lo maneja siempre su {@link Tarea}. */
@Entity
public class Paso {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tarea_id")
	private Tarea tarea;

	@Column(nullable = false, length = 200)
	private String texto;

	@Column(nullable = false)
	private boolean hecho;

	@Column(nullable = false)
	private int orden;

	protected Paso() {
		// para JPA
	}

	Paso(Tarea tarea, String texto, int orden) {
		this.id = UUID.randomUUID();
		this.tarea = tarea;
		this.texto = Objects.requireNonNull(texto);
		this.orden = orden;
	}

	void editar(String texto, Boolean hecho) {
		if (texto != null) {
			this.texto = texto;
		}
		if (hecho != null) {
			this.hecho = hecho;
		}
	}

	public UUID getId() {
		return id;
	}

	public String getTexto() {
		return texto;
	}

	public boolean isHecho() {
		return hecho;
	}

	public int getOrden() {
		return orden;
	}

}
