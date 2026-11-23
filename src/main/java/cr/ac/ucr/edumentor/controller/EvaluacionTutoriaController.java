package cr.ac.ucr.edumentor.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import cr.ac.ucr.edumentor.dto.CalificacionTutorResponse;
import cr.ac.ucr.edumentor.dto.EvaluacionRequest;
import cr.ac.ucr.edumentor.dto.EvaluacionResponse;
import cr.ac.ucr.edumentor.service.EvaluacionTutoriaService;
import jakarta.validation.Valid;

@RestController
public class EvaluacionTutoriaController {

    private final EvaluacionTutoriaService evaluaciones;

    public EvaluacionTutoriaController(EvaluacionTutoriaService evaluaciones) {
        this.evaluaciones = evaluaciones;
    }

    @PostMapping("/sesiones/{sesionId}/evaluacion")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<EvaluacionResponse> evaluar(@PathVariable Long sesionId,
                                                      @Valid @RequestBody EvaluacionRequest req,
                                                      Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(evaluaciones.evaluar(authentication.getName(), sesionId, req));
    }

    @GetMapping("/sesiones/{sesionId}/evaluacion")
    public EvaluacionResponse obtener(@PathVariable Long sesionId, Authentication authentication) {
        return evaluaciones.obtener(authentication.getName(), sesionId);
    }

    @GetMapping("/tutores/{tutorId}/calificacion")
    public CalificacionTutorResponse calificacionDelTutor(@PathVariable Long tutorId) {
        return evaluaciones.calificacionDelTutor(tutorId);
    }
}
