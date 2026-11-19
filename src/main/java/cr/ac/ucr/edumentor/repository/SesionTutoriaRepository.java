package cr.ac.ucr.edumentor.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cr.ac.ucr.edumentor.model.EstadoSesion;
import cr.ac.ucr.edumentor.model.SesionTutoria;

public interface SesionTutoriaRepository extends JpaRepository<SesionTutoria, Long> {

  @Override
  @EntityGraph(attributePaths = { "estudiante", "tutor", "materia" })
  Optional<SesionTutoria> findById(Long id);

  @EntityGraph(attributePaths = { "estudiante", "tutor", "materia" })
  List<SesionTutoria> findByEstudianteIdOrderByFechaHoraDesc(Long estudianteId);

  @EntityGraph(attributePaths = { "estudiante", "tutor", "materia" })
  List<SesionTutoria> findByTutorIdOrderByFechaHoraDesc(Long tutorId);

  @EntityGraph(attributePaths = { "estudiante", "tutor", "materia" })
  List<SesionTutoria> findAllByOrderByFechaHoraDesc();

  // Sesiones de un tutor que empiezan dentro de un rango (se usa con el día
  // completo)
  // y que están en alguno de los estados indicados. Sirve para detectar
  // traslapes.
  @Query("""
      SELECT s FROM SesionTutoria s
      WHERE s.tutor.id = :tutorId
        AND s.estado IN :estados
        AND s.fechaHora >= :desde
        AND s.fechaHora < :hasta
      """)
  List<SesionTutoria> buscarDelTutorEnRango(@Param("tutorId") Long tutorId,
      @Param("estados") Collection<EstadoSesion> estados,
      @Param("desde") LocalDateTime desde,
      @Param("hasta") LocalDateTime hasta);

  // Lo mismo pero del lado del estudiante (un estudiante tampoco puede estar en
  // dos sesiones a la vez).
  @Query("""
      SELECT s FROM SesionTutoria s
      WHERE s.estudiante.id = :estudianteId
        AND s.estado IN :estados
        AND s.fechaHora >= :desde
        AND s.fechaHora < :hasta
      """)
  List<SesionTutoria> buscarDelEstudianteEnRango(@Param("estudianteId") Long estudianteId,
      @Param("estados") Collection<EstadoSesion> estados,
      @Param("desde") LocalDateTime desde,
      @Param("hasta") LocalDateTime hasta);
}
