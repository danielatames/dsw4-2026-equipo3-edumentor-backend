package cr.ac.ucr.edumentor.dto;

import cr.ac.ucr.edumentor.model.RecursoEstudio;
import cr.ac.ucr.edumentor.model.TipoArchivo;

public record RecursoResponse(Long id, Long sesionId, String titulo, String urlRecurso, TipoArchivo tipoArchivo) {

    public static RecursoResponse de(RecursoEstudio r) {
        return new RecursoResponse(r.getId(), r.getSesion().getId(), r.getTitulo(),
                r.getUrlRecurso(), r.getTipoArchivo());
    }
}
