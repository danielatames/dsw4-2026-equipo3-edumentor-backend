package cr.ac.ucr.edumentor.controller;

import cr.ac.ucr.edumentor.dto.MateriaRequest;
import cr.ac.ucr.edumentor.dto.MateriaResponse;
import cr.ac.ucr.edumentor.dto.PaginaResponse;
import cr.ac.ucr.edumentor.service.MateriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/materias")
public class MateriaController {

    private final MateriaService materias;

    public MateriaController(MateriaService materias) {
        this.materias = materias;
    }

    // Lectura: cualquier usuario autenticado (lo exige SecurityConfig).
    @GetMapping
    public PaginaResponse<MateriaResponse> listar(
            @RequestParam(required = false) String carrera,
            @RequestParam(required = false) String nombre,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano) {
        return materias.listar(carrera, nombre, pagina, tamano);
    }

    @GetMapping("/{id}")
    public MateriaResponse obtener(@PathVariable Long id) {
        return materias.obtener(id);
    }

    // Escritura: solo el COORDINADOR.
    @PostMapping
    @PreAuthorize("hasRole('COORDINADOR')")
    public ResponseEntity<MateriaResponse> crear(@Valid @RequestBody MateriaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materias.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COORDINADOR')")
    public MateriaResponse actualizar(@PathVariable Long id, @Valid @RequestBody MateriaRequest req) {
        return materias.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COORDINADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        materias.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
