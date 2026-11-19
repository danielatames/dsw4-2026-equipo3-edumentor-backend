package cr.ac.ucr.edumentor.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sesion_tutoria")
public class SesionTutoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El estudiante que pidió la sesión (N:1 con Usuario).
    // LAZY para que no se cargue el usuario si no se necesita.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Usuario estudiante;

    // El tutor que la va a atender (N:1 con Usuario).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Usuario tutor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    // Fecha y hora de inicio. El fin se calcula con la duración.
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "duracion_minutos", nullable = false)
    private int duracionMinutos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSesion estado = EstadoSesion.SOLICITADA;

    @Column(length = 300)
    private String tema;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    // Hora en que termina la sesión. No se guarda en la BD, se calcula.
    public LocalDateTime getFechaFin() {
        return fechaHora.plusMinutes(duracionMinutos);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Usuario estudiante) {
        this.estudiante = estudiante;
    }

    public Usuario getTutor() {
        return tutor;
    }

    public void setTutor(Usuario tutor) {
        this.tutor = tutor;
    }

    public Materia getMateria() {
        return materia;
    }

    public void setMateria(Materia materia) {
        this.materia = materia;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(int duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public EstadoSesion getEstado() {
        return estado;
    }

    public void setEstado(EstadoSesion estado) {
        this.estado = estado;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
