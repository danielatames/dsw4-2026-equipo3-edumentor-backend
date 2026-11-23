package cr.ac.ucr.edumentor.model;

// Estados por los que pasa una sesión de tutoría (máquina de estados).
public enum EstadoSesion {
    SOLICITADA,
    ACEPTADA,
    RECHAZADA,
    CANCELADA,
    EN_CURSO,
    CONCLUIDA,
    EVALUADA;

    // Devuelve true si desde este estado se permite pasar al estado destino.
    public boolean puedePasarA(EstadoSesion destino) {
        return switch (this) {
            case SOLICITADA -> destino == ACEPTADA || destino == RECHAZADA || destino == CANCELADA;
            case ACEPTADA -> destino == EN_CURSO || destino == CANCELADA;
            case EN_CURSO -> destino == CONCLUIDA;
            case CONCLUIDA -> destino == EVALUADA;
            case RECHAZADA, CANCELADA, EVALUADA -> false;
        };
    }

    // Las sesiones en estos estados "ocupan" el horario, por eso cuentan para los
    // traslapes.
    // Las rechazadas, canceladas y concluidas ya no bloquean a nadie.
    public boolean ocupaHorario() {
        return this == SOLICITADA || this == ACEPTADA || this == EN_CURSO;
    }
}
