# EduMentor Hub - Backend

API REST de la plataforma de tutorías académicas entre pares.
Spring Boot 3, Spring Data JPA, Spring Security + JWT y H2.

## Requisitos
- JDK 21
- Maven 3.9+

## Ejecución local
    mvn spring-boot:run

La API queda en http://localhost:8080.
La base de datos H2 es en memoria: se crea y se precarga con los usuarios de prueba cada vez que arranca.

Consola H2: http://localhost:8080/h2-console
(JDBC URL `jdbc:h2:mem:edumentor`, usuario `sa`, contraseña vacía).

## Credenciales de prueba
| Rol | Correo | Contraseña |
|---|---|---|
| ESTUDIANTE | estudiante@ucr.ac.cr | Estudiante123* |
| TUTOR | tutor@ucr.ac.cr | Tutor123* |
| COORDINADOR | coordinador@ucr.ac.cr | Coordinador123* |

## Autenticación
1. `POST /auth/login` con `{"email": "...", "password": "..."}` devuelve un token JWT.
2. Enviar `Authorization: Bearer <token>` en las demás peticiones.
3. Sin token válido la API responde 401; con un rol sin permisos, 403.
   Ambas respuestas usan el formato RFC 7807 (Problem Details).

Endpoints actuales: `POST /auth/login`, `POST /auth/register`, `GET /usuarios/me`.

Sesiones de tutoría (`/sesiones`): `GET /sesiones`, `GET /sesiones/{id}`, `POST /sesiones` (ESTUDIANTE),
`PATCH /sesiones/{id}/aceptar|rechazar|iniciar|concluir` (TUTOR) y `PATCH /sesiones/{id}/cancelar` (ESTUDIANTE o TUTOR).
Detalle en `docs/openapi-sesiones.yaml`.
El rol COORDINADOR no puede autorregistrarse.

## Frontend
Repositorio `dsw4-2026-equipo3-edumentor-frontend`. Corre en http://localhost:4200 y necesita este backend activo.

## Documentación
- Especificación OpenAPI: `docs/openapi.yaml` (sesiones en `docs/openapi-sesiones.yaml`)
- Modelo Entidad-Relación: `docs/erd.md`

## Flujo de trabajo en Git
- Prohibido hacer commits directos a `main`.
- Una rama por funcionalidad (`feature/*`, `docs/*`, `test/*`).
- Pull Request con al menos una aprobación de un compañero.
- Commits semánticos: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`.