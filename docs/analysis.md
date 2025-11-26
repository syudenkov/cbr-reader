# CBR/CBZ/JPG Reader Application - Technical Analysis

## Executive Summary

A family-focused comic reader web application for tablets with OCR and multi-language text-to-speech capabilities. The system will support CBR/CBZ/JPG formats with on-demand audio narration in Ukrainian, English, Russian, and Belarusian languages.

**Target**: Family use (~10 concurrent users)
**Budget**: Hobby project (~$20-50/month)
**Platform**: Web app (PWA) → Android native (future)
**Cloud**: Google Cloud Platform
**Backend**: Java/Spring Boot
**Initial Library**: 600+ comics, growing to 10k

---

## 1. System Architecture

### 1.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Client Layer                            │
│                                                              │
│  ┌────────────────────────────────────────────┐            │
│  │  Web Application (React/Vanilla JS)        │            │
│  │  - Comic Reader UI                         │            │
│  │  - Library Browser                         │            │
│  │  - User Profile & Settings                 │            │
│  │  - Offline Storage (IndexedDB)             │            │
│  └────────────────────────────────────────────┘            │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ HTTPS/REST API
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  API Gateway / Load Balancer                │
│                  (GCP Cloud Load Balancing)                 │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   Backend Services                          │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Spring Boot Application (Java 17+)                  │  │
│  │                                                       │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │  │
│  │  │   REST API  │  │   OCR       │  │    TTS      │ │  │
│  │  │   Layer     │  │   Service   │  │   Service   │ │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘ │  │
│  │                                                       │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │  │
│  │  │   Auth      │  │   Comic     │  │   Analytics │ │  │
│  │  │   Service   │  │   Parser    │  │   Service   │ │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘ │  │
│  │                                                       │  │
│  │  ┌─────────────────────────────────────────────────┐ │  │
│  │  │   Admin Web UI (Spring MVC/Thymeleaf)          │ │  │
│  │  └─────────────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    External Services                        │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  GCP Vision  │  │ GCP Text-to- │  │  LLM Service │     │
│  │     API      │  │   Speech API │  │  (Gemini/    │     │
│  │   (OCR)      │  │   (4 langs)  │  │   OpenAI)    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      Data Layer                             │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  PostgreSQL  │  │ GCP Cloud    │  │  GCP Cloud   │     │
│  │  (Cloud SQL) │  │   Storage    │  │  Memorystore │     │
│  │              │  │              │  │   (Redis)    │     │
│  │  - Users     │  │  - Comics    │  │              │     │
│  │  - Metadata  │  │  - Images    │  │  - Cache     │     │
│  │  - Progress  │  │  - Audio     │  │  - Sessions  │     │
│  │  - Ratings   │  │              │  │              │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 Component Breakdown

#### 1.2.1 Frontend (Web Application)

**Technology**: React or Vanilla JS + HTML5 Canvas

**Core Modules**:
- **Comic Reader Engine**
  - Canvas-based image rendering
  - Touch gestures (pinch-zoom, pan, swipe)
  - Reading modes: single page, double spread, continuous scroll
  - Page navigation controls
  - Audio playback controls

- **Library Browser**
  - Grid/list view of comics
  - Search and filtering (genre, series, author)
  - Sorting options

- **User Features**
  - Authentication (JWT-based)
  - Profile management
  - Reading history
  - Favorites
  - Settings (language, reading preferences)

- **Offline Support**
  - IndexedDB for downloaded comics
  - Service Worker for caching (optional PWA features)
  - Sync status indicator

**Key Libraries**:
- `jszip` or `libarchive.js` - Extract CBZ files
- `unrar.js` - Extract CBR files (or server-side extraction)
- `fabric.js` or native Canvas API - Image rendering
- `howler.js` - Audio playback
- `axios` - HTTP client
- `localforage` - IndexedDB wrapper

#### 1.2.2 Backend (Spring Boot)

**Technology**: Java 17+, Spring Boot 3.x

**Architecture Pattern**: Layered Architecture
```
Controllers → Services → Repositories → Entities
```

**Core Services**:

1. **Comic Service**
   - Upload comics (multipart file upload)
   - Extract and parse CBR/CBZ files
   - Generate thumbnails
   - Metadata extraction and storage
   - Serve comic pages (streaming)

2. **OCR Service**
   - Accept OCR requests for specific comic pages
   - Call GCP Vision API
   - Integrate with LLM for panel/bubble detection
   - Store OCR results (text + coordinates)
   - Mark comics as "problematic" if errors detected
   - Queue management for async processing

3. **TTS Service**
   - Convert OCR text to speech (GCP Text-to-Speech)
   - Support multiple languages (Ukrainian, English, Russian, Belarusian)
   - Voice selection (male/female, child-friendly)
   - Cache audio files in Cloud Storage
   - Serve audio files via CDN

4. **User Service**
   - Registration and authentication (Spring Security + JWT)
   - Profile management
   - Reading progress tracking
   - Favorites and bookmarks
   - Cross-device sync

5. **Analytics Service**
   - Track reading events (start, completion, time spent)
   - Popular content tracking
   - OCR/TTS usage statistics
   - User engagement metrics

6. **Admin Service**
   - Content management UI
   - Manual tagging (genres, series, authors)
   - Review problematic comics
   - Reprocess OCR/TTS
   - User management
   - Analytics dashboard

**Key Spring Boot Dependencies**:
- Spring Web (REST API)
- Spring Security (Authentication/Authorization)
- Spring Data JPA (Database ORM)
- Spring Boot Actuator (Health checks, metrics)
- Spring Batch (Optional: bulk processing)
- Spring Cloud GCP (GCP integrations)
- Liquibase/Flyway (Database migrations)

**Additional Libraries**:
- `junrar` - RAR extraction (CBR)
- `zip4j` - ZIP extraction (CBZ)
- `thumbnailator` - Image thumbnail generation
- `google-cloud-vision` - OCR
- `google-cloud-texttospeech` - TTS
- `google-cloud-storage` - Object storage
- `jedis` or Spring Data Redis - Redis client
- `openai-java` or Gemini SDK - LLM integration

