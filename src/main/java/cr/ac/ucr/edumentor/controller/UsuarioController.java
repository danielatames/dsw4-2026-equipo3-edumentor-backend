package cr.ac.ucr.edumentor.controller;

import cr.ac.ucr.edumentor.dto.UsuarioResponse;
import cr.ac.ucr.edumentor.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final AuthService auth;

    public UsuarioController(AuthService auth) {
        this.auth = auth;
    }

    @GetMapping("/me")
    public UsuarioResponse me(Authentication authentication) {
        return auth.perfil(authentication.getName());
    }
}