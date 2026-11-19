package cr.ac.ucr.edumentor.repository;

import cr.ac.ucr.edumentor.model.DisponibilidadTutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface DisponibilidadTutorRepository extends JpaRepository<DisponibilidadTutor, Long> {

    // @EntityGraph trae el tutor en la misma consulta (JOIN).
    // Sin esto, por cada bloque Hibernate haría otra consulta para el tutor: problema N+1.
    @EntityGraph(attributePaths = "tutor")
    List<DisponibilidadTutor> findByEstaActivoTrue();

    @EntityGraph(attributePaths = "tutor")
    List<DisponibilidadTutor> findByTutorId(Long tutorId);

    @EntityGraph(attributePaths = "tutor")
    List<DisponibilidadTutor> findByTutorIdAndEstaActivoTrue(Long tutorId);

    // Bloques activos de un tutor en un día: se usan para validar solapes y disponibilidad.
    List<DisponibilidadTutor> findByTutorIdAndDiaSemanaAndEstaActivoTrue(Long tutorId, DayOfWeek diaSemana);
}
