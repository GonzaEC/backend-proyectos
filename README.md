# service-projects

Microservicio de la plataforma **LIKEN** responsable de la gestión de campos energéticos (proyectos de inversión en energía renovable).

## Stack

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.2.4 |
| ORM | Spring Data JPA + Hibernate |
| Base de datos | PostgreSQL 15 |
| Migraciones | Flyway 10 |
| Mensajería | Apache Kafka (Spring Kafka) |
| Almacenamiento | AWS S3 / MinIO (dev) |
| Build | Maven 3.9+ |

## Requisitos

- Java 21
- Maven 3.9+
- Docker + Docker Compose

## Levantar en local

```bash
# 1. Infraestructura (PostgreSQL + Kafka + MinIO)
docker-compose up -d

# 2. Variables de entorno
cp .env.example .env
# Editá .env y configurá JWT_SECRET con el mismo valor que service-users

# 3. Correr la app
./mvnw spring-boot:run
```

La app queda disponible en `http://localhost:8081`.

## Tests

```bash
# Unit tests (H2 en memoria, sin Docker)
./mvnw test

# Integration tests (requiere docker-compose up)
./mvnw test -Pintegration
```

## Variables de entorno

| Variable | Descripción | Default |
|----------|-------------|---------|
| `DB_URL` | JDBC URL de PostgreSQL | `jdbc:postgresql://localhost:5432/service_projects` |
| `DB_USERNAME` | Usuario de la DB | `postgres` |
| `DB_PASSWORD` | Contraseña de la DB | `secret` |
| `JWT_SECRET` | Clave compartida con service-users | — |
| `KAFKA_BOOTSTRAP_SERVERS` | Brokers de Kafka | `localhost:9092` |
| `AWS_ACCESS_KEY_ID` | Credencial S3 / MinIO | `minioadmin` |
| `AWS_SECRET_ACCESS_KEY` | Credencial S3 / MinIO | `minioadmin` |
| `AWS_REGION` | Región AWS | `us-east-1` |
| `S3_BUCKET_NAME` | Bucket de documentos | `sip-project-documents` |
| `S3_ENDPOINT` | Endpoint S3 (MinIO en dev) | `http://localhost:9000` |
| `PORT` | Puerto del servidor | `8081` |
| `FRONTEND_URL` | URL del frontend (CORS) | `http://localhost:5173` |

## Endpoints principales

Base path: `/api/projects`

| Método | Path | Auth | Descripción |
|--------|------|------|-------------|
| `GET` | `/api/projects` | Público | Listar proyectos (filtros: `state`, `energyType`, paginación) |
| `GET` | `/api/projects/{id}` | Público | Detalle de un proyecto |
| `POST` | `/api/projects` | `project:create` | Crear proyecto (inicia en DRAFT) |
| `PUT` | `/api/projects/{id}` | `project:update` | Editar metadata |
| `DELETE` | `/api/projects/{id}` | `project:delete` | Soft delete (solo si está en DRAFT) |
| `PUT` | `/api/projects/{id}/state` | `project:update` | Avanzar estado (`{ "state": "PRE_OPEN" }`) |
| `GET` | `/api/projects/{id}/holders` | `project:read` | Holders con tokensAmount |
| `GET` | `/api/projects/{id}/metrics` | Público | Historial de métricas |
| `POST` | `/api/projects/{id}/metrics` | `ADMIN` o owner | Registrar métrica de producción |
| `GET` | `/api/projects/{id}/documents` | `project:read` | Listar documentos |
| `POST` | `/api/projects/{id}/documents` | `project:update` | Iniciar upload → devuelve presigned S3 URL |
| `DELETE` | `/api/projects/{id}/documents/{docId}` | `project:delete` | Eliminar documento |

Todas las respuestas usan el wrapper:

```json
{
  "message": "Proyecto obtenido exitosamente",
  "data": { ... },
  "status": 200,
  "timestamp": "2026-05-17T12:00:00"
}
```

## Ciclo de vida de un proyecto

```
DRAFT → PRE_OPEN → OPEN → CLOSED
```

- Solo se puede avanzar en orden secuencial (no hay retroceso).
- Solo proyectos en `DRAFT` pueden eliminarse (soft delete).

## Eventos Kafka

**Publica:**

| Tópico | Cuándo |
|--------|--------|
| `projects.created` | Al crear un proyecto |
| `projects.state_changed` | Al cambiar el estado |
| `projects.metrics_updated` | Al registrar una métrica |

**Consume:**

| Tópico | Para qué |
|--------|---------|
| `users.token_purchased` | Actualizar holdings del usuario |
| `marketplace.order_matched` | Actualizar holdings tras venta P2P |

## Estructura de paquetes

```
src/main/java/com/plataforma/projects/
├── Application.java
├── config/          # SecurityConfig, KafkaConfig, S3Config
├── controller/      # ProjectController, MetricController, DocumentController
├── dto/             # Request/Response DTOs + ApiResponse
├── event/           # ProjectEventPublisher, UserHoldingEventConsumer
├── exception/       # Excepciones de dominio + GlobalExceptionHandler
├── model/           # Entidades JPA + enums
├── repository/      # Interfaces JpaRepository
├── security/        # JwtUtils, JwtAuthenticationFilter
└── service/         # Interfaces + impl/
```

## Contexto del ecosistema

Este servicio forma parte de la plataforma LIKEN junto con:

- **service-users** — Auth JWT, usuarios, roles, wallet
