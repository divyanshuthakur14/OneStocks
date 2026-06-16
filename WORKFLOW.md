# OneStocks — Project Workflow

## Table of Contents
1. [System Architecture](#1-system-architecture)
2. [Authentication Workflows](#2-authentication-workflows)
3. [Stock Trading Workflow](#3-stock-trading-workflow)
4. [Frontend Routing & Guard Flow](#4-frontend-routing--guard-flow)
5. [Backend Request Pipeline](#5-backend-request-pipeline)
6. [Database Entity Relationships](#6-database-entity-relationships)
7. [Real-time Price Simulation](#7-real-time-price-simulation)
8. [Local Development Setup](#8-local-development-setup)

---

## 1. System Architecture

```mermaid
graph TB
    subgraph "Client (Browser)"
        FE["Angular 21 SPA<br/>Tailwind CSS<br/>localhost:4200"]
    end

    subgraph "Backend (Spring Boot)"
        API["REST API<br/>Spring Boot 3.5<br/>Java 21<br/>localhost:8082"]
        SEC["Spring Security<br/>JWT Auth Filter"]
        SCHED["@Scheduled<br/>Price Simulator<br/>every 5s"]
    end

    subgraph "Persistence"
        DB[("MySQL 8.0+<br/>onestocks_db")]
    end

    FE -- "HTTP / JWT Bearer" --> API
    API --> SEC
    SEC --> API
    API --> DB
    SCHED --> DB
```

---

## 2. Authentication Workflows

### 2.1 Signup

```mermaid
sequenceDiagram
    actor User
    participant FE as Angular (SignupComponent)
    participant API as POST /api/auth/signup
    participant DB as MySQL

    User->>FE: Fill username, email, password
    FE->>FE: Validate fields (client-side)
    FE->>API: POST { username, email, password }
    API->>DB: Check email/username uniqueness
    alt Already exists
        DB-->>API: Duplicate entry
        API-->>FE: 409 Conflict
        FE-->>User: Show error message
    else New user
        API->>DB: Save User (BCrypt password, role=USER)
        API->>DB: Create Wallet (balance = 10,000)
        API->>DB: Generate & save RefreshToken
        API-->>FE: 200 { token, refreshToken, username, email, role }
        FE->>FE: Save to localStorage
        FE-->>User: Redirect → /login
    end
```

### 2.2 Login

```mermaid
sequenceDiagram
    actor User
    participant FE as Angular (LoginComponent)
    participant API as POST /api/auth/login
    participant DB as MySQL

    User->>FE: Enter email or username + password
    FE->>API: POST { identifier, password }
    API->>DB: Find user by email OR username
    alt User not found
        API-->>FE: 401 Unauthorized
        FE-->>User: "Invalid credentials"
    else User found
        API->>API: BCrypt.verify(password, hash)
        alt Password mismatch
            API-->>FE: 401 Unauthorized
            FE-->>User: "Invalid credentials"
        else Password OK
            API->>API: Generate JWT (24h expiry)
            API->>DB: Save RefreshToken (7-day UUID)
            API-->>FE: 200 { token, refreshToken, username, email, role }
            FE->>FE: Store in localStorage
            FE-->>User: Redirect → /home
        end
    end
```

### 2.3 Token Refresh (Silent Auto-Renewal)

```mermaid
sequenceDiagram
    participant FE as Angular (JwtInterceptor)
    participant API as POST /api/auth/refresh
    participant DB as MySQL

    FE->>API: Any authenticated request (expired JWT)
    API-->>FE: 401 Unauthorized
    FE->>FE: Detect 401 + has refreshToken in localStorage
    FE->>API: POST { refreshToken }
    API->>DB: Lookup RefreshToken
    alt Token expired or revoked
        API-->>FE: 401 Unauthorized
        FE->>FE: Clear localStorage
        FE-->>User: Redirect → /login
    else Token valid
        API->>API: Generate new JWT (24h)
        API-->>FE: 200 { token, ... }
        FE->>FE: Update token in localStorage
        FE->>API: Retry original request with new token
    end
```

### 2.4 Logout

```mermaid
sequenceDiagram
    actor User
    participant FE as Angular (AuthService)
    participant API as POST /api/auth/logout
    participant DB as MySQL

    User->>FE: Click Logout
    FE->>API: POST { token, refreshToken }
    API->>DB: Add JWT to TokenBlacklist
    API->>DB: Delete all RefreshTokens for user
    API-->>FE: 200 OK
    FE->>FE: Clear localStorage
    FE-->>User: Redirect → /login
```

---

## 3. Stock Trading Workflow

### 3.1 Browse Stocks

```mermaid
sequenceDiagram
    actor User
    participant FE as HomeComponent
    participant SVC as StockService
    participant API as GET /api/stocks
    participant DB as MySQL

    User->>FE: Navigate to /home (after login)
    FE->>SVC: getStocks()
    SVC->>API: GET /api/stocks [Bearer token]
    API->>DB: SELECT all stocks
    DB-->>API: List<Stock>
    API-->>SVC: StockSummaryResponse[] { symbol, name, sector, price, change% }
    SVC-->>FE: Observable<Stock[]>
    FE-->>User: Render stock list with live price changes
```

### 3.2 View Stock Detail

```mermaid
sequenceDiagram
    actor User
    participant FE as StockDetailModal
    participant API as GET /api/stocks/{symbol}
    participant DB as MySQL

    User->>FE: Click on stock row
    FE->>API: GET /api/stocks/AAPL [Bearer token]
    API->>DB: Find Stock by symbol
    API->>DB: Find User's Holding for this stock
    API->>DB: Find User's Wallet balance
    API-->>FE: StockDetailResponse { stock, holding: { qty, avgBuyPrice, P&L }, walletBalance }
    FE-->>User: Show modal with detail + P&L + BUY/SELL form
```

### 3.3 Execute BUY Transaction

```mermaid
sequenceDiagram
    actor User
    participant FE as StockDetailModal
    participant API as POST /api/transactions
    participant SVC as TransactionService
    participant DB as MySQL

    User->>FE: Enter quantity → Click BUY
    FE->>API: POST { symbol, quantity, type: BUY } [Bearer token]
    API->>SVC: executeBuy(userId, symbol, quantity)
    SVC->>DB: Get current stock price
    SVC->>DB: Get user Wallet
    alt Insufficient balance
        SVC-->>API: InsufficientFundsException
        API-->>FE: 400 Bad Request "Insufficient funds"
        FE-->>User: Show error
    else Sufficient balance
        SVC->>DB: Debit wallet (balance -= price × qty)
        SVC->>DB: Find or create Holding
        SVC->>DB: Update Holding (qty += purchased, recalculate avg price)
        SVC->>DB: Create Transaction (status=EXECUTED)
        SVC-->>API: TransactionResponse
        API-->>FE: 200 { transaction, balance_after }
        FE-->>User: Success message + updated balance
    end
```

### 3.4 Execute SELL Transaction

```mermaid
sequenceDiagram
    actor User
    participant FE as StockDetailModal
    participant API as POST /api/transactions
    participant SVC as TransactionService
    participant DB as MySQL

    User->>FE: Enter quantity → Click SELL
    FE->>API: POST { symbol, quantity, type: SELL } [Bearer token]
    API->>SVC: executeSell(userId, symbol, quantity)
    SVC->>DB: Find user's Holding for this stock
    alt No holding or insufficient quantity
        SVC-->>API: InvalidTransactionException
        API-->>FE: 400 Bad Request "Insufficient shares"
        FE-->>User: Show error
    else Valid holding
        SVC->>DB: Reduce Holding quantity
        SVC->>DB: If qty = 0, delete Holding
        SVC->>DB: Credit wallet (balance += price × qty)
        SVC->>DB: Create Transaction (status=EXECUTED)
        SVC-->>API: TransactionResponse
        API-->>FE: 200 { transaction, balance_after }
        FE-->>User: Success message + updated balance
    end
```

### 3.5 View Portfolio & Transaction History

```mermaid
sequenceDiagram
    actor User
    participant FE as ProfileComponent
    participant H as HoldingService
    participant T as TransactionService
    participant W as WalletService
    participant DB as MySQL

    User->>FE: Navigate to /profile
    FE->>H: getMyHoldings(username)
    FE->>T: getMyTransactions(username)
    FE->>W: getBalance()
    H->>DB: SELECT holdings WHERE user_id = ?
    T->>DB: SELECT transactions WHERE user_id = ?
    W->>DB: SELECT wallet WHERE user_id = ?
    DB-->>FE: Holdings, Transactions, Balance
    FE->>FE: Compute current value = qty × current_price
    FE->>FE: Compute P&L = current_value − cost_basis
    FE-->>User: Render portfolio + transaction history
```

---

## 4. Frontend Routing & Guard Flow

```mermaid
flowchart TD
    START([User navigates to URL]) --> ROUTER{Angular Router}

    ROUTER -- "/" --> REDIRECT[Redirect → /login]
    ROUTER -- "/login" --> LOGIN[LoginComponent - public]
    ROUTER -- "/signup" --> SIGNUP[SignupComponent - public]
    ROUTER -- "/home" --> AUTHGUARD{authGuard}
    ROUTER -- "/profile" --> AUTHGUARD

    AUTHGUARD -- "isLoggedIn() = false" --> REDIR_LOGIN[Redirect → /login]
    AUTHGUARD -- "isLoggedIn() = true" --> PROTECTED[Render protected component]

    PROTECTED -- "/home" --> HOME[HomeComponent<br/>Stock Dashboard]
    PROTECTED -- "/profile" --> PROFILE[ProfileComponent<br/>Portfolio & History]

    LOGIN -- Successful login --> NAVHOME[Navigate → /home]
    SIGNUP -- Successful signup --> NAVLOGIN[Navigate → /login]
```

---

## 5. Backend Request Pipeline

```mermaid
flowchart TD
    REQ([HTTP Request]) --> CORS[CORS Filter<br/>Allow: localhost:4200]
    CORS --> JWTFILTER{JwtAuthFilter}

    JWTFILTER -- "No Authorization header<br/>or /api/auth/** path" --> ANON[Proceed as anonymous]
    JWTFILTER -- "Has Bearer token" --> BLACKLIST{Check TokenBlacklist}

    BLACKLIST -- "Token is blacklisted" --> REJECT401[Return 401 Unauthorized]
    BLACKLIST -- "Not blacklisted" --> VALIDATE{Validate JWT signature & expiry}

    VALIDATE -- "Invalid / expired" --> REJECT401
    VALIDATE -- "Valid" --> LOADUSER[Load UserDetails from DB]
    LOADUSER --> SETCTX[Set SecurityContext]
    SETCTX --> CONTROLLER

    ANON --> SECURITY{Spring Security<br/>Authorization Check}
    CONTROLLER --> SECURITY

    SECURITY -- "/api/auth/** = public" --> HANDLER[Controller Handler]
    SECURITY -- "/api/admin/** requires ROLE_ADMIN" --> ROLECHECK{Has ADMIN role?}
    SECURITY -- "All others require AUTH" --> AUTHCHECK{Is authenticated?}

    ROLECHECK -- "No" --> REJECT403[Return 403 Forbidden]
    ROLECHECK -- "Yes" --> HANDLER
    AUTHCHECK -- "No" --> REJECT401
    AUTHCHECK -- "Yes" --> HANDLER

    HANDLER --> SERVICE[Service Layer]
    SERVICE --> REPO[Repository / JPA]
    REPO --> DB[(MySQL)]
    DB --> RESP([HTTP Response])
```

---

## 6. Database Entity Relationships

```mermaid
erDiagram
    users {
        bigint id PK
        varchar username UK
        varchar email UK
        varchar password
        enum role
        boolean remember_me
        datetime created_at
    }
    stocks {
        bigint id PK
        varchar symbol UK
        varchar name
        varchar sector
        text description
        decimal current_price
        decimal previous_close
        datetime updated_at
    }
    holdings {
        bigint id PK
        bigint user_id FK
        bigint stock_id FK
        int quantity
        decimal average_buy_price
    }
    transactions {
        bigint id PK
        bigint user_id FK
        bigint stock_id FK
        enum type
        int quantity
        decimal price_per_share
        decimal total_amount
        enum status
        datetime created_at
    }
    wallets {
        bigint id PK
        bigint user_id FK
        decimal balance
        datetime updated_at
    }
    refresh_tokens {
        bigint id PK
        varchar token UK
        datetime expiry_date
        boolean revoked
        bigint user_id FK
    }
    token_blacklist {
        bigint id PK
        varchar token UK
        datetime blacklisted_at
    }

    users ||--|| wallets : "has one"
    users ||--o| refresh_tokens : "has one"
    users ||--o{ holdings : "owns"
    users ||--o{ transactions : "makes"
    stocks ||--o{ holdings : "held in"
    stocks ||--o{ transactions : "traded in"
```

---

## 7. Real-time Price Simulation

```mermaid
flowchart LR
    TIMER(["@Scheduled every 5s<br/>(StockService)"])
    TIMER --> FETCH["Load all stocks from DB"]
    FETCH --> LOOP["For each stock"]
    LOOP --> RAND["Generate random fluctuation<br/>±0.5% of current_price"]
    RAND --> UPDATE["previousClose ← currentPrice<br/>currentPrice ← currentPrice × (1 + fluctuation)"]
    UPDATE --> SAVE["Save updated stock to DB"]
    SAVE --> CLIENTS["Next frontend poll<br/>reflects new prices"]
```

> **Note:** The frontend polls stock prices each time the user loads `/home` or opens the stock detail modal. There is no WebSocket push — the ±0.5% simulation runs server-side continuously regardless of active users.

---

## 8. Local Development Setup

```mermaid
flowchart TD
    subgraph "Prerequisites"
        JAVA["Java 21"]
        NODE["Node.js + npm 11"]
        MYSQL["MySQL 8.0 running on :3306"]
    end

    subgraph "Database Bootstrap"
        CREATEDB["CREATE DATABASE onestocks_db;"]
        SEED["DataSeeder auto-runs on startup<br/>(seeds 10 demo stocks if empty)"]
        CREATEDB --> SEED
    end

    subgraph "Start Backend"
        BE_CMD["cd backEnd/app<br/>mvn spring-boot:run"]
        BE_PORT[":8082 ready"]
        BE_CMD --> BE_PORT
    end

    subgraph "Start Frontend"
        FE_CMD["cd frontEnd/app<br/>npm install && npm start"]
        FE_PORT[":4200 ready"]
        FE_CMD --> FE_PORT
    end

    MYSQL --> CREATEDB
    JAVA --> BE_CMD
    NODE --> FE_CMD
    BE_PORT --> BROWSER["Open http://localhost:4200"]
    FE_PORT --> BROWSER
    SEED --> BROWSER
```

### Default Test Credentials
After starting the app, register a new account via `/signup`. The wallet is automatically seeded with **$10,000** on registration.

### Configuration Quick-Reference

| Setting | Value |
|---|---|
| Backend port | `8082` |
| Frontend port | `4200` |
| DB name | `onestocks_db` |
| DB user | `root` |
| JWT expiry | 24 hours |
| Refresh token expiry | 7 days |
| Price update interval | 5 seconds |
| Initial wallet balance | $10,000 |
