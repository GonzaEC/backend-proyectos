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

## Endpoints

Base path: `/api/projects`

| Método | Path | Auth | Descripción |
|--------|------|------|-------------|
| `GET` | `/api/projects` | Público | Listar proyectos (filtros: `state`, `energyType`, paginación) |
| `GET` | `/api/projects/{id}` | Público | Detalle de un proyecto |
| `POST` | `/api/projects` | `project:create` | Crear proyecto (inicia en DRAFT) |
| `PUT` | `/api/projects/{id}` | `project:update` + owner | Editar metadata |
| `DELETE` | `/api/projects/{id}` | `project:delete` + owner | Soft delete |
| `PUT` | `/api/projects/{id}/state` | `project:update` + owner | Cambiar estado |
| `GET` | `/api/projects/{id}/holders` | `project:read` | Listar holders con tokensAmount |
| `GET` | `/api/projects/{id}/metrics` | Público | Historial de métricas |
| `POST` | `/api/projects/{id}/metrics` | `project:update` + owner ó `ROLE_ADMIN` | Registrar métrica |
| `GET` | `/api/projects/{id}/documents` | `project:read` | Listar documentos |
| `POST` | `/api/projects/{id}/documents` | `project:update` + owner | Upload → presigned S3 URL |
| `DELETE` | `/api/projects/{id}/documents/{docId}` | `project:delete` + owner | Eliminar documento |

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
          ↓          ↓       ↓
       CANCELLED  CANCELLED  ✗
```

- La progresión normal es secuencial y sin retroceso.
- **Cancelación** (`CANCELLED`) es posible desde `DRAFT`, `PRE_OPEN` y `OPEN`.
- Desde `OPEN`, **solo un admin** puede cancelar (hay inversores con tokens).
- Desde `DRAFT` / `PRE_OPEN`, el dev owner también puede cancelar.
- `CLOSED` y `CANCELLED` son estados finales — no se puede salir de ellos.
- La baja (soft delete) está permitida en cualquier estado. Penalizaciones/reembolsos a definir.

## Autorización por rol

| Operación | admin | dev (owner) | dev (no owner) | investor |
|-----------|-------|-------------|----------------|----------|
| Ver proyectos / métricas | ✅ | ✅ | ✅ | ✅ |
| Ver holders / documentos | ✅ | ✅ | ✅ | ✅ |
| Crear proyecto | ✅ | ✅ | ✅ | ❌ |
| Editar / subir doc / registrar métrica | ✅ | ✅ | ❌ | ❌ |
| Avanzar estado (normal) | ✅ | ✅ | ❌ | ❌ |
| Cancelar desde DRAFT / PRE_OPEN | ✅ | ✅ | ❌ | ❌ |
| Cancelar desde OPEN | ✅ | ❌ | ❌ | ❌ |
| Eliminar proyecto | ✅ | ✅ | ❌ | ❌ |

## Eventos Kafka

**Publica:**

| Tópico | Cuándo |
|--------|--------|
| `projects.created` | Al crear un proyecto |
| `projects.state_changed` | Al cambiar el estado (incluye cancelación) |
| `projects.metrics_updated` | Al registrar una métrica |

**Consume:**

| Tópico | Publicado por | Para qué |
|--------|--------------|---------|
| `users.token_purchased` | `service-users` | Actualizar holdings del usuario |
| `marketplace.order_matched` | `service-marketplace` | Actualizar holdings tras venta P2P |

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
- **service-blockchain** — Smart contracts, mint/burn de tokens
- **service-marketplace** — Mercado secundario P2P
- **service-dividends** — Cálculo y distribución de dividendos
- **service-wallet** — Pagos fiat, PSP
- **service-kyc** — Verificación de identidad
- **service-notify** — Notificaciones
- **api-gateway** — Punto de entrada único
