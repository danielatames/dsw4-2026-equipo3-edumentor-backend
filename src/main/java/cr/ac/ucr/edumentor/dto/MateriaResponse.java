package cr.ac.ucr.edumentor.dto;

import cr.ac.ucr.edumentor.model.Materia;

public record MateriaResponse(Long id, String codigoMateria, String nombreMateria,
                              String carrera, Integer creditos) {
    public static MateriaResponse de(Materia m) {
        return new MateriaResponse(m.getId(), m.getCodigoMateria(), m.getNombreMateria(),
                m.getCarrera(), m.getCreditos());
    }
}
