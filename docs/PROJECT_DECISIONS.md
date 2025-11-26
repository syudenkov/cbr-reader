# CBR Reader - Project Decisions & Architecture

## Project Overview
**Name**: CBR/CBZ/JPG Reader
**Purpose**: Family-focused comic book reader for tablets with multi-language OCR and TTS
**Repository**: https://github.com/syudenkov/cbr-reader
**Budget**: $20-50/month (hobby project)
**Target Users**: 10 concurrent users (family)

## Key Technical Decisions

### Technology Stack
- **Backend**: Java 17+, Spring Boot 3.x
- **Frontend**: React (chosen over Vanilla JS for maintainability)
- **Database**: PostgreSQL (Cloud SQL on GCP)
- **Cache**: Skip Redis initially, use in-memory cache (add later if needed)
- **Cloud Provider**: Google Cloud Platform (GCP)
- **Deployment**: Cloud Run (serverless, cost-effective)

### External Services
- **OCR**: Google Cloud Vision API
- **TTS**: Google Cloud Text-to-Speech API (Standard voices, not Neural2)
- **LLM**: Google Gemini API (panel/bubble detection)

### Language Support
- **Primary**: Ukrainian
- **Additional**: English, Russian, Belarusian
- **Note**: Belarusian TTS may fall back to Russian if not available

### Architecture Decisions

#### Reading Features (All Modes)
- Single page view
- Double page spread (landscape)
- Continuous scroll mode
- Vertical/horizontal reading direction
- Zoom and pan (essential)
- Page thumbnails/grid view
- Table of contents
- Jump to page number

#### OCR/TTS Strategy
- **Processing**: On-demand only (cost optimization)
- **Storage**: Permanent storage of OCR results and audio files
- **Panel Detection**: LLM-based (Gemini) for speech bubble identification
- **Voice Quality**: Standard voices ($4/1M chars vs $16/1M for Neural2)
- **Error Handling**: Mark problematic comics for admin review

#### Storage Strategy
- **Initial Library**: 600+ comics (30GB)
- **Target**: 10k comics (500GB)
- **File Size Range**: 2-200MB per comic
- **Archive Format**: CBR (RAR) and CBZ (ZIP)
- **Lifecycle**: Archive rarely accessed content to Nearline storage after 1 year

#### Admin & Content Management
- **Upload**: Manual via admin UI
- **Scanning**: Backend scans storage directory for new files
- **Tagging**: Manual (genres, series, authors)
- **Moderation**: No pre-moderation required
- **UI**: Spring MVC + Thymeleaf (simpler than separate React admin)

### Cost Estimates

#### Monthly Infrastructure
- Cloud Run: $0-10/month (free tier covers low traffic)
- Cloud SQL (db-f1-micro): $9-10/month
- Cloud Storage (30GB initial): $0.60/month
- Redis: Skipped initially ($0/month, save $30/month)
- **Total**: ~$10-20/month

#### One-Time OCR/TTS Processing (600 comics)
- Cloud Vision API: $27 (18,000 pages)
- Gemini API: $36 (panel detection)
- Text-to-Speech (Standard): $86 (4 languages)
- **Total**: ~$149 (one-time)

#### Full Library (10k comics)
- Total one-time processing: ~$2,500 (Standard voices)
- Strategy: Spread processing over time, process on-demand

### Development Phases

#### Phase 1: MVP (6 weeks)
- User authentication (JWT)
- Comic upload & library browser
- Basic reader (single page, zoom, pan, swipe)
- Reading progress tracking
- PostgreSQL + Cloud Storage integration

#### Phase 2: Enhanced Reading (3-4 weeks)
- All reading modes (double page, continuous scroll)
- Page thumbnails & navigation
- Favorites & bookmarks
- Reading history
- Offline download (IndexedDB)

#### Phase 3: OCR/TTS (4-6 weeks)
- Async OCR processing (GCP Vision API)
- LLM panel detection (Gemini)
- TTS generation (4 languages)
- Audio playback (tap panel, auto-play, continuous)
- Mark problematic comics

