package cr.ac.ucr.edumentor.service;

import cr.ac.ucr.edumentor.dto.MateriaRequest;
import cr.ac.ucr.edumentor.dto.MateriaResponse;
import cr.ac.ucr.edumentor.dto.PaginaResponse;
import cr.ac.ucr.edumentor.exception.ConflictoNegocioException;
import cr.ac.ucr.edumentor.exception.RecursoNoEncontradoException;
import cr.ac.ucr.edumentor.model.Materia;
import cr.ac.ucr.edumentor.repository.MateriaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MateriaService {

    private static final int TAMANO_MAXIMO = 50;

    private final MateriaRepository materias;

    public MateriaService(MateriaRepository materias) {
        this.materias = materias;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MateriaResponse> listar(String carrera, String nombre, int pagina, int tamano) {
        int tamanoValido = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
        var pageable = PageRequest.of(Math.max(pagina, 0), tamanoValido, Sort.by("nombreMateria"));
        return PaginaResponse.de(
                materias.buscar(vacioANull(carrera), vacioANull(nombre), pageable),
                MateriaResponse::de);
    }

    @Transactional(readOnly = true)
    public MateriaResponse obtener(Long id) {
        return MateriaResponse.de(buscarPorId(id));
    }

    @Transactional
    public MateriaResponse crear(MateriaRequest req) {
        String codigo = normalizarCodigo(req.codigoMateria());
        if (materias.existsByCodigoMateria(codigo)) {
            throw new ConflictoNegocioException("Ya existe una materia con el código " + codigo);
        }
        Materia m = new Materia();
        copiarDatos(req, codigo, m);
        return MateriaResponse.de(materias.save(m));
    }

    @Transactional
    public MateriaResponse actualizar(Long id, MateriaRequest req) {
        Materia m = buscarPorId(id);
        String codigo = normalizarCodigo(req.codigoMateria());
        if (materias.existsByCodigoMateriaAndIdNot(codigo, id)) {
            throw new ConflictoNegocioException("Ya existe otra materia con el código " + codigo);
        }
        copiarDatos(req, codigo, m);
        return MateriaResponse.de(materias.save(m));
    }

    @Transactional
    public void eliminar(Long id) {
        materias.delete(buscarPorId(id));
    }

    private Materia buscarPorId(Long id) {
        return materias.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Materia no encontrada: " + id));
    }

    private void copiarDatos(MateriaRequest req, String codigo, Materia m) {
        m.setCodigoMateria(codigo);
        m.setNombreMateria(req.nombreMateria().trim());
        m.setCarrera(req.carrera().trim());
        m.setCreditos(req.creditos());
    }

    // "if0009 " y "IF0009" deben considerarse el mismo código.
    private String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase();
    }

    private String vacioANull(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
