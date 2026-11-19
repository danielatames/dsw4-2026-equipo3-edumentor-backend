package cr.ac.ucr.edumentor.dto;

import cr.ac.ucr.edumentor.model.DisponibilidadTutor;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record DisponibilidadResponse(Long id, Long tutorId, String tutorNombre,
                                     DayOfWeek diaSemana, LocalTime horaInicio,
                                     LocalTime horaFin, boolean estaActivo) {
    public static DisponibilidadResponse de(DisponibilidadTutor d) {
        return new DisponibilidadResponse(d.getId(), d.getTutor().getId(),
                d.getTutor().getNombre() + " " + d.getTutor().getApellido(),
                d.getDiaSemana(), d.getHoraInicio(), d.getHoraFin(), d.isEstaActivo());
    }
}