#### 1.2.3 Database Schema (PostgreSQL)

**Tables**:

```sql
-- Users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    preferred_language VARCHAR(10) DEFAULT 'uk',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Comics
CREATE TABLE comics (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255),
    publisher VARCHAR(255),
    series_name VARCHAR(255),
    issue_number VARCHAR(50),
    description TEXT,
    language VARCHAR(10),
    page_count INTEGER,
    file_size BIGINT,
    file_path VARCHAR(500) NOT NULL,
    cover_image_url VARCHAR(500),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Genres/Tags (many-to-many)
CREATE TABLE genres (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE comic_genres (
    comic_id BIGINT REFERENCES comics(id) ON DELETE CASCADE,
    genre_id INTEGER REFERENCES genres(id) ON DELETE CASCADE,
    PRIMARY KEY (comic_id, genre_id)
);

-- Comic Pages
CREATE TABLE comic_pages (
    id BIGSERIAL PRIMARY KEY,
    comic_id BIGINT REFERENCES comics(id) ON DELETE CASCADE,
    page_number INTEGER NOT NULL,
    image_path VARCHAR(500) NOT NULL,
    thumbnail_path VARCHAR(500),
    width INTEGER,
    height INTEGER,
    ocr_processed BOOLEAN DEFAULT FALSE,
    ocr_problematic BOOLEAN DEFAULT FALSE,
    UNIQUE (comic_id, page_number)
);

-- OCR Results
CREATE TABLE ocr_results (
    id BIGSERIAL PRIMARY KEY,
    page_id BIGINT REFERENCES comic_pages(id) ON DELETE CASCADE,
    full_text TEXT,
    language VARCHAR(10),
    confidence DECIMAL(5,2),
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    llm_processed BOOLEAN DEFAULT FALSE
);

-- OCR Text Regions (panels/bubbles detected by LLM)
CREATE TABLE text_regions (
    id BIGSERIAL PRIMARY KEY,
    ocr_result_id BIGINT REFERENCES ocr_results(id) ON DELETE CASCADE,
    region_type VARCHAR(20), -- 'panel', 'bubble', 'caption'
    text_content TEXT NOT NULL,
    x INTEGER,
    y INTEGER,
    width INTEGER,
    height INTEGER,
    reading_order INTEGER,
    tts_audio_url VARCHAR(500)
);

-- User Reading Progress
CREATE TABLE reading_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    comic_id BIGINT REFERENCES comics(id) ON DELETE CASCADE,
    current_page INTEGER DEFAULT 1,
    completed BOOLEAN DEFAULT FALSE,
    last_read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, comic_id)
);

-- Favorites
CREATE TABLE favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    comic_id BIGINT REFERENCES comics(id) ON DELETE CASCADE,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, comic_id)
);

-- Ratings & Comments
CREATE TABLE ratings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    comic_id BIGINT REFERENCES comics(id) ON DELETE CASCADE,
    rating INTEGER CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, comic_id)
);

-- Analytics Events
CREATE TABLE analytics_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    comic_id BIGINT REFERENCES comics(id) ON DELETE SET NULL,
    event_type VARCHAR(50) NOT NULL, -- 'read_start', 'read_complete', 'ocr_request', 'tts_play'
    event_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_comics_series ON comics(series_name);
CREATE INDEX idx_reading_progress_user ON reading_progress(user_id);
CREATE INDEX idx_reading_progress_comic ON reading_progress(comic_id);
CREATE INDEX idx_analytics_events_type ON analytics_events(event_type);
CREATE INDEX idx_analytics_events_created ON analytics_events(created_at);
```

#### 1.2.4 Storage Structure (GCP Cloud Storage)

**Bucket Organization**:

```
cbr-reader-comics/
├── comics/
│   ├── {comic-id}/
│   │   ├── original.cbr (or .cbz)
│   │   ├── pages/
│   │   │   ├── page-001.jpg
│   │   │   ├── page-002.jpg
│   │   │   └── ...
│   │   ├── thumbnails/
│   │   │   ├── page-001-thumb.jpg
│   │   │   └── ...
│   │   └── cover.jpg
│   └── ...
├── audio/
│   ├── {comic-id}/
│   │   ├── {page-id}/
│   │   │   ├── region-{region-id}-uk.mp3
│   │   │   ├── region-{region-id}-en.mp3
│   │   │   └── ...
│   │   └── ...
│   └── ...
└── temp/
    └── uploads/
```

**Storage Lifecycle Policies**:
- Delete temp files older than 7 days
- Archive rarely accessed comics (>1 year no access) to Nearline storage

---

## 2. Technology Stack

### 2.1 Frontend Stack

**Option A: React (Recommended)**
- **Framework**: React 18+
- **Build Tool**: Vite or Create React App
- **State Management**: Context API + useReducer (or Zustand for simplicity)
- **Routing**: React Router
- **HTTP Client**: Axios
- **UI Components**:
  - Headless UI or Radix UI (accessible primitives)
  - Tailwind CSS (styling)
- **Comic Reader**: Custom Canvas-based component
- **Offline Storage**: localForage (IndexedDB wrapper)

**Option B: Vanilla JS (Lighter)**
- Pure JavaScript (ES6+)
- HTML5 Canvas API
- Fetch API
- IndexedDB API
- CSS3 (or Tailwind)
- Build tool: Webpack or Vite

**Recommendation**: **React** for better maintainability, component reusability, and ecosystem.

### 2.2 Backend Stack

**Core**:
- **Java**: 17 or 21 (LTS)
- **Spring Boot**: 3.2+
- **Build Tool**: Maven or Gradle

