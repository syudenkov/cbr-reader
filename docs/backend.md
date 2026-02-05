# Backend Architecture

## Application Structure

The backend is a Spring Boot 3.2 application running on Java 21 with an embedded Tomcat server on port 4433. It follows a layered architecture:

```
Controller → Service → Repository → SQLite
                 ↓
          External Clients (OpenAI, Claude, Minimax)
                 ↓
            File System (comics, cache, audio)
```

### Package Layout

```
com.cbrviewer/
├── controller/      REST endpoints (7 controllers)
├── service/         Business logic (13 services)
├── model/           JPA entities (10 tables)
├── repository/      Spring Data JPA interfaces
├── dto/             Request/response data transfer objects
├── config/          Spring configuration (security, async, CORS, etc.)
├── client/          External API clients (OpenAI, Claude, Minimax)
└── exception/       Custom exceptions + global handler
```

## Controller Layer

### AuthController — `/api/auth`

| Method | Path | Request Body | Response | Description |
|--------|------|-------------|----------|-------------|
| POST | `/login` | `{username, password}` | `{id, username, email, role}` | Authenticate and create session |
| POST | `/logout` | — | 200 OK | Invalidate session |
| POST | `/register` | `{username, email, password}` | `{id, username, email, role}` | Create new account (role defaults to USER) |
| GET | `/me` | — | `{id, username, email, role}` | Get current session user |

### FileController — `/api/files`

| Method | Path | Request | Response | Description |
|--------|------|---------|----------|-------------|
| GET | `/` | `?page=0&size=20&sort=createdAt,desc` | Paginated `{content, totalPages, ...}` | List all comics |
| GET | `/{id}` | — | ComicFile object | Get comic metadata |
| POST | `/` | `MultipartFile` (max 500 MB) | ComicFile object | Upload CBR/CBZ |
| DELETE | `/{id}` | — | 200 OK | Delete comic, cache, progress, ratings |
| GET | `/{id}/page/{pageNumber}` | — | Image bytes (`image/*`) | Get page (1-indexed, cached) |
| GET | `/{id}/cover` | — | Image bytes | Get cover image |

Page responses include `Cache-Control: max-age=3600` for browser caching.

### ProgressController — `/api/progress`

| Method | Path | Request Body | Response | Description |
|--------|------|-------------|----------|-------------|
| GET | `/` | — | `[{fileId, currentPage, updatedAt}]` | All progress for current user |
| GET | `/{fileId}` | — | `{fileId, currentPage, updatedAt}` | Progress for one comic |
| PUT | `/{fileId}` | `{currentPage}` | `{fileId, currentPage, updatedAt}` | Create or update progress |

### RatingController — `/api/ratings`

| Method | Path | Request Body | Response | Description |
|--------|------|-------------|----------|-------------|
| POST | `/{fileId}` | `{rating}` (1–5) | Rating object | Submit or update rating |
| GET | `/{fileId}` | — | Rating or null | Get current user's rating |
| GET | `/{fileId}/average` | — | `{fileId, averageRating, totalRatings}` | Average and count |

### TtsController — `/api/tts`

| Method | Path | Request Body | Response | Description |
|--------|------|-------------|----------|-------------|
| POST | `/jobs` | `{fileId}` | TtsJob DTO | Create TTS processing job |
| GET | `/jobs/{jobId}` | — | TtsJob DTO with progress | Get job status (poll every 2s) |
| GET | `/audio/{fileId}/{pageNumber}` | — | MP3 bytes (`audio/mpeg`) | Stream page audio |

Returns 409 Conflict if an active job (PENDING or PROCESSING) already exists for the file.

### AdminController — `/api/admin` (ADMIN role required)

