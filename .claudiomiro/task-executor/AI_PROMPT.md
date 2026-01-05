# CBR/CBZ Viewer - Full-Stack Implementation Prompt

## 1. Purpose

**What:** Build a complete web-based CBR/CBZ comic book viewer with TTS (Text-to-Speech) capabilities powered by LLM-based OCR and speaker identification.

**Why:** Enable users to read digital comics (CBR/CBZ files) in a modern web interface with two viewing modes, persistent reading progress, ratings, and an innovative audio narration feature that uses AI to identify speakers and generate appropriate voice audio for each page.

**Success Definition:**
- Users can upload, browse, and read CBR/CBZ files in fullscreen page-by-page or scrolling mode
- Reading positions are remembered per user per file
- Users can rate files
- On-demand TTS processing: users request audio generation, system OCRs pages via LLM, identifies speakers, generates audio via Minimax API
- Admin panel for user/file management, logs, and LLM configuration
- Session-based authentication with secure HTTP-only cookies

---

## 2. Environment & Codebase Context

### Tech Stack

**Backend:**
- **Runtime:** Java 21 (LTS)
- **Framework:** Spring Boot 4.x
- **Database:** SQLite (single file database)
- **File Storage:** Local disk
- **Authentication:** Session-based with HTTP-only cookies (Spring Security)
- **Archive Libraries:**
  - ZIP: Java built-in `java.util.zip`
  - RAR: junrar or similar library
- **External APIs:**
  - OpenAI API (primary OCR via vision models)
  - Claude API (fallback OCR via vision models)
  - Minimax API (TTS generation) - https://www.minimax.io/

**Frontend:**
- **Framework:** React 18.x
- **UI Library:** MUI (Material-UI) v5 or v6
- **State Management:** Redux Toolkit
- **Build Tool:** Vite (recommended) or Create React App
- **HTTP Client:** Axios or fetch with Redux Toolkit async thunks

**Testing:**
- Backend: JUnit 5 + MockMvc + Spring Test
- Frontend: Jest + React Testing Library
- No E2E tests initially

### Project Structure (Monorepo)

```
cbr_viewer/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/cbrviewer/
│   │   │   │   ├── CbrViewerApplication.java
│   │   │   │   ├── config/
│   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   ├── CorsConfig.java
│   │   │   │   │   └── AsyncConfig.java
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   ├── FileController.java
│   │   │   │   │   ├── UserController.java
│   │   │   │   │   ├── TtsController.java
│   │   │   │   │   └── AdminController.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── AuthService.java
│   │   │   │   │   ├── FileService.java
│   │   │   │   │   ├── ArchiveService.java
│   │   │   │   │   ├── TtsService.java
│   │   │   │   │   ├── OcrService.java
│   │   │   │   │   ├── UserService.java
│   │   │   │   │   └── AuditLogService.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   ├── FileRepository.java
│   │   │   │   │   ├── RatingRepository.java
│   │   │   │   │   ├── ReadingProgressRepository.java
│   │   │   │   │   ├── TtsJobRepository.java
│   │   │   │   │   └── AuditLogRepository.java
│   │   │   │   ├── model/
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── ComicFile.java
│   │   │   │   │   ├── Rating.java
│   │   │   │   │   ├── ReadingProgress.java
│   │   │   │   │   ├── TtsJob.java
│   │   │   │   │   ├── TtsResult.java (JSON structure)
│   │   │   │   │   ├── AuditLog.java
│   │   │   │   │   └── LlmConfig.java
│   │   │   │   ├── dto/
│   │   │   │   ├── exception/
│   │   │   │   └── util/
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── schema.sql
│   │   └── test/
│   ├── build.gradle (or pom.xml)
│   └── Dockerfile (optional)
├── frontend/
│   ├── src/
│   │   ├── main.tsx
│   │   ├── App.tsx
│   │   ├── store/
│   │   │   ├── store.ts
│   │   │   ├── slices/
│   │   │   │   ├── authSlice.ts
│   │   │   │   ├── filesSlice.ts
│   │   │   │   ├── viewerSlice.ts
│   │   │   │   ├── ttsSlice.ts
│   │   │   │   └── adminSlice.ts
│   │   │   └── hooks.ts
│   │   ├── components/
│   │   │   ├── common/
│   │   │   ├── viewer/
│   │   │   │   ├── PageViewer.tsx
│   │   │   │   ├── ScrollViewer.tsx
│   │   │   │   ├── ViewerControls.tsx
│   │   │   │   └── TtsPlayer.tsx
│   │   │   ├── library/
│   │   │   │   ├── FileGrid.tsx
│   │   │   │   ├── FileCard.tsx
│   │   │   │   └── RatingStars.tsx
│   │   │   ├── admin/
│   │   │   │   ├── UserManagement.tsx
│   │   │   │   ├── FileManagement.tsx
│   │   │   │   ├── LogsView.tsx
│   │   │   │   └── LlmConfigPanel.tsx
│   │   │   └── auth/
│   │   │       ├── LoginForm.tsx
│   │   │       └── RegisterForm.tsx
│   │   ├── pages/
│   │   │   ├── HomePage.tsx
│   │   │   ├── LibraryPage.tsx
│   │   │   ├── ViewerPage.tsx
│   │   │   ├── AdminPage.tsx
│   │   │   └── LoginPage.tsx
│   │   ├── services/
│   │   │   └── api.ts
│   │   ├── hooks/
│   │   ├── theme/
│   │   │   └── theme.ts
│   │   └── types/
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   └── index.html
├── storage/                    # Comic files and generated audio
│   ├── comics/
│   └── audio/
├── README.md
└── docker-compose.yml (optional)
```

