package cr.ac.ucr.edumentor.repository;

import cr.ac.ucr.edumentor.model.Materia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MateriaRepository extends JpaRepository<Materia, Long> {

    boolean existsByCodigoMateria(String codigoMateria);

    boolean existsByCodigoMateriaAndIdNot(String codigoMateria, Long id);

    // Los filtros son opcionales: si llegan en null no se aplican.
    @Query("""
            SELECT m FROM Materia m
            WHERE (:carrera IS NULL OR LOWER(m.carrera) = LOWER(:carrera))
              AND (:nombre IS NULL OR LOWER(m.nombreMateria) LIKE LOWER(CONCAT('%', :nombre, '%')))
            """)
    Page<Materia> buscar(@Param("carrera") String carrera,
                         @Param("nombre") String nombre,
                         Pageable pageable);
}
