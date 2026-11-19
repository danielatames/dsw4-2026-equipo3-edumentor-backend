package cr.ac.ucr.edumentor.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import cr.ac.ucr.edumentor.dto.SesionRequest;
import cr.ac.ucr.edumentor.dto.SesionResponse;
import cr.ac.ucr.edumentor.exception.ConflictoNegocioException;
import cr.ac.ucr.edumentor.exception.ReglaNegocioException;
import cr.ac.ucr.edumentor.model.EstadoSesion;
import cr.ac.ucr.edumentor.model.Materia;
import cr.ac.ucr.edumentor.model.Rol;
import cr.ac.ucr.edumentor.model.SesionTutoria;
import cr.ac.ucr.edumentor.model.Usuario;
import cr.ac.ucr.edumentor.repository.MateriaRepository;
import cr.ac.ucr.edumentor.repository.SesionTutoriaRepository;
import cr.ac.ucr.edumentor.repository.UsuarioRepository;

/**
 * Para no depender de la hora real se usa un reloj fijo:
 * "hoy" es el jueves 19/11/2026 a las 10:00 (hora de Costa Rica).
 */
@ExtendWith(MockitoExtension.class)
class SesionTutoriaServiceTest {

    private static final String EMAIL_ESTUDIANTE = "estudiante@ucr.ac.cr";
    private static final String EMAIL_TUTOR = "tutor@ucr.ac.cr";
    private static final ZoneId ZONA = ZoneId.of("America/Costa_Rica");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 19, 10, 0);

    // El lunes 23/11/2026 a las 08:00, la sesión que se intenta reservar en las
    // pruebas
    private static final LocalDateTime LUNES_8AM = LocalDateTime.of(2026, 11, 23, 8, 0);

    @Mock
    private SesionTutoriaRepository sesiones;

    @Mock
    private UsuarioRepository usuarios;

    @Mock
    private MateriaRepository materias;

    @Mock
    private DisponibilidadTutorService disponibilidades;

    private SesionTutoriaService servicio;

    private Usuario estudiante;
    private Usuario tutor;
    private Materia materia;

    @BeforeEach
    void preparar() {
        // El servicio se armo a mano porque necesita el reloj fijo
        Clock relojFijo = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        servicio = new SesionTutoriaService(sesiones, usuarios, materias, disponibilidades, relojFijo);

        estudiante = usuario(1L, EMAIL_ESTUDIANTE, "Ana", "Estudiante", Rol.ESTUDIANTE);
        tutor = usuario(2L, EMAIL_TUTOR, "Luis", "Tutor", Rol.TUTOR);

        materia = new Materia();
        materia.setId(1L);
        materia.setNombreMateria("Bases de Datos II");
    }

    // ---------- Los 2 escenarios mínimos del enunciado ----------

    @Test
    void solicitarRechazaSesionQueSeTraslapaConOtraDelTutor() {
        // Dado: el tutor tiene disponibilidad, pero ya tiene una sesión aceptada de
        // 08:00 a 09:00
        prepararSolicitudValida();
        when(disponibilidades.estaDisponible(2L, LocalDateTime.of(2026, 11, 23, 8, 30), 60))
                .thenReturn(true);
        when(sesiones.buscarDelTutorEnRango(eq(2L), any(), any(), any()))
                .thenReturn(List.of(sesion(10L, EstadoSesion.ACEPTADA, LUNES_8AM, 60)));

        // Cuando: el estudiante pide una de 08:30 a 09:30 (choca con la anterior)
        var req = new SesionRequest(2L, 1L, LocalDateTime.of(2026, 11, 23, 8, 30), 60, "Repaso");

        // Entonces: se rechaza con conflicto y no se guarda nada
        assertThrows(ConflictoNegocioException.class, () -> servicio.solicitar(EMAIL_ESTUDIANTE, req));
        verify(sesiones, never()).save(any());
    }

    @Test
    void solicitarGuardaSesionCuandoHayDisponibilidadYNoHayTraslape() {
        // Dado: el tutor tiene disponibilidad y no tiene otras sesiones ese día
        prepararSolicitudValida();
        when(disponibilidades.estaDisponible(2L, LUNES_8AM, 60)).thenReturn(true);
        when(sesiones.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Cuando: el estudiante pide el lunes de 08:00 a 09:00
        var req = new SesionRequest(2L, 1L, LUNES_8AM, 60, "Repaso de JOINs");
        SesionResponse r = servicio.solicitar(EMAIL_ESTUDIANTE, req);

        // Entonces: se guarda en estado SOLICITADA con los datos correctos
        assertEquals(EstadoSesion.SOLICITADA, r.estado());
        assertEquals(LUNES_8AM, r.fechaHora());
        assertEquals(LUNES_8AM.plusMinutes(60), r.fechaFin());
        verify(sesiones).save(any(SesionTutoria.class));
    }

    // ---------- Otras reglas importantes ----------
    @Test
    void solicitarAceptaSesionQueEmpiezaJustoCuandoTerminaOtra() {
        // Dado: el tutor tiene una sesión de 08:00 a 09:00
        prepararSolicitudValida();
        when(disponibilidades.estaDisponible(2L, LocalDateTime.of(2026, 11, 23, 9, 0), 60))
                .thenReturn(true);
        when(sesiones.buscarDelTutorEnRango(eq(2L), any(), any(), any()))
                .thenReturn(List.of(sesion(10L, EstadoSesion.ACEPTADA, LUNES_8AM, 60)));
        when(sesiones.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Cuando: pide una de 09:00 a 10:00 (empieza justo cuando termina la otra)
        var req = new SesionRequest(2L, 1L, LocalDateTime.of(2026, 11, 23, 9, 0), 60, null);

        // Entonces: no hay traslape y se guarda
        assertDoesNotThrow(() -> servicio.solicitar(EMAIL_ESTUDIANTE, req));
        verify(sesiones).save(any(SesionTutoria.class));
    }

    @Test
    void solicitarRechazaSiElTutorNoTieneDisponibilidad() {
        prepararSolicitudValida();
        when(disponibilidades.estaDisponible(2L, LUNES_8AM, 60)).thenReturn(false);

        var req = new SesionRequest(2L, 1L, LUNES_8AM, 60, null);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.solicitar(EMAIL_ESTUDIANTE, req));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        verify(sesiones, never()).save(any());
    }

    @Test
    void solicitarRechazaFechaEnElPasado() {
        when(usuarios.findByEmail(EMAIL_ESTUDIANTE)).thenReturn(Optional.of(estudiante));

        // Ayer, con respecto al reloj fijo
        var req = new SesionRequest(2L, 1L, AHORA.minusDays(1), 60, null);

        assertThrows(ReglaNegocioException.class, () -> servicio.solicitar(EMAIL_ESTUDIANTE, req));
        verify(sesiones, never()).save(any());
    }

    @Test
    void cancelarRechazaSiFaltanMenosDeDosHoras() {
        // Dado: una sesión aceptada que empieza a las 11:30 y "ahora" son las 10:00
        // (falta 1h30)
        SesionTutoria s = sesion(5L, EstadoSesion.ACEPTADA, AHORA.plusMinutes(90), 60);
        when(sesiones.findById(5L)).thenReturn(Optional.of(s));

        // Entonces: no deja cancelar y el estado no cambia
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.cancelar(EMAIL_ESTUDIANTE, 5L));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        assertEquals(EstadoSesion.ACEPTADA, s.getEstado());
        verify(sesiones, never()).save(any());
    }

    @Test
    void cancelarPermiteSiFaltanExactamenteDosHoras() {
        // Dado: una sesión que empieza a las 12:00 (faltan justo 2 horas)
        SesionTutoria s = sesion(5L, EstadoSesion.ACEPTADA, AHORA.plusHours(2), 60);
        when(sesiones.findById(5L)).thenReturn(Optional.of(s));

        SesionResponse r = servicio.cancelar(EMAIL_ESTUDIANTE, 5L);

        assertEquals(EstadoSesion.CANCELADA, r.estado());
        verify(sesiones).save(s);
    }

    @Test
    void cancelarRechazaAUnUsuarioQueNoParticipaEnLaSesion() {
        SesionTutoria s = sesion(5L, EstadoSesion.ACEPTADA, AHORA.plusDays(2), 60);
        when(sesiones.findById(5L)).thenReturn(Optional.of(s));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.cancelar("otro@ucr.ac.cr", 5L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(sesiones, never()).save(any());
    }

    @Test
    void tutorPuedeAceptarUnaSolicitud() {
        SesionTutoria s = sesion(5L, EstadoSesion.SOLICITADA, AHORA.plusDays(2), 60);
        when(sesiones.findById(5L)).thenReturn(Optional.of(s));

        SesionResponse r = servicio.aceptar(EMAIL_TUTOR, 5L);

        assertEquals(EstadoSesion.ACEPTADA, r.estado());
    }

    @Test
    void noSePuedeConcluirUnaSesionQueNoHaIniciado() {
        // SOLICITADA -> CONCLUIDA no es una transición válida
        SesionTutoria s = sesion(5L, EstadoSesion.SOLICITADA, AHORA.plusDays(2), 60);
        when(sesiones.findById(5L)).thenReturn(Optional.of(s));

        assertThrows(ConflictoNegocioException.class, () -> servicio.concluir(EMAIL_TUTOR, 5L));
        assertEquals(EstadoSesion.SOLICITADA, s.getEstado());
        verify(sesiones, never()).save(any());
    }

    @Test
    void otroTutorNoPuedeAceptarSesionAjena() {
        SesionTutoria s = sesion(5L, EstadoSesion.SOLICITADA, AHORA.plusDays(2), 60);
        when(sesiones.findById(5L)).thenReturn(Optional.of(s));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.aceptar("otrotutor@ucr.ac.cr", 5L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void maquinaDeEstadosSoloPermiteLasTransicionesDefinidas() {
        assertTrue(EstadoSesion.SOLICITADA.puedePasarA(EstadoSesion.ACEPTADA));
        assertTrue(EstadoSesion.ACEPTADA.puedePasarA(EstadoSesion.EN_CURSO));
        assertTrue(EstadoSesion.EN_CURSO.puedePasarA(EstadoSesion.CONCLUIDA));
        assertFalse(EstadoSesion.EN_CURSO.puedePasarA(EstadoSesion.CANCELADA));
        // Los estados finales no tienen salida
        assertFalse(EstadoSesion.CONCLUIDA.puedePasarA(EstadoSesion.ACEPTADA));
        assertFalse(EstadoSesion.RECHAZADA.puedePasarA(EstadoSesion.ACEPTADA));
        assertFalse(EstadoSesion.CANCELADA.puedePasarA(EstadoSesion.SOLICITADA));
    }

    // ---------- Métodos auxiliares para armar los datos de prueba ----------

    // Deja listos los mocks que usa cualquier solicitud válida
    private void prepararSolicitudValida() {
        when(usuarios.findByEmail(EMAIL_ESTUDIANTE)).thenReturn(Optional.of(estudiante));
        when(usuarios.findById(2L)).thenReturn(Optional.of(tutor));
        when(materias.findById(1L)).thenReturn(Optional.of(materia));
    }

    private Usuario usuario(Long id, String email, String nombre, String apellido, Rol rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setRol(rol);
        return u;
    }

    private SesionTutoria sesion(Long id, EstadoSesion estado, LocalDateTime fechaHora, int duracion) {
        SesionTutoria s = new SesionTutoria();
        s.setId(id);
        s.setEstudiante(estudiante);
        s.setTutor(tutor);
        s.setMateria(materia);
        s.setFechaHora(fechaHora);
        s.setDuracionMinutos(duracion);
        s.setEstado(estado);
        return s;
    }
}