### Database Schema (SQLite)

```sql
-- Users table
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'USER', -- 'USER' or 'ADMIN'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Comic files metadata
CREATE TABLE comic_files (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    filename TEXT NOT NULL,
    original_filename TEXT NOT NULL,
    file_path TEXT NOT NULL,
    file_type TEXT NOT NULL, -- 'CBR' or 'CBZ'
    file_size INTEGER NOT NULL,
    page_count INTEGER NOT NULL,
    cover_image_path TEXT,
    uploaded_by INTEGER REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User reading progress (last page only)
CREATE TABLE reading_progress (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id),
    file_id INTEGER NOT NULL REFERENCES comic_files(id),
    current_page INTEGER NOT NULL DEFAULT 1,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, file_id)
);

-- User ratings
CREATE TABLE ratings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id),
    file_id INTEGER NOT NULL REFERENCES comic_files(id),
    rating INTEGER NOT NULL CHECK(rating >= 1 AND rating <= 5),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, file_id)
);

-- TTS processing jobs
CREATE TABLE tts_jobs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_id INTEGER NOT NULL REFERENCES comic_files(id),
    requested_by INTEGER NOT NULL REFERENCES users(id),
    status TEXT NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    progress INTEGER DEFAULT 0, -- 0-100
    current_page INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- TTS results stored as JSON in SQLite
CREATE TABLE tts_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_id INTEGER NOT NULL REFERENCES comic_files(id),
    page_number INTEGER NOT NULL,
    ocr_text TEXT, -- Raw extracted text
    speakers_json TEXT, -- JSON: [{"speaker": "narrator", "text": "...", "audio_path": "..."}]
    audio_file_path TEXT, -- Combined audio for the page
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(file_id, page_number)
);

-- LLM Configuration (admin-managed)
CREATE TABLE llm_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    provider TEXT NOT NULL, -- 'openai', 'claude', 'minimax'
    api_key_encrypted TEXT NOT NULL,
    model_name TEXT,
    is_active INTEGER DEFAULT 1,
    priority INTEGER DEFAULT 1, -- For fallback ordering
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Audit logs
CREATE TABLE audit_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES users(id),
    action TEXT NOT NULL,
    entity_type TEXT,
    entity_id INTEGER,
    details TEXT, -- JSON
    ip_address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- System logs
CREATE TABLE system_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    level TEXT NOT NULL, -- INFO, WARN, ERROR
    message TEXT NOT NULL,
    stack_trace TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### API Endpoints (REST)

```
Authentication:
POST   /api/auth/login          - Login with username/password
POST   /api/auth/logout         - Logout (invalidate session)
POST   /api/auth/register       - Register new user
GET    /api/auth/me             - Get current user info

Files:
GET    /api/files               - List all files (paginated)
GET    /api/files/{id}          - Get file metadata
GET    /api/files/{id}/page/{n} - Get page image (extracted on-the-fly)
GET    /api/files/{id}/cover    - Get cover image
POST   /api/files               - Upload new file (multipart)
DELETE /api/files/{id}          - Delete file (admin only)

