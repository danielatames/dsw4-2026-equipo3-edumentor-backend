package cr.ac.ucr.edumentor.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ReglaNegocioException.class)
    public ProblemDetail manejarReglaNegocio(ReglaNegocioException ex, HttpServletRequest req) {
        return problema(ex.getStatus(), titulo(ex.getStatus()), ex.getMessage(), req);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail manejarRestricciones(ConstraintViolationException ex, HttpServletRequest req) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v ->
                errores.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        ProblemDetail p = problema(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "Uno o más parámetros no son válidos", req);
        p.setProperty("errores", errores);
        return p;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail manejarIntegridad(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "Conflicto de datos",
                "La operación viola una restricción de integridad de los datos", req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail manejarAccesoDenegado(AccessDeniedException ex, HttpServletRequest req) {
        return problema(HttpStatus.FORBIDDEN, "Acceso denegado",
                "No tiene permisos para este recurso", req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail manejarNoAutenticado(AuthenticationException ex, HttpServletRequest req) {
        return problema(HttpStatus.UNAUTHORIZED, "No autenticado",
                "Se requiere un token JWT válido", req);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail manejarGenerico(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado en {}", req.getRequestURI(), ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado. Intente nuevamente más tarde", req);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e ->
                errores.putIfAbsent(e.getField(), e.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(e ->
                errores.putIfAbsent(e.getObjectName(), e.getDefaultMessage()));
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Uno o más campos no son válidos");
        p.setTitle("Solicitud inválida");
        p.setProperty("errores", errores);
        return handleExceptionInternal(ex, p, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail p && p.getInstance() == null
                && request instanceof ServletWebRequest swr) {
            p.setInstance(URI.create(swr.getRequest().getRequestURI()));
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private ProblemDetail problema(HttpStatus status, String titulo, String detalle,
            HttpServletRequest req) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detalle);
        p.setTitle(titulo);
        p.setInstance(URI.create(req.getRequestURI()));
        return p;
    }

    private String titulo(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Recurso no encontrado";
            case CONFLICT -> "Conflicto";
            case FORBIDDEN -> "Acceso denegado";
            case UNPROCESSABLE_ENTITY -> "Regla de negocio incumplida";
            default -> status.getReasonPhrase();
        };
    }
}
