package com.chi2.tareas.usuario;

import java.util.UUID;

/** Quién está usando la aplicación. Etapa 1: un usuario fijo ({@link UsuarioLocal}); etapa 2: el logueado. */
public interface UsuarioActual {

	UUID id();

}
