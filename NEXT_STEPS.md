# Next Steps - CBR Reader Development

## Immediate Actions (Week 1)

### 1. Complete MCP Setup
- [ ] Download Google Drive OAuth credentials from Google Cloud Console
- [ ] Configure Google Drive MCP with credentials
- [ ] Test all MCP servers are functioning

### 2. Backend Initialization
- [ ] Initialize Spring Boot project using Spring Initializr
  - Dependencies: Web, Security, Data JPA, PostgreSQL, Actuator
  - Java 17+
  - Spring Boot 3.2+
- [ ] Add GCP dependencies (Cloud Storage, Vision API, Text-to-Speech)
- [ ] Add archive handling libraries (junrar, zip4j)
- [ ] Configure PostgreSQL connection
- [ ] Set up Liquibase for database migrations
- [ ] Create initial database schema

### 3. Frontend Initialization
- [ ] Create React project with Vite
- [ ] Install dependencies:
  - React Router
  - Axios
  - Tailwind CSS
  - jszip / libarchive.js
  - localforage
- [ ] Set up project structure (components, pages, services)
- [ ] Configure proxy for local API calls

### 4. Infrastructure Setup
- [ ] Start local PostgreSQL with Docker Compose
- [ ] Create GCP project: "cbr-reader"
- [ ] Enable required GCP APIs:
  - Cloud Storage API
  - Cloud SQL Admin API
  - Cloud Vision API
  - Cloud Text-to-Speech API
  - Cloud AI Platform (for Gemini)
- [ ] Create GCP service account and download credentials
- [ ] Set up billing alerts ($50/month threshold)
- [ ] Create Cloud Storage bucket for comics

### 5. Proof of Concept Tests
- [ ] Test CBR extraction with sample file
- [ ] Test CBZ extraction with sample file
- [ ] Test GCP Vision API with Ukrainian comic page
- [ ] Test GCP Text-to-Speech with Ukrainian text
- [ ] Test Gemini API with sample comic page (panel detection)

## Week 2-3: MVP Backend

### Authentication & User Management
- [ ] Implement User entity and repository
- [ ] Configure Spring Security with JWT
- [ ] Create authentication endpoints (register, login)
- [ ] Implement JWT token generation and validation
- [ ] Add password encryption (BCrypt)

### Comic Management
- [ ] Create Comic entity and repository
- [ ] Create ComicPage entity and repository
- [ ] Implement file upload endpoint (multipart)
- [ ] Implement CBR/CBZ extraction service
- [ ] Implement image storage (GCP Cloud Storage)
- [ ] Generate thumbnails for pages
- [ ] Create comic metadata extraction logic
- [ ] Implement comic listing API (paginated)
- [ ] Implement comic details API
- [ ] Implement page serving API

### Reading Progress
- [ ] Create ReadingProgress entity
- [ ] Implement progress tracking endpoints
- [ ] Test progress sync

## Week 4-6: MVP Frontend

### Authentication UI
- [ ] Create login page
- [ ] Create registration page
- [ ] Implement JWT storage (localStorage)
- [ ] Configure Axios interceptors for auth
- [ ] Create protected route wrapper
- [ ] Create user profile page

### Library Browser
- [ ] Create comic grid component
- [ ] Implement search functionality
- [ ] Implement filtering (series, author)
- [ ] Implement sorting
- [ ] Create comic detail view
- [ ] Add pagination

### Comic Reader
- [ ] Create canvas-based reader component
- [ ] Implement single page view
- [ ] Implement zoom and pan gestures
- [ ] Implement swipe navigation
- [ ] Add page indicator
- [ ] Add navigation controls (prev/next)
- [ ] Integrate reading progress API
- [ ] Test on tablet device

## Phase 2: Enhanced Features (Week 7-10)

### Advanced Reader Modes
- [ ] Double page spread mode
- [ ] Continuous scroll mode
- [ ] Reading direction toggle
- [ ] Page thumbnails view
- [ ] Jump to page functionality

### User Features
- [ ] Favorites functionality
- [ ] Bookmarks (multiple per comic)
- [ ] Reading history
- [ ] Offline download (IndexedDB)
- [ ] Download manager UI

## Phase 3: OCR/TTS (Week 11-16)

### Backend OCR/TTS
- [ ] Create async job processing setup
- [ ] Implement GCP Vision API integration
- [ ] Implement Gemini API integration (panel detection)
- [ ] Create OCRResult and TextRegion entities
- [ ] Implement TTS generation service
- [ ] Store audio files in Cloud Storage
- [ ] Create processing status tracking
- [ ] Implement "mark problematic" endpoint

### Frontend Audio
- [ ] Create audio player component
- [ ] Implement tap-to-play functionality
- [ ] Add auto-play mode
- [ ] Add continuous reading mode
- [ ] Display processing status
- [ ] Add "process audio" button
- [ ] Add "report problem" button

## Phase 4: Admin & Polish (Week 17-20)

### Admin UI
- [ ] Create admin dashboard (Thymeleaf)
- [ ] Implement content upload interface
- [ ] Create tagging UI (genres, series, authors)
- [ ] Build problematic comics review page
- [ ] Add reprocess OCR/TTS functionality
- [ ] Create user management page

### Analytics
- [ ] Implement analytics event tracking
- [ ] Create analytics dashboard
- [ ] Add charts (reading stats, popular content)
- [ ] Track OCR/TTS usage

### Polish
- [ ] Ratings and comments UI
- [ ] Responsive design testing
- [ ] Performance optimization
- [ ] Error handling and loading states
- [ ] Accessibility improvements
- [ ] Cross-browser testing

## Deployment

### Staging Environment
- [ ] Create Cloud SQL instance (db-f1-micro)
- [ ] Deploy backend to Cloud Run
- [ ] Configure environment variables
- [ ] Set up Cloud CDN
- [ ] Test deployment

### Production
- [ ] Review security settings
- [ ] Set up monitoring (Cloud Monitoring)
- [ ] Configure error tracking (Sentry)
- [ ] Set up automated backups
- [ ] Deploy to production
- [ ] Monitor costs

## Testing

### Backend Tests
- [ ] Unit tests for services (70% coverage)
- [ ] Integration tests for API endpoints
- [ ] Test archive extraction
- [ ] Test GCP integrations (mocked)

### Frontend Tests
- [ ] Component unit tests
- [ ] Integration tests
- [ ] E2E tests for critical flows (Playwright)

## Documentation

- [ ] API documentation (Swagger/OpenAPI)
- [ ] Deployment guide
- [ ] User guide
- [ ] Admin guide
- [ ] Update README with setup instructions

## Future Enhancements

### Android Native App
- [ ] Choose technology (Kotlin or Flutter)
- [ ] Port core reader functionality
- [ ] Implement native gestures
- [ ] Optimize performance
- [ ] Publish to Play Store (family sharing)

### Advanced Features
- [ ] Full-text search within comics (using OCR data)
- [ ] Reading recommendations
- [ ] Multiple libraries (personal vs shared)
- [ ] Import from Google Drive
- [ ] Backup/restore functionality

---

## Current Status

✅ **Completed**:
- Technical analysis and architecture design
- Project decisions documented
- GitHub repository created
- MCP servers installed (7/7)
- Initial project files created

🔄 **In Progress**:
- Google Drive MCP configuration (waiting for OAuth credentials)
- Project initialization

⏳ **Next Up**:
- Backend initialization (Spring Boot)
- Frontend initialization (React)
- Local development environment setup

---

**Last Updated**: 2025-10-20
