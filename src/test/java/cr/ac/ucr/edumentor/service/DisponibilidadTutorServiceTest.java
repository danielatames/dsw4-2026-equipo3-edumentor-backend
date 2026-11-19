package cr.ac.ucr.edumentor.service;

import cr.ac.ucr.edumentor.dto.DisponibilidadRequest;
import cr.ac.ucr.edumentor.dto.DisponibilidadResponse;
import cr.ac.ucr.edumentor.exception.ConflictoNegocioException;
import cr.ac.ucr.edumentor.exception.ReglaNegocioException;
import cr.ac.ucr.edumentor.model.DisponibilidadTutor;
import cr.ac.ucr.edumentor.model.Rol;
import cr.ac.ucr.edumentor.model.Usuario;
import cr.ac.ucr.edumentor.repository.DisponibilidadTutorRepository;
import cr.ac.ucr.edumentor.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Prueba unitaria del servicio: los repositorios son "mocks" (objetos falsos de Mockito),
 * así se prueba solo la lógica de negocio, sin base de datos ni Spring.
 */
@ExtendWith(MockitoExtension.class)
class DisponibilidadTutorServiceTest {

    private static final String EMAIL = "tutor@ucr.ac.cr";

    @Mock
    private DisponibilidadTutorRepository disponibilidades;

    @Mock
    private UsuarioRepository usuarios;

    @InjectMocks
    private DisponibilidadTutorService servicio;

    private Usuario tutor;

    @BeforeEach
    void preparar() {
        tutor = new Usuario();
        tutor.setId(1L);
        tutor.setEmail(EMAIL);
        tutor.setNombre("Luis");
        tutor.setApellido("Tutor");
        tutor.setRol(Rol.TUTOR);
    }

    @Test
    void crearRechazaBloqueQueSeSolapaConOtro() {
        // Dado: el tutor ya tiene el lunes de 08:00 a 10:00
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.of(tutor));
        when(disponibilidades.findByTutorIdAndDiaSemanaAndEstaActivoTrue(1L, DayOfWeek.MONDAY))
                .thenReturn(List.of(bloque(10L, DayOfWeek.MONDAY, "08:00", "10:00")));

        // Cuando: intenta agregar el lunes de 09:00 a 11:00
        var req = new DisponibilidadRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));

        // Entonces: se rechaza y no se guarda nada
        assertThrows(ConflictoNegocioException.class, () -> servicio.crear(EMAIL, req));
        verify(disponibilidades, never()).save(any());
    }

    @Test
    void crearAceptaBloqueContiguoSinSolape() {
        // Dado: el tutor ya tiene el lunes de 08:00 a 10:00
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.of(tutor));
        when(disponibilidades.findByTutorIdAndDiaSemanaAndEstaActivoTrue(1L, DayOfWeek.MONDAY))
                .thenReturn(List.of(bloque(10L, DayOfWeek.MONDAY, "08:00", "10:00")));
        when(disponibilidades.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Cuando: agrega el lunes de 10:00 a 12:00 (empieza justo cuando termina el otro)
        var req = new DisponibilidadRequest(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0));
        DisponibilidadResponse r = servicio.crear(EMAIL, req);

        // Entonces: se guarda correctamente
        assertEquals(LocalTime.of(10, 0), r.horaInicio());
        assertTrue(r.estaActivo());
        verify(disponibilidades).save(any());
    }

    @Test
    void crearRechazaHoraFinAnteriorAHoraInicio() {
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.of(tutor));
        var req = new DisponibilidadRequest(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(10, 0));

        assertThrows(ReglaNegocioException.class, () -> servicio.crear(EMAIL, req));
        verify(disponibilidades, never()).save(any());
    }

    @Test
    void estaDisponibleSoloSiLaSesionCabeCompletaEnUnBloque() {
        // El lunes 23/11/2026 el tutor atiende de 08:00 a 10:00
        when(disponibilidades.findByTutorIdAndDiaSemanaAndEstaActivoTrue(1L, DayOfWeek.MONDAY))
                .thenReturn(List.of(bloque(10L, DayOfWeek.MONDAY, "08:00", "10:00")));

        LocalDateTime lunes = LocalDateTime.of(2026, 11, 23, 0, 0);
        assertTrue(servicio.estaDisponible(1L, lunes.withHour(8), 60));   // 08:00-09:00 cabe
        assertTrue(servicio.estaDisponible(1L, lunes.withHour(9), 60));   // 09:00-10:00 cabe justo
        assertFalse(servicio.estaDisponible(1L, lunes.withHour(9), 90));  // 09:00-10:30 se sale
    }

    private DisponibilidadTutor bloque(Long id, DayOfWeek dia, String inicio, String fin) {
        DisponibilidadTutor d = new DisponibilidadTutor();
        d.setId(id);
        d.setTutor(tutor);
        d.setDiaSemana(dia);
        d.setHoraInicio(LocalTime.parse(inicio));
        d.setHoraFin(LocalTime.parse(fin));
        d.setEstaActivo(true);
        return d;
    }
}
