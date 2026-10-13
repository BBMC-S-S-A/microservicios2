# Taller 02 v2.0 — Aplicación distribuida con microservicios

Universidad de Ibagué · Web, APIs and Microservices

Spring Boot 4.0.8 · Spring Cloud 2025.1.3 (Gateway 5.0.3) · Java 21 · PostgreSQL 17 · JWT RS256 · Flyway · springdoc 3.0.1 · Docker Compose · GitHub Actions

## Arquitectura

```
                 Cliente (Postman / curl)
                          │
                          ▼
              api-gateway :8080  (Spring Cloud Gateway)
              enrutamiento + X-Correlation-Id
               │                          │
     /api/auth/**                   /api/users/**
               ▼                          ▼
     auth-service :8081           user-service :8082
     BCrypt + firma JWT           CRUD perfiles + roles
     (llave PRIVADA)              (solo llave PÚBLICA)
               │                          │
               ▼                          ▼
     auth-db (auth_db) :5433      users-db (users_db) :5434
     PostgreSQL 17                PostgreSQL 17
```

| Componente   | Puerto | Responsabilidad                          | Datos    |
|--------------|--------|------------------------------------------|----------|
| api-gateway  | 8080   | Enrutamiento y Correlation ID            | —        |
| auth-service | 8081   | Registro, login, BCrypt, emisión de JWT  | auth_db  |
| user-service | 8082   | CRUD de perfiles y autorización por rol  | users_db |

Decisiones clave: *Database per Service*, JWT firmado con RSA (solo auth-service tiene la llave privada), servicios *stateless*, esquema creado por Flyway y validado por JPA (`ddl-auto: validate`), DTOs en lugar de entidades en la API.

## Estructura

```
.
├── pom.xml                      # POM padre (módulos + BOM Spring Cloud)
├── docker-compose.yml           # 5 contenedores + 2 volúmenes
├── .env.example
├── .github/workflows/ci-cd.yml  # verify + build/push a GHCR
├── scripts/generate-rsa-keys.sh
├── postman/                     # colección + ambiente
├── api-gateway/                 # edu.unibague.gateway
│   └── filter/CorrelationIdFilter
├── auth-service/                # edu.unibague.auth
│   ├── config/      RsaKeyConfig, SecurityConfig, OpenApiConfig, DemoDataConfig
│   ├── controller/  AuthController
│   ├── dto/         LoginRequest, RegisterRequest, AuthResponse, RegisterResponse
│   ├── exception/   ApiError, ConflictException, GlobalExceptionHandler
│   ├── model/       Credential, Role
│   ├── repository/  CredentialRepository
│   ├── security/    CustomUserDetailsService, PemKeyLoader
│   ├── service/     AuthService, JwtService
│   └── resources/   application.yml, db/migration/V1__create_credentials.sql, keys/
└── user-service/                # edu.unibague.users
    ├── config/      SecurityConfig, OpenApiConfig
    ├── controller/  UserController
    ├── dto/         CreateUserRequest, UpdateUserRequest, UserResponse
    ├── exception/   ApiError, ConflictException, ResourceNotFoundException, GlobalExceptionHandler
    ├── model/       UserProfile
    ├── repository/  UserProfileRepository
    ├── service/     UserProfileService
    └── resources/   application.yml, db/migration/V1__create_user_profiles.sql, keys/public.pem
```

## Ejecución

Requisitos: Docker Desktop (Compose v2), Git Bash, Postman. Opcional: Java 21 + Maven 3.9 para correr sin contenedores.

```bash
cp .env.example .env
docker compose up --build -d
docker compose ps          # 5 servicios Up
```

Health checks:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

Swagger (directo en cada servicio, no por el Gateway):

- Auth: http://localhost:8081/swagger-ui.html
- Users: http://localhost:8082/swagger-ui.html (botón **Authorize** → pegue solo el JWT)

## Prueba rápida con curl (Git Bash)

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"student","password":"Student123!"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')

curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"firstName":"Carlos","lastName":"Diaz","phone":"3001234567"}'   # 201

curl -i http://localhost:8080/api/users/me -H "Authorization: Bearer $TOKEN"  # 200
curl -i http://localhost:8080/api/users -H "Authorization: Bearer $TOKEN"     # 403 (USER)
curl -i http://localhost:8080/api/users                                        # 401 (sin JWT)
```

Usuarios de demostración (creados al arrancar auth-service):

| Rol   | Usuario | Contraseña  |
|-------|---------|-------------|
| ADMIN | admin   | Admin123!   |
| USER  | student | Student123! |

## Contrato de APIs

| Método | Endpoint            | Acceso             | Resultado                      |
|--------|---------------------|--------------------|--------------------------------|
| POST   | /api/auth/register  | Público            | Crea credencial USER (201)     |
| POST   | /api/auth/login     | Público            | Devuelve JWT (200)             |
| POST   | /api/users          | JWT                | Crea perfil del sujeto (201)   |
| GET    | /api/users/me       | JWT                | Perfil propio                  |
| GET    | /api/users          | ADMIN              | Lista perfiles                 |
| GET    | /api/users/{id}     | Propietario/ADMIN  | Consulta perfil                |
| PUT    | /api/users/{id}     | Propietario/ADMIN  | Actualiza perfil               |
| DELETE | /api/users/{id}     | ADMIN              | Elimina perfil (204)           |

Errores: 400 validación · 401 sin token / token inválido / credenciales malas · 403 rol insuficiente · 404 perfil inexistente · 409 duplicado.

## Verificar Flyway y persistencia

```bash
docker compose logs auth-service | grep -i flyway
docker compose exec auth-db  psql -U postgres -d auth_db  -c '\dt'   # credentials
docker compose exec users-db psql -U postgres -d users_db -c '\dt'   # user_profiles

docker compose down       # los datos permanecen en los volúmenes
docker compose down -v    # elimina también los volúmenes
```

## Postman

Importe `postman/Taller_02_v2.postman_collection.json` y `postman/Taller_02_v2.postman_environment.json`, seleccione el ambiente **Taller 02 v2.0 - Local** y ejecute la colección en orden (Collection Runner). Los logins guardan `studentToken`, `studentId`, `adminToken` y `adminId`.

## Ejecución sin Docker (opcional)

Levante solo las bases (`docker compose up -d auth-db users-db`) y luego, en tres terminales:

```bash
mvn -pl auth-service spring-boot:run
mvn -pl user-service spring-boot:run
mvn -pl api-gateway  spring-boot:run
```

Los valores por defecto de `application.yml` apuntan a `localhost:5433` y `localhost:5434`.

## Llaves RSA

`auth-service` tiene `private.pem` + `public.pem`; `user-service` solo `public.pem`. Para regenerarlas: `sh scripts/generate-rsa-keys.sh` (los tokens anteriores quedan inválidos). Son llaves de demostración: no reutilizar en producción.

## CI/CD

`.github/workflows/ci-cd.yml` corre en push y PR contra `main`: `mvn -B clean verify` con Java 21 y, en push a `main`, construye y publica en GHCR una imagen por servicio (`taller02-<servicio>:latest` y `:<sha>`).
