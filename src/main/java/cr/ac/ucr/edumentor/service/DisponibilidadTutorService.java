package cr.ac.ucr.edumentor.service;

import cr.ac.ucr.edumentor.dto.DisponibilidadRequest;
import cr.ac.ucr.edumentor.dto.DisponibilidadResponse;
import cr.ac.ucr.edumentor.exception.ConflictoNegocioException;
import cr.ac.ucr.edumentor.exception.RecursoNoEncontradoException;
import cr.ac.ucr.edumentor.exception.ReglaNegocioException;
import cr.ac.ucr.edumentor.model.DisponibilidadTutor;
import cr.ac.ucr.edumentor.model.Rol;
import cr.ac.ucr.edumentor.model.Usuario;
import cr.ac.ucr.edumentor.repository.DisponibilidadTutorRepository;
import cr.ac.ucr.edumentor.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
public class DisponibilidadTutorService {

    // Ordena los bloques de lunes a domingo y, dentro del día, por hora de inicio.
    private static final Comparator<DisponibilidadTutor> POR_DIA_Y_HORA =
            Comparator.comparing(DisponibilidadTutor::getDiaSemana)
                    .thenComparing(DisponibilidadTutor::getHoraInicio);

    private final DisponibilidadTutorRepository disponibilidades;
    private final UsuarioRepository usuarios;

    public DisponibilidadTutorService(DisponibilidadTutorRepository disponibilidades,
                                      UsuarioRepository usuarios) {
        this.disponibilidades = disponibilidades;
        this.usuarios = usuarios;
    }

    // ---------- Consultas ----------

    @Transactional(readOnly = true)
    public List<DisponibilidadResponse> listarActivas() {
        return aRespuesta(disponibilidades.findByEstaActivoTrue());
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadResponse> listarPorTutor(Long tutorId) {
        Usuario tutor = usuarios.findById(tutorId)
                .filter(u -> u.getRol() == Rol.TUTOR)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tutor no encontrado: " + tutorId));
        return aRespuesta(disponibilidades.findByTutorIdAndEstaActivoTrue(tutor.getId()));
    }

    // El tutor ve todos sus bloques, también los desactivados.
    @Transactional(readOnly = true)
    public List<DisponibilidadResponse> listarMias(String emailTutor) {
        Usuario tutor = buscarTutor(emailTutor);
        return aRespuesta(disponibilidades.findByTutorId(tutor.getId()));
    }

    // ---------- Escritura (solo el tutor dueño) ----------

    @Transactional
    public DisponibilidadResponse crear(String emailTutor, DisponibilidadRequest req) {
        Usuario tutor = buscarTutor(emailTutor);
        validarHorario(req.horaInicio(), req.horaFin());
        validarSinSolape(tutor.getId(), req.diaSemana(), req.horaInicio(), req.horaFin(), null);

        DisponibilidadTutor d = new DisponibilidadTutor();
        d.setTutor(tutor);
        d.setDiaSemana(req.diaSemana());
        d.setHoraInicio(req.horaInicio());
        d.setHoraFin(req.horaFin());
        d.setEstaActivo(true);
        return DisponibilidadResponse.de(disponibilidades.save(d));
    }

    @Transactional
    public DisponibilidadResponse actualizar(String emailTutor, Long id, DisponibilidadRequest req) {
        DisponibilidadTutor d = buscarPropio(emailTutor, id);
        validarHorario(req.horaInicio(), req.horaFin());
        // Un bloque inactivo no choca con nadie; se valida al reactivarlo.
        if (d.isEstaActivo()) {
            validarSinSolape(d.getTutor().getId(), req.diaSemana(), req.horaInicio(), req.horaFin(), d.getId());
        }
        d.setDiaSemana(req.diaSemana());
        d.setHoraInicio(req.horaInicio());
        d.setHoraFin(req.horaFin());
        return DisponibilidadResponse.de(disponibilidades.save(d));
    }

    @Transactional
    public DisponibilidadResponse cambiarEstado(String emailTutor, Long id, boolean activo) {
        DisponibilidadTutor d = buscarPropio(emailTutor, id);
        if (activo && !d.isEstaActivo()) {
            validarSinSolape(d.getTutor().getId(), d.getDiaSemana(), d.getHoraInicio(), d.getHoraFin(), d.getId());
        }
        d.setEstaActivo(activo);
        return DisponibilidadResponse.de(disponibilidades.save(d));
    }

    // ---------- Contrato para SesionTutoria (Persona 3) ----------

    /**
     * Indica si la sesión [fechaHora, fechaHora + duracion) cabe completa dentro
     * de algún bloque activo del tutor ese día de la semana.
     * Solo revisa el horario del tutor; los traslapes con otras sesiones
     * los valida el servicio de sesiones.
     */
    @Transactional(readOnly = true)
    public boolean estaDisponible(Long tutorId, LocalDateTime fechaHora, int duracionMinutos) {
        if (duracionMinutos <= 0) {
            return false;
        }
        LocalTime inicio = fechaHora.toLocalTime();
        LocalTime fin = inicio.plusMinutes(duracionMinutos);
        if (!fin.isAfter(inicio)) {
            return false; // la sesión pasaría de medianoche
        }
        return disponibilidades
                .findByTutorIdAndDiaSemanaAndEstaActivoTrue(tutorId, fechaHora.getDayOfWeek())
                .stream()
                .anyMatch(b -> !inicio.isBefore(b.getHoraInicio()) && !fin.isAfter(b.getHoraFin()));
    }

    // ---------- Reglas de negocio ----------

    private void validarHorario(LocalTime inicio, LocalTime fin) {
        if (!fin.isAfter(inicio)) {
            throw new ReglaNegocioException("La hora de fin debe ser posterior a la hora de inicio");
        }
    }

    /**
     * Dos rangos [inicioA, finA) y [inicioB, finB) se solapan si
     * inicioA < finB y inicioB < finA. Bloques contiguos (8-10 y 10-12) sí se permiten.
     */
    private void validarSinSolape(Long tutorId, DayOfWeek dia, LocalTime inicio, LocalTime fin,
                                  Long idExcluido) {
        for (DisponibilidadTutor b : disponibilidades.findByTutorIdAndDiaSemanaAndEstaActivoTrue(tutorId, dia)) {
            boolean esElMismo = b.getId().equals(idExcluido);
            boolean seSolapa = inicio.isBefore(b.getHoraFin()) && b.getHoraInicio().isBefore(fin);
            if (!esElMismo && seSolapa) {
                throw new ConflictoNegocioException("El bloque se solapa con otro bloque activo de "
                        + b.getHoraInicio() + " a " + b.getHoraFin());
            }
        }
    }

    private Usuario buscarTutor(String email) {
        Usuario u = usuarios.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if (u.getRol() != Rol.TUTOR) {
            throw new ReglaNegocioException(HttpStatus.FORBIDDEN,
                    "Solo los usuarios con rol TUTOR pueden tener disponibilidad");
        }
        return u;
    }

    private DisponibilidadTutor buscarPropio(String emailTutor, Long id) {
        DisponibilidadTutor d = disponibilidades.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Bloque de disponibilidad no encontrado: " + id));
        if (!d.getTutor().getEmail().equals(emailTutor)) {
            throw new ReglaNegocioException(HttpStatus.FORBIDDEN,
                    "Solo puede modificar sus propios bloques de disponibilidad");
        }
        return d;
    }

    private List<DisponibilidadResponse> aRespuesta(List<DisponibilidadTutor> bloques) {
        return bloques.stream().sorted(POR_DIA_Y_HORA).map(DisponibilidadResponse::de).toList();
    }
}
