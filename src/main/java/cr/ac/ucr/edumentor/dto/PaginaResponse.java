package cr.ac.ucr.edumentor.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Respuesta paginada genérica. Se usa en lugar de devolver el Page de Spring
 * directamente para que el JSON tenga una forma simple y estable.
 */
public record PaginaResponse<T>(List<T> contenido, int pagina, int tamano,
                                long totalElementos, int totalPaginas) {

    public static <E, T> PaginaResponse<T> de(Page<E> page, Function<E, T> convertir) {
        return new PaginaResponse<>(
                page.getContent().stream().map(convertir).toList(),
                page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
