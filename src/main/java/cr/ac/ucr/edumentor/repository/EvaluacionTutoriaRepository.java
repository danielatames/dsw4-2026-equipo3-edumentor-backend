package cr.ac.ucr.edumentor.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cr.ac.ucr.edumentor.model.EvaluacionTutoria;

public interface EvaluacionTutoriaRepository extends JpaRepository<EvaluacionTutoria, Long> {

    Optional<EvaluacionTutoria> findBySesionId(Long sesionId);

    long countBySesionTutorId(Long tutorId);

    @Query("SELECT AVG(e.calificacionEstrellas) FROM EvaluacionTutoria e WHERE e.sesion.tutor.id = :tutorId")
    Double promedioDelTutor(@Param("tutorId") Long tutorId);
}
