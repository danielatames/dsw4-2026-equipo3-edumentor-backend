package cr.ac.ucr.edumentor.dto;

import jakarta.validation.constraints.*;

public record MateriaRequest(
        @NotBlank @Size(max = 20) String codigoMateria,
        @NotBlank @Size(max = 120) String nombreMateria,
        @NotBlank @Size(max = 120) String carrera,
        @NotNull @Min(1) @Max(12) Integer creditos) {
}