| Method | Path | Request Body | Response | Description |
|--------|------|-------------|----------|-------------|
| GET | `/users` | `?page=0&size=20` | Paginated user list | List active users |
| GET | `/users/{id}` | — | User object | Get user details |
| POST | `/users` | `{username, email, password, role}` | User object | Create user |
| PUT | `/users/{id}` | `{username?, email?, password?, role?}` | User object | Update user fields |
| DELETE | `/users/{id}` | — | 200 OK | Soft-delete (sets deletedAt) |
| PUT | `/users/{id}/role` | `{role}` | User object | Assign role with safeguards |
| GET | `/users/{id}/activity` | — | Audit log entries | User's admin action history |
| GET | `/llm-configs` | — | `[{provider, modelName, isActive, ...}]` | List LLM configs (keys masked) |
| GET | `/llm-configs/{id}` | — | LLM config (key masked) | Get single config |
| PUT | `/llm-configs/{id}` | `{apiKey?, modelName?, isActive?}` | LLM config | Update config |
| POST | `/llm-configs/{id}/test` | — | `{success, responseTimeMs, message}` | Test provider connection |

**Role assignment safeguards:**
- Cannot demote yourself
- Cannot remove the last ADMIN

## Service Layer

### AuthService

Handles user registration and login. Passwords are hashed with BCrypt. On login, user ID and role are stored in the HTTP session.

### ComicFileService

Manages comic lifecycle: upload validation, UUID-based storage, page count detection, and deletion (removes file from disk, cache entries, progress records, and ratings).

**Upload validation chain:**
1. File must not be null or empty
2. File size must not exceed 500 MB
3. Filename must be safe (no path traversal)
4. File type determined by magic bytes (ZIP for CBZ, RAR for CBR)
5. Archive integrity check (can open, not corrupt, contains images)

### ArchiveService

Extracts pages from CBR/CBZ archives with a caching layer.

**Cache behavior:**
1. Check cache directory for any image extension (.jpg, .jpeg, .png, .gif, .webp, .bmp)
2. On cache hit: touch the file (update last-modified) and return bytes
3. On cache miss: extract from archive, write to cache, return bytes

Pages within an archive are sorted alphabetically (comics typically name files `001.jpg`, `002.jpg`, etc.) and accessed by index. Page numbers in the API are 1-indexed.

### OcrService

Orchestrates OCR text extraction from comic page images using a cascade pattern:

1. Try **OpenAI Vision** (GPT-4o) with 3 retries and exponential backoff
2. On failure, fall back to **Claude Vision** (Claude 3.5 Sonnet) with 3 retries
3. On both failures, return a failed result (the TTS job continues to the next page)

The OCR prompt asks the model to extract dialogue as a JSON array of `{speaker, text}` objects.

**Voice characteristic inference** (heuristic-based):
- **Gender**: Pattern matching on character names (e.g., "Batman" → MALE, "Wonder Woman" → FEMALE)
- **Age**: Keyword detection (kid/teen → YOUNG, old/elder → ELDERLY, default → ADULT)
- **Emotion**: Punctuation and keyword analysis (`!!` → ANGRY, `?` → CURIOUS, `...` → SAD)

### TtsService

Manages TTS job lifecycle. Prevents duplicate concurrent jobs on the same file. Submits work to the async processor and provides status polling.

### TtsJobProcessor

Runs asynchronously on a single-threaded executor. Processes one job at a time:

```
For each page in comic:
  1. Extract page image from archive
  2. Run OCR (OpenAI → Claude fallback)
  3. For each speaker segment:
     a. Map voice characteristics to a Minimax voice ID
     b. Synthesize speech via Minimax API
  4. Save audio as /storage/audio/{jobId}/page_{pageNum}.mp3
  5. Save TtsResult record (OCR text + speakers JSON + audio path)
  6. Update job progress percentage
```

**Status progression:** PENDING → PROCESSING → COMPLETED (or FAILED)

On per-page errors, the job logs the error and continues to the next page. Fatal errors set the entire job to FAILED with an error message.

### VoiceService

Deterministically maps voice characteristics (gender, age, emotion) to Minimax voice IDs. The lookup key format is `GENDER-AGE-EMOTION` (e.g., `MALE-ADULT-ANGRY`). Falls back to neutral emotion, then to `narrator_neutral`.

