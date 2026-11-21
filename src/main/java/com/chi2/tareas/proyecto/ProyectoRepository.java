package com.chi2.tareas.proyecto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProyectoRepository extends JpaRepository<Proyecto, UUID> {

	List<Proyecto> findByUsuarioIdOrderByNombreAsc(UUID usuarioId);

	Optional<Proyecto> findByIdAndUsuarioId(UUID id, UUID usuarioId);

	Optional<Proyecto> findByUsuarioIdAndNombreIgnoreCase(UUID usuarioId, String nombre);

	long countByUsuarioId(UUID usuarioId);

}
