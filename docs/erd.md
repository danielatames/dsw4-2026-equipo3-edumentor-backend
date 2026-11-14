# Modelo Entidad-Relación

```mermaid
erDiagram
    USUARIO {
        BIGINT id PK
        VARCHAR_20 carne UK "NOT NULL"
        VARCHAR_120 email UK "NOT NULL"
        VARCHAR_255 password_hash "NOT NULL, BCrypt"
        VARCHAR_80 nombre "NOT NULL"
        VARCHAR_80 apellido "NOT NULL"
        VARCHAR_20 rol "NOT NULL: ESTUDIANTE, TUTOR, COORDINADOR"
    }
    USUARIO ||--o{ DISPONIBILIDAD_TUTOR : "tutor tiene (1:N)"
    USUARIO ||--o{ SESION_TUTORIA : "estudiante solicita (1:N)"
    USUARIO ||--o{ SESION_TUTORIA : "tutor atiende (1:N)"
```

Las demás entidades (Materia, DisponibilidadTutor, SesionTutoria, RecursoEstudio y EvaluacionTutoria) las agrega cada responsable en su rama; el diagrama final se integra al cierre.