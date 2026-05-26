# Promotions System - Distributed Architecture

Event-Driven Architecture (EDA) project for managing product promotions using microservices.

## Architecture

- **Frontend:** Vue 3 + Vuetify
- **Gateway / API:** Spring Boot (REST + SSE)
- **Microservices:** Promotion, Ranking, Notification (Spring Boot)
- **Message Broker:** RabbitMQ
- **Databases:** PostgreSQL (One per microservice)
- **External API:** Resend (Email delivery)

## Prerequisites

- Docker and Docker Compose
- Java 17+
- Node.js 20+
- Maven

## Environment Setup

Create a `.env` file in the root directory:

```env
PROMO_DB_USER=your_user
PROMO_DB_PASS=your_password
RANKING_DB_USER=your_user
RANKING_DB_PASS=your_password
NOTIF_DB_USER=your_user
NOTIF_DB_PASS=your_password
RESEND_APY_KEY=your_api_key

