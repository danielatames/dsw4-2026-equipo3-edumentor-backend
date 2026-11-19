package cr.ac.ucr.edumentor.controller;

import cr.ac.ucr.edumentor.dto.DisponibilidadRequest;
import cr.ac.ucr.edumentor.dto.DisponibilidadResponse;
import cr.ac.ucr.edumentor.dto.EstadoDisponibilidadRequest;
import cr.ac.ucr.edumentor.service.DisponibilidadTutorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/disponibilidades")
public class DisponibilidadTutorController {

    private final DisponibilidadTutorService disponibilidades;

    public DisponibilidadTutorController(DisponibilidadTutorService disponibilidades) {
        this.disponibilidades = disponibilidades;
    }

    // Lectura: cualquier usuario autenticado.
    @GetMapping
    public List<DisponibilidadResponse> listarActivas() {
        return disponibilidades.listarActivas();
    }

    @GetMapping("/tutor/{tutorId}")
    public List<DisponibilidadResponse> listarPorTutor(@PathVariable Long tutorId) {
        return disponibilidades.listarPorTutor(tutorId);
    }

    // Escritura: solo el TUTOR, y siempre sobre sus propios bloques.
    // authentication.getName() es el email que viene dentro del JWT.
    @GetMapping("/mias")
    @PreAuthorize("hasRole('TUTOR')")
    public List<DisponibilidadResponse> listarMias(Authentication authentication) {
        return disponibilidades.listarMias(authentication.getName());
    }

    @PostMapping
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<DisponibilidadResponse> crear(@Valid @RequestBody DisponibilidadRequest req,
                                                        Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disponibilidades.crear(authentication.getName(), req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TUTOR')")
    public DisponibilidadResponse actualizar(@PathVariable Long id,
                                             @Valid @RequestBody DisponibilidadRequest req,
                                             Authentication authentication) {
        return disponibilidades.actualizar(authentication.getName(), id, req);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('TUTOR')")
    public DisponibilidadResponse cambiarEstado(@PathVariable Long id,
                                                @Valid @RequestBody EstadoDisponibilidadRequest req,
                                                Authentication authentication) {
        return disponibilidades.cambiarEstado(authentication.getName(), id, req.activo());
    }
}
