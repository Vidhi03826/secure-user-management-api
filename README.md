# Secure User Management API

A security-focused REST API built with Java 22 and Spring Boot 4, designed to demonstrate production-oriented backend engineering beyond basic CRUD.

The system implements stateless JWT authentication, refresh-token lifecycle management, role-based authorization, BCrypt password hashing, account lockout protection, validation, centralized exception handling, OpenAPI documentation, CORS configuration, automated testing, and environment-based configuration.

**Repository:** https://github.com/Vidhi03826/secure-user-management-api

---

## Why This Project

This project was built to understand the engineering behind a secure backend service:

- How authentication and authorization are separated
- How JWT-based stateless security works end to end
- How roles are mapped to API access
- How refresh tokens are validated, revoked, and rotated
- How passwords are protected with BCrypt
- How failed authentication attempts can trigger account lockout
- How APIs are validated and errors are handled consistently
- How business logic is tested independently from infrastructure
- How the real Spring Security pipeline is integration-tested
- How configuration is externalized for different environments

The goal is not simply to expose CRUD endpoints, but to build a backend with clear boundaries, security controls, testability, and deployment readiness.

---

## Key Features

### Authentication

- User registration
- Secure login
- JWT access tokens
- Refresh tokens
- Refresh-token rotation
- Refresh-token revocation on logout
- Stateless authentication

### Authorization

- Role-Based Access Control (RBAC)
- `USER` and `ADMIN` roles
- Protected user endpoints
- Admin-only user management endpoints
- Custom `401 Unauthorized` handling
- Custom `403 Forbidden` handling

### Account Security

- BCrypt password hashing
- Failed login attempt tracking
- Account lockout after 5 failed attempts
- Admin unlock operation
- Secure password change
- Duplicate-email protection

### API Engineering

- Layered architecture
- DTO-based request/response design
- Bean Validation
- Global exception handling
- Consistent error responses
- CORS configuration
- Environment-based configuration
- Swagger / OpenAPI documentation

### Testing

- JWT service unit tests
- Authentication service unit tests
- User service unit tests
- Controller tests
- Spring Security authorization tests
- End-to-end JWT/RBAC integration tests

### Deployment Readiness

- Maven Wrapper
- Dockerfile
- Docker Compose configuration
- Environment-variable driven configuration

---

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 22 | Backend development |
| Spring Boot 4.1.1 | Application framework |
| Spring Security 7.1.1 | Authentication and authorization |
| JJWT 0.13.0 | JWT creation and validation |
| Spring Data JPA | Data access |
| Hibernate 7.4.5 | ORM |
| MySQL 8.4 | Relational database |
| BCrypt | Password hashing |
| Spring Validation | Request validation |
| Swagger / OpenAPI | API documentation |
| JUnit | Automated testing |
| Mockito | Unit-test mocking |
| Docker / Docker Compose | Containerization |

---

## Architecture

```text
                              Client
                                |
                                v
                         +----------------+
                         | REST API       |
                         | Controllers    |
                         +--------+-------+
                                  |
                                  v
                    +-----------------------------+
                    | Spring Security             |
                    |                             |
                    |  JWT Authentication Filter  |
                    |  Authentication              |
                    |  Authorization / RBAC        |
                    +-------------+---------------+
                                  |
                                  v
                         +----------------+
                         | Service Layer  |
                         | Business Logic |
                         +--------+-------+
                                  |
                                  v
                         +----------------+
                         | Repository     |
                         | JPA / Hibernate|
                         +--------+-------+
                                  |
                                  v
                             +---------+
                             |  MySQL   |
                             +---------+
```

### Layer Responsibilities

**Controller layer**

Handles HTTP requests, request validation, endpoint mapping, and API responses.

**Security layer**

Authenticates requests, establishes the security context, and enforces authorization rules.

**Service layer**

Contains business rules such as registration, login, role management, password changes, account lockout, and token lifecycle operations.

**Repository layer**

Provides persistence through Spring Data JPA.

**Database layer**

Stores users, roles, user-role relationships, and refresh-token state.

---

## Authentication Architecture

### Registration Flow

```text
POST /api/auth/register
        |
        v
Validate request
        |
        v
Check email uniqueness
        |
        v
BCrypt password hashing
        |
        v
Assign USER role
        |
        v
Persist User
        |
        v
Return UserResponse
```

The API does not expose the user's password in its response DTO.

### Login Flow

