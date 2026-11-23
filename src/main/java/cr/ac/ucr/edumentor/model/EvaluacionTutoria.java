package cr.ac.ucr.edumentor.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "evaluacion_tutoria")
public class EvaluacionTutoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_id", nullable = false, unique = true)
    private SesionTutoria sesion;

    @Column(name = "calificacion_estrellas", nullable = false)
    private int calificacionEstrellas;

    @Column(length = 500)
    private String comentario;

    @Column(name = "fecha_evaluacion", nullable = false)
    private LocalDateTime fechaEvaluacion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SesionTutoria getSesion() {
        return sesion;
    }

    public void setSesion(SesionTutoria sesion) {
        this.sesion = sesion;
    }

    public int getCalificacionEstrellas() {
        return calificacionEstrellas;
    }

    public void setCalificacionEstrellas(int calificacionEstrellas) {
        this.calificacionEstrellas = calificacionEstrellas;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public LocalDateTime getFechaEvaluacion() {
        return fechaEvaluacion;
    }

    public void setFechaEvaluacion(LocalDateTime fechaEvaluacion) {
        this.fechaEvaluacion = fechaEvaluacion;
    }
}
