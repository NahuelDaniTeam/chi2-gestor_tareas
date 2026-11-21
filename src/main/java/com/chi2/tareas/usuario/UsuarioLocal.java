package com.chi2.tareas.usuario;

import java.util.UUID;

import com.chi2.tareas.TareasProperties;

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