```text
POST /api/auth/login
        |
        v
Check account status
        |
        v
AuthenticationManager
        |
        v
CustomUserDetailsService
        |
        v
Verify BCrypt password
        |
        +--------------------------+
        |                          |
        v                          v
Access JWT                 Refresh Token
short-lived                longer-lived
        |                          |
        +------------+-------------+
                     |
                     v
               TokenResponse
```

### Protected Request Flow

```text
Authorization: Bearer <JWT>
              |
              v
      JwtAuthenticationFilter
              |
              v
      Extract username from JWT
              |
              v
     Load UserDetails from DB
              |
              v
        Validate JWT
              |
              v
      Create Authentication
              |
              v
       SecurityContext
              |
              v
     Authorization / RBAC
              |
              v
          Controller
```

---

## Refresh Token Lifecycle

Refresh tokens are stored server-side with an expiry and revocation state.

```text
Client
  |
  | refresh token
  v
POST /api/auth/refresh
  |
  v
Verify token
  |
  +--> Invalid  -> 401
  |
  +--> Revoked  -> 401
  |
  +--> Expired  -> 401
  |
  v
Generate new access token
  |
  v
Revoke old refresh token
  |
  v
Create replacement refresh token
  |
  v
Return new token pair
```

A successful refresh rotates the refresh token so the previously used token is revoked.

Logout revokes the supplied refresh token.

---

## Authorization and RBAC

**RBAC (Role-Based Access Control)** means API access is determined by the authenticated user's assigned role.

The current roles are:

```text
USER
ADMIN
```

### USER permissions

Authenticated users can manage their own profile:

```text
GET  /api/users/me
PUT  /api/users/me
PUT  /api/users/me/password
```

### ADMIN permissions

Administrators can manage users:

```text
GET    /api/users
GET    /api/users/{id}
DELETE /api/users/{id}
PUT    /api/users/{id}/role
PUT    /api/users/{id}/unlock
```

### Authentication vs Authorization

**Authentication** answers:

> Who are you?

**Authorization** answers:

> Are you allowed to perform this action?

Examples:

```text
No credentials
    -> 401 Unauthorized

Authenticated USER
    -> /api/users/me
    -> 200 OK

Authenticated USER
    -> /api/users
    -> 403 Forbidden

Authenticated ADMIN
    -> /api/users
    -> 200 OK
```

---

## Account Lockout

The application tracks failed login attempts per user.

```text
Failure #1  -> 1 attempt
Failure #2  -> 2 attempts
Failure #3  -> 3 attempts
Failure #4  -> 4 attempts
Failure #5  -> account locked
```

After the fifth failed attempt:

```text
accountLocked = true
```

A locked account cannot authenticate until it is unlocked by an administrator.

Unlocking resets both:

```text
failedLoginAttempts = 0
accountLocked = false
```

---

## Security Model

### Password Security

Passwords are hashed with BCrypt before persistence.

```text
Plain Password
      |
      v
    BCrypt
      |
      v
Password Hash
      |
      v
    MySQL
```

### Stateless Authentication

The application uses stateless security rather than server-side login sessions.

```text
SessionCreationPolicy.STATELESS
```

Authentication is established from the JWT on each protected request.

### Security Exception Handling

The application distinguishes between authentication and authorization failures:

```text
401 Unauthorized
```

means the request is not authenticated.

```text
403 Forbidden
```

means the request is authenticated but does not have sufficient privileges.

---

## API Reference

### Authentication

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register a new user |
| POST | `/api/auth/login` | Public | Authenticate and receive tokens |
| POST | `/api/auth/refresh` | Public | Rotate refresh token and issue a new access token |
| POST | `/api/auth/logout` | Public | Revoke refresh token |

### User Profile

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| GET | `/api/users/me` | Authenticated | Get current user's profile |
| PUT | `/api/users/me` | Authenticated | Update current user's profile |
| PUT | `/api/users/me/password` | Authenticated | Change current user's password |

### Administration

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| GET | `/api/users` | ADMIN | List users |
| GET | `/api/users/{id}` | ADMIN | Get a user by ID |
| DELETE | `/api/users/{id}` | ADMIN | Delete a user |
| PUT | `/api/users/{id}/role` | ADMIN | Change a user's role |
| PUT | `/api/users/{id}/unlock` | ADMIN | Unlock a locked account |

---

## API Security Matrix

