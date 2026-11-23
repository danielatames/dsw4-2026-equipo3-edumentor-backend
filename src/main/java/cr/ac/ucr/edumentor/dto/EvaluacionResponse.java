package cr.ac.ucr.edumentor.dto;

import java.time.LocalDateTime;

import cr.ac.ucr.edumentor.model.EvaluacionTutoria;

public record EvaluacionResponse(Long id, Long sesionId, int calificacionEstrellas,
        String comentario, LocalDateTime fechaEvaluacion) {

    public static EvaluacionResponse de(EvaluacionTutoria e) {
        return new EvaluacionResponse(e.getId(), e.getSesion().getId(), e.getCalificacionEstrellas(),
                e.getComentario(), e.getFechaEvaluacion());
    }
}
