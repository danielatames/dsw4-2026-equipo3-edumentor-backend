package cr.ac.ucr.edumentor.dto;

public record AuthResponse(String token, String tipo, UsuarioResponse usuario) {
    
}