Reading Progress:
GET    /api/progress/{fileId}       - Get current reading position
PUT    /api/progress/{fileId}       - Update reading position

Ratings:
GET    /api/ratings/{fileId}        - Get user's rating for file
POST   /api/ratings/{fileId}        - Rate a file (1-5)
GET    /api/ratings/{fileId}/avg    - Get average rating

TTS Processing:
POST   /api/tts/request/{fileId}    - Request TTS processing for file
GET    /api/tts/status/{jobId}      - Get job status and progress
GET    /api/tts/result/{fileId}     - Get TTS results for file
GET    /api/tts/audio/{fileId}/{page} - Stream audio for specific page

Admin:
GET    /api/admin/users             - List users
POST   /api/admin/users             - Create user
PUT    /api/admin/users/{id}        - Update user
DELETE /api/admin/users/{id}        - Delete user
GET    /api/admin/files             - List all files (admin view)
GET    /api/admin/logs              - Get system logs
GET    /api/admin/audit             - Get audit logs
GET    /api/admin/llm-config        - Get LLM configurations
PUT    /api/admin/llm-config/{id}   - Update LLM configuration
```

---

## 3. Related Code Context

Since this is a **greenfield project**, there is no existing code. However, follow these patterns from Context7 research:

### Spring Boot Patterns to Follow

**REST Controller Pattern:**
```java
@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileService fileService;

    @GetMapping
    public ResponseEntity<Page<FileDto>> getAllFiles(Pageable pageable) {
        return ResponseEntity.ok(fileService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileDto> getFile(@PathVariable Long id) {
        return fileService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(FileNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("File not found", ex.getMessage()));
    }
}
```

**Async Processing Pattern (for TTS):**
```java
@Service
@EnableAsync
public class TtsService {
    @Async
    public CompletableFuture<TtsResult> processFile(Long fileId, Long jobId) {
        // Update job status to PROCESSING
        // For each page:
        //   1. Extract image from archive
        //   2. Send to LLM for OCR
        //   3. Parse speakers from OCR result
        //   4. Generate audio via Minimax
        //   5. Store result
        //   6. Update progress
        // Update job status to COMPLETED
        return CompletableFuture.completedFuture(result);
    }
}
```

### React + Redux Toolkit Patterns to Follow

**Slice Pattern:**
```typescript
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';

export const fetchFiles = createAsyncThunk(
  'files/fetchAll',
  async (_, { rejectWithValue }) => {
    try {
      const response = await api.get('/files');
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data);
    }
  }
);

