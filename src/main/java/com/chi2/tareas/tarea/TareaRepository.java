package com.chi2.tareas.tarea;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	/** Para las estadísticas: finalizadas desde un momento. */
	List<Tarea> findByUsuarioIdAndAmbitoAndCompletadaEnGreaterThanEqual(UUID usuarioId, Ambito ambito, Instant desde);

	/** Para las estadísticas: todas las que no están finalizadas. */
	List<Tarea> findByUsuarioIdAndAmbitoAndEstadoNot(UUID usuarioId, Ambito ambito, EstadoTarea estado);

	/** Recordatorios que ya llegaron y todavía no se avisaron (de tareas sin terminar). */
	List<Tarea> findByUsuarioIdAndAmbitoAndRecordatorioLessThanEqualAndRecordatorioAvisadoFalseAndEstadoNot(
			UUID usuarioId, Ambito ambito, Instant momento, EstadoTarea estado);

	/**
	 * Buscador: título o descripción que contengan el texto, sin distinguir mayúsculas ni tildes.
	 * Primero las sin terminar, después las más recientes.
	 */
	@Query(value = "select * from tarea where usuario_id = :usuario and ambito = 'PERSONAL' and eliminada_en is null"
			+ " and (unaccent(lower(titulo)) like unaccent(lower(:patron)) escape '!'"
			+ " or unaccent(lower(coalesce(descripcion, ''))) like unaccent(lower(:patron)) escape '!')"
			+ " order by (estado = 'FINALIZADA'), actualizada_en desc limit :limite", nativeQuery = true)
	List<Tarea> buscar(@Param("usuario") UUID usuarioId, @Param("patron") String patron, @Param("limite") int limite);

	// ---------- Papelera: consultas nativas, porque la entidad oculta las eliminadas ----------

	@Query(value = "select * from tarea where usuario_id = :usuario and ambito = 'PERSONAL'"
			+ " and eliminada_en is not null order by eliminada_en desc", nativeQuery = true)
	List<Tarea> papelera(@Param("usuario") UUID usuarioId);

	@Modifying(clearAutomatically = true)
	@Query(value = "update tarea set eliminada_en = null, actualizada_en = :ahora where id = :id"
			+ " and usuario_id = :usuario and eliminada_en is not null", nativeQuery = true)
	int restaurar(@Param("id") UUID id, @Param("usuario") UUID usuarioId, @Param("ahora") Instant ahora);

	@Modifying(clearAutomatically = true)
	@Query(value = "delete from tarea where id = :id and usuario_id = :usuario and eliminada_en is not null",
			nativeQuery = true)
	int eliminarDefinitivamente(@Param("id") UUID id, @Param("usuario") UUID usuarioId);

	@Modifying(clearAutomatically = true)
	@Query(value = "delete from tarea where usuario_id = :usuario and eliminada_en is not null", nativeQuery = true)
	int vaciarPapelera(@Param("usuario") UUID usuarioId);

	@Modifying(clearAutomatically = true)
	@Query(value = "delete from tarea where eliminada_en < :limite", nativeQuery = true)
	int purgarEliminadasAntesDe(@Param("limite") Instant limite);

}
