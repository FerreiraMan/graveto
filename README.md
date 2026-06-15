# Graveto

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)

Modular finance and portfolio management system. REST API for tracking accounts, transactions, transfers, and spending analytics — built with Spring Boot and Spring Modulith.

## Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 4.0 + Spring Modulith |
| Security | Spring Security + JWT (Auth0 java-jwt) |
| Persistence | Spring Data JPA + PostgreSQL 16 |
| Migrations | Flyway |
| Build | Gradle 9 |
| Containerisation | Docker + Docker Compose |
| Testing | JUnit 5 + Testcontainers + REST Assured |

## Modules

```
graveto/
├── identity/        # User registration, login, JWT issuance
├── moneytracker/
│   ├── accounts/    # Financial accounts management
│   ├── categories/  # Transaction categories (system + user-defined)
│   ├── transactions/# Income/expense transactions + transfers
│   └── analytics/   # Cash flow and category spending reports
└── common/          # Shared domain, JPA base, exception handling
```

## Features

### Identity
- User registration and login
- JWT-based authentication (1h expiry)

### Accounts
- Create accounts with currency, initial balance, and institution
- Fetch single account (with membership roles)
- List all accounts for authenticated user
- Add members to an account with a specific role (owner, viewer, etc.)

### Transactions
- Create income/expense transactions against an account and category
- Paginated, filterable transaction list (by account, category, date range, type, status)
- Update and soft-delete transactions

### Transfers
- Transfer funds between two accounts (creates correlated debit/credit pair)
- Fetch, update, and delete transfers by correlation ID

### Categories
- System-provided default categories (hierarchical)
- User-defined custom categories with optional parent and transaction type
- List all categories (system + user's own)

### Analytics
- **Cash flow report**: monthly income, expense, and net flow for a given year
- **Category spending report**: yearly and monthly totals per category (with subcategory breakdown)

## Getting Started

### Prerequisites

| Mode | Requirements |
|---|---|
| Docker (recommended) | Docker + Docker Compose |
| Local dev | Java 21 + Docker (for DB auto-start) |

### Run with Docker

```bash
./gradlew initApplication
```

Builds the JAR and starts the app + PostgreSQL via Docker Compose. API available at `http://localhost:8080/api`.

```bash
./gradlew finalizeBuild   # stop and remove containers
```

### Run locally (dev)

Spring Boot Docker Compose support auto-starts PostgreSQL if Docker is available.

```bash
./gradlew bootRun
```

### Configuration

Copy `.env.example` to `.env` and adjust values for your environment:

```bash
cp .env.example .env
```

| Variable | Default | Description |
|---|---|---|
| `GRAVETO_POSTGRES_DB` | `graveto_db` | Database name |
| `GRAVETO_POSTGRES_USER` | `graveto_user` | PostgreSQL admin user (container bootstrap) |
| `GRAVETO_POSTGRES_PASSWORD` | `graveto_password` | PostgreSQL admin password |
| `GRAVETO_APP_USER` | `graveto_app_user` | App runtime user (CRUD only) |
| `GRAVETO_APP_PASSWORD` | `graveto_app_password` | App runtime password |
| `GRAVETO_MIGRATOR_USER` | `graveto_migrator_user` | Flyway migrations user (DDL) |
| `GRAVETO_MIGRATOR_PASSWORD` | `graveto_migrator_password` | Flyway migrations password |
| `jwt.signing-secret` | `jwt_graveto_secret` | JWT signing secret — **change in production** |
| `jwt.expiration-ms` | `3600000` | JWT expiry in milliseconds (default: 1h) |

### Database Security

The project follows the principle of least privilege with separate database users:

| User | Purpose | Permissions |
|---|---|---|
| `graveto_user` | Container bootstrap / admin | Superuser (internal only) |
| `graveto_migrator_user` | Flyway migrations (DDL) | CREATE, ALTER, DROP on schema |
| `graveto_app_user` | Application runtime (DML) | SELECT, INSERT, UPDATE, DELETE |

Users are created automatically on first container start via the init script baked into the custom PostgreSQL image. The app connects with the least-privileged user; Flyway uses the migrator user exclusively for schema changes.

## API Overview

All endpoints are prefixed with `/api`. Protected endpoints require `Authorization: Bearer <token>`.

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | ✗ | Register user |
| POST | `/api/auth/login` | ✗ | Login, returns JWT |
| POST | `/api/accounts` | ✓ | Create account |
| GET | `/api/accounts` | ✓ | List accounts |
| GET | `/api/accounts/{sid}` | ✓ | Get account detail |
| PATCH | `/api/accounts/{sid}/close` | ✓ | Close account |
| POST | `/api/accounts/{sid}/memberships` | ✓ | Add member to account |
| POST | `/api/transactions` | ✓ | Create transaction |
| GET | `/api/transactions` | ✓ | List transactions (paginated) |
| PATCH | `/api/transactions/{sid}` | ✓ | Update transaction |
| DELETE | `/api/transactions/{sid}` | ✓ | Delete transaction |
| POST | `/api/transfers` | ✓ | Create transfer |
| GET | `/api/transfers/{correlationId}` | ✓ | Get transfer |
| PATCH | `/api/transfers/{correlationId}` | ✓ | Update transfer |
| DELETE | `/api/transfers/{correlationId}` | ✓ | Delete transfer |
| GET | `/api/categories` | ✓ | List categories |
| POST | `/api/categories` | ✓ | Create category |
| GET | `/api/analytics/{accountSid}/cash-flow` | ✓ | Cash flow report |
| GET | `/api/analytics/{accountSid}/category-spending` | ✓ | Category spending report |

### Authentication flow

```bash
# 1. Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "secret"}'

# 2. Login — returns a JWT
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "secret"}'

# 3. Use the token
curl http://localhost:8080/api/accounts \
  -H "Authorization: Bearer <token>"
```

## Testing

```bash
./gradlew test              # unit tests
./gradlew integrationTest   # integration tests (requires Docker)
./gradlew check             # both
```