**Key Dependencies**:
```xml
<!-- Spring Boot Starters -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<!-- GCP -->
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>spring-cloud-gcp-starter-storage</artifactId>
</dependency>
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-vision</artifactId>
</dependency>
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-texttospeech</artifactId>
</dependency>

<!-- Database -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>

<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Archive Handling -->
<dependency>
    <groupId>com.github.junrar</groupId>
    <artifactId>junrar</artifactId>
    <version>7.5.5</version>
</dependency>
<dependency>
    <groupId>net.lingala.zip4j</groupId>
    <artifactId>zip4j</artifactId>
    <version>2.11.5</version>
</dependency>

<!-- Image Processing -->
<dependency>
    <groupId>net.coobird</groupId>
    <artifactId>thumbnailator</artifactId>
    <version>0.4.20</version>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>

<!-- LLM Integration -->
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-aiplatform</artifactId>
</dependency>
```

### 2.3 Database & Storage

- **Primary Database**: PostgreSQL 15+ (GCP Cloud SQL)
- **Cache**: Redis 7+ (GCP Memorystore)
- **Object Storage**: GCP Cloud Storage (Standard class)

### 2.4 External Services

**OCR**:
- **Primary**: Google Cloud Vision API
  - Text Detection API
  - Supports multiple languages (Ukrainian, English, Russian)
  - Pay-per-use: ~$1.50 per 1000 images

**TTS**:
- **Primary**: Google Cloud Text-to-Speech API
  - WaveNet voices (high quality, natural)
  - Neural2 voices (even better, slightly more expensive)
  - Language support: uk-UA, en-US, ru-RU
  - Belarusian: May need fallback to Russian or custom voice
  - Pay-per-use: ~$4-16 per 1M characters (depending on voice quality)

**LLM** (Panel/Bubble Detection):
- **Option A**: Google Gemini (GCP native, cost-effective)
- **Option B**: OpenAI GPT-4 Vision (better accuracy, higher cost)
- **Use Case**: Send comic page image → LLM identifies panels, speech bubbles, captions → returns coordinates and reading order

### 2.5 Infrastructure (GCP)

**Compute**:
- **Option A**: Cloud Run (Recommended for hobby budget)
  - Serverless container platform
  - Auto-scaling (0 to N instances)
  - Pay only for requests
  - Perfect for low-traffic family use

- **Option B**: Compute Engine (VM)
  - e2-micro or e2-small instance
  - More predictable costs
  - Good for always-on service

**Database**:
- Cloud SQL (PostgreSQL)
  - db-f1-micro or db-g1-small (for hobby project)
  - Automated backups
  - High availability (optional)

**Cache**:
- Memorystore (Redis)
  - Basic tier (1GB)
  - Used for session storage, API response caching

**Storage**:
- Cloud Storage
  - Standard storage class for active comics
  - Nearline for archived content (>1 year no access)
  - Estimated: 600 comics × 50MB avg = 30GB initially
  - 10k comics × 50MB = 500GB at full scale

**CDN**:
- Cloud CDN (optional, but recommended)
  - Cache comic images and audio files
  - Reduce latency for repeated access
  - Reduce egress costs

**Networking**:
- Cloud Load Balancing
- Cloud Armor (optional DDoS protection)

---

## 3. Feature Breakdown & Implementation

### 3.1 Phase 1: MVP (Core Reading Experience)

**Timeline**: 4-6 weeks

**Features**:
1. **User Authentication**
   - Registration (username, email, password)
   - Login (JWT-based)
   - Profile page (display name, language preference)

2. **Comic Upload & Management (Admin)**
   - Manual upload via admin UI
   - Automatic file scanning (watch directory in Cloud Storage)
   - Extract CBR/CBZ files
   - Generate thumbnails and metadata
   - Store in database

3. **Comic Library Browser**
   - Grid view with cover images
   - Basic search (title, author)
   - Filter by series
   - Sort by title, upload date

4. **Comic Reader**
   - Single page view
   - Touch gestures:
     - Swipe left/right for page navigation
     - Pinch-to-zoom
     - Pan when zoomed
   - Page indicator (e.g., "Page 5 / 32")
   - Navigation controls (prev/next buttons)

5. **Reading Progress**
   - Auto-save current page
   - Resume from last page on re-open

**Tech Implementation**:
- Spring Boot backend with REST API
- React frontend with Canvas-based reader
- PostgreSQL for metadata and progress
- Cloud Storage for comics
- Docker containerization
- Deploy to Cloud Run

**Estimated Costs** (Phase 1):
- Cloud Run: $0-10/month (free tier covers low traffic)
- Cloud SQL (db-f1-micro): $7-10/month
- Cloud Storage (30GB): $0.50/month
- **Total**: ~$10-20/month

---

### 3.2 Phase 2: Enhanced Reading Features

**Timeline**: 3-4 weeks

**Features**:
1. **Advanced Reading Modes**
   - Double page spread (landscape)
   - Continuous scroll mode
   - Vertical/horizontal reading direction toggle
   - Reading mode persistence per user

2. **Page Navigation**
   - Page thumbnails grid view
   - Table of contents (if metadata available)
   - Jump to page number

3. **Favorites & Bookmarks**
   - Add comics to favorites
   - Multiple bookmarks per comic
   - Bookmark notes (optional)

4. **Reading History**
   - List of recently read comics
   - Completion tracking
   - Reading statistics (pages read, time spent)

5. **Offline Download**
   - Download comic for offline reading
   - Store in IndexedDB
   - Sync status indicator
   - Manage downloaded comics

6. **Enhanced Library**
   - Advanced filtering (genre, author, series)
   - Sorting options (rating, popularity, date added)
   - Continue reading section

**Tech Implementation**:
- Extend reader component with new modes
- IndexedDB for offline storage
- Service Worker for background sync (optional)
- Enhanced API endpoints for favorites, bookmarks

**Estimated Costs**: Same as Phase 1 (~$10-20/month)

---

### 3.3 Phase 3: OCR & TTS Integration

**Timeline**: 4-6 weeks

**Features**:
1. **On-Demand OCR**
   - User clicks "Process Audio" button for a comic
   - Backend processes all pages asynchronously
   - Progress indicator
   - Store OCR results in database

2. **LLM Panel Detection**
   - Send page images to Gemini/GPT-4 Vision
   - Detect panels, speech bubbles, captions
   - Extract reading order and coordinates
   - Store text regions with metadata