| Endpoint | Public | USER | ADMIN |
|---|:---:|:---:|:---:|
| `/api/auth/register` | ✅ | ✅ | ✅ |
| `/api/auth/login` | ✅ | ✅ | ✅ |
| `/api/auth/refresh` | ✅ | ✅ | ✅ |
| `/api/auth/logout` | ✅ | ✅ | ✅ |
| `/api/users/me` | ❌ | ✅ | ✅ |
| `/api/users/me/password` | ❌ | ✅ | ✅ |
| `/api/users` | ❌ | ❌ | ✅ |
| `/api/users/{id}` | ❌ | ❌ | ✅ |
| `/api/users/{id}/role` | ❌ | ❌ | ✅ |
| `/api/users/{id}/unlock` | ❌ | ❌ | ✅ |
| `/api/users/{id}` DELETE | ❌ | ❌ | ✅ |

---

## Database Design

### Main Tables

```text
users
roles
user_roles
refresh_tokens
```

### Relationships

```text
User
 |
 +---- many-to-many ----> Role
 |
 +---- one-to-many -----> RefreshToken
```

### Users

Stores:

- User ID
- Name
- Email
- BCrypt password hash
- Failed login attempts
- Account lock state

### Roles

Stores application roles such as:

```text
USER
ADMIN
```

### User Roles

Join table implementing the many-to-many User/Role relationship.

### Refresh Tokens

Stores:

- Refresh token value
- Associated user
- Expiration time
- Revocation state

---

## DTO Design

The API uses DTOs rather than exposing JPA entities directly.

Examples include:

```text
RegisterRequest
LoginRequest
RefreshTokenRequest
UpdateProfileRequest
ChangePasswordRequest
RoleUpdateRequest
TokenResponse
UserResponse
```

This provides a clean boundary between the API contract and persistence model.

For example, `UserResponse` exposes user information and roles without exposing the stored password.

---

## Validation and Error Handling

Incoming requests are validated using Jakarta Bean Validation.

Examples:

```text
@NotBlank
@Email
@Size
```

Application exceptions are centralized through a global exception handler.

The API provides structured errors containing information such as:

```text
status
message
path
timestamp
```

Typical HTTP responses include:

```text
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
```

---

## Testing Strategy

The project uses multiple testing layers instead of relying only on manual Postman testing.

### Unit Tests

**JwtServiceTest**

Covers JWT generation, username extraction, and token validation.

**AuthServiceTest — 8 tests**

Covers:

- Registration
- Duplicate-email protection
- Successful login
- Invalid credentials
- Failed-login tracking
- Account lockout
- Locked-account handling
- Refresh/logout behavior

**UserServiceTest — 15 tests**

Covers:

- Current-user retrieval
- User listing
- User lookup
- Profile updates
- Duplicate-email protection
- Password change
- Incorrect-password handling
- Role updates
- User deletion
- Account unlocking

### Controller Tests

Controller-level tests verify request mapping, response status, and JSON response behavior independently of the business layer.

### Security Tests

The security test suite verifies the authorization contract:

```text
No authentication  -> 401
USER -> own endpoint -> 200
USER -> admin endpoint -> 403
ADMIN -> admin endpoint -> 200
```

### Integration Tests

The integration suite starts the real Spring Boot application and verifies the actual JWT/RBAC pipeline:

```text
Register
   |
   v
Login
   |
   v
Real JWT
   |
   v
Bearer Authorization Header
   |
   v
JwtAuthenticationFilter
   |
   v
CustomUserDetailsService
   |
   v
SecurityContext
   |
   v
RBAC
   |
   v
Protected Endpoint
```

The real integration scenarios cover unauthenticated access, normal-user access, and administrator access.

---

## Swagger / OpenAPI

Interactive API documentation is available with Swagger UI.

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI specification:

```text
http://localhost:8080/v3/api-docs
```

Protected endpoints can be tested through Swagger using the **Authorize** button with:

```text
Bearer <access-token>
```

---

## CORS

CORS is explicitly configured for frontend integration.

Local frontend development uses:

```text
http://localhost:5173
```

The frontend origin can be configured using:

```text
FRONTEND_URL
```

This keeps environment-specific frontend configuration outside Java source code.

---

## Configuration and Secrets

Environment-specific values are externalized using environment variables.

