package cr.ac.ucr.edumentor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EvaluacionRequest(
        @NotNull @Min(1) @Max(5) Integer calificacionEstrellas,
        @Size(max = 500) String comentario) {
}
