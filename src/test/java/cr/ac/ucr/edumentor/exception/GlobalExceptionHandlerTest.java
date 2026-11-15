package cr.ac.ucr.edumentor.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ControladorPrueba())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void reglaDeNegocioRespondeConStatusYContratoRfc7807() throws Exception {
        mvc.perform(get("/prueba/regla"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflicto"))
                .andExpect(jsonPath("$.detail").value("Horario traslapado"))
                .andExpect(jsonPath("$.instance").value("/prueba/regla"));
    }

    @Test
    void recursoNoEncontradoRespondeCuatrocientosCuatro() throws Exception {
        mvc.perform(get("/prueba/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.instance").value("/prueba/no-encontrado"));
    }

    @Test
    void validacionDevuelveErroresPorCampo() throws Exception {
        mvc.perform(post("/prueba/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Solicitud inválida"))
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.instance").value("/prueba/validar"));
    }

    @Test
    void responseStatusExceptionConservaDetalleEInstancia() throws Exception {
        mvc.perform(get("/prueba/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciales inválidas"))
                .andExpect(jsonPath("$.instance").value("/prueba/status"));
    }

    @Test
    void errorInesperadoNoExponeDetallesInternos() throws Exception {
        mvc.perform(get("/prueba/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Error interno"))
                .andExpect(jsonPath("$.detail").value(
                        "Ocurrió un error inesperado. Intente nuevamente más tarde"));
    }

    @RestController
    @RequestMapping("/prueba")
    static class ControladorPrueba {

        record Entrada(@NotBlank String nombre) {
        }

        @GetMapping("/regla")
        void regla() {
            throw new ConflictoNegocioException("Horario traslapado");
        }

        @GetMapping("/no-encontrado")
        void noEncontrado() {
            throw new RecursoNoEncontradoException("Sesión no encontrada");
        }

        @PostMapping("/validar")
        void validar(@Valid @RequestBody Entrada entrada) {
        }

        @GetMapping("/status")
        void statusException() {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        @GetMapping("/error")
        void error() {
            throw new IllegalStateException("detalle interno");
        }
    }
}