**Voice inventory:**
- Male voices: hero_young_male, villain_young_male, narrator_deep_male, villain_deep_male, mentor_old_male
- Female voices: heroine_young_female, villainess_young_female, narrator_female, villainess_female, mentor_old_female
- Default: narrator_neutral

### RatingService

Submit or update a user's 1–5 star rating for a comic. Enforces one rating per user per file via a unique constraint. Calculates averages from all ratings for a file.

### ProgressService

Tracks reading position per user per file. Creates a new record on first access (defaulting to page 1) or updates the existing one. The frontend debounces updates by 2 seconds to avoid excessive API calls during rapid page turning.

### UserService (Admin)

Full user CRUD with safeguards:
- **Create**: Validates username/email uniqueness, hashes password
- **Update**: Partial updates (only provided fields change)
- **Delete**: Soft delete (sets `deletedAt` timestamp)
- **Role assignment**: Prevents self-demotion and removing the last admin

All admin actions are recorded in the audit log.

### LlmConfigService

Manages encrypted LLM provider configurations:
- **Storage**: API keys encrypted with AES-256-GCM before persistence
- **Retrieval**: Keys masked on read (show prefix + `****` + last 4 chars)
- **Validation**: Format checks per provider (OpenAI `sk-*`, Claude `sk-ant-*`, Minimax 16+ chars)
- **Testing**: Makes a minimal API call to verify the key works, returns success/failure with response time

### AuditLogService

Records admin actions with: acting user ID, action type, affected entity type/ID, and JSON details.

**Logged actions:** CREATE_USER, UPDATE_USER, DELETE_USER, ASSIGN_ROLE, LLM_CONFIG_UPDATE, LLM_CONFIG_TEST

## Entity Model

### User

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated PK |
| username | String | Unique, not null |
| email | String | Unique, not null |
| passwordHash | String | BCrypt hash |
| role | String | "USER" or "ADMIN" (default: USER) |
| createdAt | LocalDateTime | Set on creation |
| updatedAt | LocalDateTime | Set on creation and update |
| deletedAt | LocalDateTime | Null unless soft-deleted |

### ComicFile

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated PK |
| filename | String | UUID-based storage name |
| originalFilename | String | User's original filename |
| filePath | String | Absolute path on disk |
| fileType | String | "CBR" or "CBZ" |
| fileSize | Long | Bytes |
| pageCount | Integer | Number of image files in archive |
| coverImagePath | String | Path to extracted cover (nullable) |
| uploadedBy | Long | User ID of uploader |
| createdAt | LocalDateTime | Upload timestamp |
| updatedAt | LocalDateTime | Last modified |

### ReadingProgress

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated PK |
| userId | Long | FK to users |
| fileId | Long | FK to comic_files |
| currentPage | Integer | Last read page (default: 1) |
| updatedAt | LocalDateTime | Last progress update |

Unique constraint on (userId, fileId).

### Rating

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated PK |
| userId | Long | FK to users |
| fileId | Long | FK to comic_files |
| rating | Integer | 1–5 |
| createdAt | LocalDateTime | Rating timestamp |

Unique constraint on (userId, fileId).

### TtsJob

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated PK |
| fileId | Long | FK to comic_files |
| requestedBy | Long | User ID who requested |
| status | String | PENDING, PROCESSING, COMPLETED, FAILED |
| progress | Integer | 0–100 percent |
| currentPage | Integer | Page being processed |
| errorMessage | String | Null unless FAILED |
| createdAt | LocalDateTime | Job creation |
| updatedAt | LocalDateTime | Last status change |

### TtsResult

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated PK |
| fileId | Long | FK to comic_files |
| pageNumber | Integer | 1-indexed page |
| ocrText | String (TEXT) | Full OCR extracted text |
| speakersJson | String (TEXT) | JSON array of `{speaker, text, voice}` |
| audioFilePath | String | API path (e.g., `/api/tts/audio/42/1`) |
| createdAt | LocalDateTime | Result creation |