const filesSlice = createSlice({
  name: 'files',
  initialState: {
    items: [],
    status: 'idle', // 'idle' | 'loading' | 'succeeded' | 'failed'
    error: null,
  },
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchFiles.pending, (state) => {
        state.status = 'loading';
      })
      .addCase(fetchFiles.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.items = action.payload;
      })
      .addCase(fetchFiles.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload;
      });
  },
});
```

**MUI Theme Pattern:**
```typescript
import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    mode: 'dark', // Comic readers often prefer dark mode
    primary: { main: '#1976d2' },
    secondary: { main: '#dc004e' },
  },
  components: {
    MuiButton: {
      defaultProps: { variant: 'contained' },
    },
  },
});
```

---

## 4. Acceptance Criteria

### Authentication & Authorization
- [ ] Users can register with username, email, and password
- [ ] Users can login with username/password
- [ ] Session is maintained via HTTP-only secure cookie
- [ ] Session expires after configurable timeout (default: 24 hours)
- [ ] Unauthorized requests return 401
- [ ] Admin-only endpoints return 403 for non-admin users
- [ ] Logout invalidates server-side session

### File Management
- [ ] Admin can upload CBR/CBZ files via multipart form
- [ ] System extracts and counts pages on upload
- [ ] System generates/extracts cover image on upload
- [ ] Files are stored on disk with unique filenames (UUID)
- [ ] File metadata stored in SQLite
- [ ] Admin can delete files (also removes from disk)
- [ ] Users can browse files in a grid/list view

### Viewing
- [ ] Page-by-page mode: displays one page at a time with prev/next navigation
- [ ] Scrolling mode: displays all pages in vertical scroll (lazy loaded)
- [ ] Fullscreen mode available in both viewing modes
- [ ] Keyboard navigation: arrow keys for prev/next, ESC for exit fullscreen
- [ ] Pages extracted on-the-fly from archive (with caching)
- [ ] Responsive images that fit viewport

### Reading Progress
- [ ] Current page saved automatically when user navigates
- [ ] Progress stored per user per file (unique constraint)
- [ ] When opening a file, resume from last position
- [ ] Only stores last page number (not scroll position)

### Ratings
- [ ] Users can rate files 1-5 stars
- [ ] Rating updates if user re-rates
- [ ] Average rating displayed on file cards
- [ ] Only authenticated users can rate

### TTS Processing
- [ ] User clicks "Generate Audio" button to request TTS
- [ ] System creates TTS job with PENDING status
- [ ] Job processes asynchronously in background
- [ ] Progress updates as each page is processed (0-100%)
- [ ] User can poll job status endpoint for progress
- [ ] OCR via OpenAI Vision API (primary), fallback to Claude Vision
- [ ] OCR identifies dialogue, narration, and speakers
- [ ] TTS generates audio per speaker using Minimax API
- [ ] Results stored as JSON in SQLite
- [ ] Audio files stored on disk
- [ ] User can play audio per page after processing complete
- [ ] Error handling: if OCR/TTS fails, job marked FAILED with error message

### Admin Panel
- [ ] Only accessible to users with ADMIN role
- [ ] User Management: list, create, update, delete users
- [ ] File Management: list, delete files, view upload stats
- [ ] Logs: view system logs (filterable by level, date)
- [ ] Audit Logs: view user actions (who did what when)
- [ ] LLM Config: manage API keys for OpenAI, Claude, Minimax
- [ ] API keys stored encrypted in database

### Error Handling
- [ ] Invalid file format returns 400 with clear message
- [ ] File not found returns 404
- [ ] Server errors return 500 with generic message (details logged)
- [ ] Frontend displays user-friendly error messages
- [ ] All errors logged to system_logs table

### Performance
- [ ] Page images cached after first extraction (in-memory or disk cache)
- [ ] Lazy loading for scroll mode (load pages as user scrolls)
- [ ] Pagination for file lists (default 20 per page)
- [ ] SQLite optimized with proper indexes

---

## 4.1 Guardrails (Prohibitions)

### Scope Guardrails
- [ ] DO NOT implement features not listed above (no scope creep)
- [ ] DO NOT add OAuth/SSO - only session-based auth was chosen
- [ ] DO NOT add WebSocket for progress - use polling (REST API chosen)
- [ ] DO NOT add E2E tests - only unit + integration tests
- [ ] DO NOT add Docker/containerization unless explicitly requested later

### Architecture Guardrails
- [ ] DO NOT use JWT tokens - session-based auth was explicitly chosen
- [ ] DO NOT use React Context for state - Redux Toolkit was chosen
- [ ] DO NOT pre-extract files on upload - on-the-fly extraction was chosen
- [ ] DO NOT store TTS results in separate files - JSON in SQLite was chosen
- [ ] DO NOT add Nginx/CDN - Spring Boot serves everything for MVP

### Code Quality Guardrails
- [ ] DO NOT create unnecessary abstractions
- [ ] DO NOT add comments to obvious code
- [ ] DO NOT add unused dependencies
- [ ] AVOID over-engineering the LLM fallback (simple try-catch is enough)

### Security Guardrails
- [ ] NEVER store API keys in plain text - encrypt in database
- [ ] NEVER expose stack traces to frontend - log and return generic error
- [ ] NEVER trust user input - validate all inputs
- [ ] NEVER store passwords in plain text - use bcrypt or similar
- [ ] DO NOT disable CSRF protection for session-based auth

---

## 5. Implementation Guidance

### Layer 0: Foundation (Must Complete First)

1. **Project Setup**
   - Initialize monorepo structure
   - Backend: Spring Boot 4 project with Gradle or Maven
   - Frontend: React 18 + Vite + TypeScript
   - Configure SQLite datasource
   - Create database schema (all tables)

2. **Authentication System**
   - Spring Security configuration for session-based auth
   - User entity and repository
   - Auth controller (login, logout, register, me)
   - Password hashing with BCrypt
   - Session configuration (timeout, cookie settings)

### Layer 1: Core Features (Can Run in Parallel)

3. **File Management (Backend)**
   - ComicFile entity and repository
   - File upload endpoint (multipart)
   - Archive extraction service (ZIP + RAR)
   - Page serving endpoint (extract on-the-fly)
   - Cover image extraction
   - Caching layer for extracted pages

4. **File Browsing (Frontend)**
   - Redux store setup
   - Files slice with async thunks
   - File grid component with MUI Cards
   - File upload dialog (admin only)
   - Cover image display

5. **Viewer (Frontend)**
   - Page-by-page viewer component
   - Scroll viewer component (lazy loading)
   - Fullscreen mode toggle
   - Keyboard navigation
   - Viewer controls (zoom, mode switch)

6. **Reading Progress**
   - ReadingProgress entity and repository
   - Progress endpoints (get, update)
   - Frontend: auto-save on page change
   - Frontend: resume from last position on open

7. **Ratings**
   - Rating entity and repository
   - Rating endpoints
   - Frontend: star rating component
   - Frontend: average rating display

### Layer 2: TTS Processing

8. **TTS Backend Infrastructure**
   - TtsJob and TtsResult entities
   - Job creation endpoint
   - Status polling endpoint
   - Async processing service
   - Progress tracking

9. **OCR Service**
   - OpenAI Vision API integration
   - Claude Vision API integration (fallback)
   - Prompt engineering for dialogue/speaker extraction
   - JSON parsing of LLM response

10. **TTS Generation**
    - Minimax API integration
    - Speaker-to-voice mapping
    - Audio file generation and storage
    - Result storage in database

11. **TTS Frontend**
    - Request TTS button
    - Progress indicator (polling)
    - Audio player per page
    - TTS status display

### Layer 3: Admin Panel

12. **Admin Backend**
    - Admin-only security configuration
    - User management endpoints
    - Log viewing endpoints
    - LLM config endpoints
    - API key encryption/decryption

13. **Admin Frontend**
    - Admin route guard
    - User management UI
    - File management UI
    - Logs viewer (with filters)
    - LLM config editor

### Layer 4: Polish & Testing

14. **Testing**
    - Backend: JUnit tests for services
    - Backend: MockMvc tests for controllers
    - Frontend: Jest tests for slices
    - Frontend: RTL tests for key components

15. **Audit Logging**
    - AuditLog entity and service
    - Log user actions (login, upload, delete, etc.)
    - Admin audit log viewer

---

## 5.1 Testing Guidance

### Backend Testing (JUnit + MockMvc)

**Unit Tests:**
- Test service methods with mocked repositories
- Test archive extraction with sample CBZ/CBR files
- Test OCR response parsing
- Test password hashing

**Integration Tests:**
- Test controllers with MockMvc
- Test security configuration (auth required, admin-only)
- Test file upload flow
- Test SQLite operations

**Coverage Target:**
- Services: 80%+
- Controllers: 70%+ (happy path + error cases)

### Frontend Testing (Jest + RTL)

**Unit Tests:**
- Test Redux slices (reducers, actions)
- Test async thunk state transitions

**Component Tests:**
- Test viewer navigation
- Test rating component
- Test login form validation

**Coverage Target:**
- Slices: 90%+
- Components: 60%+ (key interactions)

---

## 6. Verification and Traceability

Each requirement from INITIAL_PROMPT.md must be verifiable:

| Requirement | Verification Method |
|-------------|---------------------|
| CBR/CBZ viewing | Open file, verify pages render correctly |
| Fullscreen mode | Click fullscreen button, verify browser fullscreen |
| Scrolling mode | Switch to scroll mode, verify all pages load on scroll |
| Page-by-page mode | Navigate with arrows, verify single page display |
| Remember positions | Close and reopen file, verify resume position |
| Rate files | Rate a file, refresh, verify rating persisted |
| TTS on-demand | Click generate audio, verify job created |
| LLM OCR | Check TTS result JSON contains extracted text |
| Speaker identification | Verify speakers_json contains speaker labels |
| Minimax TTS | Play audio, verify speech output |
| Results stored | Query database, verify tts_results populated |
| Admin user management | Create/edit/delete user in admin panel |
| Admin file management | Upload/delete files in admin panel |
| Admin logs | View logs in admin panel, verify entries |
| Admin audit logs | Perform actions, verify audit log entries |
| Admin LLM config | Update API keys, verify saved |
| Session-based auth | Login, verify cookie set, logout, verify session invalid |

---

## 7. Reasoning Boundaries

### Follow Existing Patterns
- Use standard Spring Boot controller/service/repository layering
- Use Redux Toolkit best practices (slices, async thunks)
- Use MUI components and theming system

### Avoid Over-Engineering
- Simple try-catch for LLM fallback, no complex circuit breaker
- In-memory cache for pages is sufficient (no Redis for MVP)
- SQLite is adequate for single-server deployment
- No microservices, keep monolithic

### When Uncertain
- If OCR prompt doesn't work well, iterate on prompt engineering
- If Minimax API has issues, document in error message
- If performance is slow, add indexes to SQLite first

### Boundaries
- Do NOT implement real-time WebSocket updates (polling is sufficient)
- Do NOT implement multi-language support initially
- Do NOT implement file sharing between users
- Do NOT implement comments or social features
- Do NOT implement cloud storage (disk only for MVP)

---

## 8. External API Integration Details

### OpenAI Vision API (Primary OCR)

```
POST https://api.openai.com/v1/chat/completions
Headers:
  Authorization: Bearer {API_KEY}
  Content-Type: application/json

