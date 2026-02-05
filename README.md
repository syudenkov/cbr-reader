# CBR Viewer

A full-featured web-based comic book reader with AI-powered text-to-speech. Upload CBR/CBZ archives, read them in your browser, track progress across sessions, rate your collection, and generate spoken audio from comic dialogue using OCR and TTS.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, TypeScript 5.7, Vite 6 |
| UI | Material-UI 6, Emotion (dark theme) |
| State | Redux Toolkit 2.5, React Redux 9 |
| Backend | Java 21, Spring Boot 3.2 |
| Database | SQLite 3.44 (file-based, zero config) |
| Security | Spring Security, BCrypt, AES-256-GCM |
| Archives | JunRar 7.5.5 (CBR), Java ZIP (CBZ) |
| AI/OCR | OpenAI Vision (GPT-4o), Claude Vision (fallback) |
| TTS | Minimax API (speech synthesis) |
| PWA | vite-plugin-pwa (installable, auto-update) |

## Prerequisites

- **Java 21** (OpenJDK recommended)
- **Node.js 18+** and npm
- **Gradle** (wrapper included, no global install needed)

## Quick Start

### Development (two terminals)

```bash
# Terminal 1 — Backend (port 4433)
cd backend
./gradlew bootRun

# Terminal 2 — Frontend dev server (port 3344, proxies /api to backend)
cd frontend
npm install
npm run dev
```

Open [http://localhost:3344](http://localhost:3344) in your browser.

### Production (single JAR)

```bash
cd backend
./gradlew build
java -jar build/libs/cbr-viewer-1.0.0.jar
```

The Gradle build automatically runs `npm install`, `npm run build`, and copies the frontend into the JAR's static resources. The application serves everything from port **4433**.

## Build Commands

| Command | Location | Description |
|---------|----------|-------------|
| `npm run dev` | `frontend/` | Vite dev server with hot reload (port 3344) |
| `npm run build` | `frontend/` | TypeScript check + production build |
| `npm run test` | `frontend/` | Run Jest unit tests |
| `./gradlew bootRun` | `backend/` | Start Spring Boot in dev mode (port 4433) |
| `./gradlew build` | `backend/` | Build fat JAR (includes frontend) |
| `./gradlew test` | `backend/` | Run JUnit + Spring Boot tests |

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SQLITE_PATH` | `./data/cbr_viewer.db` | SQLite database file location |
| `COMICS_PATH` | `./backend/storage/comics` | Directory for uploaded comic files |
| `AUDIO_PATH` | `./backend/storage/audio` | Directory for generated TTS audio |
| `CACHE_PATH` | `./backend/storage/cache` | Extracted page image cache |
| `CORS_ORIGINS` | `http://localhost:3344` | Allowed CORS origins (comma-separated) |
| `ENCRYPTION_MASTER_KEY` | Dev default | AES-256 key for API key encryption (64 hex chars) |
| `COOKIE_SECURE` | `false` | Set `true` in production (HTTPS) |

Generate a production encryption key:

```bash
openssl rand -hex 32
```

## Project Structure

```
cbr_viewer/
├── frontend/                    React + TypeScript SPA
│   ├── src/
│   │   ├── pages/              Route-level page components
│   │   ├── components/         UI components by feature area
│   │   │   ├── auth/           Route protection
│   │   │   ├── layout/         App shell and navbar
│   │   │   ├── library/        Comic cards, upload, ratings
│   │   │   ├── viewer/         Page/scroll renderer, controls, audio
│   │   │   └── admin/          Admin panel components
│   │   ├── store/              Redux store and slices
│   │   ├── services/           Axios API service layer
│   │   ├── hooks/              Custom React hooks
│   │   └── theme/              MUI dark theme configuration
│   └── vite.config.ts          Build config, PWA, dev proxy
│
├── backend/                     Spring Boot Java application
│   └── src/main/java/com/cbrviewer/
│       ├── controller/         REST API endpoints (7 controllers)
│       ├── service/            Business logic (13 services)
│       ├── model/              JPA entities (10 tables)
│       ├── repository/         Spring Data JPA repositories
│       ├── dto/                Request/response DTOs
│       ├── config/             Security, async, CORS, cache
│       ├── client/             External API clients (OpenAI, Claude, Minimax)
│       └── exception/          Custom exceptions + global handler
│
├── docs/                        Documentation
│   ├── architecture.md         System design and data model
│   ├── frontend.md             Frontend architecture and components
│   ├── backend.md              Backend services and API reference
│   └── testplan.md             Manual test plan
│
└── storage/                     Runtime file storage
    ├── comics/                 Uploaded CBR/CBZ files
    ├── cache/                  Extracted page images
    └── audio/                  Generated TTS MP3 files
```

## Key Features

- **Comic Library** — Upload CBR/CBZ files, browse covers, search and sort
- **Dual-Mode Viewer** — Single-page or continuous scroll, zoom 50–200%, fullscreen
- **Reading Progress** — Auto-saved per user, visual progress badges on covers
- **Ratings** — 5-star per-user ratings with community averages
- **TTS Pipeline** — Async OCR + speech synthesis with progress tracking
- **Admin Panel** — User management, LLM configuration, audit logs
- **PWA** — Installable on desktop/mobile, standalone mode

## Documentation

See the [`docs/`](docs/) directory for detailed documentation:

- **[Architecture](docs/architecture.md)** — System design, auth flow, database schema, security model
- **[Frontend](docs/frontend.md)** — Components, Redux state, routes, keyboard shortcuts
- **[Backend](docs/backend.md)** — Controllers, services, entities, API reference
- **[Test Plan](docs/testplan.md)** — Manual test cases organized by feature