### LlmConfig

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated PK |
| provider | String | "openai", "claude", or "minimax" |
| apiKeyEncrypted | String | AES-256-GCM encrypted |
| modelName | String | Default: gpt-4o / claude-3-5-sonnet / speech-01 |
| isActive | Integer | 0 or 1 (default: 1) |
| priority | Integer | Lower = higher priority (default: 1) |
| createdAt | LocalDateTime | Config creation |
| updatedAt | LocalDateTime | Last update |

### AuditLog

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated PK |
| userId | Long | Admin who performed action |
| action | String | Action type (CREATE_USER, etc.) |
| entityType | String | Affected entity type (User, LlmConfig) |
| entityId | Long | Affected entity ID |
| details | String (TEXT) | JSON details of the change |
| ipAddress | String | Nullable (planned) |
| createdAt | LocalDateTime | Action timestamp |

### SystemLog

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated PK |
| level | String | INFO, WARN, or ERROR |
| message | String (TEXT) | Log message |
| stackTrace | String (TEXT) | Stack trace for errors |
| createdAt | LocalDateTime | Log timestamp |

### FeatureFlag

Schema exists but has minimal usage in the current codebase.

## Repository Layer

Each entity has a corresponding Spring Data JPA repository. Notable custom queries:

| Repository | Method | Description |
|-----------|--------|-------------|
| UserRepository | `findAllActive(Pageable)` | Excludes soft-deleted users |
| UserRepository | `findByIdActive(Long)` | Single non-deleted user |
| UserRepository | `countByRole(String)` | For last-admin protection |
| ComicFileRepository | `findByUploadedBy(Long)` | User's uploads |
| RatingRepository | `findByUserIdAndFileId(Long, Long)` | User's rating for a file |
| ReadingProgressRepository | `findByUserId(Long)` | All progress for a user |
| TtsJobRepository | `findByFileIdAndStatus(Long, String)` | Active job check |
| TtsResultRepository | `findByFileIdAndPageNumber(Long, Integer)` | Specific page result |
| LlmConfigRepository | `findByIsActiveOrderByPriorityAsc(Integer)` | Active configs by priority |
| AuditLogRepository | `findByUserIdOrderByCreatedAtDesc(Long, Pageable)` | Paginated user activity |

## Configuration

### SecurityConfig

- **Password encoding**: BCrypt
- **Session management**: Server-side sessions (not stateless)
- **CSRF**: Disabled (SPA with SameSite=Lax cookies)
- **CORS**: Configured via CorsConfig
- **Auth filter**: `SessionAuthenticationFilter` reads userId/role from HttpSession and populates Spring Security context

### AsyncConfig

Single-threaded executor for TTS jobs:
- Core/max pool size: 1 (serial processing)
- Queue capacity: 100 jobs
- Thread prefix: `tts-`

### CorsConfig

- Origins: Configurable via `CORS_ORIGINS` env var
- Methods: GET, POST, PUT, DELETE, OPTIONS
- Credentials: Allowed
- Max age: 3600 seconds

### RestTemplate Beans

| Bean | Timeout | Purpose |
|------|---------|---------|
| `restTemplate` | 30s | General HTTP calls |
| `openAiRestTemplate` | 30s | OpenAI API calls |
| `claudeRestTemplate` | 30s | Claude API calls |
| `minimaxRestTemplate` | 60s | Minimax TTS calls (longer for audio generation) |

## External Integrations

### OpenAI Client

- **Endpoint**: `https://api.openai.com/v1/chat/completions`
- **Model**: GPT-4o (Vision)
- **Purpose**: Primary OCR provider for comic page dialogue extraction
- **Retry**: 3 attempts with exponential backoff (1s, 2s, 4s)
- **Handles**: HTTP 429 (rate limit), 5xx (server errors), timeouts
- **Image format**: Base64 data URI (`data:image/jpeg;base64,...`)
- **Max tokens**: 1000

### Claude Client

- **Endpoint**: `https://api.anthropic.com/v1/messages`
- **Model**: Claude 3.5 Sonnet
- **Purpose**: Fallback OCR when OpenAI fails
- **Retry**: Same 3-attempt exponential backoff
- **Headers**: `x-api-key`, `anthropic-version: 2023-06-01`
- **Image format**: Raw Base64 (no data URI prefix)

