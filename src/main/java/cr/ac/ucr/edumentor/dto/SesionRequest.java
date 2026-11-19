package cr.ac.ucr.edumentor.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SesionRequest(
                @NotNull Long tutorId,
                @NotNull Long materiaId,
                @NotNull LocalDateTime fechaHora,
                @NotNull @Min(30) @Max(180) Integer duracionMinutos,
                @Size(max = 300) String tema) {
}