Body:
{
  "model": "gpt-4o",
  "messages": [
    {
      "role": "user",
      "content": [
        {
          "type": "text",
          "text": "Analyze this comic book page. Extract all text including dialogue and narration. Identify speakers for each dialogue. Return JSON format: {\"segments\": [{\"type\": \"dialogue|narration\", \"speaker\": \"character name or narrator\", \"text\": \"the text\"}]}"
        },
        {
          "type": "image_url",
          "image_url": {"url": "data:image/jpeg;base64,{base64_image}"}
        }
      ]
    }
  ]
}
```

### Claude Vision API (Fallback OCR)

```
POST https://api.anthropic.com/v1/messages
Headers:
  x-api-key: {API_KEY}
  anthropic-version: 2023-06-01
  Content-Type: application/json

Body:
{
  "model": "claude-3-5-sonnet-20241022",
  "max_tokens": 4096,
  "messages": [
    {
      "role": "user",
      "content": [
        {
          "type": "image",
          "source": {
            "type": "base64",
            "media_type": "image/jpeg",
            "data": "{base64_image}"
          }
        },
        {
          "type": "text",
          "text": "Analyze this comic book page..."
        }
      ]
    }
  ]
}
```

### Minimax TTS API

Refer to https://www.minimax.io/ documentation for exact API format. Expected structure:

```
POST https://api.minimax.chat/v1/tts
Headers:
  Authorization: Bearer {API_KEY}
  Content-Type: application/json

