package cr.ac.ucr.edumentor.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

// Ejemplo JSON: {"diaSemana": "MONDAY", "horaInicio": "08:00", "horaFin": "10:00"}
public record DisponibilidadRequest(
        @NotNull DayOfWeek diaSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin) {
}
