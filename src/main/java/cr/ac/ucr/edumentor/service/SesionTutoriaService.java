package cr.ac.ucr.edumentor.service;

import cr.ac.ucr.edumentor.dto.SesionRequest;
import cr.ac.ucr.edumentor.dto.SesionResponse;
import cr.ac.ucr.edumentor.exception.ConflictoNegocioException;
import cr.ac.ucr.edumentor.exception.RecursoNoEncontradoException;
import cr.ac.ucr.edumentor.exception.ReglaNegocioException;
import cr.ac.ucr.edumentor.model.EstadoSesion;
import cr.ac.ucr.edumentor.model.Materia;
import cr.ac.ucr.edumentor.model.Rol;
import cr.ac.ucr.edumentor.model.SesionTutoria;
import cr.ac.ucr.edumentor.model.Usuario;
import cr.ac.ucr.edumentor.repository.MateriaRepository;
import cr.ac.ucr.edumentor.repository.SesionTutoriaRepository;
import cr.ac.ucr.edumentor.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class SesionTutoriaService {

    // Horas mínimas de anticipación para poder cancelar una sesión.
    private static final long HORAS_MINIMAS_CANCELACION = 2;

    // Estados que bloquean el horario (los que cuentan para los traslapes).
    private static final Set<EstadoSesion> ESTADOS_QUE_OCUPAN = EnumSet.of(
            EstadoSesion.SOLICITADA, EstadoSesion.ACEPTADA, EstadoSesion.EN_CURSO);

    private final SesionTutoriaRepository sesiones;
    private final UsuarioRepository usuarios;
    private final MateriaRepository materias;
    private final DisponibilidadTutorService disponibilidades;
    private final Clock reloj;

    public SesionTutoriaService(SesionTutoriaRepository sesiones,
                                UsuarioRepository usuarios,
                                MateriaRepository materias,
                                DisponibilidadTutorService disponibilidades,
                                Clock reloj) {
        this.sesiones = sesiones;
        this.usuarios = usuarios;
        this.materias = materias;
        this.disponibilidades = disponibilidades;
        this.reloj = reloj;
    }

    // ---------- Consultas ----------

    // Cada rol ve cosas distintas: el estudiante sus solicitudes, el tutor las que le piden
    // y el coordinador todas. El filtro por estado es opcional (si viene null se devuelven todas).
    @Transactional(readOnly = true)
    public List<SesionResponse> listar(String email, EstadoSesion estado) {
        Usuario usuario = buscarUsuario(email);
        List<SesionTutoria> lista = switch (usuario.getRol()) {
            case ESTUDIANTE -> sesiones.findByEstudianteIdOrderByFechaHoraDesc(usuario.getId());
            case TUTOR -> sesiones.findByTutorIdOrderByFechaHoraDesc(usuario.getId());
            case COORDINADOR -> sesiones.findAllByOrderByFechaHoraDesc();
        };
        return lista.stream()
                .filter(s -> estado == null || s.getEstado() == estado)
                .map(SesionResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public SesionResponse obtener(String email, Long id) {
        Usuario usuario = buscarUsuario(email);
        SesionTutoria s = buscarSesion(id);
        // El coordinador ve todo; los demás solo las sesiones donde participan.
        boolean participa = esElEstudiante(s, email) || esElTutor(s, email);
        if (usuario.getRol() != Rol.COORDINADOR && !participa) {
            throw new ReglaNegocioException(HttpStatus.FORBIDDEN, "No tiene acceso a esta sesión");
        }
        return SesionResponse.de(s);
    }

    // ---------- Solicitar (solo estudiantes) ----------

    @Transactional
    public SesionResponse solicitar(String emailEstudiante, SesionRequest req) {
        Usuario estudiante = buscarUsuario(emailEstudiante);
        if (estudiante.getRol() != Rol.ESTUDIANTE) {
            throw new ReglaNegocioException(HttpStatus.FORBIDDEN,
                    "Solo los estudiantes pueden solicitar sesiones");
        }

        // La sesión tiene que ser a futuro
        if (!req.fechaHora().isAfter(LocalDateTime.now(reloj))) {
            throw new ReglaNegocioException("La sesión debe ser en una fecha y hora futura");
        }

        Usuario tutor = usuarios.findById(req.tutorId())
                .filter(u -> u.getRol() == Rol.TUTOR)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tutor no encontrado: " + req.tutorId()));
        Materia materia = materias.findById(req.materiaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Materia no encontrada: " + req.materiaId()));

        LocalDateTime inicio = req.fechaHora();
        LocalDateTime fin = inicio.plusMinutes(req.duracionMinutos());

        // 1) La sesión tiene que caber en un bloque de disponibilidad del tutor.
        if (!disponibilidades.estaDisponible(tutor.getId(), inicio, req.duracionMinutos())) {
            throw new ReglaNegocioException("El tutor no tiene disponibilidad en ese horario");
        }

        // 2) No puede chocar con otra sesión del tutor ni del estudiante.
        validarSinTraslape(tutor.getId(), estudiante.getId(), inicio, fin);

        SesionTutoria s = new SesionTutoria();
        s.setEstudiante(estudiante);
        s.setTutor(tutor);
        s.setMateria(materia);
        s.setFechaHora(inicio);
        s.setDuracionMinutos(req.duracionMinutos());
        s.setTema(req.tema());
        s.setEstado(EstadoSesion.SOLICITADA);
        s.setFechaCreacion(LocalDateTime.now(reloj));
        return SesionResponse.de(sesiones.save(s));
    }

    // ---------- Acciones sobre una sesión ----------

    // El tutor acepta o rechaza una solicitud que le hicieron a él.
    @Transactional
    public SesionResponse aceptar(String emailTutor, Long id) {
        SesionTutoria s = buscarSesionDelTutor(emailTutor, id);
        return cambiarEstado(s, EstadoSesion.ACEPTADA);
    }

    @Transactional
    public SesionResponse rechazar(String emailTutor, Long id) {
        SesionTutoria s = buscarSesionDelTutor(emailTutor, id);
        return cambiarEstado(s, EstadoSesion.RECHAZADA);
    }

    @Transactional
    public SesionResponse iniciar(String emailTutor, Long id) {
        SesionTutoria s = buscarSesionDelTutor(emailTutor, id);
        return cambiarEstado(s, EstadoSesion.EN_CURSO);
    }

    @Transactional
    public SesionResponse concluir(String emailTutor, Long id) {
        SesionTutoria s = buscarSesionDelTutor(emailTutor, id);
        return cambiarEstado(s, EstadoSesion.CONCLUIDA);
    }

    // Puede cancelar el estudiante que la pidió o el tutor que la atiende,
    // pero solo si faltan 2 horas o más para que empiece.
    @Transactional
    public SesionResponse cancelar(String email, Long id) {
        SesionTutoria s = buscarSesion(id);
        if (!esElEstudiante(s, email) && !esElTutor(s, email)) {
            throw new ReglaNegocioException(HttpStatus.FORBIDDEN,
                    "Solo el estudiante o el tutor de la sesión pueden cancelarla");
        }
        // Primero se revisa que el estado permita cancelar...
        if (!s.getEstado().puedePasarA(EstadoSesion.CANCELADA)) {
            throw new ConflictoNegocioException("No se puede cancelar una sesión en estado " + s.getEstado());
        }
        // ...y después la regla de las 2 horas.
        Duration faltante = Duration.between(LocalDateTime.now(reloj), s.getFechaHora());
        if (faltante.compareTo(Duration.ofHours(HORAS_MINIMAS_CANCELACION)) < 0) {
            throw new ReglaNegocioException(
                    "Solo se puede cancelar con al menos " + HORAS_MINIMAS_CANCELACION + " horas de anticipación");
        }
        return cambiarEstado(s, EstadoSesion.CANCELADA);
    }

    // ---------- Reglas de negocio ----------

    /**
     * Dos sesiones [inicioA, finA) y [inicioB, finB) se traslapan si
     * inicioA < finB y inicioB < finA (misma fórmula que en disponibilidad).
     * Una que termina a las 10:00 y otra que empieza a las 10:00 NO se traslapan.
     * Como ninguna sesión pasa de medianoche, basta con revisar las sesiones de ese día.
     */
    private void validarSinTraslape(Long tutorId, Long estudianteId,
                                    LocalDateTime inicio, LocalDateTime fin) {
        LocalDateTime desde = inicio.toLocalDate().atStartOfDay();
        LocalDateTime hasta = desde.plusDays(1);

        for (SesionTutoria s : sesiones.buscarDelTutorEnRango(tutorId, ESTADOS_QUE_OCUPAN, desde, hasta)) {
            if (seTraslapan(s, inicio, fin)) {
                throw new ConflictoNegocioException("El tutor ya tiene una sesión de "
                        + s.getFechaHora().toLocalTime() + " a " + s.getFechaFin().toLocalTime());
            }
        }
        for (SesionTutoria s : sesiones.buscarDelEstudianteEnRango(estudianteId, ESTADOS_QUE_OCUPAN, desde, hasta)) {
            if (seTraslapan(s, inicio, fin)) {
                throw new ConflictoNegocioException("Usted ya tiene una sesión de "
                        + s.getFechaHora().toLocalTime() + " a " + s.getFechaFin().toLocalTime());
            }
        }
    }

    private boolean seTraslapan(SesionTutoria existente, LocalDateTime inicio, LocalDateTime fin) {
        return inicio.isBefore(existente.getFechaFin()) && existente.getFechaHora().isBefore(fin);
    }

    // Aplica la transición solo si la máquina de estados la permite.
    private SesionResponse cambiarEstado(SesionTutoria s, EstadoSesion destino) {
        if (!s.getEstado().puedePasarA(destino)) {
            throw new ConflictoNegocioException(
                    "No se puede pasar de " + s.getEstado() + " a " + destino);
        }
        s.setEstado(destino);
        sesiones.save(s);
        return SesionResponse.de(s);
    }

    // ---------- Auxiliares ----------

    private Usuario buscarUsuario(String email) {
        return usuarios.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private SesionTutoria buscarSesion(Long id) {
        return sesiones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada: " + id));
    }

    // Busca la sesión y revisa que el tutor que hace la acción sea el dueño de la sesión.
    private SesionTutoria buscarSesionDelTutor(String emailTutor, Long id) {
        SesionTutoria s = buscarSesion(id);
        if (!esElTutor(s, emailTutor)) {
            throw new ReglaNegocioException(HttpStatus.FORBIDDEN,
                    "Solo el tutor asignado puede realizar esta acción sobre la sesión");
        }
        return s;
    }

    private boolean esElTutor(SesionTutoria s, String email) {
        return s.getTutor().getEmail().equals(email);
    }

    private boolean esElEstudiante(SesionTutoria s, String email) {
        return s.getEstudiante().getEmail().equals(email);
    }
}
