package cr.ac.ucr.edumentor.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cr.ac.ucr.edumentor.dto.RecursoRequest;
import cr.ac.ucr.edumentor.dto.RecursoResponse;
import cr.ac.ucr.edumentor.service.RecursoEstudioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/sesiones/{sesionId}/recursos")
public class RecursoEstudioController {

    private final RecursoEstudioService recursos;

    public RecursoEstudioController(RecursoEstudioService recursos) {
        this.recursos = recursos;
    }

    @GetMapping
    public List<RecursoResponse> listar(@PathVariable Long sesionId, Authentication authentication) {
        return recursos.listar(authentication.getName(), sesionId);
    }

    @PostMapping
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<RecursoResponse> agregar(@PathVariable Long sesionId,
                                                   @Valid @RequestBody RecursoRequest req,
                                                   Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recursos.agregar(authentication.getName(), sesionId, req));
    }

    @DeleteMapping("/{recursoId}")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long sesionId, @PathVariable Long recursoId,
                                         Authentication authentication) {
        recursos.eliminar(authentication.getName(), sesionId, recursoId);
        return ResponseEntity.noContent().build();
    }
}
