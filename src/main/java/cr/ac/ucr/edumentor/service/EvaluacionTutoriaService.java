package cr.ac.ucr.edumentor.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cr.ac.ucr.edumentor.dto.CalificacionTutorResponse;
import cr.ac.ucr.edumentor.dto.EvaluacionRequest;
import cr.ac.ucr.edumentor.dto.EvaluacionResponse;
import cr.ac.ucr.edumentor.exception.ConflictoNegocioException;
import cr.ac.ucr.edumentor.exception.RecursoNoEncontradoException;
import cr.ac.ucr.edumentor.exception.ReglaNegocioException;
import cr.ac.ucr.edumentor.model.EstadoSesion;
import cr.ac.ucr.edumentor.model.EvaluacionTutoria;
import cr.ac.ucr.edumentor.model.Rol;
import cr.ac.ucr.edumentor.model.SesionTutoria;
import cr.ac.ucr.edumentor.model.Usuario;
import cr.ac.ucr.edumentor.repository.EvaluacionTutoriaRepository;
import cr.ac.ucr.edumentor.repository.SesionTutoriaRepository;
import cr.ac.ucr.edumentor.repository.UsuarioRepository;

@Service
public class EvaluacionTutoriaService {

    private final EvaluacionTutoriaRepository evaluaciones;
    private final SesionTutoriaRepository sesiones;
    private final UsuarioRepository usuarios;
    private final Clock reloj;

    public EvaluacionTutoriaService(EvaluacionTutoriaRepository evaluaciones,
                                    SesionTutoriaRepository sesiones,
                                    UsuarioRepository usuarios,
                                    Clock reloj) {
        this.evaluaciones = evaluaciones;
        this.sesiones = sesiones;
        this.usuarios = usuarios;
        this.reloj = reloj;
    }

    @Transactional
    public EvaluacionResponse evaluar(String emailEstudiante, Long sesionId, EvaluacionRequest req) {
        SesionTutoria sesion = buscarSesion(sesionId);
        if (!sesion.getEstudiante().getEmail().equals(emailEstudiante)) {
            throw new ReglaNegocioException(HttpStatus.FORBIDDEN,
                    "Solo el estudiante de la sesión puede evaluarla");
        }
        if (!sesion.getEstado().puedePasarA(EstadoSesion.EVALUADA)) {
            String detalle = sesion.getEstado() == EstadoSesion.EVALUADA
                    ? "La sesión ya fue evaluada"
                    : "Solo se pueden evaluar sesiones concluidas";
            throw new ConflictoNegocioException(detalle);
        }

        EvaluacionTutoria evaluacion = new EvaluacionTutoria();
        evaluacion.setSesion(sesion);
        evaluacion.setCalificacionEstrellas(req.calificacionEstrellas());
        evaluacion.setComentario(normalizar(req.comentario()));
        evaluacion.setFechaEvaluacion(LocalDateTime.now(reloj));
        EvaluacionTutoria guardada = evaluaciones.save(evaluacion);

        sesion.setEstado(EstadoSesion.EVALUADA);
        sesiones.save(sesion);
        actualizarPromedio(sesion.getTutor());
        return EvaluacionResponse.de(guardada);
    }

    @Transactional(readOnly = true)
    public EvaluacionResponse obtener(String email, Long sesionId) {
        SesionTutoria sesion = buscarSesion(sesionId);
        validarAcceso(sesion, email);
        return evaluaciones.findBySesionId(sesionId)
                .map(EvaluacionResponse::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("La sesión aún no tiene evaluación"));
    }

    @Transactional(readOnly = true)
    public CalificacionTutorResponse calificacionDelTutor(Long tutorId) {
        Usuario tutor = usuarios.findById(tutorId)
                .filter(u -> u.getRol() == Rol.TUTOR)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tutor no encontrado: " + tutorId));
        return CalificacionTutorResponse.de(tutor);
    }

    private void actualizarPromedio(Usuario tutor) {
        Double promedio = evaluaciones.promedioDelTutor(tutor.getId());
        long total = evaluaciones.countBySesionTutorId(tutor.getId());
        tutor.setPromedioCalificacion(promedio == null ? 0.0 : redondear(promedio));
        tutor.setTotalEvaluaciones((int) total);
        usuarios.save(tutor);
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private String normalizar(String comentario) {
        if (comentario == null || comentario.isBlank()) {
            return null;
        }
        return comentario.trim();
    }

    private SesionTutoria buscarSesion(Long id) {
        return sesiones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada: " + id));
    }

    private void validarAcceso(SesionTutoria sesion, String email) {
        boolean participa = sesion.getTutor().getEmail().equals(email)
                || sesion.getEstudiante().getEmail().equals(email);
        if (participa) {
            return;
        }
        Usuario usuario = usuarios.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if (usuario.getRol() != Rol.COORDINADOR) {
            throw new ReglaNegocioException(HttpStatus.FORBIDDEN, "No tiene acceso a esta sesión");
        }
    }
}
