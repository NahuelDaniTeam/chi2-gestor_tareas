package com.chi2.tareas.tarea;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TareaRepository extends JpaRepository<Tarea, UUID> {

	Optional<Tarea> findByIdAndUsuarioIdAndAmbito(UUID id, UUID usuarioId, Ambito ambito);

	List<Tarea> findByUsuarioIdAndAmbitoAndHorizonte(UUID usuarioId, Ambito ambito, Horizonte horizonte);

	/** Candidatas para un rango: el filtro fino lo hace {@link Rango#incluye}. */
	List<Tarea> findByUsuarioIdAndAmbitoAndFechaReferenciaBetween(UUID usuarioId, Ambito ambito, LocalDate desde,
			LocalDate hasta);

	List<Tarea> findByUsuarioIdAndAmbitoAndFechaReferenciaGreaterThanEqual(UUID usuarioId, Ambito ambito,
			LocalDate desde);

	/** Candidatas a vencidas: sin terminar y con fecha de referencia anterior a {@code fecha}. */
	List<Tarea> findByUsuarioIdAndAmbitoAndEstadoNotAndFechaReferenciaBefore(UUID usuarioId, Ambito ambito,
			EstadoTarea estado, LocalDate fecha);

}
