# OneStocks — Authentication & Security Implementation

**Author:** Divyanshu Singh Thakur  
**Date:** April 2026  
**Stack:** Spring Boot · Spring Security · JWT · JPA/Hibernate (Backend) · Angular · RxJS (Frontend)

---

## Table of Contents

### Backend
1. [Architecture Overview](#architecture-overview)
2. [Authentication Flow](#authentication-flow)
3. [Controllers](#controllers)
4. [DTOs — Data Transfer Objects](#dtos--data-transfer-objects)
5. [Models / Entities](#models--entities)
6. [Repositories](#repositories)
7. [Security Layer](#security-layer)
8. [Services](#services)
9. [Exception Handling](#exception-handling)
10. [Key Design Decisions](#key-design-decisions)

### Frontend
11. [Frontend Architecture Overview](#frontend-architecture-overview)
12. [AuthService (`auth.ts`)](#authservice-authts)
13. [LoginComponent (`login/login.ts`)](#logincomponent-loginlogints)
14. [SignupComponent (`signup/signup.ts`)](#signupcomponent-signupsignupts)
15. [Route Guards](#route-guards)
16. [JWT Interceptor (`jwt-interceptor.ts`)](#jwt-interceptor-jwt-interceptorts)
17. [Test Files](#test-files)

---

## Architecture Overview

The backend follows a standard layered Spring Boot architecture for a stock trading application. The authentication subsystem is built on **stateless JWT-based authentication** with a **dual-token strategy** (short-lived access tokens + long-lived refresh tokens) and a **token blacklist** for secure logout.

```
Client (Angular @ localhost:4200)
        │
        ▼
┌─────────────────────────────────────────┐
│              Controllers                │
│   AuthController    AdminController     │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│               Services                  │
│  AuthService  RefreshTokenService       │
│              TokenBlacklistService      │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│             Repositories                │
│  UserRepository  RefreshTokenRepository │
│           TokenBlacklistRepository      │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│          Database (JPA/Hibernate)       │
│   User   RefreshToken   TokenBlacklist  │
└─────────────────────────────────────────┘

Cross-cutting Security Layer:
  SecurityConfig → JwtAuthFilter → JwtService → UserDetailsServiceImpl
  CorsConfig → GlobalExceptionHandler
```

---

## Authentication Flow

### Signup
```
POST /api/auth/signup
  └─ Validate fields (bean validation)
  └─ Check passwords match
  └─ Check email/username uniqueness
  └─ Hash password (BCrypt)
  └─ Save User with role=USER
  └─ Generate JWT access token
  └─ Generate UUID refresh token (stored in DB)
  └─ Return AuthResponse {success, token, refreshToken, username, email, role}
```

### Login
```
POST /api/auth/login
  └─ Accept identifier (email OR username) + password
  └─ Load user by email, fallback to username
  └─ Validate password against BCrypt hash
  └─ Generate new JWT access token
  └─ Generate new refresh token (old one replaced)
  └─ Return AuthResponse
```

### Token Refresh
```
POST /api/auth/refresh
  └─ Look up refresh token in DB
  └─ Verify not expired and not revoked
  └─ Generate new JWT access token for that user
  └─ Return new access token in AuthResponse
```

### Logout
```
POST /api/auth/logout
  └─ Add current JWT access token to TokenBlacklist
  └─ Delete all refresh tokens for the user
  └─ JwtAuthFilter will reject future requests with blacklisted token
```

### Every Authenticated Request
```
HTTP Request
  └─ JwtAuthFilter intercepts
  └─ Extract Bearer token from Authorization header
  └─ Check TokenBlacklist → reject with 401 if found
  └─ Validate JWT signature & expiry via JwtService
  └─ Load UserDetails via UserDetailsServiceImpl
  └─ Set SecurityContextHolder → request proceeds
```

---

## Controllers

### `AuthController` — `/api/auth`

| Method | Endpoint    | Request Body    | Description                              |
|--------|-------------|-----------------|------------------------------------------|
| POST   | `/signup`   | `SignupRequest` | Register a new user with USER role       |
| POST   | `/login`    | `LoginRequest`  | Login with email/username + password     |
| POST   | `/refresh`  | `RefreshRequest`| Get a new access token via refresh token |
| POST   | `/logout`   | `LogoutRequest` | Invalidate tokens and log the user out   |

All endpoints delegate to `AuthService` and return `AuthResponse`. The controller layer is kept thin — no business logic lives here.

### `AdminController` — `/api/admin`

| Method | Endpoint     | Auth Required | Description                 |
|--------|--------------|---------------|-----------------------------|
| GET    | `/dashboard` | ADMIN role    | Returns admin welcome message|

This endpoint is secured at the `SecurityConfig` level, requiring the `ADMIN` role. It demonstrates role-based access control (RBAC) in the system.

---

## DTOs — Data Transfer Objects

All DTOs live in the `com.onestocks.app.dto` package.

### `SignupRequest`
Represents the user registration form payload.

| Field             | Type    | Constraint              |
|-------------------|---------|-------------------------|
| `username`        | String  | 3–50 characters, required|
| `email`           | String  | Valid email format       |
| `password`        | String  | Minimum 6 characters     |
| `confirmPassword` | String  | Must match `password`    |
| `rememberMe`      | boolean | Extends token expiry     |

### `LoginRequest`
Supports login via either email or username.

| Field        | Type    | Notes                          |
|--------------|---------|--------------------------------|
| `identifier` | String  | Accepts email OR username      |
| `password`   | String  | Raw password (BCrypt checked)  |
| `rememberMe` | boolean | Extends token expiry if `true` |

### `RefreshRequest`
Minimal payload for token refresh.

| Field          | Type   |
|----------------|--------|
| `refreshToken` | String |

### `LogoutRequest`
Carries both tokens to allow full invalidation.

| Field          | Type   |
|----------------|--------|
| `token`        | String | (JWT access token)   |
| `refreshToken` | String | (UUID refresh token) |

### `AuthResponse`
Unified response DTO returned by all auth endpoints.

| Field          | Type    | Notes                          |
|----------------|---------|--------------------------------|
| `success`      | boolean | `true` on success              |
| `message`      | String  | Human-readable status message  |
| `token`        | String  | JWT access token               |
| `refreshToken` | String  | UUID refresh token             |
| `username`     | String  | Authenticated user's username  |
| `email`        | String  | Authenticated user's email     |
| `role`         | String  | User's role (USER / ADMIN)     |

### `ErrorResponse`
Java record used by `GlobalExceptionHandler`.

| Field     | Type   |
|-----------|--------|
| `error`   | String |
| `message` | String |

---

## Models / Entities

All entities live in the `com.onestocks.app.model` package.

### `User`
The core user entity, also implementing Spring Security's `UserDetails` interface for seamless integration.

| Column       | Type        | Notes                                          |
|--------------|-------------|------------------------------------------------|
| `id`         | Long (PK)   | Auto-generated                                 |
| `username`   | String      | Unique                                         |
| `email`      | String      | Unique                                         |
| `password`   | String      | BCrypt-hashed                                  |
| `role`       | Role (enum) | `USER` or `ADMIN` (stored as string)           |
| `rememberMe` | boolean     | Influences refresh token expiration duration   |
| `createdAt`  | LocalDateTime| Auto-set via `@PrePersist`                    |

By implementing `UserDetails`, the `User` entity provides `getAuthorities()` (maps Role to `ROLE_USER` / `ROLE_ADMIN`), `isAccountNonExpired()`, `isEnabled()`, etc., all returning `true` to keep accounts active by default.

### `Role`
Simple two-value enum driving authorization decisions:
- `USER` — standard platform user
- `ADMIN` — elevated privileges (e.g., `/api/admin/**`)

### `RefreshToken`
Stores issued refresh tokens in the database, enabling controlled revocation.

| Column       | Type      | Notes                              |
|--------------|-----------|------------------------------------|
| `id`         | Long (PK) | Auto-generated                     |
| `token`      | String    | UUID, unique in DB                 |
| `expiryDate` | Instant   | Configurable via `app.jwt.refresh-expiration-ms` |
| `revoked`    | boolean   | Set to `true` on logout            |
| `user`       | User      | `@OneToOne` — one token per user   |

### `TokenBlacklist`
Stores invalidated JWT access tokens to prevent reuse after logout.

| Column          | Type          | Notes                             |
|-----------------|---------------|-----------------------------------|
| `id`            | Long (PK)     | Auto-generated                    |
| `token`         | String(512)   | Full JWT string, unique           |
| `blacklistedAt` | LocalDateTime | Timestamp of invalidation         |

The 512-character column length accommodates full JWT strings. The `@Column(unique=true)` constraint prevents duplicate blacklist entries.

---

## Repositories

All repositories extend `JpaRepository` and live in `com.onestocks.app.repository`.

### `UserRepository`

| Method                         | Description                            |
|--------------------------------|----------------------------------------|
| `findByEmail(String)`          | Used for email-based login             |
| `findByUsername(String)`       | Used for username-based login          |
| `existsByEmail(String)`        | Duplicate email check during signup    |
| `existsByUsername(String)`     | Duplicate username check during signup |

### `RefreshTokenRepository`

| Method                                    | Description                            |
|-------------------------------------------|----------------------------------------|
| `findByToken(String)`                     | Look up token during refresh flow      |
| `deleteByUser(User)` `@Transactional`     | Remove all tokens on logout            |
| `existsByToken(String)`                   | Existence check                        |

### `TokenBlacklistRepository`

| Method                  | Description                                  |
|-------------------------|----------------------------------------------|
| `existsByToken(String)` | Called by `JwtAuthFilter` on every request   |

---

## Security Layer

### `SecurityConfig`
The central Spring Security configuration (`@EnableWebSecurity`).

**Key decisions:**
- **Stateless sessions** — `SessionCreationPolicy.STATELESS` ensures no server-side HTTP session is maintained; every request must carry a valid JWT.
- **CSRF disabled** — Safe for stateless APIs consumed by an SPA; CSRF protection is browser-session-based and irrelevant here.
- **Public endpoints:** `POST /api/auth/**` is open without authentication.
- **Admin endpoints:** `GET /api/admin/**` requires `ADMIN` role.
- **All other endpoints** require any authenticated user.
- **JWT filter wired before** `UsernamePasswordAuthenticationFilter` via `addFilterBefore`.
- **BCrypt password encoder** registered as a bean and injected into `AuthenticationProvider`.

### `JwtAuthFilter` (extends `OncePerRequestFilter`)
Intercepts every incoming HTTP request exactly once.

**Processing pipeline:**
1. Extract `Authorization` header; skip filter chain if missing or not `Bearer`.
2. Parse the JWT to extract the username claim.
3. **Check `TokenBlacklistService`** — if the token is blacklisted, respond `401 Unauthorized` immediately and halt.
4. If `SecurityContext` has no existing authentication, validate the token via `JwtService`.
5. On valid token, create a `UsernamePasswordAuthenticationToken` with user authorities and set it in `SecurityContextHolder`.
6. Continue the filter chain.

### `JwtService`
Stateless utility class for JWT operations using the **JJWT library** with **HS256 (HMAC-SHA-256)** signing.

| Method                                        | Description                                          |
|-----------------------------------------------|------------------------------------------------------|
| `generateToken(UserDetails)`                  | Creates a token with default expiry                  |
| `generateToken(Map<String,Object>, UserDetails)` | Creates a token with extra claims (e.g., rememberMe) |
| `extractUsername(String)`                     | Reads `sub` claim from token                         |
| `isTokenValid(String, UserDetails)`            | Checks signature, expiry, and username match         |

The signing key is derived from a Base64-encoded secret stored in `application.properties` (`app.jwt.secret`). Token expiry is configurable via `app.jwt.expiration-ms`.

### `CorsConfig`
Configures Cross-Origin Resource Sharing to allow the Angular frontend at `http://localhost:4200` to communicate with the API.

- **Allowed origins:** `http://localhost:4200`
- **Allowed methods:** GET, POST, PUT, DELETE
- **Allowed headers:** All (`*`)
- Applied globally via `WebMvcConfigurer`.

### `UserDetailsServiceImpl`
Implements Spring's `UserDetailsService` interface.

- `loadUserByUsername(String email)` — loads a `User` entity by email from the database. The returned `User` implements `UserDetails`, so no wrapping is needed. Throws `UsernameNotFoundException` if not found.

---

## Services

### `AuthService`
The central business logic hub for all authentication operations.

#### `signup(SignupRequest)`
1. Validates that `password` equals `confirmPassword` (throws if not).
2. Checks that neither the email nor the username is already taken.
3. Encodes the password using `BCryptPasswordEncoder`.
4. Creates and persists a `User` with `role = USER`.
5. Calls `JwtService.generateToken()` for the access token.
6. Calls `RefreshTokenService.createRefreshToken()` for the refresh token.
7. Returns a fully populated `AuthResponse`.

#### `login(LoginRequest)`
1. Attempts to find the user by email; falls back to username lookup.
2. Verifies the raw password against the stored BCrypt hash using `PasswordEncoder.matches()`.
3. Generates a new JWT access token.
4. Creates a new refresh token (the old one is replaced in DB).
5. Returns `AuthResponse`.

#### `refresh(RefreshRequest)`
1. Retrieves the `RefreshToken` entity by token string.
2. Checks `isExpired()` — throws if the token is past its `expiryDate`.
3. Checks `isRevoked()` — throws if the token was invalidated.
4. Generates a fresh JWT access token for the associated user.
5. Returns `AuthResponse` with the new access token (refresh token stays the same).

#### `logout(LogoutRequest)`
1. Calls `TokenBlacklistService.blacklist(token)` to invalidate the JWT.
2. Calls `RefreshTokenService.revokeByUser(user)` to delete all refresh tokens for that user.
3. Returns a success `AuthResponse`.

### `RefreshTokenService`
Manages the full lifecycle of refresh tokens.

| Method                         | Description                                                      |
|--------------------------------|------------------------------------------------------------------|
| `createRefreshToken(User)`     | Deletes existing token for user, creates new UUID-based token    |
| `findByToken(String)`          | Returns `Optional<RefreshToken>` for lookup                      |
| `isExpired(RefreshToken)`      | Returns `true` if `expiryDate` is before `Instant.now()`         |
| `isRevoked(RefreshToken)`      | Returns `true` if `revoked` flag is set                          |
| `revokeByUser(User)`           | Deletes all refresh tokens for a user (used during logout)       |

Expiry duration is configurable via `app.jwt.refresh-expiration-ms` (injected via `@Value`).

### `TokenBlacklistService`
Lightweight service managing the JWT blacklist for secure logout.

| Method                    | Description                                                       |
|---------------------------|-------------------------------------------------------------------|
| `blacklist(String token)` | Persists the token to `TokenBlacklist` only if not already present |
| `isBlacklisted(String)`   | Queries `TokenBlacklistRepository.existsByToken()` — used in filter |

The idempotency guard in `blacklist()` prevents duplicate DB entries if logout is called multiple times.

---

## Exception Handling

### `GlobalExceptionHandler` (`@RestControllerAdvice`)

Provides centralized error handling across all controllers. Maps specific exceptions to HTTP responses:

| Exception                          | HTTP Status        | Response Format  |
|------------------------------------|--------------------|------------------|
| `ResourceNotFoundException`        | `404 Not Found`    | `AuthResponse`   |
| `InvalidTransactionException`      | `400 Bad Request`  | `AuthResponse`   |
| `InsufficientFundsException`       | `400 Bad Request`  | `AuthResponse`   |
| `MethodArgumentNotValidException`  | `400 Bad Request`  | `ErrorResponse`  |
| Generic `Exception`                | `500 Internal Server Error` | `AuthResponse` |

Bean validation errors (`@Valid` failures on request bodies) are captured here — the handler collects the first field error and returns it as an `ErrorResponse { error, message }`.

---

## Key Design Decisions

### 1. Dual-Token Strategy
Short-lived JWTs (stateless, fast to verify) are paired with long-lived refresh tokens (stateful, stored in DB). This gives a good balance of performance and security: access tokens expire quickly, while refresh tokens can be explicitly revoked on logout.

### 2. Token Blacklist for Logout
Since JWTs are stateless, the only way to truly invalidate one before expiry is to maintain a blacklist. Every request hits `TokenBlacklistService.isBlacklisted()` in the filter — this adds one DB read per request but guarantees that logged-out tokens cannot be reused.

### 3. `User` Implements `UserDetails`
Rather than wrapping `User` in a separate `UserDetails` adapter class, the `User` entity itself implements `UserDetails`. This eliminates a mapping layer and keeps the security integration direct.

### 4. Flexible Login Identifier
`LoginRequest` accepts a single `identifier` field that is tried against both email and username. This improves UX without requiring separate endpoints.

### 5. `rememberMe` Flag
Both `SignupRequest` and `LoginRequest` carry a `rememberMe` boolean. This is stored on the `User` entity and can be read by `JwtService` to generate longer-lived tokens, supporting persistent sessions without a separate mechanism.

### 6. Stateless Session Management
`SecurityConfig` enforces `SessionCreationPolicy.STATELESS`, meaning Spring Security never creates or uses an HTTP session. All auth state lives in the JWT and the DB tables (`RefreshToken`, `TokenBlacklist`).

### 7. One Refresh Token Per User
`RefreshTokenService.createRefreshToken()` always deletes the existing token before creating a new one (`deleteByUser` is `@Transactional`). This enforces single-device semantics — a new login invalidates the previous session's refresh token.

---

---

# Frontend — Authentication & Security Implementation

**Framework:** Angular (Standalone Components) · RxJS · Angular Signals  
**Location:** `frontEnd/app/src/app/`

---

## Frontend Architecture Overview

The Angular frontend mirrors the backend's dual-token strategy. Tokens are stored in `localStorage` and attached to every non-auth HTTP request via a functional interceptor. Route access is controlled by two functional guards. All auth state is centralised in a single injectable service.

```
User (Browser)
      │
      ▼
┌─────────────────────────────────────────┐
│           Angular Router                │
│  authGuard ──► /home, /dashboard, ...   │
│  adminGuard ──► /admin/**               │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│          Components                     │
│   LoginComponent    SignupComponent     │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│           AuthService                   │
│  signup / login / refresh / logout      │
│  saveSession / clearSession             │
│  getToken / isLoggedIn / isAdmin        │
└──────────────┬──────────────────────────┘
               │ HttpClient (via jwtInterceptor)
┌──────────────▼──────────────────────────┐
│    Spring Boot API  (localhost:8082)    │
│    /api/auth/**                         │
└─────────────────────────────────────────┘

localStorage keys: token · refreshToken · username · email · role
```

---

## AuthService (`auth.ts`)

**Path:** `frontEnd/app/src/app/auth.ts`  
**Provider:** `providedIn: 'root'` — single shared instance across the app.

### Interfaces exported from this file

#### `SignupRequest`
| Field             | Type    |
|-------------------|---------|
| `username`        | string  |
| `email`           | string  |
| `password`        | string  |
| `confirmPassword` | string  |
| `rememberMe`      | boolean |

#### `LoginRequest`
| Field        | Type    |
|--------------|---------|
| `identifier` | string  | Accepts email **or** username |
| `password`   | string  |
| `rememberMe` | boolean |

#### `AuthResponse`
| Field          | Type    |
|----------------|---------|
| `success`      | boolean |
| `message`      | string  |
| `token`        | string  | JWT access token |
| `refreshToken` | string  | UUID refresh token |
| `username`     | string  |
| `email`        | string  |
| `role`         | string  | `"USER"` or `"ADMIN"` |

### Methods

| Method                              | Returns                  | Description |
|-------------------------------------|--------------------------|-------------|
| `signup(data: SignupRequest)`       | `Observable<AuthResponse>` | POST `/api/auth/signup`; calls `saveSession` on success |
| `login(data: LoginRequest)`         | `Observable<AuthResponse>` | POST `/api/auth/login`; calls `saveSession` on success |
| `refresh()`                         | `Observable<AuthResponse>` | POST `/api/auth/refresh` with stored refresh token; updates `token` in localStorage on success |
| `logout()`                          | `Observable<AuthResponse>` | POST `/api/auth/logout` with both tokens; calls `clearSession` regardless of response |
| `clearSession()`                    | `void`                   | Removes all five auth keys from `localStorage` |
| `getToken()`                        | `string \| null`         | Reads `localStorage.token` |
| `getRefreshToken()`                 | `string \| null`         | Reads `localStorage.refreshToken` |
| `getUsername()`                     | `string \| null`         | Reads `localStorage.username` |
| `getRole()`                         | `string \| null`         | Reads `localStorage.role` |
| `isLoggedIn()`                      | `boolean`                | `true` when a token exists in localStorage |
| `isAdmin()`                         | `boolean`                | `true` when role is exactly `"ADMIN"` |

### Session persistence strategy

`saveSession()` is a private helper called via RxJS `tap` inside `signup` and `login`. It writes all five keys atomically from the `AuthResponse`. On `refresh`, only `token` is overwritten (the refresh token and user info remain unchanged).

---

## LoginComponent (`login/login.ts`)

**Path:** `frontEnd/app/src/app/login/login.ts`  
**Selector:** `app-login`  
**Type:** Standalone component  
**Imports:** `CommonModule`, `FormsModule`, `RouterLink`

### State (Angular Signals)

| Signal           | Type      | Purpose |
|------------------|-----------|---------|
| `errorMessage`   | `signal('')` | Displays validation or API errors inline |
| `successMessage` | `signal('')` | Displays welcome message before redirect |
| `isLoading`      | `signal(false)` | Disables the submit button during the HTTP call |

### Template-bound properties (two-way via `ngModel`)

| Property     | Type    |
|--------------|---------|
| `identifier` | string  | Email or username |
| `password`   | string  |
| `rememberMe` | boolean |

### `onSubmit()` flow

```
onSubmit()
  ├─ Clear both signals
  ├─ Guard: both identifier and password required → set errorMessage, return
  ├─ isLoading = true
  └─ authService.login({ identifier, password, rememberMe }).subscribe
        ├─ next (success=true)  → isLoading=false, successMessage set,
        │                          navigate to /home after 1 500 ms
        ├─ next (success=false) → isLoading=false, errorMessage = response.message
        └─ error                → isLoading=false, errorMessage = err.error?.message
```

---

## SignupComponent (`signup/signup.ts`)

**Path:** `frontEnd/app/src/app/signup/signup.ts`  
**Selector:** `app-signup`  
**Type:** Standalone component  
**Imports:** `CommonModule`, `FormsModule`, `RouterLink`

### State (Angular Signals)

| Signal           | Type      | Purpose |
|------------------|-----------|---------|
| `errorMessage`   | `signal('')` | Validation / API error display |
| `successMessage` | `signal('')` | Confirmation message before redirect |
| `isLoading`      | `signal(false)` | Disables submit during HTTP call |

### Template-bound properties

| Property          | Type    |
|-------------------|---------|
| `username`        | string  |
| `email`           | string  |
| `password`        | string  |
| `confirmPassword` | string  |
| `rememberMe`      | boolean |

### `onSubmit()` flow

```
onSubmit()
  ├─ Clear both signals
  ├─ Guard: all four text fields required → errorMessage, return
  ├─ Guard: password !== confirmPassword  → errorMessage, return
  ├─ isLoading = true
  └─ authService.signup({ username, email, password, confirmPassword, rememberMe }).subscribe
        ├─ next (success=true)  → isLoading=false, successMessage set,
        │                          navigate to /login after 1 500 ms
        ├─ next (success=false) → isLoading=false, errorMessage = response.message
        └─ error                → isLoading=false, errorMessage = err.error?.message
```

---

## Route Guards

Both guards are **functional** (`CanActivateFn`) and use Angular's `inject()` pattern, compatible with Angular's standalone / provideRouter API.

### `authGuard` — `auth-guard.ts`

Protects any route that requires an authenticated user.

```
authGuard()
  ├─ isLoggedIn() === true  → return true (allow navigation)
  └─ else                   → router.navigate(['/login']), return false
```

**Typical usage:**
```typescript
{ path: 'home', component: HomeComponent, canActivate: [authGuard] }
```

### `adminGuard` — `admin-guard.ts`

Protects admin-only routes. Has two distinct redirect paths to avoid leaking the existence of admin pages to regular users.

```
adminGuard()
  ├─ isLoggedIn() && isAdmin()  → return true
  ├─ !isLoggedIn()              → router.navigate(['/login']), return false
  └─ isLoggedIn() && !isAdmin() → router.navigate(['/home']),  return false
```

**Typical usage:**
```typescript
{ path: 'admin', component: AdminComponent, canActivate: [adminGuard] }
```

---

## JWT Interceptor (`jwt-interceptor.ts`)

**Path:** `frontEnd/app/src/app/jwt-interceptor.ts`  
**Type:** Functional `HttpInterceptorFn`  
**Registration:** Added to `provideHttpClient(withInterceptors([jwtInterceptor]))` in `app.config.ts`.

### Processing pipeline

```
Every outgoing HTTP request
  ├─ URL contains /api/auth/  → pass through unchanged (login/signup/refresh don't need a token)
  ├─ token exists             → clone request, set Authorization: Bearer <token>
  └─ forward cloned request
        └─ on 401 error AND refreshToken present
              └─ authService.refresh()
                    ├─ success → retry original request with new token (via switchMap)
                    └─ failure → clearSession(), navigate(['/login']), rethrow error
```

### Key implementation detail

The interceptor uses `switchMap` to seamlessly retry the original request after a successful token refresh — the calling component never knows a refresh occurred. The inner `catchError` on the refresh call itself ensures that a broken refresh token (expired, revoked) always forces logout rather than leaving the app in a partially-authenticated state.

### Why `/api/auth/` endpoints are excluded

Adding a `Bearer` token to login/signup/refresh calls is harmless but unnecessary. More importantly, if the interceptor tried to refresh on a 401 from `/api/auth/refresh` itself, it would enter an infinite loop. The URL check prevents this.

---

## Test Files

All test files are scaffolded by the Angular CLI and currently contain the default creation smoke test. They verify that the injectable/function/component can be instantiated inside a `TestBed` context.

| File | Tests |
|------|-------|
| `auth.spec.ts` | `AuthService` — `should be created` |
| `auth-guard.spec.ts` | `authGuard` — `should be created` |
| `admin-guard.spec.ts` | `adminGuard` — `should be created` |
| `jwt-interceptor.spec.ts` | `jwtInterceptor` — `should be created` |
| `login/login.spec.ts` | `LoginComponent` — `should create` |
| `signup/signup.spec.ts` | `SignupComponent` — `should create` |

> **Note on `auth.spec.ts`:** The scaffolded file imports `Auth` (the class name the CLI generated) rather than `AuthService`. If the service was renamed after generation, this import will need updating before `ng test` passes.