Body:
{
  "text": "The dialogue text",
  "voice": "voice_id",
  "format": "mp3"
}

Response: Audio file binary or URL
```

---

## 9. Configuration

### application.yml (Backend)

```yaml
spring:
  application:
    name: cbr-viewer
  datasource:
    url: jdbc:sqlite:${SQLITE_PATH:./data/cbr_viewer.db}
    driver-class-name: org.sqlite.JDBC
  servlet:
    session:
      timeout: 24h
      cookie:
        http-only: true
        secure: ${COOKIE_SECURE:false}
        same-site: lax
  servlet:
    multipart:
      max-file-size: 500MB
      max-request-size: 500MB

server:
  port: 8080

app:
  storage:
    comics-path: ${STORAGE_PATH:./storage/comics}
    audio-path: ${STORAGE_PATH:./storage/audio}
    cache-path: ${CACHE_PATH:./storage/cache}
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:5173}
```

### Frontend Environment

```
VITE_API_BASE_URL=http://localhost:8080/api
```

---

## 10. Deliverables Checklist

- [ ] Backend Spring Boot 4 application
- [ ] Frontend React 18 application
- [ ] SQLite database with schema
- [ ] Working authentication (session-based)
- [ ] File upload and viewing (both modes)
- [ ] Reading progress tracking
- [ ] Rating system
- [ ] TTS processing pipeline
- [ ] Admin panel
- [ ] Unit tests (backend + frontend)
- [ ] Integration tests (backend)
- [ ] README with setup instructions
