# CBR Reader

A family-focused comic book reader for tablets with multi-language OCR and text-to-speech support.

## Overview

CBR Reader is a web application designed for reading digital comics (CBR/CBZ/JPG formats) on tablets, with support for on-demand OCR and multi-language text-to-speech narration. Built for family use with support for Ukrainian, English, Russian, and Belarusian languages.

## Features

### Core Reading
- Support for CBR, CBZ, and JPG formats
- Multiple reading modes:
  - Single page view
  - Double page spread (landscape)
  - Continuous scroll
- Touch gestures: pinch-to-zoom, pan, swipe
- Page navigation: thumbnails, table of contents, jump to page
- Reading progress tracking with cross-device sync
- Offline download support

### Audio Features (OCR + TTS)
- On-demand OCR processing
- AI-powered panel and speech bubble detection
- Multi-language text-to-speech (Ukrainian, English, Russian, Belarusian)
- Multiple playback modes:
  - Tap individual panels
  - Auto-play entire page
  - Continuous reading mode
- Permanent caching of audio files

### User Features
- User authentication and profiles
- Favorites and bookmarks
- Reading history
- Ratings and comments
- Content filtering

### Admin Features
- Content management dashboard
- Manual tagging (genres, series, authors)
- Review and reprocess problematic OCR
- Analytics dashboard

## Technology Stack

### Backend
- Java 17+
- Spring Boot 3.x
- PostgreSQL
- Google Cloud Platform (GCP)
  - Cloud Run (deployment)
  - Cloud Storage (comics & audio)
  - Cloud SQL (database)
  - Vision API (OCR)
  - Text-to-Speech API
  - Gemini API (panel detection)

### Frontend
- React 18+
- HTML5 Canvas (comic rendering)
- IndexedDB (offline storage)
- Tailwind CSS

## Project Structure

```
cbr-reader/
├── backend/              # Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
│   └── pom.xml
├── frontend/             # React application
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   └── services/
│   └── package.json
├── docs/                 # Documentation
│   ├── analysis.md       # Technical analysis
│   └── PROJECT_DECISIONS.md
└── docker-compose.yml    # Local development environment
```

## Getting Started

### Prerequisites
- Java 17+
- Node.js 18+
- Docker & Docker Compose
- GCP account and credentials

### Local Development Setup

1. Clone the repository:
```bash
git clone https://github.com/syudenkov/cbr-reader.git
cd cbr-reader
```

2. Start local services:
```bash
docker-compose up -d
```

3. Run backend:
```bash
cd backend
./mvnw spring-boot:run
```

4. Run frontend:
```bash
cd frontend
npm install
npm start
```

5. Access the application:
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- Admin UI: http://localhost:8080/admin

## Development Roadmap

- **Phase 1 (6 weeks)**: MVP - Core reading experience
- **Phase 2 (3-4 weeks)**: Enhanced reading features
- **Phase 3 (4-6 weeks)**: OCR/TTS integration
- **Phase 4 (3-4 weeks)**: Admin UI, analytics, polish
- **Future**: Android native app

See [docs/analysis.md](docs/analysis.md) for detailed technical analysis.

## Cost Estimates

### Monthly Infrastructure
- Cloud Run: $0-10/month
- Cloud SQL: $9-10/month
- Cloud Storage: $0.60-10/month
- **Total**: ~$10-20/month

### One-Time Processing (600 comics)
- OCR + TTS: ~$149
- See [docs/analysis.md](docs/analysis.md) for details

## License

Private family project - not licensed for public use.

## Contributing

This is a private family project. External contributions are not accepted at this time.

## Support

For issues or questions, please open an issue on GitHub.

---

**Repository**: https://github.com/syudenkov/cbr-reader
**Last Updated**: 2025-10-20
