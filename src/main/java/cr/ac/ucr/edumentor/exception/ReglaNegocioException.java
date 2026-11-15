package cr.ac.ucr.edumentor.exception;

import org.springframework.http.HttpStatus;

public class ReglaNegocioException extends RuntimeException {

    private final HttpStatus status;

    public ReglaNegocioException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public ReglaNegocioException(String mensaje) {
        this(HttpStatus.UNPROCESSABLE_ENTITY, mensaje);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
