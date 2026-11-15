package cr.ac.ucr.edumentor.exception;

import org.springframework.http.HttpStatus;

public class ConflictoNegocioException extends ReglaNegocioException {

    public ConflictoNegocioException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }
}
