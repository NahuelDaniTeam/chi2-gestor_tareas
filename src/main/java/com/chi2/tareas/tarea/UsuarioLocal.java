package com.chi2.tareas.tarea;

import java.util.UUID;

import org.springframework.stereotype.Component;

/** Etapa 1 (uso personal, sin login): siempre el mismo usuario, el de {@code chi2.tareas.usuario-local}. */
@Component
public class UsuarioLocal implements UsuarioActual {

	private final UUID id;

	public UsuarioLocal(TareasProperties properties) {
		this.id = properties.usuarioLocal();
	}

	@Override
	public UUID id() {
		return id;
	}

}
