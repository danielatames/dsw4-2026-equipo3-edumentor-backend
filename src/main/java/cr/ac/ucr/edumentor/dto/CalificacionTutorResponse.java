package cr.ac.ucr.edumentor.dto;

import cr.ac.ucr.edumentor.model.Usuario;

public record CalificacionTutorResponse(Long tutorId, String tutorNombre,
        double promedio, int totalEvaluaciones) {

    public static CalificacionTutorResponse de(Usuario tutor) {
        return new CalificacionTutorResponse(tutor.getId(),
                tutor.getNombre() + " " + tutor.getApellido(),
                tutor.getPromedioCalificacion(), tutor.getTotalEvaluaciones());
    }
}
