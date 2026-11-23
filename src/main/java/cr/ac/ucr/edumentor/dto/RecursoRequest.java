package cr.ac.ucr.edumentor.dto;

import cr.ac.ucr.edumentor.model.TipoArchivo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RecursoRequest(
        @NotBlank @Size(max = 150) String titulo,
        @NotBlank @Size(max = 500) @Pattern(regexp = "^https?://\\S+$", message = "debe ser una URL http o https válida") String urlRecurso,
        @NotNull TipoArchivo tipoArchivo) {
}
