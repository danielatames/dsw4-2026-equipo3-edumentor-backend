package cr.ac.ucr.edumentor.service;

import java.time.LocalDateTime;
import java.util.List;
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

import cr.ac.ucr.edumentor.dto.RecursoRequest;
import cr.ac.ucr.edumentor.dto.RecursoResponse;
import cr.ac.ucr.edumentor.exception.ConflictoNegocioException;
import cr.ac.ucr.edumentor.exception.RecursoNoEncontradoException;
import cr.ac.ucr.edumentor.exception.ReglaNegocioException;
import cr.ac.ucr.edumentor.model.EstadoSesion;
import cr.ac.ucr.edumentor.model.RecursoEstudio;
import cr.ac.ucr.edumentor.model.Rol;
import cr.ac.ucr.edumentor.model.SesionTutoria;
import cr.ac.ucr.edumentor.model.TipoArchivo;
import cr.ac.ucr.edumentor.model.Usuario;
import cr.ac.ucr.edumentor.repository.RecursoEstudioRepository;
import cr.ac.ucr.edumentor.repository.SesionTutoriaRepository;
import cr.ac.ucr.edumentor.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class RecursoEstudioServiceTest {

    private static final String EMAIL_ESTUDIANTE = "estudiante@ucr.ac.cr";
    private static final String EMAIL_TUTOR = "tutor@ucr.ac.cr";

    @Mock
    private RecursoEstudioRepository recursos;

    @Mock
    private SesionTutoriaRepository sesiones;

    @Mock
    private UsuarioRepository usuarios;

    private RecursoEstudioService servicio;
    private Usuario estudiante;
    private Usuario tutor;

    @BeforeEach
    void preparar() {
        servicio = new RecursoEstudioService(recursos, sesiones, usuarios);
        estudiante = usuario(1L, EMAIL_ESTUDIANTE, Rol.ESTUDIANTE);
        tutor = usuario(2L, EMAIL_TUTOR, Rol.TUTOR);
    }

    @Test
    void agregarGuardaElRecursoCuandoLaSesionEstaAceptada() {
        SesionTutoria sesion = sesion(7L, EstadoSesion.ACEPTADA);
        when(sesiones.findById(7L)).thenReturn(Optional.of(sesion));
        when(recursos.save(any(RecursoEstudio.class))).thenAnswer(inv -> inv.getArgument(0));

        RecursoResponse r = servicio.agregar(EMAIL_TUTOR, 7L,
                new RecursoRequest("  Guía de JOINs ", " https://ejemplo.com/guia.pdf ", TipoArchivo.PDF));

        assertEquals("Guía de JOINs", r.titulo());
        assertEquals("https://ejemplo.com/guia.pdf", r.urlRecurso());
        assertEquals(TipoArchivo.PDF, r.tipoArchivo());
        assertEquals(7L, r.sesionId());
        verify(recursos).save(any(RecursoEstudio.class));
    }

    @Test
    void agregarRechazaSiLaSesionSigueSolicitada() {
        SesionTutoria sesion = sesion(7L, EstadoSesion.SOLICITADA);
        when(sesiones.findById(7L)).thenReturn(Optional.of(sesion));

        assertThrows(ConflictoNegocioException.class, () -> servicio.agregar(EMAIL_TUTOR, 7L,
                new RecursoRequest("Guía", "https://ejemplo.com", TipoArchivo.ENLACE)));

        verify(recursos, never()).save(any());
    }

    @Test
    void agregarRechazaSiLaSesionFueCancelada() {
        SesionTutoria sesion = sesion(7L, EstadoSesion.CANCELADA);
        when(sesiones.findById(7L)).thenReturn(Optional.of(sesion));

        assertThrows(ConflictoNegocioException.class, () -> servicio.agregar(EMAIL_TUTOR, 7L,
                new RecursoRequest("Guía", "https://ejemplo.com", TipoArchivo.ENLACE)));

        verify(recursos, never()).save(any());
    }

    @Test
    void agregarRechazaAUnTutorQueNoEsElDeLaSesion() {
        SesionTutoria sesion = sesion(7L, EstadoSesion.ACEPTADA);
        when(sesiones.findById(7L)).thenReturn(Optional.of(sesion));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> servicio.agregar(
                "otro.tutor@ucr.ac.cr", 7L, new RecursoRequest("Guía", "https://ejemplo.com", TipoArchivo.ENLACE)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(recursos, never()).save(any());
    }

    @Test
    void listarPermiteAlEstudianteDeLaSesion() {
        SesionTutoria sesion = sesion(7L, EstadoSesion.ACEPTADA);
        when(sesiones.findById(7L)).thenReturn(Optional.of(sesion));
        when(recursos.findBySesionIdOrderByIdAsc(7L)).thenReturn(List.of(recurso(1L, sesion), recurso(2L, sesion)));

        List<RecursoResponse> lista = servicio.listar(EMAIL_ESTUDIANTE, 7L);

        assertEquals(2, lista.size());
        assertEquals(1L, lista.get(0).id());
    }

    @Test
    void listarPermiteAlCoordinador() {
        SesionTutoria sesion = sesion(7L, EstadoSesion.ACEPTADA);
        Usuario coordinador = usuario(8L, "coordinador@ucr.ac.cr", Rol.COORDINADOR);
        when(sesiones.findById(7L)).thenReturn(Optional.of(sesion));
        when(usuarios.findByEmail("coordinador@ucr.ac.cr")).thenReturn(Optional.of(coordinador));
        when(recursos.findBySesionIdOrderByIdAsc(7L)).thenReturn(List.of());

        assertEquals(0, servicio.listar("coordinador@ucr.ac.cr", 7L).size());
    }

    @Test
    void listarRechazaAUnEstudianteAjeno() {
        SesionTutoria sesion = sesion(7L, EstadoSesion.ACEPTADA);
        Usuario ajeno = usuario(9L, "ajeno@ucr.ac.cr", Rol.ESTUDIANTE);
        when(sesiones.findById(7L)).thenReturn(Optional.of(sesion));
        when(usuarios.findByEmail("ajeno@ucr.ac.cr")).thenReturn(Optional.of(ajeno));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.listar("ajeno@ucr.ac.cr", 7L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(recursos, never()).findBySesionIdOrderByIdAsc(any());
    }

    @Test
    void eliminarBorraElRecursoDeLaSesion() {
        SesionTutoria sesion = sesion(7L, EstadoSesion.ACEPTADA);
        RecursoEstudio recurso = recurso(4L, sesion);
        when(sesiones.findById(7L)).thenReturn(Optional.of(sesion));
        when(recursos.findByIdAndSesionId(4L, 7L)).thenReturn(Optional.of(recurso));

        servicio.eliminar(EMAIL_TUTOR, 7L, 4L);

        verify(recursos).delete(recurso);
    }

    @Test
    void eliminarFallaSiElRecursoNoPerteneceALaSesion() {
        SesionTutoria sesion = sesion(7L, EstadoSesion.ACEPTADA);
        when(sesiones.findById(7L)).thenReturn(Optional.of(sesion));
        when(recursos.findByIdAndSesionId(4L, 7L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.eliminar(EMAIL_TUTOR, 7L, 4L));
        verify(recursos, never()).delete(any());
    }

    private RecursoEstudio recurso(Long id, SesionTutoria sesion) {
        RecursoEstudio r = new RecursoEstudio();
        r.setId(id);
        r.setSesion(sesion);
        r.setTitulo("Recurso " + id);
        r.setUrlRecurso("https://ejemplo.com/" + id);
        r.setTipoArchivo(TipoArchivo.ENLACE);
        return r;
    }

    private SesionTutoria sesion(Long id, EstadoSesion estado) {
        SesionTutoria s = new SesionTutoria();
        s.setId(id);
        s.setEstudiante(estudiante);
        s.setTutor(tutor);
        s.setEstado(estado);
        s.setFechaHora(LocalDateTime.of(2026, 11, 25, 10, 0));
        s.setDuracionMinutos(60);
        return s;
    }

    private Usuario usuario(Long id, String email, Rol rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        u.setNombre("Nombre");
        u.setApellido("Apellido");
        u.setRol(rol);
        return u;
    }
}
