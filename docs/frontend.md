# Frontend Architecture

## Technology Choices

| Technology | Version | Role |
|-----------|---------|------|
| React | 18.3 | UI framework (function components + hooks) |
| TypeScript | 5.7 | Type safety across the entire frontend |
| Vite | 6.0 | Build tool with hot module replacement |
| Redux Toolkit | 2.5 | Predictable state management with async thunks |
| React Router | 6.28 | Client-side routing |
| Material-UI | 6.2 | Component library (dark theme) |
| Emotion | 11.14 | CSS-in-JS (MUI's styling engine) |
| Axios | 1.7 | HTTP client with interceptors |
| vite-plugin-pwa | 0.21 | Service worker and installability |

**Why these choices:**
- **Redux Toolkit** over plain React context because the app has cross-cutting state (auth, viewer, TTS jobs) that multiple unrelated components need to access.
- **MUI** provides a complete dark-theme component set out of the box, avoiding custom CSS for standard UI patterns.
- **Vite** for sub-second hot reloads during development and optimized production builds with automatic code splitting.

## Application Routes

| Path | Page Component | Auth Required | Description |
|------|---------------|---------------|-------------|
| `/` | — | — | Redirects to `/library` (authenticated) or `/login` |
| `/login` | `LoginPage` | No | Username/password login form |
| `/library` | `LibraryPage` | Yes | Comic grid with upload, delete, ratings, progress |
| `/viewer/:fileId` | `ViewerPage` | Yes | Comic reader with page/scroll modes |

A `ProtectedRoute` wrapper component checks Redux auth state before rendering protected pages. While the initial auth check is in progress (`loading: true`), it renders nothing to prevent a flash redirect to the login page.

## Component Hierarchy

```
App (Router + ThemeProvider)
│
├── LoginPage
│   └── Card with TextField, Button, Alert
│
├── LibraryPage
│   └── AppLayout (navbar with title, username, logout)
│       ├── Toolbar (upload button)
│       ├── Grid
│       │   └── ComicCard  ×N
│       │       ├── CardMedia (cover image with skeleton loader)
│       │       ├── ProgressBadge (circular %, top-right overlay)
│       │       ├── RatingWidget (5-star with average)
│       │       └── IconButton (delete)
│       ├── UploadDialog (file picker, progress bar, validation)
│       ├── DeleteConfirmDialog (confirmation with filename)
│       └── Snackbar (success/error notifications)
│
└── ViewerPage
    ├── ViewerToolbar (sticky top)
    │   ├── Back button → /library
    │   ├── Title + "Page X / Y"
    │   ├── View mode toggle (page ↔ scroll)
    │   └── Fullscreen toggle
    │
    ├── PageRenderer (page mode)
    │   └── Single <img> with CSS transform zoom
    │
    ├── ScrollRenderer (scroll mode)
    │   └── All pages as <img> with IntersectionObserver
    │
    ├── AudioPlayer (if TTS results exist)
    │   ├── Play/Pause button
    │   ├── Progress slider + time display
    │   └── Volume control with mute
    │
    ├── TtsJobButton (create/monitor TTS job)
    │
    └── ViewerControls (page mode only, sticky bottom)
        ├── Previous / Next buttons
        ├── Page counter
        └── Zoom slider (50%–200%)
```

## Redux State Management

The store has six slices. Each slice is self-contained with its own async thunks.

### Store Structure

```
store
├── auth        Authentication state
├── files       Library file list and upload state
├── viewer      Viewer page state, progress, preloading
├── tts         TTS job tracking and audio playback
├── admin       Admin panel data (placeholder)
└── ui          Theme toggle and notifications (placeholder)
```

### auth Slice

Manages login/logout and session restoration.

**State:**

```typescript
{
  isAuthenticated: boolean   // true after successful login or fetchCurrentUser
  user: {                    // null when not authenticated
    id: number
    username: string
    email: string
    role: 'USER' | 'ADMIN'
  } | null
  loading: boolean           // starts true to prevent redirect race condition
  error: string | null       // login error message
}
```

**Thunks:**
- `login({username, password})` — POST `/api/auth/login`, stores user on success
- `logout()` — POST `/api/auth/logout`, clears state even if server call fails
- `fetchCurrentUser()` — GET `/api/auth/me`, restores session on page reload

### files Slice

Manages the comic library list, uploads, and deletions.

**State:**

```typescript
{
  items: ComicFile[]                    // all comics in library
  status: 'idle' | 'loading' | ...     // fetch status
  currentFile: ComicFile | null         // selected file
  uploadProgress: number                // 0–100
  uploadStatus: 'idle' | 'uploading' | 'succeeded' | 'failed'
  uploadError: string | null
  filters: {
    search: string
    sortBy: 'title' | 'uploadDate' | 'rating'
    sortOrder: 'asc' | 'desc'
  }
}
```

**Thunks:**
- `fetchFiles()` — GET `/api/files`, extracts `content` from paginated response
- `uploadFile(file, onProgress)` — POST `/api/files` with FormData, tracks upload percentage
- `deleteFile(fileId)` — DELETE `/api/files/{fileId}`, removes from local list on success

### viewer Slice

Manages the reader state including page navigation, zoom, fullscreen, and preloading.

**State:**

```typescript
{
  currentFile: ComicFile | null
  mode: 'page' | 'scroll'              // persisted to localStorage
  currentPage: number
  totalPages: number
  zoomLevel: number                     // 50–200, persisted to localStorage
  isFullscreen: boolean
  preloadedPages: Record<number, string> // pageNum → blob URL
  pageLoadingStatus: string
  fileLoadingStatus: string
  progressLoadingStatus: string
}
```

**Thunks:**
- `fetchFileMetadata(fileId)` — loads comic metadata
- `preloadPage({fileId, pageNumber})` — fetches page as blob, creates object URL
- `fetchReadingProgress(fileId)` — restores last page from server
- `updateReadingProgress({fileId, currentPage})` — saves current page (debounced 2s)

**localStorage persistence:**
- `viewer_zoom_level` — remembered across sessions
- `viewer_mode` — page or scroll preference remembered

### tts Slice

Tracks TTS job creation, polling, and audio playback state.

**State:**

```typescript
{
  jobs: Record<number, TtsJob>   // fileId → latest job
  activeJob: TtsJob | null
  currentAudio: {
    fileId: number | null
    pageNumber: number | null
    playing: boolean
    volume: number               // 0–100
    currentTime: number
    duration: number
  }
}
```

**Thunks:**
- `createTtsJob(fileId)` — POST `/api/tts/jobs`
- `pollJobStatus(jobId)` — GET `/api/tts/jobs/{jobId}` (called on 2s interval)

### Data Flow Example: Page Navigation

```
User presses → key
       │
       ▼
useKeyboardNavigation hook
       │
       ▼
dispatch(setCurrentPage(currentPage + 1))
       │
       ├──► viewer slice reducer updates state
       │
       ├──► ViewerPage re-renders, dispatches preloadPage()
       │         │
       │         ▼
       │    preloadPage thunk fetches blob from /api/files/{id}/page/{n}
       │         │
       │         ▼
       │    PageRenderer receives new imageUrl prop, displays image
       │
       └──► ViewerPage dispatches updateReadingProgress() (debounced 2s)
                 │
                 ▼
            PUT /api/progress/{fileId} saves server-side
```

## Services Layer

All services use a shared Axios instance configured in `axiosConfig.ts`:

```typescript
baseURL: '/api'
withCredentials: true          // include session cookie
'Content-Type': 'application/json'
```

| Service | Endpoints | Purpose |
|---------|-----------|---------|
| `authService` | login, logout, getCurrentUser | Session management |
| `fileService` | fetchFiles, uploadFile, deleteFile | Library CRUD |
| `viewerService` | fetchFileMetadata, loadPageImage | Page rendering (blob responses) |
| `progressService` | getAllProgress, getProgress, updateProgress | Reading position tracking |
| `ratingService` | submitRating, getAverageRating, getUserRating | Star ratings |
| `ttsService` | createTtsJob, getJobStatus, getAudioUrl | TTS pipeline |

**Image loading note:** `viewerService.loadPageImage()` uses `responseType: 'blob'` to receive binary image data. It creates object URLs via `URL.createObjectURL()` which are stored in Redux and revoked when the viewer unmounts.

## Theme and Styling

The application uses a **dark-only** Material-UI theme defined in `src/theme/theme.ts`.

### Color System

| Role | Color | Usage |
|------|-------|-------|
| Primary | `#2196f3` (Blue) | Buttons, links, active states |
| Secondary | `#ff5722` (Orange-red) | Accent elements |
| Background | `#0a0a0a` | Page background |
| Paper | `#1a1a1a` | Cards and dialogs |
| Text primary | `#ffffff` | Main text |
| Text secondary | `#9a9a9a` | Supporting text |
| Success | `#4caf50` | Success messages |
| Error | `#f44336` | Errors and warnings |

### Typography

- **Font family:** Inter, Roboto, Helvetica, Arial, sans-serif
- **Headings:** Bold (700) or semi-bold (600), scaled from 2.5rem (h1) to 1rem (h6)
- **Body:** Regular weight (400), 1rem and 0.875rem
- **Buttons:** textTransform set to `none` (overrides MUI's default uppercase)

### Component Overrides

- `MuiButton` — Default variant `contained`, border-radius 4px
- `MuiCard` — Border-radius 8px
- `MuiPaper` / `MuiAppBar` — Background images removed for flat appearance

## Keyboard Shortcuts

### Viewer Navigation

| Key | Action |
|-----|--------|
| `←` Arrow Left | Previous page |
| `→` Arrow Right | Next page |
| `Home` | Jump to first page |
| `End` | Jump to last page |
| `+` or `=` | Zoom in (max 200%) |
| `-` or `_` | Zoom out (min 50%) |
| `F` or `F11` | Toggle fullscreen |
| `Escape` | Exit fullscreen |

### Audio Player

| Key | Action |
|-----|--------|
| `Space` | Play / Pause (suppressed when input focused) |
| `M` | Mute / Unmute |

Keyboard shortcuts are implemented via the `useKeyboardNavigation` custom hook in `src/hooks/useKeyboardNavigation.ts`, which registers global `keydown` event listeners.

## PWA Capabilities

The app is configured as a Progressive Web App via `vite-plugin-pwa`:

- **Installable** — Meets PWA install criteria with a web manifest, service worker, and icons (192px and 512px).
- **Standalone mode** — Opens in its own window without browser chrome (`display: standalone`).
- **Auto-update** — Service worker updates automatically when a new build is deployed (`registerType: 'autoUpdate'`).
- **Theme integration** — Theme color `#2196f3`, background `#0a0a0a` match the dark UI.

### Build Output and Code Splitting

Vite produces optimized chunks with manual splitting:

| Chunk | Contents |
|-------|----------|
| `react-vendor` | react, react-dom, react-router-dom |
| `redux-vendor` | @reduxjs/toolkit, react-redux |
| `mui-vendor` | @mui/material, @mui/icons-material |
| `main` | Application code |

This splitting ensures that vendor libraries (which change infrequently) are cached separately from application code.
