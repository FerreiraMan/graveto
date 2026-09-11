# Graveto

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)

Modular finance and portfolio management system. REST API for tracking accounts, transactions, transfers, recurring transactions/transfers, spending analytics, and investment portfolios — built with Spring Boot and Spring Modulith.

## Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 4.0 + Spring Modulith |
| Security | Spring Security + JWT (Auth0 java-jwt) |
| Persistence | Spring Data JPA + PostgreSQL 16 |
| Migrations | Flyway |
| HTTP Client | Spring RestClient |
| Build | Gradle 9 |
| Containerisation | Docker + Docker Compose |
| Testing | JUnit 5 + Testcontainers + REST Assured + MockServer |

## Modules

```
graveto/
├── identity/           # User registration, login, JWT issuance
├── moneytracker/
│   ├── accounts/       # Financial accounts management
│   ├── categories/     # Transaction categories (system + user-defined, filterable)
│   ├── transactions/   # Income/expense transactions, transfers, recurring transactions/transfers
│   └── analytics/      # Cash flow and category spending reports
├── portfolio/
│   ├── brokers/        # Investment platforms (DEGIRO, Trading 212, etc.)
│   ├── assets/         # Tradeable instruments (ETFs, stocks) + Yahoo Finance integration
│   ├── orders/         # Buy/sell order recording and updates
│   ├── positions/      # Portfolio positions (maintained aggregates) + valuation
│   └── stockexchange/  # Stock exchanges reference data (seeded)
└── common/             # Shared domain, JPA base, HTTP infra, exception handling, scheduling
```

## Features

### Identity
- User registration and login
- JWT-based authentication (1h expiry)
- Password reset request: single-use, time-limited token (SHA-256 hashed at rest) delivered by email;
  the endpoint returns the same generic response whether or not the account exists

### Accounts
- Create accounts with currency, initial balance, and institution
- Fetch single account (with membership roles)
- List all accounts for authenticated user
- Add members to an account with a specific role (owner, viewer, etc.)
- Close accounts (requires zero balance)

### Transactions
- Create income/expense transactions against an account and category
- Paginated, filterable, sortable transaction list (by account, category, date range, type, status)
- Update and soft-delete transactions (balance reverted on delete)

### Transfers
- Transfer funds between two accounts (creates correlated debit/credit pair)
- Fetch, update, and delete transfers by correlation ID

### Recurring Transactions
- Schedule recurring income/expense transactions (daily, weekly, bi-weekly, monthly, annually)
- Configurable day-of-week/day-of-month, optional end date, business-day adjustment
- Update schedule, frequency, status (active/paused), or next execution date
- List with filtering by status and account; cancel (soft-terminates the schedule)
- Daily scheduler creates due transactions and advances the next execution date

### Recurring Transfers
- Same recurring scheduling model as recurring transactions, applied to transfers between two accounts
- Create, update, list (filter by status/source/destination account), and cancel
- Daily scheduler creates due transfers and advances the next execution date

### Categories
- System-provided default categories (hierarchical)
- User-defined custom categories with optional parent and transaction type
- List categories filtered by display name, account, parent, and transaction type

### Analytics
- **Cash flow report**: monthly and yearly income, expense, and net income/expense for a given year, plus the resulting account balance at the end of each month/year
- Transfers (`TRANSFER_IN`/`TRANSFER_OUT`) are included in the balance calculation but reported separately (`transfersIn`/`transfersOut`) from income/expense, so they never distort net income/expense figures
- **Category spending report**: yearly and monthly totals per category (with subcategory breakdown), expense transactions only

### Brokers
- Create investment platform accounts (e.g. DEGIRO, Trading 212)
- Membership-based access control (OWNER, VIEWER) per broker
- Fetch single broker with membership details
- List all brokers for authenticated user
- Optional back-reference to a moneytracker account (loose coupling)

### Assets
- Search tradeable instruments via Yahoo Finance API (autocomplete)
- Create/follow assets (find-or-create pattern, avoids duplicates)
- Automatic price enrichment on asset creation via Yahoo Finance quote API
- Twice-daily scheduled price update for all tracked assets
- Rate-limited external API calls (per-user burst window)
- Stock exchange reference data (seeded via migrations, includes Yahoo Finance suffix)

### Orders
- Record buy/sell orders against a broker and asset
- Update order (quantity, price, fees, executed date, notes) with position recalculation
- Automatic position creation/update on order creation (weighted average cost basis)
- Order fees tracking

### Positions & Valuation
- Maintained aggregates (not derived on the fly) — quantity, average cost, total invested
- Updated transactionally within the same transaction as order creation/update
- One position per broker+asset pair
- Per-position valuation (market value, unrealized P&L, P&L %) using latest tracked asset price
- Portfolio-level valuation summary per broker

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
| `POSTGRES_DB` | `graveto_db` | Database name |
| `POSTGRES_USER` | `graveto_user` | PostgreSQL admin user (container bootstrap) |
| `POSTGRES_PASSWORD` | `graveto_password` | PostgreSQL admin password |
| `GRAVETO_APP_USER` | `graveto_app_user` | App runtime user (CRUD only) |
| `GRAVETO_APP_PASSWORD` | `graveto_app_password` | App runtime password |
| `GRAVETO_MIGRATOR_USER` | `graveto_migrator_user` | Flyway migrations user (DDL) |
| `GRAVETO_MIGRATOR_PASSWORD` | `graveto_migrator_password` | Flyway migrations password |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Comma-separated list of allowed CORS origins |
| `YFINANCE_API_KEY` | `yfinance_api_key` | Yahoo Finance API key |
| `SPRING_MAIL_HOST` | `smtp.gmail.com` | SMTP host used for outbound email |
| `SPRING_MAIL_PORT` | `587` | SMTP port (STARTTLS) |
| `SPRING_MAIL_USERNAME` | `email_username` | SMTP account username |
| `SPRING_MAIL_PASSWORD` | `email_password` | SMTP account password / app password |