The application supports:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
JWT_SECRET
FRONTEND_URL
```

A safe configuration template is provided in:

```text
.env.example
```

The actual `.env` file is excluded from version control.

**Never commit production database credentials, JWT secrets, or other sensitive values to Git.**

---

## Running Locally

### Prerequisites

- Java 22
- MySQL 8.4
- Git

### 1. Clone the repository

```bash
git clone https://github.com/Vidhi03826/secure-user-management-api.git
cd secure-user-management-api
```

### 2. Create the database

```sql
CREATE DATABASE secure_user_db;
```

### 3. Configure application properties

Use `.env.example` as the template for the required environment variables.

### 4. Run with Maven Wrapper

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

The API runs on:

```text
http://localhost:8080
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

---

## Building and Testing

Build the project without tests:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Run all tests:

```powershell
.\mvnw.cmd test
```

---

## Docker

The repository includes:

```text
Dockerfile
docker-compose.yml
```

The intended container architecture is:

```text
+--------------------------+
| Spring Boot API :8080    |
+------------+-------------+
             |
             v
+--------------------------+
| MySQL :3306              |
+--------------------------+
```

Build and start the stack with:

```bash
docker compose up --build
```

The Docker configuration uses environment variables so the application configuration can be adapted for different environments.

---

## Project Structure

```text
secure-user-management/
|
+-- src/
|   +-- main/
|   |   +-- java/com/vidhi/secureusermanagement/
|   |   |   +-- config/
|   |   |   +-- controller/
|   |   |   +-- dto/
|   |   |   +-- entity/
|   |   |   +-- exception/
|   |   |   +-- jwt/
|   |   |   +-- repository/
|   |   |   +-- security/
|   |   |   +-- service/
|   |   |
|   |   +-- resources/
|   |       +-- application.properties
|   |
|   +-- test/
|       +-- java/com/vidhi/secureusermanagement/
|
+-- .env.example
+-- .gitignore
+-- Dockerfile
+-- docker-compose.yml
+-- mvnw
+-- mvnw.cmd
+-- pom.xml
+-- README.md
```

---

## Engineering Decisions

### Layered Architecture

Controllers, services, repositories, and security responsibilities are separated to improve maintainability and testability.

### Stateless JWT Authentication

Authentication state is carried by signed JWTs instead of server-side HTTP sessions.

### Role-Based Authorization

Access policies are expressed in terms of application roles, keeping authorization rules explicit and centralized.

### DTO Boundaries

DTOs isolate the public API contract from persistence entities and help enforce validation and controlled data exposure.

### Centralized Exceptions

A global exception handler keeps API error responses consistent across controllers.

### Token Lifecycle Management

Access tokens are short-lived while refresh tokens provide a controlled mechanism for obtaining new access tokens, with revocation and rotation support.

### Environment-Based Configuration

Environment variables make the same application portable across local, test, and deployment environments.

---

## Security Considerations

Implemented:

- BCrypt password hashing
- Stateless JWT authentication
- Refresh-token expiry checks
- Refresh-token revocation
- Refresh-token rotation
- Role-based authorization
- Account lockout
- Admin unlock
- Validation
- Centralized exception handling
- Environment-based secret configuration
- Explicit CORS configuration

Potential next hardening steps include stronger refresh-token storage, rate limiting, OAuth2/OIDC, observability, and CI/CD security controls.

---

## Future Improvements

The project is intentionally structured so more advanced backend capabilities can be added without replacing its core architecture.

Planned directions include:

- Redis-based caching
- Kafka-based asynchronous event processing
- API rate limiting
- OAuth2 / OpenID Connect
- Advanced database indexing and query optimization
- Observability and centralized logging
- CI/CD pipeline
- Kubernetes deployment
- Distributed locking
- API versioning
- Pagination and advanced filtering
- Stronger refresh-token storage hardening

Redis and Kafka are intentionally kept for a separate high-scale backend project rather than being added here without a concrete architectural need.

---

## What This Project Demonstrates

This project demonstrates practical experience with:

```text
Java
Spring Boot
Spring Security
JWT
RBAC
BCrypt
Refresh Token Management
REST API Design
JPA / Hibernate
MySQL
DTOs
Validation
Exception Handling
Unit Testing
Integration Testing
Swagger / OpenAPI
CORS
Environment Configuration
Docker
Git / GitHub
```

More importantly, it demonstrates the ability to reason about **authentication, authorization, application boundaries, security controls, testing strategy, and deployment concerns as one backend system**.

---

## Author

### Vidhi Nema

GitHub: https://github.com/Vidhi03826

Repository: https://github.com/Vidhi03826/secure-user-management-api

---

## License

This project is intended as a learning and portfolio project. Add a license here if you choose to distribute it under a specific open-source license.
