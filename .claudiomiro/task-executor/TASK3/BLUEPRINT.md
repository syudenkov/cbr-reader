<!-- BLUEPRINT: Read-only after creation -->
@dependencies [TASK1]
@difficulty medium
@files [backend/src/main/java/com/cbrviewer/controller/FileController.java, backend/src/main/java/com/cbrviewer/service/FileService.java, backend/src/main/java/com/cbrviewer/service/ArchiveService.java, backend/src/main/java/com/cbrviewer/model/ComicFile.java, backend/src/main/java/com/cbrviewer/repository/FileRepository.java, backend/src/main/java/com/cbrviewer/dto/FileDto.java, backend/src/main/java/com/cbrviewer/dto/FileUploadResponse.java, backend/src/main/java/com/cbrviewer/config/StorageConfig.java]

# BLUEPRINT: TASK3

## 1. IDENTITY

### This Task IS:
- Creating ComicFile entity matching comic_files table schema
- Creating FileRepository with Spring Data JDBC
- Creating ArchiveService for CBR/CBZ extraction (ZIP via java.util.zip, RAR via junrar)
- Creating FileService for file upload, metadata storage, page serving, cover extraction
- Creating FileController with endpoints: GET /api/files, GET /api/files/{id}, GET /api/files/{id}/page/{n}, GET /api/files/{id}/cover, POST /api/files, DELETE /api/files/{id}
- Implementing page caching layer (in-memory cache for extracted pages)
- Configuring storage paths for comics and cache directories

### This Task IS NOT:
- Creating frontend file browsing UI (TASK4)
- Creating viewer components (TASK5)
- Implementing reading progress (TASK6)
- Implementing ratings (TASK7)
- Implementing TTS processing (TASK8-11)

### Anti-Hallucination Anchors:
- If archive extraction fails -> Check file format, throw appropriate exception
- If page number out of range -> Return 404 with clear message
- If storage path unclear -> Follow AI_PROMPT.md:L853-856 config
- If ComicFile fields unclear -> Follow AI_PROMPT.md:L171-183 schema exactly

### Guardrails (Prohibitions):
**Scope Guardrails:**
- [ ] DO NOT implement frontend components - belongs to TASK4/TASK5
- [ ] DO NOT implement reading progress - belongs to TASK6
- [ ] DO NOT implement TTS processing - belongs to TASK8-11

**Architecture Guardrails:**
- [ ] DO NOT pre-extract all pages on upload - on-the-fly extraction chosen (AI_PROMPT.md:L517)
- [ ] DO NOT use Redis for caching - in-memory cache for MVP (AI_PROMPT.md:L719)
- [ ] DO NOT serve files via Nginx/CDN - Spring Boot serves everything (AI_PROMPT.md:L519)

**Quality Guardrails:**
- [ ] DO NOT add complex caching framework - simple ConcurrentHashMap sufficient
- [ ] DO NOT add image processing beyond extraction - serve original images

**Security Guardrails:**
- [ ] NEVER allow path traversal in file access - validate all paths
- [ ] NEVER expose internal file paths to frontend - use IDs only
- [ ] NEVER accept arbitrary file types - validate CBR/CBZ only

## 2. CONTEXT CHAIN

### Priority 0 - LEGACY REFERENCE:
None - no legacy systems configured

### Priority 1 - READ FIRST (Required):
- `../AI_PROMPT.md` - File management requirements (§4), API endpoints (§2)
- AI_PROMPT.md:L439-445 for file management acceptance criteria
- AI_PROMPT.md:L274-280 for file API endpoints

### Priority 2 - READ BEFORE CODING:
- AI_PROMPT.md:L171-183 for comic_files table schema
- AI_PROMPT.md:L316-340 for REST controller pattern
- AI_PROMPT.md:L853-856 for storage path configuration

### Priority 3 - REFERENCE IF NEEDED:
- AI_PROMPT.md:L29-31 for archive library choices
- Context7 for junrar usage if needed

### Inherited From Dependencies:
- TASK0: Project structure, schema.sql with comic_files table, application.yml
- TASK1: SecurityConfig (authenticated endpoints), GlobalExceptionHandler

## 3. EXECUTION CONTRACT