> **Note:** all variables above are wired through `docker-compose.yml` into the `backend` and `postgres`
> containers, and are also read directly by Spring from the process environment for `./gradlew bootRun`
> (`application.yml`).

JWT signing secret and expiration (`jwt.signing-secret`, `jwt.expiration` in `application.yml`) are
currently hardcoded, not environment-driven — **change them directly in `application.yml` before any
production deployment.**

Password reset token settings (`security.forgot-password.token-bytes`, `security.forgot-password.token-expiration`
in `application.yml`) control token entropy and lifetime. Defaults are 32 random bytes and 30 minutes.

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

### Identity

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | ✗ | Register user |
| POST | `/api/auth/login` | ✗ | Login, returns JWT |
| POST | `/api/auth/forgot-password` | ✗ | Request a password reset token (emailed; generic response either way) |

### Accounts

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/accounts` | ✓ | Create account |
| GET | `/api/accounts` | ✓ | List accounts |
| GET | `/api/accounts/{sid}` | ✓ | Get account detail |
| PATCH | `/api/accounts/{sid}/close` | ✓ | Close account |
| POST | `/api/accounts/{sid}/memberships` | ✓ | Add member to account |

### Transactions & Transfers

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/transactions` | ✓ | Create transaction |
| GET | `/api/transactions` | ✓ | List transactions (paginated, filterable) |
| PATCH | `/api/transactions/{sid}` | ✓ | Update transaction |
| DELETE | `/api/transactions/{sid}` | ✓ | Delete transaction |
| POST | `/api/transfers` | ✓ | Create transfer |
| GET | `/api/transfers/{correlationId}` | ✓ | Get transfer |
| PATCH | `/api/transfers/{correlationId}` | ✓ | Update transfer |
| DELETE | `/api/transfers/{correlationId}` | ✓ | Delete transfer |

### Recurring Transactions & Transfers

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/recurring-transactions` | ✓ | Create recurring transaction |
| GET | `/api/recurring-transactions` | ✓ | List recurring transactions (filter by status, account) |
| PATCH | `/api/recurring-transactions/{sid}` | ✓ | Update recurring transaction |
| DELETE | `/api/recurring-transactions/{sid}` | ✓ | Cancel recurring transaction |
| POST | `/api/recurring-transfers` | ✓ | Create recurring transfer |
| GET | `/api/recurring-transfers` | ✓ | List recurring transfers (filter by status, source/destination account) |
| PATCH | `/api/recurring-transfers/{sid}` | ✓ | Update recurring transfer |
| DELETE | `/api/recurring-transfers/{sid}` | ✓ | Cancel recurring transfer |

### Categories & Analytics

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/categories` | ✓ | List categories (filter by display name, account, parent, type) |
| POST | `/api/categories` | ✓ | Create category |
| GET | `/api/analytics/{accountSid}/cash-flow?year=` | ✓ | Cash flow report (`year` optional, defaults to current year) |
| GET | `/api/analytics/{accountSid}/category-spending?year=` | ✓ | Category spending report (`year` optional, defaults to current year) |

### Portfolio

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/brokers` | ✓ | Create broker |
| GET | `/api/brokers` | ✓ | List brokers |
| GET | `/api/brokers/{sid}` | ✓ | Get broker detail |
| GET | `/api/assets/search` | ✓ | Search assets (Yahoo Finance) |
| POST | `/api/assets` | ✓ | Create/follow asset |
| POST | `/api/orders` | ✓ | Create order (+ position update) |
| PATCH | `/api/orders` | ✓ | Update order (+ position recalculation) |
| GET | `/api/brokers/{brokerSid}/positions` | ✓ | Position valuation overview |
| GET | `/api/brokers/{brokerSid}/positions/summary` | ✓ | Portfolio valuation summary |

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

## Scheduled Tasks

All schedules run on the `Europe/Lisbon` timezone and are configurable via `scheduled.cron.*` properties.

| Task | Default Schedule | Description |
|---|---|---|
| Asset price update | Daily at 10:30 and 17:30 | Fetches current market prices from Yahoo Finance for all tracked assets |
| Recurring transaction processing | Daily at 10:00 | Creates due recurring transactions and advances their next execution date |
| Recurring transfer processing | Daily at 10:00 | Creates due recurring transfers and advances their next execution date |

## Testing

```bash
./gradlew test              # unit tests
./gradlew integrationTest   # integration tests (requires Docker)
./gradlew check             # both + coverage verification
```

Integration tests use Testcontainers (PostgreSQL) and MockServer (Yahoo Finance API stub).

### Coverage

JaCoCo enforces 80% instruction and branch coverage. Excluded from coverage: domain entities, DTOs, commands, payloads, configuration, exceptions, and package-info files.
