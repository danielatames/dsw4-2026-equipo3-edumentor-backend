package cr.ac.ucr.edumentor.dto;

import cr.ac.ucr.edumentor.model.Rol;
import cr.ac.ucr.edumentor.model.Usuario;

public record UsuarioResponse(Long id, String carne, String email,String nombre, String apellido, Rol rol) {
    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getCarne(), u.getEmail(),u.getNombre(), u.getApellido(), u.getRol());
    }
}