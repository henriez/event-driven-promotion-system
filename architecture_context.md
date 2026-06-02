# System Architecture & Implementation Guide: Deal Aggregator Platform

## 1. System Overview & Tech Stack
The system is an event-driven microservices architecture.
* **Framework:** Spring Boot 3.2.x (Java)
* **Databases:** PostgreSQL (Database-per-service pattern)
* **Message Broker:** RabbitMQ (AMQP)
* **Observability:** Micrometer Tracing with Zipkin (W3C Trace Context) and SLF4J/Logback for file-based logging.
* **Security:** RSA Asymmetric JWT (JSON Web Tokens).

## 2. Architectural Rules & Patterns
* **No Shared Databases:** Microservices do not share database connections. Cross-domain data needs must be handled via Event-Carried State Transfer (duplicating IDs via RabbitMQ).
* **Stateless Edge Validation:** The API Gateway must not connect to a database. It validates JWTs entirely in-memory using a cached Public Key.
* **Fail Fast vs. The Asynchronous Gap:** * Schema and signature validation happen synchronously at the Gateway (HTTP 400/401).
  * Business logic failures (e.g., invalid prices) happen asynchronously in the domain services. These trigger Dead Letter Queue (DLQ) events, and the Notification service emails the store.

## 3. Microservices Domain Specification

### MS-Auth (Identity Provider)
* **Responsibility:** Manages authentication state for human users (B2C) and external stores (B2B). Issues signed JWTs using an RSA Private Key.
* **Endpoints:** Synchronous `POST /api/auth/login`.

### MS-Gateway (Edge Router & Bridge)
* **Responsibility:** JWT signature verification (using MS-Auth's Public Key), rate limiting, HTTP routing, and SSE management.
* **SSE Bridge:** Holds open connections (`GET /api/stream`). Listens to RabbitMQ (`promotion.created`, `promotion.hot`) and translates them into SSE text streams to the web client.

### MS-Promotion (Core Domain & Metrics)
* **Responsibility:** The single source of truth for deals. Processes incoming promotions, handles user interactions (votes/clicks), and calculates algorithmic heat scores.
* **Database Optimization (Vertical Partitioning):** The database is split into `promotion` (static text data) and `promotion_metrics` (highly volatile integer/decimal data). This preserves PostgreSQL HOT (Heap-Only Tuple) optimizations on the main table during voting spikes.
* **Performance Strategy:** Interaction writes (clicks/upvotes) must be batched or cached in memory to reduce continuous index B-Tree rebalancing on the `heat_score` column.

### MS-Notification (Communication)
* **Responsibility:** Dispatches external emails via the Resend API. Maps categories to users and alerts stores of Hot Deals or DLQ ingestion failures.
* **Coupling:** Fully decoupled from domain entities. It relies strictly on the string payload of RabbitMQ events.

## 4. Database Schema (DDL)

```sql
-- MS-Auth Database
CREATE TABLE "user" (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);
CREATE INDEX idx_users_email ON "user"(email);

CREATE TABLE store_credentials (
    store_id VARCHAR(100) PRIMARY KEY,
    public_key TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

-- MS-Promotion Database
CREATE TABLE promotion (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2),
    category VARCHAR(100) NOT NULL,
    store_id VARCHAR(100) NOT NULL,
    url TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    valid_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_promotion_status ON promotion(status);
CREATE INDEX idx_promotion_category ON promotion(category);

CREATE TABLE promotion_metrics (
    promotion_id BIGINT PRIMARY KEY,
    upvotes INT DEFAULT 0,
    heat_score DECIMAL(8,4) DEFAULT 0.0000,
    last_calculated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_metrics_heat_score ON promotion_metrics(heat_score DESC);

-- MS-Notification Database
CREATE TABLE subscriber (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE subscriber_preference (
    id BIGSERIAL PRIMARY KEY,
    subscriber_id BIGINT NOT NULL,
    category VARCHAR(100) NOT NULL,
    CONSTRAINT fk_subscriber FOREIGN KEY (subscriber_id) REFERENCES subscriber(id) ON DELETE CASCADE
);
CREATE INDEX idx_sub_pref_category ON subscriber_preference(category);

CREATE TABLE store_contact (
    store_id VARCHAR(100) PRIMARY KEY,
    email VARCHAR(255) NOT NULL
);

CREATE TABLE notification_log (
    id BIGSERIAL PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    promotion_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    dispatched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);