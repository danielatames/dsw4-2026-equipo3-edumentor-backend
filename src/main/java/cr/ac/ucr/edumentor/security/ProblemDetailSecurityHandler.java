package cr.ac.ucr.edumentor.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
public class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper mapper;

    public ProblemDetailSecurityHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res,
            AuthenticationException ex) throws IOException {
        escribir(req, res, HttpStatus.UNAUTHORIZED, "No autenticado",
                "Se requiere un token JWT válido");
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res,
            AccessDeniedException ex) throws IOException {
        escribir(req, res, HttpStatus.FORBIDDEN, "Acceso denegado",
                "No tiene permisos para este recurso");
    }

    private void escribir(HttpServletRequest req, HttpServletResponse res,
            HttpStatus status, String title, String detail) throws IOException {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setTitle(title);
        p.setInstance(URI.create(req.getRequestURI()));
        res.setStatus(status.value());
        res.setContentType("application/problem+json");
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getWriter(), p);
    }
}