### 3.1 Pre-Conditions (VERIFY BEFORE ANY CODE):
| Check | Command | Expected |
|-------|---------|----------|
| TASK1 completed | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/config/SecurityConfig.java` | File exists |
| Schema has comic_files table | `grep -q "CREATE TABLE comic_files" /home/stress/projects/cbr_viewer/backend/src/main/resources/schema.sql` | Exit 0 |
| junrar in build.gradle | `grep -q "junrar" /home/stress/projects/cbr_viewer/backend/build.gradle` | Exit 0 |
| Storage paths in application.yml | `grep -q "comics-path" /home/stress/projects/cbr_viewer/backend/src/main/resources/application.yml` | Exit 0 |

**HARD STOP:** If ANY check fails -> status: blocked

### 3.2 Success Criteria (VERIFY AFTER COMPLETE):
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Backend compiles | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/backend && ./gradlew build -x test --quiet` | - |
| FileController exists | AI_PROMPT.md:L64 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/controller/FileController.java` | - |
| ArchiveService exists | AI_PROMPT.md:L71 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/ArchiveService.java` | - |
| ComicFile entity exists | AI_PROMPT.md:L85 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/model/ComicFile.java` | - |
| CBZ extraction supported | AI_PROMPT.md:L29 | AUTO | `grep -q "java.util.zip" /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/ArchiveService.java` | - |
| CBR extraction supported | AI_PROMPT.md:L30 | AUTO | `grep -q "junrar" /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/ArchiveService.java` | - |
| File upload endpoint | AI_PROMPT.md:L278 | AUTO | `grep -q "POST.*files" /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/controller/FileController.java` | - |

### 3.3 Output Artifacts:
| Artifact | Type | Path | Verification |
|----------|------|------|--------------|
| FileController | CREATE | backend/src/main/java/com/cbrviewer/controller/FileController.java | `test -f` |
| FileService | CREATE | backend/src/main/java/com/cbrviewer/service/FileService.java | `test -f` |
| ArchiveService | CREATE | backend/src/main/java/com/cbrviewer/service/ArchiveService.java | `test -f` |
| ComicFile entity | CREATE | backend/src/main/java/com/cbrviewer/model/ComicFile.java | `test -f` |
| FileRepository | CREATE | backend/src/main/java/com/cbrviewer/repository/FileRepository.java | `test -f` |
| StorageConfig | CREATE | backend/src/main/java/com/cbrviewer/config/StorageConfig.java | `test -f` |
| DTOs | CREATE | backend/src/main/java/com/cbrviewer/dto/FileDto.java, FileUploadResponse.java | `test -f` |

## 4. IMPLEMENTATION STRATEGY

### Phase 1: Preparation
1. Read AI_PROMPT.md file management requirements
2. Verify TASK0 and TASK1 artifacts exist
3. Review comic_files schema from AI_PROMPT.md:L171-183

**Gate:** All pre-conditions verified, schema understood

### Phase 2: Model Layer
1. Create ComicFile.java entity with fields: id, filename, originalFilename, filePath, fileType, fileSize, pageCount, coverImagePath, uploadedBy, createdAt, updatedAt
2. Create FileRepository.java with findAll (paginated), findById methods
3. Create FileDto.java for API responses
4. Create FileUploadResponse.java for upload result

**Gate:** Model classes compile, match schema

### Phase 3: Storage Configuration
1. Create StorageConfig.java:
   - Read paths from application.yml (comics-path, audio-path, cache-path)
   - Create directories if not exist on startup
   - Provide methods to get storage paths

**Gate:** Storage directories created on startup

### Phase 4: Archive Service
1. Create ArchiveService.java:
   - extractPage(filePath, pageNumber) - extract single page on-the-fly
   - getPageCount(filePath) - count pages in archive
   - extractCover(filePath) - extract first page as cover
   - Support CBZ via java.util.zip.ZipFile
   - Support CBR via com.github.junrar
   - Sort entries by filename for correct page order
   - Cache extracted pages in ConcurrentHashMap (key: fileId_pageNumber)

**Gate:** Archive extraction works for both formats

### Phase 5: File Service
1. Create FileService.java:
   - uploadFile(MultipartFile, userId) - save to disk with UUID, extract metadata, save to DB
   - findAll(Pageable) - list files with pagination
   - findById(id) - get file metadata
   - getPage(fileId, pageNumber) - get page image bytes (via ArchiveService)
   - getCover(fileId) - get cover image bytes
   - deleteFile(fileId) - remove from DB and disk
   - Validate file type (CBR/CBZ only)

**Gate:** FileService compiles, uses ArchiveService

### Phase 6: Controller Layer
1. Create FileController.java with:
   - GET /api/files - paginated list (authenticated)
   - GET /api/files/{id} - single file metadata
   - GET /api/files/{id}/page/{n} - page image (image/jpeg or image/png)
   - GET /api/files/{id}/cover - cover image
   - POST /api/files - multipart upload (admin only)
   - DELETE /api/files/{id} - delete file (admin only)
2. Return appropriate content types for images
3. Handle errors: 400 for invalid format, 404 for not found

**Gate:** All endpoints defined, follow API spec

### Phase 7: Validation
1. Verify backend compiles: `./gradlew build -x test`
2. Verify all files created
3. Verify both CBZ and CBR extraction code present
4. Mark task complete

## 5. UNCERTAINTY LOG

| ID | Topic | Assumption | Confidence | Evidence |
|----|-------|------------|------------|----------|
| U1 | junrar API | Using Archive class for RAR extraction | MEDIUM | Standard junrar usage, may need Context7 |
| U2 | Page ordering | Sort archive entries alphabetically for page order | HIGH | Standard comic archive convention |
| U3 | Image format detection | Detect JPEG/PNG from file extension or magic bytes | MEDIUM | May need Content-Type detection |
| U4 | Cache eviction | No eviction for MVP, may need LRU later | MEDIUM | AI_PROMPT.md doesn't specify |

### Stop Rule:
LOW confidence on critical decision -> BLOCKED (do not proceed with guesses)

## 6. INTEGRATION IMPACT

### Files Modified:
| File | Modification | Who Imports | Impact |
|------|--------------|-------------|--------|
| SecurityConfig.java | Add file endpoints to security rules | - | Endpoint access |

### Files Created:
| File | Imports From | Exports |
|------|--------------|---------|
| FileController.java | FileService, DTOs | File REST endpoints |
| FileService.java | FileRepository, ArchiveService, StorageConfig | File business logic |
| ArchiveService.java | java.util.zip, junrar | Archive extraction |
| ComicFile.java | - | ComicFile entity |
| FileRepository.java | ComicFile | Data access |
| StorageConfig.java | - | Storage paths |
| FileDto.java | - | DTO |
| FileUploadResponse.java | - | DTO |

### Breaking Changes:
None - new endpoints only
