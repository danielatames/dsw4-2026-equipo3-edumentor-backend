package cr.ac.ucr.edumentor.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cr.ac.ucr.edumentor.dto.RecursoRequest;
import cr.ac.ucr.edumentor.dto.RecursoResponse;
import cr.ac.ucr.edumentor.exception.ConflictoNegocioException;
import cr.ac.ucr.edumentor.exception.RecursoNoEncontradoException;
import cr.ac.ucr.edumentor.exception.ReglaNegocioException;
import cr.ac.ucr.edumentor.model.EstadoSesion;
import cr.ac.ucr.edumentor.model.RecursoEstudio;
import cr.ac.ucr.edumentor.model.Rol;
import cr.ac.ucr.edumentor.model.SesionTutoria;
import cr.ac.ucr.edumentor.model.Usuario;
import cr.ac.ucr.edumentor.repository.RecursoEstudioRepository;
import cr.ac.ucr.edumentor.repository.SesionTutoriaRepository;
import cr.ac.ucr.edumentor.repository.UsuarioRepository;

@Service
public class RecursoEstudioService {

    private static final Set<EstadoSesion> ESTADOS_QUE_ADMITEN_RECURSOS = EnumSet.of(
            EstadoSesion.ACEPTADA, EstadoSesion.EN_CURSO, EstadoSesion.CONCLUIDA, EstadoSesion.EVALUADA);

    private final RecursoEstudioRepository recursos;
    private final SesionTutoriaRepository sesiones;
    private final UsuarioRepository usuarios;

    public RecursoEstudioService(RecursoEstudioRepository recursos,
                                 SesionTutoriaRepository sesiones,
                                 UsuarioRepository usuarios) {
        this.recursos = recursos;
        this.sesiones = sesiones;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<RecursoResponse> listar(String email, Long sesionId) {
        SesionTutoria sesion = buscarSesion(sesionId);
        validarAcceso(sesion, email);
        return recursos.findBySesionIdOrderByIdAsc(sesionId).stream()
                .map(RecursoResponse::de)
                .toList();
    }

    @Transactional
    public RecursoResponse agregar(String emailTutor, Long sesionId, RecursoRequest req) {
        SesionTutoria sesion = buscarSesionDelTutor(emailTutor, sesionId);
        if (!ESTADOS_QUE_ADMITEN_RECURSOS.contains(sesion.getEstado())) {
            throw new ConflictoNegocioException(
                    "No se pueden agregar recursos a una sesión en estado " + sesion.getEstado());
        }
        RecursoEstudio recurso = new RecursoEstudio();
        recurso.setSesion(sesion);
        recurso.setTitulo(req.titulo().trim());
        recurso.setUrlRecurso(req.urlRecurso().trim());
        recurso.setTipoArchivo(req.tipoArchivo());
        return RecursoResponse.de(recursos.save(recurso));
    }

    @Transactional
    public void eliminar(String emailTutor, Long sesionId, Long recursoId) {
        buscarSesionDelTutor(emailTutor, sesionId);
        RecursoEstudio recurso = recursos.findByIdAndSesionId(recursoId, sesionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recurso no encontrado: " + recursoId));
        recursos.delete(recurso);
    }

    private SesionTutoria buscarSesion(Long id) {
        return sesiones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada: " + id));
    }

    private SesionTutoria buscarSesionDelTutor(String emailTutor, Long sesionId) {
        SesionTutoria sesion = buscarSesion(sesionId);
        if (!sesion.getTutor().getEmail().equals(emailTutor)) {
            throw new ReglaNegocioException(HttpStatus.FORBIDDEN,
                    "Solo el tutor asignado puede administrar los recursos de la sesión");
        }
        return sesion;
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
