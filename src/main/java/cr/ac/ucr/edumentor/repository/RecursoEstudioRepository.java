package cr.ac.ucr.edumentor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cr.ac.ucr.edumentor.model.RecursoEstudio;

public interface RecursoEstudioRepository extends JpaRepository<RecursoEstudio, Long> {

    List<RecursoEstudio> findBySesionIdOrderByIdAsc(Long sesionId);

    Optional<RecursoEstudio> findByIdAndSesionId(Long id, Long sesionId);
}