### Minimax Client

- **Endpoint**: `https://api.minimax.chat/v1/tts`
- **Purpose**: Text-to-speech synthesis
- **Retry**: 3 attempts with exponential backoff
- **Text limit**: 500 characters per call (truncated in current implementation)
- **Response**: Raw MP3 audio bytes
- **Timeout**: 60 seconds (longer than other clients)

## Exception Handling

A `GlobalExceptionHandler` (`@RestControllerAdvice`) maps exceptions to HTTP responses:

| Exception | HTTP Status | Error Title |
|-----------|-------------|-------------|
| UserAlreadyExistsException | 409 Conflict | User Already Exists |
| InvalidCredentialsException | 401 Unauthorized | Unauthorized |
| UserNotFoundException | 404 Not Found | Not Found |
| InvalidRoleException | 400 Bad Request | Bad Request |
| InvalidArchiveException | 400 Bad Request | Invalid Archive |
| OcrException | 503 Service Unavailable | OCR Service Unavailable |
| LlmConfigNotFoundException | 404 Not Found | Not Found |
| InvalidApiKeyException | 400 Bad Request | Bad Request |
| LlmConfigTestException | 500 Internal Server Error | Internal Server Error |
| AccessDeniedException | 403 Forbidden | Forbidden |
| MethodArgumentNotValidException | 400 Bad Request | Field-level validation errors |
| Exception (catch-all) | 500 Internal Server Error | An unexpected error occurred |

Response format:

```json
{
  "error": "Error Title",
  "message": "Detailed error description"
}
```

## File Processing Pipeline

### Upload Flow

```
POST /api/files (MultipartFile)
  │
  ├─ Validate: not null, not empty, ≤ 500 MB, safe filename
  │
  ├─ Detect type via magic bytes: ZIP → CBZ, RAR → CBR
  │
  ├─ Generate UUID filename, preserve original name
  │
  ├─ Save to disk: /storage/comics/{userId}/{uuid}.ext
  │
  ├─ Validate archive: can open, not corrupt, contains images
  │
  ├─ Count pages (image files in archive)
  │
  ├─ Create ComicFile entity → persist to database
  │
  └─ Return ComicFileResponse
```

### Page Retrieval Flow

```
GET /api/files/{id}/page/{pageNumber}
  │
  ├─ Validate: file exists, page number in range [1, pageCount]
  │
  ├─ Check cache: /storage/cache/{fileId}/{pageNumber}.*
  │   │
  │   ├─ Hit: touch file (update last-modified), return bytes
  │   │
  │   └─ Miss: continue to extraction
  │
  ├─ Extract from archive:
  │   ├─ List image files, sort alphabetically
  │   ├─ Extract by 0-based index
  │   └─ Detect format via magic bytes
  │
  ├─ Write to cache for future requests
  │
  ├─ Set headers: Content-Type, Cache-Control: max-age=3600
  │
  └─ Return image bytes
```

### TTS Processing Flow (Async)

```
POST /api/tts/jobs {fileId}
  │
  ├─ Check no active job exists (PENDING/PROCESSING)
  │
  ├─ Create TtsJob (status=PENDING, progress=0)
  │
  └─ Submit to @Async("ttsTaskExecutor")
       │
       └─ For each page (1..pageCount):
            │
            ├─ Extract page image
            │
            ├─ OCR cascade:
            │   ├─ OpenAI Vision (3 retries, exponential backoff)
            │   └─ Claude Vision (3 retries, fallback)
            │
            ├─ For each {speaker, text} segment:
            │   ├─ Infer voice characteristics (gender, age, emotion)
            │   ├─ Map to Minimax voice ID
            │   └─ Synthesize speech → MP3 bytes
            │
            ├─ Save audio: /storage/audio/{jobId}/page_{pageNum}.mp3
            │
            ├─ Save TtsResult (OCR text, speakers JSON, audio path)
            │
            └─ Update progress: (currentPage / totalPages) × 100
```
