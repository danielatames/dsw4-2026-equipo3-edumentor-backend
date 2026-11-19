package cr.ac.ucr.edumentor.dto;

import java.time.LocalDateTime;

import cr.ac.ucr.edumentor.model.EstadoSesion;
import cr.ac.ucr.edumentor.model.SesionTutoria;

public record SesionResponse(Long id,
        Long estudianteId, String estudianteNombre,
        Long tutorId, String tutorNombre,
        Long materiaId, String materiaNombre,
        LocalDateTime fechaHora, LocalDateTime fechaFin,
        int duracionMinutos, EstadoSesion estado, String tema) {

    public static SesionResponse de(SesionTutoria s) {
        return new SesionResponse(s.getId(),
                s.getEstudiante().getId(),
                s.getEstudiante().getNombre() + " " + s.getEstudiante().getApellido(),
                s.getTutor().getId(),
                s.getTutor().getNombre() + " " + s.getTutor().getApellido(),
                s.getMateria().getId(), s.getMateria().getNombreMateria(),
                s.getFechaHora(), s.getFechaFin(),
                s.getDuracionMinutos(), s.getEstado(), s.getTema());
    }
}
