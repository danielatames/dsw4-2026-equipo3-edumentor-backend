package cr.ac.ucr.edumentor.dto;

import cr.ac.ucr.edumentor.model.Rol;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(max = 20) String carne,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 80) String nombre,
        @NotBlank @Size(max = 80) String apellido,
        @NotNull Rol rol) {
}