3. **Text-to-Speech Generation**
   - Convert OCR text to speech (4 languages)
   - Generate audio files per text region
   - Store in Cloud Storage
   - Link audio URLs to text regions

4. **Audio Playback**
   - Tap panel to play audio
   - Auto-play mode (reads page sequentially)
   - Continuous reading mode (entire comic)
   - Visual indicator of active panel
   - Playback controls (play/pause, next/previous)

5. **Voice Selection**
   - Choose from available voices
   - Male/female options
   - Child-friendly voices
   - Per-language voice settings

6. **Mark Problematic Comics**
   - User reports OCR issues
   - Flag comic for admin review
   - Admin UI to review and reprocess

**Tech Implementation**:
- Async job queue (Spring's @Async or external queue like GCP Pub/Sub)
- Integration with GCP Vision API
- Integration with Gemini API for panel detection
- Integration with GCP Text-to-Speech API
- Audio player component in frontend
- Admin review interface

**Estimated Costs** (Phase 3 with OCR/TTS usage):
- Base infrastructure: $10-20/month
- Cloud Vision API: $1.50 per 1000 pages
  - 600 comics × 30 pages avg = 18,000 pages
  - One-time cost: ~$27
- Text-to-Speech API: ~$4-16 per 1M characters
  - Estimate: 30 pages × 200 words × 6 chars avg = 36,000 chars per comic
  - 600 comics = 21.6M characters
  - One-time cost: $86-345 (depends on voice quality)
- Gemini API: ~$0.002 per image (multimodal)
  - 18,000 pages × $0.002 = $36
- **Initial Processing Total**: ~$150-400 (one-time)
- **Monthly**: $10-20 (infrastructure only, minimal ongoing OCR/TTS)

**Cost Optimization**:
- Use Standard voices instead of Neural2 (cheaper)
- Cache all OCR/TTS results (never reprocess unless requested)
- For 10k comics, spread processing over time

---

### 3.4 Phase 4: Admin, Analytics & Polish

**Timeline**: 3-4 weeks

**Features**:
1. **Admin Web UI**
   - Content management dashboard
   - Upload comics (drag-and-drop)
   - Edit metadata (title, author, series, genre)
   - Manual tagging (genres, add new tags)
   - Bulk operations (tag multiple comics)
   - View problematic comics
   - Reprocess OCR/TTS
   - User management (view users, disable accounts if needed)

2. **Content Filtering**
   - Tag-based filtering
   - Hide/show content based on tags
   - User-level content preferences

3. **Ratings & Comments**
   - 5-star rating system
   - Text comments
   - Display average rating
   - Sort by rating

4. **Analytics Dashboard**
   - Reading completion rates (per comic, per user)
   - Popular content (most read, highest rated)
   - User engagement metrics (time spent, pages read)
   - OCR/TTS usage statistics
   - Charts and graphs (Chart.js or Recharts)

5. **UI Polish**
   - Responsive design (tablets, landscape phones)
   - Dark mode (optional)
   - Loading states and error handling
   - Animations and transitions
   - Accessibility improvements

**Tech Implementation**:
- Spring MVC with Thymeleaf for admin UI (or separate React admin app)
- Analytics event tracking (store in database)
- Aggregation queries for dashboard
- Charts library (Chart.js)

**Estimated Costs**: Same as Phase 3 (~$10-20/month)

---

## 4. Data Flow Examples

### 4.1 Comic Upload & Processing Flow

```
1. Admin uploads CBR/CBZ file via admin UI
   ↓
2. Spring Boot receives multipart file upload
   ↓
3. Store original file in Cloud Storage (comics/{comic-id}/original.cbr)
   ↓
4. Extract archive to temp directory
   ↓
5. For each image file:
   a. Validate image format
   b. Store in Cloud Storage (comics/{comic-id}/pages/page-{num}.jpg)
   c. Generate thumbnail
   d. Store thumbnail (comics/{comic-id}/thumbnails/page-{num}-thumb.jpg)
   e. Insert comic_pages record
   ↓
6. Extract metadata (title from filename, page count, file size)
   ↓
7. Generate cover image (first page or specified)
   ↓
8. Insert comics record in database
   ↓
9. Return success response
   ↓
10. Admin tags comic with genres, series, author
```

### 4.2 OCR + TTS Processing Flow

```
1. User clicks "Process Audio" button for a comic
   ↓
2. Frontend sends POST /api/comics/{id}/process-audio
   ↓
3. Backend creates async job (returns job ID immediately)
   ↓
4. Background worker processes pages sequentially:

   For each page:
   a. Fetch image from Cloud Storage
      ↓
   b. Send to LLM (Gemini) with prompt:
      "Identify all text regions in this comic page. For each region,
       provide: type (panel/bubble/caption), bounding box (x,y,w,h),
       and reading order."
      ↓
   c. LLM returns JSON with text regions and coordinates
      ↓
   d. Send image to GCP Vision API for OCR
      ↓
   e. Vision API returns full OCR text with coordinates
      ↓
   f. Match OCR text to LLM-detected regions
      ↓
   g. Store ocr_results record
      ↓
   h. For each text region:
      - Extract text content
      - Store text_regions record
      - Send text to GCP Text-to-Speech API (4 languages)
      - Receive audio data
      - Store audio files in Cloud Storage
      - Update text_regions.tts_audio_url
      ↓
   i. Mark page as ocr_processed = true

5. After all pages processed, update job status to "completed"
   ↓
6. Notify user (WebSocket or polling)
```

### 4.3 Reading with Audio Playback Flow

```
1. User opens comic reader
   ↓
2. Frontend fetches comic metadata and pages
   ↓
3. Display first page (or last read page)
   ↓
4. Check if OCR processed for current page
   ↓
5. If yes, fetch text_regions for current page
   ↓
6. User taps on a panel
   ↓
7. Frontend detects tap coordinates (x, y)
   ↓
8. Match coordinates to text_regions bounding boxes
   ↓
9. Find matching region
   ↓
10. Fetch audio URL for user's preferred language
    ↓
11. Load and play audio file
    ↓
12. Highlight panel while playing (optional)
    ↓
13. On audio end, move to next region if in auto-play mode
```

---

## 5. API Design

### 5.1 REST API Endpoints

**Authentication**:
```
POST   /api/auth/register          - Register new user
POST   /api/auth/login             - Login (returns JWT)
POST   /api/auth/logout            - Logout
GET    /api/auth/me                - Get current user
PUT    /api/auth/me                - Update profile
```

**Comics**:
```
GET    /api/comics                 - List all comics (paginated, filtered)
GET    /api/comics/{id}            - Get comic details
GET    /api/comics/{id}/pages      - Get all pages for a comic
GET    /api/comics/{id}/pages/{num} - Get specific page image
POST   /api/comics/{id}/process-audio - Trigger OCR/TTS processing
GET    /api/comics/{id}/process-status - Check processing status
POST   /api/comics/{id}/report     - Report problematic comic
```

**Text Regions & Audio**:
```
GET    /api/comics/{id}/pages/{num}/regions - Get text regions for a page
GET    /api/regions/{id}/audio/{lang}       - Get audio URL for region
```

**User Features**:
```
GET    /api/users/me/progress              - Get all reading progress
GET    /api/users/me/progress/{comicId}    - Get progress for specific comic
PUT    /api/users/me/progress/{comicId}    - Update reading progress
POST   /api/users/me/favorites/{comicId}   - Add to favorites
DELETE /api/users/me/favorites/{comicId}   - Remove from favorites
GET    /api/users/me/favorites             - Get all favorites
GET    /api/users/me/history               - Get reading history
```

**Ratings & Comments**:
```
GET    /api/comics/{id}/ratings            - Get all ratings for comic
POST   /api/comics/{id}/ratings            - Add/update rating
DELETE /api/comics/{id}/ratings            - Delete rating
```

**Admin** (requires ADMIN role):
```
POST   /api/admin/comics                   - Upload comic
PUT    /api/admin/comics/{id}              - Update comic metadata
DELETE /api/admin/comics/{id}              - Delete comic
GET    /api/admin/comics/problematic       - List problematic comics
POST   /api/admin/comics/{id}/reprocess    - Reprocess OCR/TTS
GET    /api/admin/users                    - List all users
GET    /api/admin/analytics                - Get analytics data
```

**Analytics**:
```
POST   /api/analytics/events               - Track analytics event
GET    /api/analytics/popular              - Get popular comics
GET    /api/analytics/stats                - Get overall statistics
```

### 5.2 Example API Responses

**GET /api/comics**:
```json
{
  "content": [
    {
      "id": 1,
      "title": "Spider-Man #1",
      "author": "Stan Lee",
      "publisher": "Marvel",
      "seriesName": "Spider-Man",
      "issueNumber": "1",
      "language": "en",
      "pageCount": 32,
      "coverImageUrl": "https://storage.googleapis.com/.../cover.jpg",
      "avgRating": 4.5,
      "ocrProcessed": true,
      "uploadedAt": "2024-01-15T10:30:00Z"
    }
  ],
  "totalElements": 600,
  "totalPages": 60,
  "size": 10,
  "number": 0
}
```

**GET /api/comics/{id}/pages/{num}/regions**:
```json
{
  "pageNumber": 5,
  "regions": [
    {
      "id": 123,
      "type": "bubble",
      "textContent": "Привіт! Як справи?",
      "x": 100,
      "y": 50,
      "width": 200,
      "height": 80,
      "readingOrder": 1,
      "audioUrls": {
        "uk": "https://storage.googleapis.com/.../region-123-uk.mp3",
        "en": "https://storage.googleapis.com/.../region-123-en.mp3",
        "ru": "https://storage.googleapis.com/.../region-123-ru.mp3"
      }
    }
  ]
}
```

---

## 6. Security Considerations

### 6.1 Authentication & Authorization

**Authentication**:
- JWT-based (stateless)
- Access token (short-lived, 15-30 min)
- Refresh token (long-lived, stored in httpOnly cookie)
- Password hashing: BCrypt (Spring Security default)

**Authorization**:
- Role-based access control (RBAC)
- Roles: USER, ADMIN
- Spring Security method-level security (`@PreAuthorize`)

**Example**:
```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/api/admin/comics")
public ResponseEntity<Comic> uploadComic(@RequestParam("file") MultipartFile file) {
    // ...
}
```

### 6.2 API Security

**Rate Limiting**:
- Spring Security + Bucket4j
- Limit API requests per user (e.g., 100 req/min)
- Prevent abuse of expensive OCR/TTS endpoints

**CORS**:
- Configure allowed origins
- For development: localhost
- For production: your domain only

**File Upload Security**:
- Validate file type (only CBR, CBZ, JPG, PNG)
- File size limits (e.g., max 200MB per comic)
- Virus scanning (optional, GCP Cloud Security Scanner)

**Input Validation**:
- Validate all user inputs (Spring Validation)
- Sanitize inputs to prevent SQL injection, XSS

### 6.3 Data Privacy

**User Data**:
- Minimal data collection (username, email, reading preferences)
- No third-party tracking/analytics
- HTTPS only (TLS encryption in transit)
- Encrypted database credentials (GCP Secret Manager)

**Content Security**:
- Users can only access their own data (progress, favorites)
- Comics are accessible to all authenticated users (shared family library)

---

## 7. Performance Optimization

### 7.1 Frontend Optimization

**Image Loading**:
- Lazy loading (load images on demand)
- Progressive loading (low-res preview → high-res)
- Image optimization: WebP format (smaller size, better quality)
- Responsive images (serve appropriate size for device)

**Caching**:
- Browser caching for static assets (images, audio)
- Service Worker caching (optional PWA)
- IndexedDB for offline comics

**Reader Performance**:
- Canvas rendering optimization (requestAnimationFrame)
- Debounce zoom/pan events
- Pre-load adjacent pages (page N-1, N+1)

### 7.2 Backend Optimization

**Database**:
- Indexes on frequently queried fields (see schema)
- Connection pooling (HikariCP, Spring Boot default)
- Query optimization (avoid N+1 queries)
- Pagination for large result sets

**Caching**:
- Redis for:
  - Session storage
  - API response caching (comic metadata, page lists)
  - OCR results caching
- Spring Cache abstraction (`@Cacheable`)

**File Serving**:
- Stream large files (don't load entirely in memory)
- Use Cloud Storage signed URLs (temporary, secure links)
- CDN for static assets (Cloud CDN)

**Async Processing**:
- OCR/TTS processing in background jobs
- Don't block API responses
- Use `@Async` or GCP Pub/Sub for job queue

### 7.3 Storage Optimization

**Image Compression**:
- JPEG quality: 80-85% (good balance)
- Resize images if excessively large (e.g., max 2048px width)
- Thumbnails: 200x300px for cover, 100x150px for page grid

**Storage Classes**:
- Standard for active comics
- Nearline for archived content (auto-transition after 1 year)

**Lifecycle Policies**:
- Delete temp files after 7 days
- Archive old logs

---

## 8. Cost Breakdown (GCP)

### 8.1 Monthly Costs (Steady State)

**Compute (Cloud Run)**:
- Pricing: $0.00002400 per vCPU-second, $0.00000250 per GiB-second
- Free tier: 2M requests, 360k GiB-second, 180k vCPU-second per month
- Estimated usage (10 family users, moderate use):
  - ~50k requests/month (well within free tier)
- **Cost**: $0-5/month

**Database (Cloud SQL - db-f1-micro)**:
- Pricing: ~$7.50/month (shared-core instance)
- Storage: $0.17/GB/month (10GB = $1.70/month)
- **Cost**: ~$9-10/month

**Cache (Memorystore - Redis Basic 1GB)**:
- Pricing: ~$30/month (cheapest tier)
- **Alternative**: Use local Redis in Docker (free, but less reliable)
- **Cost**: $0 (skip Redis initially) or $30/month

**Storage (Cloud Storage)**:
- Standard storage: $0.02/GB/month
- Initial: 30GB = $0.60/month
- At scale (500GB): $10/month
- Egress: First 1GB free, then $0.12/GB (minimal for 10 users)
- **Cost**: $0.60-10/month

**Cloud CDN** (optional):
- Cache fills: $0.08/GB
- Cache egress: $0.04-0.12/GB (cheaper than direct egress)
- Minimal cost for low traffic
- **Cost**: $0-2/month

**Total Monthly (Steady State)**:
- Without Redis: ~$10-20/month
- With Redis: ~$40-50/month

**Recommendation**: Skip Memorystore Redis initially. Use in-memory caching in Spring Boot. Add Redis later if needed.

### 8.2 One-Time Processing Costs (OCR/TTS)

**Cloud Vision API (OCR)**:
- $1.50 per 1000 images
- 600 comics × 30 pages = 18,000 pages
- Cost: $27

**Gemini API (Panel Detection)**:
- $0.002 per image (multimodal input)
- 18,000 pages × $0.002 = $36

**Text-to-Speech API**:
- Standard voices: $4 per 1M characters
- Neural2 voices: $16 per 1M characters
- Estimate: 21.6M characters (600 comics, 4 languages)
- Standard cost: $86
- Neural2 cost: $346

**Total One-Time (600 comics)**:
- Standard voices: $149
- Neural2 voices: $409

**For 10k comics (full library)**:
- Standard voices: ~$2,500
- Neural2 voices: ~$6,800

**Cost Optimization Strategies**:
1. Use Standard voices (good enough for kids)
2. Generate only Ukrainian initially, add other languages on-demand
3. Spread processing over months (not all at once)
4. Only process popular/requested comics

---

## 9. Development Roadmap

### 9.1 Phase 1: MVP (Weeks 1-6)

**Week 1-2: Project Setup**
- Initialize Spring Boot project
- Set up PostgreSQL database (local + Cloud SQL)
- Configure GCP Cloud Storage
- Initialize React project
- Set up Docker development environment
- Create CI/CD pipeline (optional)

**Week 3-4: Backend Core**
- User authentication (Spring Security + JWT)
- Comic upload API
- CBR/CBZ extraction
- Database models and repositories
- Image storage in Cloud Storage
- Thumbnail generation
- Comic metadata API

**Week 5-6: Frontend Core**
- User authentication UI (login, register)
- Comic library browser (grid view)
- Comic reader (single page, zoom, pan, swipe)
- Reading progress tracking
- Basic responsive design

**Deliverable**: Working comic reader with user accounts

---

### 9.2 Phase 2: Enhanced Features (Weeks 7-10)

**Week 7-8: Advanced Reader**
- Double page spread mode
- Continuous scroll mode
- Page thumbnails grid
- Jump to page
- Favorites and bookmarks
- Enhanced library filtering

**Week 9-10: Offline & History**
- Offline download feature
- IndexedDB storage
- Reading history
- Reading statistics
- UI polish

**Deliverable**: Full-featured comic reader

---

### 9.3 Phase 3: OCR/TTS (Weeks 11-16)

**Week 11-12: OCR Integration**
- Async job processing setup
- GCP Vision API integration
- OCR result storage
- Processing status tracking
- Admin UI for triggering OCR

**Week 13-14: LLM Panel Detection**
- Gemini API integration
- Panel/bubble detection logic
- Text region extraction
- Coordinate mapping

**Week 15-16: TTS & Playback**
- GCP Text-to-Speech integration
- Multi-language audio generation
- Audio storage
- Frontend audio player
- Tap-to-play functionality
- Auto-play modes
- Mark problematic comics

**Deliverable**: Full OCR/TTS functionality

---

### 9.4 Phase 4: Admin & Polish (Weeks 17-20)

**Week 17-18: Admin UI**
- Admin dashboard
- Content management (CRUD)
- Manual tagging
- Problematic comics review
- Reprocess OCR/TTS
- User management

**Week 19: Ratings & Analytics**
- Ratings and comments
- Analytics event tracking
- Analytics dashboard
- Charts and visualizations

**Week 20: Final Polish**
- UI/UX improvements
- Performance optimization
- Bug fixes
- Testing (unit, integration, E2E)
- Documentation

**Deliverable**: Production-ready application

---

### 9.5 Future Enhancements (Post-Launch)

**Android Native App**:
- Kotlin + Jetpack Compose or Flutter
- Better performance for comic rendering
- Native gesture support
- Better offline capabilities
- Timeline: 2-3 months

**Advanced Features**:
- Search within comic text (full-text search with OCR data)
- Reading recommendations (based on history, ratings)
- Social features (share favorites with family)
- Multiple libraries (personal vs. shared)
- Parental controls (if needed later)
- Import from other sources (URLs, Google Drive)

---

## 10. Testing Strategy

### 10.1 Backend Testing

**Unit Tests**:
- JUnit 5 + Mockito
- Test services, repositories
- Test archive extraction logic
- Test OCR/TTS integration (mocked APIs)
- Target coverage: 70%+

**Integration Tests**:
- Spring Boot Test (`@SpringBootTest`)
- Test API endpoints
- Test database interactions
- Test file upload/download
- Use Testcontainers for PostgreSQL

**Example**:
```java
@SpringBootTest
@AutoConfigureMockMvc
class ComicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnComicsList() throws Exception {
        mockMvc.perform(get("/api/comics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }
}
```

### 10.2 Frontend Testing

**Unit Tests**:
- Jest + React Testing Library
- Test components
- Test utilities and helpers
- Target coverage: 60%+

**Integration Tests**:
- Test user flows
- Test API integration (mocked backend)

**E2E Tests** (optional):
- Playwright or Cypress
- Test critical user journeys:
  - Login → browse → read comic → save progress
  - Upload comic → tag → view in library

### 10.3 Manual Testing

**Device Testing**:
- Test on actual tablets (10" Android)
- Test on smartphones (landscape)
- Test touch gestures
- Test audio playback

**Browser Testing**:
- Chrome, Firefox, Safari (if iOS later)
- Desktop and mobile browsers

---

## 11. Deployment

### 11.1 Development Environment

**Local Development**:
```
docker-compose.yml:
  - Spring Boot app (port 8080)
  - PostgreSQL (port 5432)
  - Redis (port 6379) [optional]
  - React dev server (port 3000)
```

**Environment Variables**:
```properties
# application-dev.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/cbr_reader
spring.datasource.username=dev
spring.datasource.password=dev
spring.cloud.gcp.storage.bucket=cbr-reader-comics-dev
google.cloud.vision.api-key=${GCP_API_KEY}
google.cloud.tts.api-key=${GCP_API_KEY}
```

### 11.2 Production Deployment (GCP)

**Option A: Cloud Run (Recommended)**

**Steps**:
1. Build Docker image:
```dockerfile
FROM eclipse-temurin:17-jre-alpine
COPY target/cbr-reader.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

2. Push to Google Container Registry:
```bash
docker build -t gcr.io/your-project/cbr-reader .
docker push gcr.io/your-project/cbr-reader
```

3. Deploy to Cloud Run:
```bash
gcloud run deploy cbr-reader \
  --image gcr.io/your-project/cbr-reader \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars "SPRING_PROFILES_ACTIVE=prod"
```

4. Configure Cloud SQL connection (via Cloud SQL Proxy)

5. Set up custom domain (optional)

**Option B: Compute Engine (VM)**

**Steps**:
1. Create e2-small instance
2. Install Docker
3. Run Docker Compose:
```yaml
version: '3.8'
services:
  app:
    image: gcr.io/your-project/cbr-reader
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
```

### 11.3 Frontend Deployment

**Build React app**:
```bash
npm run build
```

**Serve from Spring Boot**:
- Copy React build output to `src/main/resources/static`
- Spring Boot serves as static files
- Single deployment artifact

**Alternative: Separate hosting**:
- Firebase Hosting (free tier)
- Cloud Storage + Cloud CDN (static website)
- Vercel/Netlify (free tier)

---

## 12. Monitoring & Maintenance

### 12.1 Application Monitoring

**Spring Boot Actuator**:
- Health checks: `/actuator/health`
- Metrics: `/actuator/metrics`
- Integrate with GCP Cloud Monitoring

**Logging**:
- Logback (Spring Boot default)
- Structured JSON logging
- Send to GCP Cloud Logging
- Log levels: INFO for production, DEBUG for development

**Error Tracking**:
- Sentry (free tier for small projects)
- Captures exceptions and stack traces
- Email notifications for errors

**Uptime Monitoring**:
- GCP Cloud Monitoring (free tier)
- UptimeRobot (free for basic monitoring)
- Health check endpoint: `/actuator/health`

### 12.2 Performance Monitoring

**Metrics to Track**:
- API response times
- Database query performance
- OCR/TTS processing time
- Storage usage
- Bandwidth usage

**Tools**:
- GCP Cloud Monitoring
- Spring Boot Actuator metrics
- PostgreSQL slow query log

### 12.3 Maintenance Tasks

**Regular**:
- Database backups (automated by Cloud SQL)
- Review error logs (weekly)
- Monitor costs (monthly)
- Update dependencies (quarterly)

**As Needed**:
- Reprocess problematic comics
- Add new genres/tags
- User support (password resets, etc.)

---

## 13. Risks & Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| High OCR/TTS costs | Medium | High | Process on-demand only; use Standard voices; cache forever |
| Poor OCR accuracy for Ukrainian | Medium | Medium | Test with sample comics first; manual review process; consider alternative OCR engines |
| Belarusian TTS not available | High | Low | Use Russian as fallback; document limitation |
| Storage costs exceed budget | Low | Medium | Lifecycle policies; compress images; monitor usage |
| Slow comic loading on tablets | Medium | Medium | Image optimization; lazy loading; CDN; pre-loading |
| LLM panel detection errors | Medium | Medium | Allow manual correction; iterative prompt engineering; fallback to simple OCR |
| GCP free tier exceeded | Low | Medium | Monitor usage closely; alerts on thresholds; cost caps |
| User-uploaded malicious files | Low | High | File type validation; size limits; virus scanning (optional) |
| Database growth beyond plan | Low | Medium | Pagination; data archival; cleanup old analytics events |
| Gemini API rate limits | Low | Medium | Process in batches; retry logic; spread over time |

---

## 14. Open Questions & Decisions Needed

### 14.1 Technical Decisions

**1. React vs. Vanilla JS**:
- **Recommendation**: React (better maintainability, ecosystem)
- **Decision**: ?

**2. Redis in production**:
- **Options**:
  - Skip initially (use in-memory cache)
  - Use Memorystore ($30/month)
  - Self-hosted Redis in Docker on VM
- **Recommendation**: Skip initially, add later if performance issues
- **Decision**: ?

**3. LLM for panel detection**:
- **Options**:
  - Gemini (GCP native, cheaper)
  - GPT-4 Vision (better accuracy, higher cost)
  - Skip LLM, use OCR only (cheaper, less accurate panel detection)
- **Recommendation**: Start with Gemini, test accuracy, upgrade to GPT-4 if needed
- **Decision**: ?

**4. TTS voice quality**:
- **Options**:
  - Standard voices ($4/1M chars) - good quality
  - WaveNet voices ($16/1M chars) - very natural
  - Neural2 voices ($16/1M chars) - best quality
- **Recommendation**: Standard voices for MVP, upgrade later
- **Decision**: ?

**5. Admin UI technology**:
- **Options**:
  - Spring MVC + Thymeleaf (server-side, simpler)
  - Separate React admin app (more work, better UX)
- **Recommendation**: Thymeleaf for MVP (faster development)
- **Decision**: ?

### 14.2 Feature Prioritization

**1. Must-have for MVP**:
- User authentication ✓
- Comic upload & library ✓
- Basic reader (single page, zoom) ✓
- Reading progress ✓

**2. Nice-to-have for MVP**:
- Double page mode
- Continuous scroll
- Offline download
- Favorites

**Decision**: Confirm MVP scope?

### 14.3 Deployment Strategy

**1. Hosting**:
- **Recommendation**: Cloud Run (cheapest, easiest for low traffic)
- **Alternative**: Compute Engine VM (more predictable costs)
- **Decision**: ?

**2. Domain**:
- Use custom domain (e.g., comics.yourfamily.com)?
- Or use default Cloud Run URL?
- **Decision**: ?

---

## 15. Next Steps

### 15.1 Immediate Actions

1. **Validate Architecture**
   - Review this analysis
   - Confirm technical decisions
   - Prioritize features for MVP

2. **Set Up GCP Project**
   - Create new GCP project
   - Enable APIs (Cloud Storage, Cloud SQL, Vision API, Text-to-Speech)
   - Set up billing alerts ($50/month threshold)
   - Create service accounts and credentials

3. **Initialize Projects**
   - Create Spring Boot project (Spring Initializr)
   - Create React project (Vite or CRA)
   - Set up Git repository
   - Create Docker Compose for local development

4. **Proof of Concept**
   - Test CBR/CBZ extraction
   - Test GCP Vision API with sample Ukrainian comic page
   - Test GCP Text-to-Speech with Ukrainian text
   - Test Gemini panel detection with sample page

### 15.2 Week 1 Tasks

**Backend**:
- [ ] Initialize Spring Boot project (Java 17, Spring Boot 3.2)
- [ ] Add dependencies (Web, Security, JPA, PostgreSQL, Cloud Storage)
- [ ] Configure PostgreSQL locally
- [ ] Create User entity and repository
- [ ] Implement JWT authentication
- [ ] Create basic REST API structure

**Frontend**:
- [ ] Initialize React project
- [ ] Set up routing (React Router)
- [ ] Create login/register pages
- [ ] Implement JWT storage and axios interceptors
- [ ] Create basic layout (header, sidebar, main content)

**Infrastructure**:
- [ ] Create GCP project
- [ ] Set up Cloud Storage bucket
- [ ] Create Cloud SQL instance (db-f1-micro)
- [ ] Configure local development environment (Docker Compose)

**Testing**:
- [ ] Test CBR extraction with junrar
- [ ] Test CBZ extraction with zip4j
- [ ] Test GCP Vision API (sample request)
- [ ] Test GCP TTS API (Ukrainian sample)

---

## 16. Conclusion

This comic reader application is **highly feasible** as a family hobby project with the specified budget. The recommended approach is:

**Core Stack**:
- **Backend**: Java 17 + Spring Boot 3.x
- **Frontend**: React + TypeScript (or Vanilla JS)
- **Database**: PostgreSQL (Cloud SQL)
- **Storage**: GCP Cloud Storage
- **Cloud**: Google Cloud Platform
- **OCR**: GCP Vision API
- **TTS**: GCP Text-to-Speech API
- **LLM**: Gemini API (panel detection)

**Deployment**:
- Cloud Run (serverless, pay-per-use)
- Estimated cost: $10-20/month ongoing + $150-400 one-time OCR/TTS processing

**Timeline**:
- MVP (Phase 1): 6 weeks
- Full web app (Phases 1-4): 20 weeks (~5 months)
- Android native: Additional 2-3 months

**Key Success Factors**:
1. Start with MVP (core reading experience)
2. Test OCR/TTS with sample Ukrainian comics early
3. Process audio on-demand to control costs
4. Cache all OCR/TTS results permanently
5. Use Standard TTS voices (good enough, much cheaper)
6. Monitor GCP costs closely

**Biggest Risks**:
1. OCR accuracy for Ukrainian (mitigation: test early, manual review process)
2. TTS costs (mitigation: on-demand processing, Standard voices, cache forever)
3. Belarusian TTS availability (mitigation: fallback to Russian)

This project aligns well with your skills (Java/Spring, React, GCP) and is achievable as a solo developer with Claude Code assistance. The phased approach allows you to deliver value incrementally and control costs effectively.

Ready to start coding?
