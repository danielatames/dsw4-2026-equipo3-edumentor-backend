# Diagrama de Clases UML del Backend

Modelado de la arquitectura en capas de Spring Boot:
**Controller → Service → Repository → Entity**, con los DTOs que viajan entre el cliente y la API.

Para que se pueda leer, el diagrama se divide en varias vistas:

1. [Vista general por capas](#1-vista-general-por-capas)
2. [Modelo de dominio (entidades `@Entity`)](#2-modelo-de-dominio)
3. [Materias](#3-materias)
4. [Disponibilidad del tutor](#4-disponibilidad-del-tutor)
5. [Sesiones de tutoría](#5-sesiones-de-tutoría)
6. [Recursos y evaluaciones](#6-recursos-y-evaluaciones)
7. [Autenticación y seguridad](#7-autenticación-y-seguridad)
8. [Manejo de errores RFC 7807](#8-manejo-de-errores-rfc-7807)

**Notación**

| Relación | Símbolo | Significado en este proyecto |
|---|---|---|
| Dependencia | `..>` | Una clase usa a otra (por ejemplo, el controlador llama al servicio, o crea un DTO) |
| Asociación | `-->` | Una entidad referencia a otra con `@ManyToOne` / `@OneToOne` |
| Agregación | `o--` | Un DTO contiene a otro DTO o una lista de ellos |
| Herencia | `<\|--` | `extends` |
| Realización | `..\|>` | `implements` |

Estereotipos: `<<Entity>>`, `<<Repository>>`, `<<Service>>`, `<<RestController>>`, `<<DTO>>` (records de Java) y `<<enumeration>>`.

---

## 1. Vista general por capas

Cada controlador solo conoce a su servicio; cada servicio usa repositorios (nunca otros controladores).
La única dependencia entre servicios es `SesionTutoriaService → DisponibilidadTutorService.estaDisponible(...)`.

```mermaid
classDiagram
    direction LR

    class AuthController:::controller
    class UsuarioController:::controller
    class MateriaController:::controller
    class DisponibilidadTutorController:::controller
    class SesionTutoriaController:::controller
    class RecursoEstudioController:::controller
    class EvaluacionTutoriaController:::controller

    class AuthService:::service
    class MateriaService:::service
    class DisponibilidadTutorService:::service
    class SesionTutoriaService:::service
    class RecursoEstudioService:::service
    class EvaluacionTutoriaService:::service

    class UsuarioRepository:::repo
    class MateriaRepository:::repo
    class DisponibilidadTutorRepository:::repo
    class SesionTutoriaRepository:::repo
    class RecursoEstudioRepository:::repo
    class EvaluacionTutoriaRepository:::repo

    AuthController ..> AuthService
    UsuarioController ..> AuthService
    MateriaController ..> MateriaService
    DisponibilidadTutorController ..> DisponibilidadTutorService
    SesionTutoriaController ..> SesionTutoriaService
    RecursoEstudioController ..> RecursoEstudioService
    EvaluacionTutoriaController ..> EvaluacionTutoriaService

    AuthService ..> UsuarioRepository
    MateriaService ..> MateriaRepository
    DisponibilidadTutorService ..> DisponibilidadTutorRepository
    DisponibilidadTutorService ..> UsuarioRepository
    SesionTutoriaService ..> SesionTutoriaRepository
    SesionTutoriaService ..> UsuarioRepository
    SesionTutoriaService ..> MateriaRepository
    SesionTutoriaService ..> DisponibilidadTutorService : estaDisponible()
    RecursoEstudioService ..> RecursoEstudioRepository
    RecursoEstudioService ..> SesionTutoriaRepository
    RecursoEstudioService ..> UsuarioRepository
    EvaluacionTutoriaService ..> EvaluacionTutoriaRepository
    EvaluacionTutoriaService ..> SesionTutoriaRepository
    EvaluacionTutoriaService ..> UsuarioRepository

    classDef controller fill:#E8F0FE,stroke:#1A4FA0
    classDef service fill:#E6F4EA,stroke:#1E7B34
    classDef repo fill:#FEF7E0,stroke:#A06A00
```

---

## 2. Modelo de dominio

Entidades JPA (`@Entity`) con sus atributos y asociaciones. Todas las relaciones `@ManyToOne` /
`@OneToOne` usan `FetchType.LAZY`; los repositorios las cargan con `@EntityGraph` cuando se
necesitan, para evitar el problema de consultas N+1.

```mermaid
classDiagram
    class Usuario {
        <<Entity>>
        -Long id
        -String carne
        -String email
        -String passwordHash
        -String nombre
        -String apellido
        -Rol rol
        -Double promedioCalificacion
        -Integer totalEvaluaciones
    }
    class Materia {
        <<Entity>>
        -Long id
        -String codigoMateria
        -String nombreMateria
        -String carrera
        -Integer creditos
    }
    class DisponibilidadTutor {
        <<Entity>>
        -Long id
        -Usuario tutor
        -DayOfWeek diaSemana
        -LocalTime horaInicio
        -LocalTime horaFin
        -boolean estaActivo
    }
    class SesionTutoria {
        <<Entity>>
        -Long id
        -Usuario estudiante
        -Usuario tutor
        -Materia materia
        -LocalDateTime fechaHora
        -int duracionMinutos
        -EstadoSesion estado
        -String tema
        -LocalDateTime fechaCreacion
    }
    class RecursoEstudio {
        <<Entity>>
        -Long id
        -SesionTutoria sesion
        -String titulo
        -String urlRecurso
        -TipoArchivo tipoArchivo
    }
    class EvaluacionTutoria {
        <<Entity>>
        -Long id
        -SesionTutoria sesion
        -int calificacionEstrellas
        -String comentario
        -LocalDateTime fechaEvaluacion
    }
    class Rol {
        <<enumeration>>
        ESTUDIANTE
        TUTOR
        COORDINADOR
    }
    class EstadoSesion {
        <<enumeration>>
        SOLICITADA
        ACEPTADA
        RECHAZADA
        CANCELADA
        EN_CURSO
        CONCLUIDA
        EVALUADA
        +puedePasarA(EstadoSesion destino) boolean
        +ocupaHorario() boolean
    }
    class TipoArchivo {
        <<enumeration>>
        ENLACE
        PDF
        VIDEO
        DOCUMENTO
        PRESENTACION
        OTRO
    }

    DisponibilidadTutor "0..*" --> "1" Usuario : tutor
    SesionTutoria "0..*" --> "1" Usuario : estudiante
    SesionTutoria "0..*" --> "1" Usuario : tutor
    SesionTutoria "0..*" --> "1" Materia : materia
    RecursoEstudio "0..*" --> "1" SesionTutoria : sesion
    EvaluacionTutoria "0..1" --> "1" SesionTutoria : sesion
    Usuario --> Rol
    SesionTutoria --> EstadoSesion
    RecursoEstudio --> TipoArchivo
```

---

## 3. Materias

Catálogo de asignaturas. Lectura para cualquier usuario autenticado; crear, editar y eliminar
solo para el COORDINADOR (`@PreAuthorize`). La regla "código único" vive en el servicio.

```mermaid
classDiagram
    direction TB
    note for MateriaController "Ruta base: /materias"
    class MateriaController {
        <<RestController>>
        -MateriaService materias
        +listar(String carrera, String nombre, int pagina, int tamano) PaginaResponse~MateriaResponse~
        +obtener(Long id) MateriaResponse
        +crear(MateriaRequest req) ResponseEntity~MateriaResponse~
        +actualizar(Long id, MateriaRequest req) MateriaResponse
        +eliminar(Long id) ResponseEntity~Void~
    }
    class MateriaService {
        <<Service>>
        -MateriaRepository materias
        +listar(String carrera, String nombre, int pagina, int tamano) PaginaResponse~MateriaResponse~
        +obtener(Long id) MateriaResponse
        +crear(MateriaRequest req) MateriaResponse
        +actualizar(Long id, MateriaRequest req) MateriaResponse
        +eliminar(Long id) void
        -normalizarCodigo(String codigo) String
    }
    class MateriaRepository {
        <<Repository>>
        +existsByCodigoMateria(String codigo) boolean
        +existsByCodigoMateriaAndIdNot(String codigo, Long id) boolean
        +buscar(String carrera, String nombre, Pageable pageable) Page~Materia~
    }
    class JpaRepository {
        <<interface>>
    }
    class Materia {
        <<Entity>>
    }
    class MateriaRequest {
        <<DTO>>
        +String codigoMateria
        +String nombreMateria
        +String carrera
        +Integer creditos
    }
    class MateriaResponse {
        <<DTO>>
        +Long id
        +String codigoMateria
        +String nombreMateria
        +String carrera
        +Integer creditos
        +de(Materia m)$ MateriaResponse
    }
    class PaginaResponse~T~ {
        <<DTO>>
        +List~T~ contenido
        +int pagina
        +int tamano
        +long totalElementos
        +int totalPaginas
        +de(Page~E~ page, Function convertir)$ PaginaResponse~T~
    }

    MateriaController ..> MateriaService
    MateriaController ..> MateriaRequest
    MateriaService ..> MateriaRepository
    MateriaService ..> MateriaResponse : crea
    MateriaService ..> PaginaResponse : crea
    MateriaRepository ..|> JpaRepository
    MateriaRepository ..> Materia : gestiona
    MateriaResponse ..> Materia : se construye desde
    PaginaResponse o-- MateriaResponse : contenido
```

---

## 4. Disponibilidad del tutor

Bloques del horario semanal. Solo el TUTOR crea y modifica sus propios bloques. El servicio valida
que la hora de fin sea posterior a la de inicio y que no haya solapes
(`inicioA < finB && inicioB < finA`) entre bloques activos del mismo tutor y día.
`estaDisponible(...)` es el contrato que usa `SesionTutoriaService` al reservar.

```mermaid
classDiagram
    direction TB
    note for DisponibilidadTutorController "Ruta base: /disponibilidades"
    class DisponibilidadTutorController {
        <<RestController>>
        -DisponibilidadTutorService disponibilidades
        +listarActivas() List~DisponibilidadResponse~
        +listarPorTutor(Long tutorId) List~DisponibilidadResponse~
        +listarMias(Authentication auth) List~DisponibilidadResponse~
        +crear(DisponibilidadRequest req, Authentication auth) ResponseEntity~DisponibilidadResponse~
        +actualizar(Long id, DisponibilidadRequest req, Authentication auth) DisponibilidadResponse
        +cambiarEstado(Long id, EstadoDisponibilidadRequest req, Authentication auth) DisponibilidadResponse
    }
    class DisponibilidadTutorService {
        <<Service>>
        -DisponibilidadTutorRepository disponibilidades
        -UsuarioRepository usuarios
        +listarActivas() List~DisponibilidadResponse~
        +listarPorTutor(Long tutorId) List~DisponibilidadResponse~
        +listarMias(String emailTutor) List~DisponibilidadResponse~
        +crear(String emailTutor, DisponibilidadRequest req) DisponibilidadResponse
        +actualizar(String emailTutor, Long id, DisponibilidadRequest req) DisponibilidadResponse
        +cambiarEstado(String emailTutor, Long id, boolean activo) DisponibilidadResponse
        +estaDisponible(Long tutorId, LocalDateTime fechaHora, int duracionMinutos) boolean
        -validarHorario(LocalTime inicio, LocalTime fin) void
        -validarSinSolape(Long tutorId, DayOfWeek dia, LocalTime inicio, LocalTime fin, Long idExcluido) void
    }
    class DisponibilidadTutorRepository {
        <<Repository>>
        +findByEstaActivoTrue() List~DisponibilidadTutor~
        +findByTutorId(Long tutorId) List~DisponibilidadTutor~
        +findByTutorIdAndEstaActivoTrue(Long tutorId) List~DisponibilidadTutor~
        +findByTutorIdAndDiaSemanaAndEstaActivoTrue(Long tutorId, DayOfWeek dia) List~DisponibilidadTutor~
    }
    class UsuarioRepository {
        <<Repository>>
    }
    class DisponibilidadTutor {
        <<Entity>>
    }
    class DisponibilidadRequest {
        <<DTO>>
        +DayOfWeek diaSemana
        +LocalTime horaInicio
        +LocalTime horaFin
    }
    class EstadoDisponibilidadRequest {
        <<DTO>>
        +Boolean activo
    }
    class DisponibilidadResponse {
        <<DTO>>
        +Long id
        +Long tutorId
        +String tutorNombre
        +DayOfWeek diaSemana
        +LocalTime horaInicio
        +LocalTime horaFin
        +boolean estaActivo
        +de(DisponibilidadTutor d)$ DisponibilidadResponse
    }

    DisponibilidadTutorController ..> DisponibilidadTutorService
    DisponibilidadTutorController ..> DisponibilidadRequest
    DisponibilidadTutorController ..> EstadoDisponibilidadRequest
    DisponibilidadTutorService ..> DisponibilidadTutorRepository
    DisponibilidadTutorService ..> UsuarioRepository
    DisponibilidadTutorService ..> DisponibilidadResponse : crea
    DisponibilidadTutorRepository ..> DisponibilidadTutor : gestiona
    DisponibilidadResponse ..> DisponibilidadTutor : se construye desde
```

---

## 5. Sesiones de tutoría

Flujo principal y máquina de estados. El servicio valida transiciones (`EstadoSesion.puedePasarA`),
traslapes de horario del tutor y del estudiante, la disponibilidad del tutor y la cancelación con
al menos 2 horas de anticipación. `Clock` se inyecta para poder probar la regla de tiempo.

```mermaid
classDiagram
    direction TB
    note for SesionTutoriaController "Ruta base: /sesiones"
    class SesionTutoriaController {
        <<RestController>>
        -SesionTutoriaService sesiones
        +listar(EstadoSesion estado, Authentication auth) List~SesionResponse~
        +obtener(Long id, Authentication auth) SesionResponse
        +solicitar(SesionRequest req, Authentication auth) ResponseEntity~SesionResponse~
        +aceptar(Long id, Authentication auth) SesionResponse
        +rechazar(Long id, Authentication auth) SesionResponse
        +iniciar(Long id, Authentication auth) SesionResponse
        +concluir(Long id, Authentication auth) SesionResponse
        +cancelar(Long id, Authentication auth) SesionResponse
    }
    class SesionTutoriaService {
        <<Service>>
        -SesionTutoriaRepository sesiones
        -UsuarioRepository usuarios
        -MateriaRepository materias
        -DisponibilidadTutorService disponibilidades
        -Clock reloj
        +listar(String email, EstadoSesion estado) List~SesionResponse~
        +obtener(String email, Long id) SesionResponse
        +solicitar(String emailEstudiante, SesionRequest req) SesionResponse
        +aceptar(String emailTutor, Long id) SesionResponse
        +rechazar(String emailTutor, Long id) SesionResponse
        +iniciar(String emailTutor, Long id) SesionResponse
        +concluir(String emailTutor, Long id) SesionResponse
        +cancelar(String email, Long id) SesionResponse
    }
    class SesionTutoriaRepository {
        <<Repository>>
        +findById(Long id) Optional~SesionTutoria~
        +findByEstudianteIdOrderByFechaHoraDesc(Long estudianteId) List~SesionTutoria~
        +findByTutorIdOrderByFechaHoraDesc(Long tutorId) List~SesionTutoria~
        +findAllByOrderByFechaHoraDesc() List~SesionTutoria~
        +buscarDelTutorEnRango(Long tutorId, Collection estados, LocalDateTime desde, LocalDateTime hasta) List~SesionTutoria~
        +buscarDelEstudianteEnRango(Long estudianteId, Collection estados, LocalDateTime desde, LocalDateTime hasta) List~SesionTutoria~
    }
    class DisponibilidadTutorService {
        <<Service>>
        +estaDisponible(Long tutorId, LocalDateTime fechaHora, int duracionMinutos) boolean
    }
    class MateriaRepository {
        <<Repository>>
    }
    class UsuarioRepository {
        <<Repository>>
    }
    class SesionTutoria {
        <<Entity>>
    }
    class EstadoSesion {
        <<enumeration>>
        +puedePasarA(EstadoSesion destino) boolean
        +ocupaHorario() boolean
    }
    class SesionRequest {
        <<DTO>>
        +Long tutorId
        +Long materiaId
        +LocalDateTime fechaHora
        +Integer duracionMinutos
        +String tema
    }
    class SesionResponse {
        <<DTO>>
        +Long id
        +Long estudianteId
        +String estudianteNombre
        +Long tutorId
        +String tutorNombre
        +Long materiaId
        +String materiaNombre
        +LocalDateTime fechaHora
        +LocalDateTime fechaFin
        +int duracionMinutos
        +EstadoSesion estado
        +String tema
    }

    SesionTutoriaController ..> SesionTutoriaService
    SesionTutoriaController ..> SesionRequest
    SesionTutoriaService ..> SesionTutoriaRepository
    SesionTutoriaService ..> UsuarioRepository
    SesionTutoriaService ..> MateriaRepository
    SesionTutoriaService ..> DisponibilidadTutorService : estaDisponible()
    SesionTutoriaService ..> EstadoSesion : valida transiciones
    SesionTutoriaService ..> SesionResponse : crea
    SesionTutoriaRepository ..> SesionTutoria : gestiona
    SesionResponse ..> SesionTutoria : se construye desde
```

---

## 6. Recursos y evaluaciones

Cierre del flujo de una sesión. El tutor asocia recursos a sus sesiones; el estudiante evalúa una
sesión CONCLUIDA (una sola vez), la sesión pasa a EVALUADA y se recalcula el promedio del tutor
(`Usuario.promedioCalificacion` y `Usuario.totalEvaluaciones`).

```mermaid
classDiagram
    direction TB
    note for RecursoEstudioController "Ruta base: /sesiones/{sesionId}/recursos"
    class RecursoEstudioController {
        <<RestController>>
        -RecursoEstudioService recursos
        +listar(Long sesionId, Authentication auth) List~RecursoResponse~
        +agregar(Long sesionId, RecursoRequest req, Authentication auth) ResponseEntity~RecursoResponse~
        +eliminar(Long sesionId, Long recursoId, Authentication auth) ResponseEntity~Void~
    }
    class RecursoEstudioService {
        <<Service>>
        -RecursoEstudioRepository recursos
        -SesionTutoriaRepository sesiones
        -UsuarioRepository usuarios
        +listar(String email, Long sesionId) List~RecursoResponse~
        +agregar(String emailTutor, Long sesionId, RecursoRequest req) RecursoResponse
        +eliminar(String emailTutor, Long sesionId, Long recursoId) void
    }
    class RecursoEstudioRepository {
        <<Repository>>
        +findBySesionIdOrderByIdAsc(Long sesionId) List~RecursoEstudio~
        +findByIdAndSesionId(Long id, Long sesionId) Optional~RecursoEstudio~
    }
    class EvaluacionTutoriaController {
        <<RestController>>
        -EvaluacionTutoriaService evaluaciones
        +evaluar(Long sesionId, EvaluacionRequest req, Authentication auth) ResponseEntity~EvaluacionResponse~
        +obtener(Long sesionId, Authentication auth) EvaluacionResponse
        +calificacionDelTutor(Long tutorId) CalificacionTutorResponse
    }
    class EvaluacionTutoriaService {
        <<Service>>
        -EvaluacionTutoriaRepository evaluaciones
        -SesionTutoriaRepository sesiones
        -UsuarioRepository usuarios
        -Clock reloj
        +evaluar(String emailEstudiante, Long sesionId, EvaluacionRequest req) EvaluacionResponse
        +obtener(String email, Long sesionId) EvaluacionResponse
        +calificacionDelTutor(Long tutorId) CalificacionTutorResponse
    }
    class EvaluacionTutoriaRepository {
        <<Repository>>
        +findBySesionId(Long sesionId) Optional~EvaluacionTutoria~
        +promedioDelTutor(Long tutorId) Double
    }
    class SesionTutoriaRepository {
        <<Repository>>
    }
    class UsuarioRepository {
        <<Repository>>
    }
    class RecursoEstudio {
        <<Entity>>
    }
    class EvaluacionTutoria {
        <<Entity>>
    }
    class RecursoRequest {
        <<DTO>>
        +String titulo
        +String urlRecurso
        +TipoArchivo tipoArchivo
    }
    class RecursoResponse {
        <<DTO>>
        +Long id
        +Long sesionId
        +String titulo
        +String urlRecurso
        +TipoArchivo tipoArchivo
    }
    class EvaluacionRequest {
        <<DTO>>
        +Integer calificacionEstrellas
        +String comentario
    }
    class EvaluacionResponse {
        <<DTO>>
        +Long id
        +Long sesionId
        +int calificacionEstrellas
        +String comentario
        +LocalDateTime fechaEvaluacion
    }
    class CalificacionTutorResponse {
        <<DTO>>
        +Long tutorId
        +String tutorNombre
        +double promedio
        +int totalEvaluaciones
    }

    RecursoEstudioController ..> RecursoEstudioService
    RecursoEstudioController ..> RecursoRequest
    RecursoEstudioService ..> RecursoEstudioRepository
    RecursoEstudioService ..> SesionTutoriaRepository
    RecursoEstudioService ..> UsuarioRepository
    RecursoEstudioService ..> RecursoResponse : crea
    RecursoEstudioRepository ..> RecursoEstudio : gestiona

    EvaluacionTutoriaController ..> EvaluacionTutoriaService
    EvaluacionTutoriaController ..> EvaluacionRequest
    EvaluacionTutoriaService ..> EvaluacionTutoriaRepository
    EvaluacionTutoriaService ..> SesionTutoriaRepository
    EvaluacionTutoriaService ..> UsuarioRepository : actualiza promedio
    EvaluacionTutoriaService ..> EvaluacionResponse : crea
    EvaluacionTutoriaService ..> CalificacionTutorResponse : crea
    EvaluacionTutoriaRepository ..> EvaluacionTutoria : gestiona
```

---

## 7. Autenticación y seguridad

Login y registro emiten un JWT firmado (`JwtService`). En cada petición, `JwtAuthFilter` valida el
token y carga el rol; las rutas se protegen en `SecurityConfig` y con `@PreAuthorize` en los
controladores. Las respuestas 401 y 403 salen en formato RFC 7807 desde `ProblemDetailSecurityHandler`.

```mermaid
classDiagram
    direction TB
    note for AuthController "Ruta base: /auth"
    class AuthController {
        <<RestController>>
        -AuthService auth
        +login(LoginRequest req) AuthResponse
        +register(RegisterRequest req) ResponseEntity~AuthResponse~
    }
    note for UsuarioController "Ruta base: /usuarios"
    class UsuarioController {
        <<RestController>>
        -AuthService auth
        +me(Authentication auth) UsuarioResponse
    }
    class AuthService {
        <<Service>>
        -UsuarioRepository usuarios
        -PasswordEncoder encoder
        -JwtService jwt
        +registrar(RegisterRequest req) AuthResponse
        +login(LoginRequest req) AuthResponse
        +perfil(String email) UsuarioResponse
    }
    class UsuarioRepository {
        <<Repository>>
        +findByEmail(String email) Optional~Usuario~
        +existsByEmail(String email) boolean
        +existsByCarne(String carne) boolean
    }
    class Usuario {
        <<Entity>>
    }
    class JwtService {
        <<Service>>
        -SecretKey key
        -long expirationMs
        +generarToken(Usuario usuario) String
        +extraerClaims(String token) Claims
        +esValido(String token) boolean
    }
    class JwtAuthFilter {
        <<Component>>
        -JwtService jwtService
        #doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) void
    }
    class OncePerRequestFilter {
        <<abstract>>
    }
    class SecurityConfig {
        <<Configuration>>
        +passwordEncoder() PasswordEncoder
        +filterChain(HttpSecurity http, JwtAuthFilter filtro, ProblemDetailSecurityHandler handler) SecurityFilterChain
        +corsConfigurationSource() CorsConfigurationSource
    }
    class ProblemDetailSecurityHandler {
        <<Component>>
        +commence(...) void
        +handle(...) void
    }
    class AuthenticationEntryPoint {
        <<interface>>
    }
    class AccessDeniedHandler {
        <<interface>>
    }
    class LoginRequest {
        <<DTO>>
        +String email
        +String password
    }
    class RegisterRequest {
        <<DTO>>
        +String carne
        +String email
        +String password
        +String nombre
        +String apellido
        +Rol rol
    }
    class AuthResponse {
        <<DTO>>
        +String token
        +String tipo
        +UsuarioResponse usuario
    }
    class UsuarioResponse {
        <<DTO>>
        +Long id
        +String carne
        +String email
        +String nombre
        +String apellido
        +Rol rol
        +de(Usuario u)$ UsuarioResponse
    }

    AuthController ..> AuthService
    AuthController ..> LoginRequest
    AuthController ..> RegisterRequest
    UsuarioController ..> AuthService
    AuthService ..> UsuarioRepository
    AuthService ..> JwtService
    AuthService ..> AuthResponse : crea
    UsuarioRepository ..> Usuario : gestiona
    AuthResponse o-- UsuarioResponse : usuario
    JwtAuthFilter --|> OncePerRequestFilter
    JwtAuthFilter ..> JwtService
    SecurityConfig ..> JwtAuthFilter
    SecurityConfig ..> ProblemDetailSecurityHandler
    ProblemDetailSecurityHandler ..|> AuthenticationEntryPoint
    ProblemDetailSecurityHandler ..|> AccessDeniedHandler
```

---

## 8. Manejo de errores RFC 7807

Los servicios lanzan excepciones de negocio; `GlobalExceptionHandler` (`@RestControllerAdvice`) las
convierte en un `ProblemDetail` con `title`, `status`, `detail` e `instance`.

```mermaid
classDiagram
    direction TB
    class RuntimeException
    class ReglaNegocioException {
        -HttpStatus status
        +ReglaNegocioException(HttpStatus status, String mensaje)
        +ReglaNegocioException(String mensaje)
        +getStatus() HttpStatus
    }
    class ConflictoNegocioException
    note for ConflictoNegocioException "Responde 409 Conflict"
    class RecursoNoEncontradoException
    note for RecursoNoEncontradoException "Responde 404 Not Found"
    class GlobalExceptionHandler {
        <<RestControllerAdvice>>
        +manejarReglaNegocio(ReglaNegocioException ex) ProblemDetail
        +manejarRestricciones(ConstraintViolationException ex) ProblemDetail
        +manejarIntegridad(DataIntegrityViolationException ex) ProblemDetail
        +manejarAccesoDenegado(AccessDeniedException ex) ProblemDetail
        +manejarNoAutenticado(AuthenticationException ex) ProblemDetail
        +manejarGenerico(Exception ex) ProblemDetail
        #handleMethodArgumentNotValid(...) ResponseEntity
    }
    class ResponseEntityExceptionHandler {
        <<abstract>>
    }
    class ProblemDetail {
        +String title
        +int status
        +String detail
        +URI instance
    }

    RuntimeException <|-- ReglaNegocioException
    ReglaNegocioException <|-- ConflictoNegocioException
    ReglaNegocioException <|-- RecursoNoEncontradoException
    ResponseEntityExceptionHandler <|-- GlobalExceptionHandler
    GlobalExceptionHandler ..> ReglaNegocioException : captura
    GlobalExceptionHandler ..> ProblemDetail : crea
```
