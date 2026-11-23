package cr.ac.ucr.edumentor.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EstadoSesionEvaluadaTest {

    @Test
    void unaSesionConcluidaSoloPuedePasarAEvaluada() {
        assertTrue(EstadoSesion.CONCLUIDA.puedePasarA(EstadoSesion.EVALUADA));
        assertFalse(EstadoSesion.CONCLUIDA.puedePasarA(EstadoSesion.CANCELADA));
        assertFalse(EstadoSesion.CONCLUIDA.puedePasarA(EstadoSesion.EN_CURSO));
    }

    @Test
    void soloUnaSesionConcluidaPuedeSerEvaluada() {
        for (EstadoSesion estado : EstadoSesion.values()) {
            assertTrue(estado.puedePasarA(EstadoSesion.EVALUADA) == (estado == EstadoSesion.CONCLUIDA),
                    "Transición inesperada desde " + estado);
        }
    }

    @Test
    void evaluadaEsUnEstadoFinalQueNoOcupaHorario() {
        for (EstadoSesion destino : EstadoSesion.values()) {
            assertFalse(EstadoSesion.EVALUADA.puedePasarA(destino));
        }
        assertFalse(EstadoSesion.EVALUADA.ocupaHorario());
    }
}
