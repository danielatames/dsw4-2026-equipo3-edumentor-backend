package cr.ac.ucr.edumentor.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

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

@ExtendWith(MockitoExtension.class)
class EvaluacionTutoriaServiceTest {

    private static final String EMAIL_ESTUDIANTE = "estudiante@ucr.ac.cr";
    private static final String EMAIL_TUTOR = "tutor@ucr.ac.cr";
    private static final ZoneId ZONA = ZoneId.of("America/Costa_Rica");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 25, 15, 0);

    @Mock
    private EvaluacionTutoriaRepository evaluaciones;

    @Mock
    private SesionTutoriaRepository sesiones;

    @Mock
    private UsuarioRepository usuarios;

    private EvaluacionTutoriaService servicio;
    private Usuario estudiante;
    private Usuario tutor;

    @BeforeEach
    void preparar() {
        Clock relojFijo = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        servicio = new EvaluacionTutoriaService(evaluaciones, sesiones, usuarios, relojFijo);
        estudiante = usuario(1L, EMAIL_ESTUDIANTE, Rol.ESTUDIANTE);
        tutor = usuario(2L, EMAIL_TUTOR, Rol.TUTOR);
    }

    @Test
    void evaluarGuardaCalificacionMarcaSesionComoEvaluadaYActualizaPromedio() {
        SesionTutoria sesion = sesion(7L, EstadoSesion.CONCLUIDA);
        when(sesiones.findById(7L)).thenReturn(Optional.of(sesion));
        when(evaluaciones.save(any(EvaluacionTutoria.class))).thenAnswer(inv -> inv.getArgument(0));
        when(evaluaciones.promedioDelTutor(2L)).thenReturn(4.3333333);
        when(evaluaciones.countBySesionTutorId(2L)).thenReturn(3L);

        EvaluacionResponse r = servicio.evaluar(EMAIL_ESTUDIANTE, 7L, new EvaluacionRequest(5, "  Excelente  "));

        assertEquals(5, r.calificacionEstrellas());
        assertEquals("Excelente", r.comentario());
        assertEquals(AHORA, r.fechaEvaluacion());
        assertEquals(EstadoSesion.EVALUADA, sesion.getEstado());
        assertEquals(4.33, tutor.getPromedioCalificacion());
        assertEquals(3, tutor.getTotalEvaluaciones());
        verify(sesiones).save(sesion);
        verify(usuarios).save(tutor);
    }

    @Test
    void evaluarGuardaComentarioVacioComoNulo() {
        SesionTutoria sesion = sesion(7L, EstadoSesion.CONCLUIDA);
        when(sesiones.findById(7L)).thenReturn(Optional.of(sesion));
        when(evaluaciones.save(any(EvaluacionTutoria.class))).thenAnswer(inv -> inv.getArgument(0));
        when(evaluaciones.promedioDelTutor(2L)).thenReturn(4.0);
        when(evaluaciones.countBySesionTutorId(2L)).thenReturn(1L);

        EvaluacionResponse r = servicio.evaluar(EMAIL_ESTUDIANTE, 7L, new EvaluacionRequest(4, "   "));

        assertEquals(null, r.comentario());
    }

    @Test
    void evaluarRechazaSiLaSesionNoEstaConcluida() {
        SesionTutoria sesion = sesion(7L, EstadoSesion.ACEPTADA);
        when(sesiones.findById(7L)).thenReturn(Optional.of(sesion));

        ConflictoNegocioException ex = assertThrows(ConflictoNegocioException.class,
                () -> servicio.evaluar(EMAIL_ESTUDIANTE, 7L, new EvaluacionRequest(5, null)));

        assertEquals("Solo se pueden evaluar sesiones concluidas", ex.getMessage());
        assertEquals(EstadoSesion.ACEPTADA, sesion.getEstado());
        verify(evaluaciones, never()).save(any());
    }

    @Test
    void evaluarRechazaSiLaSesionYaFueEvaluada() {
        SesionTutoria sesion = sesion(7L, EstadoSesion.EVALUADA);
        when(sesiones.findById(7L)).thenReturn(Optional.of(sesion));

        ConflictoNegocioException ex = assertThrows(ConflictoNegocioException.class,
                () -> servicio.evaluar(EMAIL_ESTUDIANTE, 7L, new EvaluacionRequest(3, null)));

        assertEquals("La sesión ya fue evaluada", ex.getMessage());
        verify(evaluaciones, never()).save(any());
    }

    @Test
    void evaluarRechazaAQuienNoEsElEstudianteDeLaSesion() {
        SesionTutoria sesion = sesion(7L, EstadoSesion.CONCLUIDA);
        when(sesiones.findById(7L)).thenReturn(Optional.of(sesion));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.evaluar("otro@ucr.ac.cr", 7L, new EvaluacionRequest(5, null)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(EstadoSesion.CONCLUIDA, sesion.getEstado());
        verify(evaluaciones, never()).save(any());
    }

    @Test
    void evaluarFallaSiLaSesionNoExiste() {
        when(sesiones.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.evaluar(EMAIL_ESTUDIANTE, 99L, new EvaluacionRequest(5, null)));
    }

    @Test
    void obtenerDevuelveLaEvaluacionAlTutorDeLaSesion() {
        SesionTutoria sesion = sesion(7L, EstadoSesion.EVALUADA);
        EvaluacionTutoria e = new EvaluacionTutoria();
        e.setId(3L);
        e.setSesion(sesion);
        e.setCalificacionEstrellas(4);
        e.setFechaEvaluacion(AHORA);
        when(sesiones.findById(7L)).thenReturn(Optional.of(sesion));
        when(evaluaciones.findBySesionId(7L)).thenReturn(Optional.of(e));

        EvaluacionResponse r = servicio.obtener(EMAIL_TUTOR, 7L);

        assertEquals(4, r.calificacionEstrellas());
        assertEquals(7L, r.sesionId());
    }

    @Test
    void obtenerFallaSiLaSesionNoTieneEvaluacion() {
        SesionTutoria sesion = sesion(7L, EstadoSesion.CONCLUIDA);
        when(sesiones.findById(7L)).thenReturn(Optional.of(sesion));
        when(evaluaciones.findBySesionId(7L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(EMAIL_ESTUDIANTE, 7L));
    }

    @Test
    void obtenerRechazaAUnEstudianteAjeno() {
        SesionTutoria sesion = sesion(7L, EstadoSesion.EVALUADA);
        Usuario ajeno = usuario(9L, "ajeno@ucr.ac.cr", Rol.ESTUDIANTE);
        when(sesiones.findById(7L)).thenReturn(Optional.of(sesion));
        when(usuarios.findByEmail("ajeno@ucr.ac.cr")).thenReturn(Optional.of(ajeno));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.obtener("ajeno@ucr.ac.cr", 7L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void obtenerPermiteAlCoordinador() {
        SesionTutoria sesion = sesion(7L, EstadoSesion.EVALUADA);
        Usuario coordinador = usuario(8L, "coordinador@ucr.ac.cr", Rol.COORDINADOR);
        EvaluacionTutoria e = new EvaluacionTutoria();
        e.setId(3L);
        e.setSesion(sesion);
        e.setCalificacionEstrellas(5);
        e.setFechaEvaluacion(AHORA);
        when(sesiones.findById(7L)).thenReturn(Optional.of(sesion));
        when(usuarios.findByEmail("coordinador@ucr.ac.cr")).thenReturn(Optional.of(coordinador));
        when(evaluaciones.findBySesionId(7L)).thenReturn(Optional.of(e));

        assertEquals(5, servicio.obtener("coordinador@ucr.ac.cr", 7L).calificacionEstrellas());
    }

    @Test
    void calificacionDelTutorDevuelvePromedioYTotal() {
        tutor.setPromedioCalificacion(4.5);
        tutor.setTotalEvaluaciones(8);
        when(usuarios.findById(2L)).thenReturn(Optional.of(tutor));

        CalificacionTutorResponse r = servicio.calificacionDelTutor(2L);

        assertEquals(4.5, r.promedio());
        assertEquals(8, r.totalEvaluaciones());
        assertEquals("Luis Tutor", r.tutorNombre());
    }

    @Test
    void calificacionDelTutorFallaSiElUsuarioNoEsTutor() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(estudiante));

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.calificacionDelTutor(1L));
    }

    private SesionTutoria sesion(Long id, EstadoSesion estado) {
        SesionTutoria s = new SesionTutoria();
        s.setId(id);
        s.setEstudiante(estudiante);
        s.setTutor(tutor);
        s.setEstado(estado);
        s.setFechaHora(AHORA.minusDays(1));
        s.setDuracionMinutos(60);
        return s;
    }

    private Usuario usuario(Long id, String email, Rol rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        u.setNombre(rol == Rol.TUTOR ? "Luis" : "Ana");
        u.setApellido(rol == Rol.TUTOR ? "Tutor" : "Estudiante");
        u.setRol(rol);
        return u;
    }
}
