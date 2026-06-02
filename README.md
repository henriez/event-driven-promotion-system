# Deal Aggregator — Event-Driven Microservices Platform

Event-Driven Architecture (EDA) platform for managing product promotions using microservices. Built with Spring Boot 3.4, PostgreSQL, RabbitMQ, Zipkin, and Vue 3 + Vuetify.

## Architecture Overview

```
┌─────────────┐     ┌──────────────┐      ┌────────────────┐
│   Vue 3     │────▶│  MS-Gateway  │─────▶│  RabbitMQ      │
│  Frontend   │◀────│  (SSE Push)  │      │  promotion.ex  │
└─────────────┘     └──────┬───────┘      └───┬──┬──┬──┬───┘
                           │                  │  │  │  │
                     JWT   │           ┌──────┘  │  │  └──────────┐
                     Auth  │           │         │  │             │
                    ┌──────┴──────┐    │    ┌────┘  └────┐    ┌───┴──────────┐
                    │ MS-Auth     │    │    │ MS-Promo   │    │ MS-Notify    │
                    │ (Identity)  │    │    │ (Core)     │    │ (Email)      │
                    │ PostgreSQL  │    │    │ PostgreSQL │    │ PostgreSQL   │
                    └─────────────┘    │    └────────────┘    └──────────────┘
                                       │         │                    │
                                  DLQ Events    │              Resend API
                                       └─────────┘
```

## Microservices

| Service | Port | Debug | Responsibility |
|---------|------|-------|----------------|
| **MS-Gateway** | 8080 | 5000 | JWT validation, HTTP routing, SSE bridge to web clients |
| **MS-Promotion** | 8081 | 5001 | Core domain — promotion ingestion, metrics, heat score calculation |
| **MS-Authentication** | 8082 | 5002 | Identity provider — login, JWT issuance using RSA private key |
| **MS-Notification** | 8083 | 5003 | Email dispatch via Resend API — hot deals, DLQ failure alerts |

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Framework | Spring Boot 3.4.5 (Java 17) |
| Database | PostgreSQL 16 (database-per-service) |
| Message Broker | RabbitMQ 3 (management UI on port 15672) |
| Observability | Micrometer Tracing + Zipkin (port 9411) |
| Security | RSA Asymmetric JWT (JJWT 0.12.6) |
| Frontend | Vue 3 + Vuetify 4 |
| Build | Maven |

## Prerequisites

- Docker and Docker Compose
- Java 17+
- Node.js 20+
- Make sure ports 8080-8083, 5000-5003, 5432-5434, 5672, 15672, 9411 are free

## Quick Start

### 1. Clone and configure environment

```bash
git clone https://github.com/henriez/event-driven-promotion-system.git && cd event-driven-promotion-system
cp .env.example .env
```

Edit `.env` with your own credentials if needed (defaults work for local dev).

### 2. Start infrastructure (Docker)

```bash
docker compose up -d
```

This starts:
- `postgres-promotion` on port 5432
- `postgres-auth` on port 5433
- `postgres-notification` on port 5434
- `rabbitmq-server` on ports 5672 (AMQP) and 15672 (management UI)
- `zipkin` on port 9411

### 3. Start microservices

```bash
chmod +x run.sh
./run.sh
```

This launches all four services in background. Logs are written to `logs/`.

| Log File | Service |
|----------|---------|
| `logs/gateway.log` | MS-Gateway |
| `logs/promotion.log` | MS-Promotion |
| `logs/authentication.log` | MS-Authentication |
| `logs/notification.log` | MS-Notification |

### 4. Start frontend

```bash
cd frontend
npm install
npm run dev
```

The dev server starts on `http://localhost:5173` and proxies `/api` to the gateway on `http://localhost:8080`.

## Environment Variables

| Variable | Description |
|----------|-------------|
| `PROMO_DB_URL` | Promotion JDBC URL |
| `PROMO_DB_USER` | Promotion DB user |
| `PROMO_DB_PASS` | Promotion DB password |
| `PROMO_DB_NAME` | Promotion DB name |
| `AUTH_DB_URL` | Auth JDBC URL |
| `AUTH_DB_USER` | Auth DB user |
| `AUTH_DB_PASS` | Auth DB password |
| `AUTH_DB_NAME` | Auth DB name |
| `AUTH_PRIVATE_KEY` | RSA private key (PKCS8 PEM) for JWT signing |
| `AUTH_PUBLIC_KEY` | RSA public key (PEM) for gateway JWT validation |
| `NOTIF_DB_URL` | Notification JDBC URL |
| `NOTIF_DB_USER` | Notification DB user |
| `NOTIF_DB_PASS` | Notification DB password |
| `NOTIF_DB_NAME` | Notification DB name |
| `RABBITMQ_HOST` | RabbitMQ host |
| `RABBITMQ_PORT` | RabbitMQ port |
| `ZIPKIN_URL` | Zipkin endpoint (e.g. `http://localhost:9411/api/v2/spans`) |
| `RESEND_API_KEY` | Resend API key for email dispatch |

## API Endpoints

### MS-Authentication

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/auth/login` | Authenticate user (B2C) or store (B2B) |
| `GET` | `/api/auth/public-key` | Get RSA public key in PEM format |

### MS-Gateway

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/promotions` | Submit a new promotion (requires JWT) |
| `GET` | `/api/promotions` | Get historical promotion feed (requires JWT) |
| `GET` | `/api/stream` | SSE stream for real-time promotion updates |

### RabbitMQ Topology

| Exchange | Type | Routing Keys |
|----------|------|-------------|
| `promotion.exchange` | Topic | `promotion.received`, `promotion.created`, `promotion.hot` |
| `promotion.dlx` | Topic | `promotion.dlq` |

| Queue | Binding | DLX |
|-------|---------|-----|
| `promotion.received.queue` | `promotion.received` | `promotion.dlx` → `promotion.dlq` |
| `promotion.created.queue` | `promotion.created` | — |
| `promotion.hot.queue` | `promotion.hot` | — |
| `promotion.dlq.queue` | `promotion.dlq` | — |

## SSE Event Types

| SSE Event | Trigger | Frontend Handler |
|-----------|---------|------------------|
| *(unnamed)* | `promotion.created` | `EventSource.onmessage` — prepends to feed |
| `hot-deal` | `promotion.hot` | `EventSource.addEventListener('hot-deal')` — shows snackbar |

## Observability

- **Zipkin UI:** `http://localhost:9411` — trace requests across services
- **RabbitMQ Management:** `http://localhost:15672` (guest/guest) — inspect queues, messages, DLQ
- **Logs:** Each service writes to `logs/<service-name>.log` with trace/span IDs

## Debugging

Each microservice starts with a JDWP debug agent. Attach your IDE:

| Service | Debug Port |
|---------|-----------|
| MS-Gateway | 5000 |
| MS-Promotion | 5001 |
| MS-Authentication | 5002 |
| MS-Notification | 5003 |

