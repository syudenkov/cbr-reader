# Architecture

## System Overview

CBR Viewer is a monolithic web application that bundles a React SPA inside a Spring Boot backend. In production, a single JAR serves both the API and the frontend static assets.

```
┌─────────────────────────────────────────────────────────────┐
│                        Browser                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │           React SPA (MUI, Redux Toolkit)              │  │
│  │                                                       │  │
│  │  Pages: Login │ Library │ Viewer                      │  │
│  │  State: auth │ files │ viewer │ tts │ admin │ ui      │  │
│  └────────────────────────┬──────────────────────────────┘  │
│                           │ /api/* (Axios, session cookie)  │
└───────────────────────────┼─────────────────────────────────┘
                            │
┌───────────────────────────┼─────────────────────────────────┐
│                   Spring Boot (port 4433)                    │
│                           │                                  │
│  ┌────────────────────────▼──────────────────────────────┐  │
│  │              REST Controllers (7)                      │  │
│  │  Auth │ Files │ Pages │ Progress │ Ratings │ TTS │Admin│  │
│  └────────────────────────┬──────────────────────────────┘  │
│                           │                                  │
│  ┌────────────────────────▼──────────────────────────────┐  │
│  │              Services (13)                             │  │
│  │  Archive │ OCR │ TTS │ Voice │ Rating │ Progress │ ...│  │
│  └──────┬─────────────────┬──────────────────┬───────────┘  │
│         │                 │                  │               │
│  ┌──────▼──────┐  ┌──────▼──────┐  ┌───────▼────────────┐  │
│  │   SQLite    │  │  File I/O   │  │  External APIs     │  │
│  │  (JPA/      │  │  (comics,   │  │  (OpenAI, Claude,  │  │
│  │  Hibernate) │  │  cache,     │  │   Minimax)         │  │
│  │             │  │  audio)     │  │                    │  │
│  └─────────────┘  └─────────────┘  └────────────────────┘  │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

## Deployment Model

The project uses a **monolith-in-a-JAR** deployment strategy. Gradle orchestrates the full build:

```
┌──────────────────────────────────────────────────┐
│                 ./gradlew build                   │
│                                                   │
│  1. npmInstall      → npm install (frontend/)     │
│  2. buildFrontend   → npm run build (vite)        │
│  3. copyFrontend    → dist/ → resources/static/   │
│  4. compileJava     → compile backend sources      │
│  5. processResources→ bundle static + config       │
│  6. bootJar         → fat JAR with everything      │
└──────────────────────────────────────────────────┘
```

The resulting JAR contains the compiled Java classes, all Spring Boot dependencies, and the complete frontend build under `static/`. A `SpaController` forwards non-API, non-static requests to `index.html` so client-side routing works.

### Development Mode

In development, frontend and backend run separately:

- **Frontend** — Vite dev server on port 3344 with hot module replacement. API calls to `/api/*` are proxied to the backend.
- **Backend** — Spring Boot on port 4433, CORS configured to accept requests from `http://localhost:3344`.

## Authentication Flow

The application uses **session-based authentication** with HTTP-only cookies. There are no JWTs.

```
┌──────────┐                    ┌──────────────┐              ┌──────────┐
│  Browser  │                    │  Spring Boot  │              │  SQLite  │
└─────┬─────┘                    └──────┬───────┘              └────┬─────┘
      │                                 │                           │
      │  POST /api/auth/login           │                           │
      │  {username, password}           │                           │
      ├────────────────────────────────►│                           │
      │                                 │  SELECT * FROM users      │
      │                                 │  WHERE username = ?       │
      │                                 ├──────────────────────────►│
      │                                 │◄─────── User row ────────┤
      │                                 │                           │
      │                                 │  BCrypt.matches(password, │
      │                                 │    passwordHash)          │
      │                                 │                           │
      │  200 OK + Set-Cookie: SESSION   │                           │
      │  {id, username, email, role}    │                           │
      │◄────────────────────────────────┤                           │
      │                                 │                           │
      │  GET /api/auth/me               │                           │
      │  Cookie: SESSION=abc123         │                           │
      ├────────────────────────────────►│                           │
      │                                 │  Read HttpSession         │
      │                                 │  → userId, role           │
      │  200 OK {user object}           │                           │
      │◄────────────────────────────────┤                           │
      │                                 │                           │
      │  POST /api/auth/logout          │                           │
      ├────────────────────────────────►│                           │
      │                                 │  session.invalidate()     │
      │  200 OK                         │                           │
      │◄────────────────────────────────┤                           │
```

**Session details:**
- Timeout: 24 hours
- Cookie: HttpOnly, SameSite=Lax, Secure=configurable
- A `SessionAuthenticationFilter` runs before each request, reads `userId` and `role` from the session, and populates Spring Security's `SecurityContext`.

**Endpoint security rules:**

| Access Level | Endpoints |
|-------------|-----------|
| Public | `/`, `/index.html`, `/assets/**`, `/api/auth/login`, `/api/auth/register` |
| Authenticated | `/api/files/**`, `/api/ratings/**`, `/api/progress/**`, `/api/tts/**` |
| Admin only | `/api/admin/**` |
| Actuator | `/actuator/health`, `/actuator/info` |

## Database Schema

The application uses SQLite with 10 entities managed by Hibernate/JPA. All tables use auto-generated Long primary keys.

```
┌──────────────────┐       ┌──────────────────────┐
│      users       │       │     comic_files      │
├──────────────────┤       ├──────────────────────┤
│ id          (PK) │       │ id            (PK)   │
│ username  (UQ)   │◄──┐   │ filename             │
│ email     (UQ)   │   │   │ originalFilename     │
│ passwordHash     │   │   │ filePath             │
│ role             │   │   │ fileType (CBR|CBZ)   │
│ createdAt        │   │   │ fileSize             │
│ updatedAt        │   │   │ pageCount            │
│ deletedAt (soft) │   │   │ coverImagePath       │
└──────┬───────────┘   │   │ uploadedBy     (FK)──┼──► users.id
       │               │   │ createdAt            │
       │               │   │ updatedAt            │
       │               │   └──────────┬───────────┘
       │               │              │
       │   ┌───────────┼──────────────┼──────────────┐
       │   │           │              │              │
       ▼   ▼           │              ▼              ▼
┌──────────────────┐   │   ┌──────────────────┐  ┌──────────────────┐
│  reading_progress│   │   │     ratings      │  │    tts_jobs      │
├──────────────────┤   │   ├──────────────────┤  ├──────────────────┤
│ id          (PK) │   │   │ id          (PK) │  │ id          (PK) │
│ userId      (FK)─┼───┘   │ userId      (FK)─┼──┘ fileId      (FK)─┤
│ fileId      (FK)─┼──►    │ fileId      (FK)─┼──► │ requestedBy(FK)│
│ currentPage      │       │ rating (1-5)     │    │ status         │
│ updatedAt        │       │ createdAt        │    │ progress (0-100)│
└──────────────────┘       └──────────────────┘    │ currentPage    │
  UQ(userId,fileId)          UQ(userId,fileId)     │ errorMessage   │
                                                   │ createdAt      │
                                                   │ updatedAt      │
                                                   └───────┬────────┘
                                                           │
                                                           ▼
                                                   ┌──────────────────┐
                                                   │   tts_results    │
                                                   ├──────────────────┤
                                                   │ id          (PK) │
                                                   │ fileId      (FK) │
                                                   │ pageNumber       │
                                                   │ ocrText    (TEXT)│
                                                   │ speakersJson(TEXT│
                                                   │ audioFilePath    │
                                                   │ createdAt        │
                                                   └──────────────────┘

┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│    llm_config    │  │   audit_logs     │  │   system_logs    │
├──────────────────┤  ├──────────────────┤  ├──────────────────┤
│ id          (PK) │  │ id          (PK) │  │ id          (PK) │
│ provider         │  │ userId      (FK) │  │ level            │
│ apiKeyEncrypted  │  │ action           │  │ message    (TEXT)│
│ modelName        │  │ entityType       │  │ stackTrace (TEXT)│
│ isActive         │  │ entityId         │  │ createdAt        │
│ priority         │  │ details    (TEXT)│  └──────────────────┘
│ createdAt        │  │ ipAddress        │
│ updatedAt        │  │ createdAt        │  ┌──────────────────┐
└──────────────────┘  └──────────────────┘  │  feature_flags   │
                                            ├──────────────────┤
                                            │ id          (PK) │
                                            │ (schema exists,  │
                                            │  minimal usage)  │
                                            └──────────────────┘
```

### Entity Relationships

- **User → ComicFile**: One user uploads many comics (`uploadedBy` foreign key).
- **User + ComicFile → ReadingProgress**: Each user has at most one progress record per comic (unique constraint on `userId, fileId`).
- **User + ComicFile → Rating**: Each user can rate each comic once (unique constraint on `userId, fileId`). Ratings are 1–5.
- **ComicFile → TtsJob**: A comic can have multiple TTS jobs over time, but only one active (PENDING/PROCESSING) at a time.
- **ComicFile → TtsResult**: One result per page per file, containing OCR text, speaker segments as JSON, and a path to the generated MP3.
- **LlmConfig**: Standalone configuration per AI provider (openai, claude, minimax). API keys are AES-256-GCM encrypted.
- **AuditLog**: Records admin actions (user CRUD, LLM config changes) with the acting user's ID.
- **SystemLog**: Records system-level events (API call failures, encryption errors) with level, message, and optional stack trace.

## Storage Layout

All runtime files live under `backend/storage/` (configurable via environment variables):

```
storage/
├── comics/                     Uploaded archives
│   └── {userId}/
│       └── {uuid}.cbr|cbz     UUID-named to avoid collisions
│
├── cache/                      Extracted page images (LRU)
│   └── {fileId}/
│       └── {pageNumber}.jpg    Cached on first access
│
└── audio/                      Generated TTS audio
    └── {jobId}/
        └── page_{pageNum}.mp3  One MP3 per page
```

**Cache management:**
- Maximum size: 100 MB (configurable)
- TTL: 20 days
- Cleanup: Daily cron at 2:00 AM, evicts by last-modified time

## Security Model

### Password Hashing

User passwords are hashed with **BCrypt** (Spring Security's default encoder). Plain-text passwords are never stored or logged.

### API Key Encryption

LLM provider API keys (OpenAI, Claude, Minimax) are encrypted at rest using **AES-256-GCM**:

- The master key is provided via the `ENCRYPTION_MASTER_KEY` environment variable (64 hex characters = 32 bytes).
- Each encrypted value uses a unique random IV (initialization vector).
- Keys are decrypted only at the moment of use (API calls to external providers).
- On retrieval through the admin API, keys are masked (prefix + `****` + last 4 characters).

### Role-Based Access Control

Two roles exist: `USER` and `ADMIN`.

| Capability | USER | ADMIN |
|-----------|------|-------|
| Login/logout | Yes | Yes |
| Browse library | Yes | Yes |
| Upload/delete own comics | Yes | Yes |
| Read comics | Yes | Yes |
| Rate comics | Yes | Yes |
| Track reading progress | Yes | Yes |
| Request TTS generation | Yes | Yes |
| Manage users | No | Yes |
| Configure LLM providers | No | Yes |
| View audit logs | No | Yes |

### Additional Security Measures

- **CSRF disabled** — Standard for SPA + session cookie architectures where SameSite=Lax provides equivalent protection.
- **CORS** — Restricted to configured origins (default: `http://localhost:3344`).
- **Session timeout** — 24 hours of inactivity.
- **Soft delete** — Users are marked with `deletedAt` rather than physically removed, preserving audit trail integrity.
- **Audit logging** — All admin actions (user creation, role changes, LLM config updates) are logged with the acting admin's ID.

## API Overview

### AuthController — `/api/auth`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/login` | Authenticate and create session |
| POST | `/api/auth/logout` | Invalidate session |
| POST | `/api/auth/register` | Create new user account |
| GET | `/api/auth/me` | Get current authenticated user |

### FileController — `/api/files`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/files` | List comics (paginated, sorted) |
| GET | `/api/files/{id}` | Get comic metadata |
| POST | `/api/files` | Upload CBR/CBZ file (multipart, max 500 MB) |
| DELETE | `/api/files/{id}` | Delete comic and associated data |
| GET | `/api/files/{id}/page/{num}` | Get page image (cached) |
| GET | `/api/files/{id}/cover` | Get cover image |

### ProgressController — `/api/progress`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/progress` | Get all progress for current user |
| GET | `/api/progress/{fileId}` | Get progress for specific comic |
| PUT | `/api/progress/{fileId}` | Update reading progress |

### RatingController — `/api/ratings`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/ratings/{fileId}` | Submit or update rating (1–5) |
| GET | `/api/ratings/{fileId}` | Get current user's rating |
| GET | `/api/ratings/{fileId}/average` | Get average rating and count |

### TtsController — `/api/tts`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/tts/jobs` | Create TTS job for a comic |
| GET | `/api/tts/jobs/{jobId}` | Get job status and progress |
| GET | `/api/tts/audio/{fileId}/{page}` | Stream page audio (MP3) |

### AdminController — `/api/admin`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/admin/users` | List users (paginated) |
| GET | `/api/admin/users/{id}` | Get user details |
| POST | `/api/admin/users` | Create user |
| PUT | `/api/admin/users/{id}` | Update user |
| DELETE | `/api/admin/users/{id}` | Soft-delete user |
| PUT | `/api/admin/users/{id}/role` | Assign role |
| GET | `/api/admin/users/{id}/activity` | Get user's audit log |
| GET | `/api/admin/llm-configs` | List LLM configurations |
| GET | `/api/admin/llm-configs/{id}` | Get LLM config (masked key) |
| PUT | `/api/admin/llm-configs/{id}` | Update LLM config |
| POST | `/api/admin/llm-configs/{id}/test` | Test provider connection |
