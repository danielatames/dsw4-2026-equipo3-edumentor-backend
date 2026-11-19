package cr.ac.ucr.edumentor.config;

import cr.ac.ucr.edumentor.model.DisponibilidadTutor;
import cr.ac.ucr.edumentor.model.Usuario;
import cr.ac.ucr.edumentor.repository.DisponibilidadTutorRepository;
import cr.ac.ucr.edumentor.repository.UsuarioRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Precarga bloques de disponibilidad para el tutor de prueba.
 * Se ejecuta con ApplicationReadyEvent, es decir, después de DataInitializer,
 * para asegurar que el tutor ya exista.
 */
@Component
public class DisponibilidadInitializer {

    private static final String EMAIL_TUTOR = "tutor@ucr.ac.cr";

    private final DisponibilidadTutorRepository disponibilidades;
    private final UsuarioRepository usuarios;

    public DisponibilidadInitializer(DisponibilidadTutorRepository disponibilidades,
                                     UsuarioRepository usuarios) {
        this.disponibilidades = disponibilidades;
        this.usuarios = usuarios;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void precargar() {
        Usuario tutor = usuarios.findByEmail(EMAIL_TUTOR).orElse(null);
        if (tutor == null || !disponibilidades.findByTutorId(tutor.getId()).isEmpty()) return;
        crear(tutor, DayOfWeek.MONDAY, "08:00", "10:00");
        crear(tutor, DayOfWeek.MONDAY, "13:00", "16:00");
        crear(tutor, DayOfWeek.WEDNESDAY, "09:00", "12:00");
        crear(tutor, DayOfWeek.FRIDAY, "14:00", "17:00");
    }

    private void crear(Usuario tutor, DayOfWeek dia, String inicio, String fin) {
        DisponibilidadTutor d = new DisponibilidadTutor();
        d.setTutor(tutor);
        d.setDiaSemana(dia);
        d.setHoraInicio(LocalTime.parse(inicio));
        d.setHoraFin(LocalTime.parse(fin));
        d.setEstaActivo(true);
        disponibilidades.save(d);
    }
}