#### Phase 4: Admin & Polish (3-4 weeks)
- Admin web UI (content management)
- Manual tagging
- Ratings & comments
- Analytics dashboard
- UI/UX polish

#### Future: Android Native
- Timeline: 2-3 months post-web launch
- Technology: Kotlin + Jetpack Compose or Flutter
- Focus: Better performance, native gestures

### Feature Decisions

#### Must-Have for MVP
- User authentication ✓
- Comic upload & library ✓
- Basic reader (single page, zoom) ✓
- Reading progress ✓

#### Phase 2 (Enhanced)
- All reading modes
- Offline download
- Favorites & history

#### Phase 3 (Audio)
- OCR/TTS integration

#### Phase 4 (Admin)
- Admin UI, analytics, ratings

#### Excluded Features
- Parental controls (not needed for family use)
- Age ratings (not needed)
- Reading time limits (not needed)
- Social features (no sharing with friends)
- Gamification (no badges/achievements)
- Monetization (free service)

### User Features

#### Accounts & Profiles
- Individual user profiles (not anonymous)
- No separate parent/child accounts
- No family account features
- Cross-device progress sync

#### Content Features
- Favorites
- Bookmarks (multiple per comic)
- Reading history
- Ratings & comments
- Content filtering (by tags, not parental controls)

#### Analytics Tracking
- Reading completion rates
- Popular content
- User engagement time
- OCR/TTS usage statistics

### Security & Compliance

#### Authentication
- JWT-based (stateless)
- BCrypt password hashing
- Role-based access: USER, ADMIN

#### Data Privacy
- No COPPA compliance needed (family only)
- No GDPR requirements (no EU focus)
- Minimal data collection
- No third-party tracking

#### Content Security
- File type validation (CBR, CBZ, JPG only)
- File size limits (max 200MB)
- No copyright/DMCA considerations (personal use)

### Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| High OCR/TTS costs | On-demand processing, Standard voices, permanent caching |
| Poor Ukrainian OCR accuracy | Test early with samples, manual review process |
| Belarusian TTS unavailable | Fallback to Russian voices |
| Storage costs | Lifecycle policies, image compression, monitoring |
| LLM panel detection errors | Allow manual correction, iterative prompting |

### Open Questions (Resolved)
- ✅ React vs Vanilla JS: **React** (better maintainability)
- ✅ Redis in production: **Skip initially** (save $30/month)
- ✅ LLM for panels: **Gemini** (GCP native, cheaper)
- ✅ TTS quality: **Standard voices** (good enough, $4 vs $16 per 1M chars)
- ✅ Admin UI: **Thymeleaf** (faster development)
- ✅ Hosting: **Cloud Run** (cheapest for low traffic)

### Development Environment

#### Local Setup
- Docker Compose for PostgreSQL
- Spring Boot (port 8080)
- React dev server (port 3000)
- No Redis locally (in-memory cache)

#### Required Tools
- Java 17+
- Node.js 18+
- Docker & Docker Compose
- GCP SDK (gcloud)
- Git

### Database Schema Highlights
- **Users**: Authentication, profiles, preferences
- **Comics**: Metadata, file paths, series info
- **Comic Pages**: Individual page tracking, OCR status
- **OCR Results**: Full text, language, confidence
- **Text Regions**: Panel/bubble coordinates, audio URLs
- **Reading Progress**: Current page, completion status
- **Favorites, Ratings, Analytics**: User engagement tracking

### API Design
- RESTful API (Spring Boot)
- JWT authentication
- Endpoints for: auth, comics, pages, regions, audio, progress, favorites, ratings, admin
- Pagination for large result sets
- Async processing for OCR/TTS

### Next Steps
1. Initialize Spring Boot project
2. Set up local PostgreSQL
3. Create React frontend
4. Implement authentication
5. Build comic upload & extraction
6. Create basic reader UI
7. Test with sample comics

---

**Last Updated**: 2025-10-20
**Decision Authority**: Solo developer (syudenkov) + Claude Code
