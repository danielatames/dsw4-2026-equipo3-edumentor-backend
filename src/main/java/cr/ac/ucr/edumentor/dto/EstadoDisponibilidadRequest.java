package cr.ac.ucr.edumentor.dto;

import jakarta.validation.constraints.NotNull;

// Ejemplo JSON: {"activo": false}
public record EstadoDisponibilidadRequest(@NotNull Boolean activo) {
}
