package cr.ac.ucr.edumentor.service;

import cr.ac.ucr.edumentor.dto.*;
import cr.ac.ucr.edumentor.model.Rol;
import cr.ac.ucr.edumentor.model.Usuario;
import cr.ac.ucr.edumentor.repository.UsuarioRepository;
import cr.ac.ucr.edumentor.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse registrar(RegisterRequest req) {
        if (req.rol() == Rol.COORDINADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No se permite el autorregistro con el rol COORDINADOR");
        }
        if (usuarios.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado");
        }
        if (usuarios.existsByCarne(req.carne())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El carné ya está registrado");
        }
        Usuario u = new Usuario();
        u.setCarne(req.carne());
        u.setEmail(req.email());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setNombre(req.nombre());
        u.setApellido(req.apellido());
        u.setRol(req.rol());
        usuarios.save(u);
        return new AuthResponse(jwt.generarToken(u), "Bearer", UsuarioResponse.de(u));
    }

    public AuthResponse login(LoginRequest req) {
        Usuario u = usuarios.findByEmail(req.email())
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        return new AuthResponse(jwt.generarToken(u), "Bearer", UsuarioResponse.de(u));
    }

    public UsuarioResponse perfil(String email) {
        return usuarios.findByEmail(email)
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
}