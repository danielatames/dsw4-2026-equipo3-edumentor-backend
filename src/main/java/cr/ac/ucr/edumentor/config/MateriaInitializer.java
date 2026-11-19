package cr.ac.ucr.edumentor.config;

import cr.ac.ucr.edumentor.model.Materia;
import cr.ac.ucr.edumentor.repository.MateriaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Precarga un catálogo pequeño de materias para pruebas,
 * igual que DataInitializer lo hace con los usuarios.
 */
@Component
public class MateriaInitializer implements CommandLineRunner {

    private final MateriaRepository materias;

    public MateriaInitializer(MateriaRepository materias) {
        this.materias = materias;
    }

    @Override
    public void run(String... args) {
        crear("IF0001", "Introducción a la Programación", "Informática Empresarial", 4);
        crear("IF0002", "Programación I", "Informática Empresarial", 4);
        crear("IF0009", "Desarrollo de Software IV", "Informática Empresarial", 4);
        crear("MA0001", "Cálculo I", "Informática Empresarial", 3);
        crear("MA0320", "Estadística", "Ingeniería Industrial", 3);
    }

    private void crear(String codigo, String nombre, String carrera, int creditos) {
        if (materias.existsByCodigoMateria(codigo)) return;
        Materia m = new Materia();
        m.setCodigoMateria(codigo);
        m.setNombreMateria(nombre);
        m.setCarrera(carrera);
        m.setCreditos(creditos);
        materias.save(m);
    }
}
