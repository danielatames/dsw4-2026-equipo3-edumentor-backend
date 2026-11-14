package cr.ac.ucr.edumentor.config;

import cr.ac.ucr.edumentor.model.Rol;
import cr.ac.ucr.edumentor.model.Usuario;
import cr.ac.ucr.edumentor.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public DataInitializer(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        crear("C00001", "estudiante@ucr.ac.cr", "Estudiante123*", "Ana", "Estudiante", Rol.ESTUDIANTE);
        crear("C00002", "tutor@ucr.ac.cr", "Tutor123*", "Luis", "Tutor", Rol.TUTOR);
        crear("C00003", "coordinador@ucr.ac.cr", "Coordinador123*", "Marta", "Coordinadora", Rol.COORDINADOR);
    }

    private void crear(String carne, String email, String pass, String nombre,
                       String apellido, Rol rol) {
        if (usuarios.existsByEmail(email)) return;
        Usuario u = new Usuario();
        u.setCarne(carne);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(pass));
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setRol(rol);
        usuarios.save(u);
    }
}