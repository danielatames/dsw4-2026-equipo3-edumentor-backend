package cr.ac.ucr.edumentor.controller;

import cr.ac.ucr.edumentor.dto.SesionRequest;
import cr.ac.ucr.edumentor.dto.SesionResponse;
import cr.ac.ucr.edumentor.model.EstadoSesion;
import cr.ac.ucr.edumentor.service.SesionTutoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sesiones")
public class SesionTutoriaController {

    private final SesionTutoriaService sesiones;

    public SesionTutoriaController(SesionTutoriaService sesiones) {
        this.sesiones = sesiones;
    }

    // Lista según el rol de quien consulta (el servicio decide qué le toca ver).
    // El parámetro estado es opcional: /sesiones?estado=ACEPTADA
    @GetMapping
    public List<SesionResponse> listar(@RequestParam(required = false) EstadoSesion estado,
                                       Authentication authentication) {
        return sesiones.listar(authentication.getName(), estado);
    }

    @GetMapping("/{id}")
    public SesionResponse obtener(@PathVariable Long id, Authentication authentication) {
        return sesiones.obtener(authentication.getName(), id);
    }

    // Solo el ESTUDIANTE puede pedir una sesión.
    // authentication.getName() es el email que viene dentro del JWT.
    @PostMapping
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<SesionResponse> solicitar(@Valid @RequestBody SesionRequest req,
                                                    Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sesiones.solicitar(authentication.getName(), req));
    }

    // Acciones del tutor sobre sus sesiones.
    @PatchMapping("/{id}/aceptar")
    @PreAuthorize("hasRole('TUTOR')")
    public SesionResponse aceptar(@PathVariable Long id, Authentication authentication) {
        return sesiones.aceptar(authentication.getName(), id);
    }

    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('TUTOR')")
    public SesionResponse rechazar(@PathVariable Long id, Authentication authentication) {
        return sesiones.rechazar(authentication.getName(), id);
    }

    @PatchMapping("/{id}/iniciar")
    @PreAuthorize("hasRole('TUTOR')")
    public SesionResponse iniciar(@PathVariable Long id, Authentication authentication) {
        return sesiones.iniciar(authentication.getName(), id);
    }

    @PatchMapping("/{id}/concluir")
    @PreAuthorize("hasRole('TUTOR')")
    public SesionResponse concluir(@PathVariable Long id, Authentication authentication) {
        return sesiones.concluir(authentication.getName(), id);
    }

    // Cancelar lo pueden hacer el estudiante o el tutor de la sesión.
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'TUTOR')")
    public SesionResponse cancelar(@PathVariable Long id, Authentication authentication) {
        return sesiones.cancelar(authentication.getName(), id);
    }
}
