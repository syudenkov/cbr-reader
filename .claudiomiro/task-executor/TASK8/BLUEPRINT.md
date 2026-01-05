<!-- BLUEPRINT: Read-only after creation -->
@dependencies [TASK3]
@difficulty medium
@files [backend/src/main/java/com/cbrviewer/model/TtsJob.java, backend/src/main/java/com/cbrviewer/model/TtsResult.java, backend/src/main/java/com/cbrviewer/repository/TtsJobRepository.java, backend/src/main/java/com/cbrviewer/repository/TtsResultRepository.java, backend/src/main/java/com/cbrviewer/service/TtsService.java, backend/src/main/java/com/cbrviewer/controller/TtsController.java, backend/src/main/java/com/cbrviewer/dto/TtsJobDto.java, backend/src/main/java/com/cbrviewer/dto/TtsRequestDto.java, backend/src/main/java/com/cbrviewer/config/AsyncConfig.java]

# BLUEPRINT: TASK8

## 1. IDENTITY

### This Task IS:
- Creating TtsJob entity matching tts_jobs table schema (status, progress tracking)
- Creating TtsResult entity matching tts_results table schema (OCR text, speakers JSON, audio path)
- Creating TtsJobRepository and TtsResultRepository
- Creating TtsService with job creation, async processing orchestration, progress updates
- Creating TtsController with POST /api/tts/request/{fileId}, GET /api/tts/status/{jobId}, GET /api/tts/result/{fileId}, GET /api/tts/audio/{fileId}/{page}
- Configuring AsyncConfig for background processing
- Implementing job status flow: PENDING -> PROCESSING -> COMPLETED/FAILED

### This Task IS NOT:
- Implementing OCR logic (TASK9)
- Implementing TTS generation (TASK10)
- Implementing TTS frontend (TASK11)
- Implementing admin LLM config (TASK12)

### Anti-Hallucination Anchors:
- If TtsJob schema unclear -> Follow AI_PROMPT.md:L206-216 exactly
- If TtsResult schema unclear -> Follow AI_PROMPT.md:L219-228 exactly
- If endpoint paths unclear -> Follow AI_PROMPT.md:L291-295 API spec
- If async pattern unclear -> Follow AI_PROMPT.md:L344-361 pattern

### Guardrails (Prohibitions):
**Scope Guardrails:**
- [ ] DO NOT implement OCR API calls - belongs to TASK9
- [ ] DO NOT implement Minimax TTS calls - belongs to TASK10
- [ ] DO NOT implement WebSocket progress updates - polling only (AI_PROMPT.md:L511)

**Architecture Guardrails:**
- [ ] DO NOT use WebSocket for progress - REST polling chosen (AI_PROMPT.md:L511)
- [ ] DO NOT store results in separate files - JSON in SQLite (AI_PROMPT.md:L518)

**Quality Guardrails:**
- [ ] DO NOT add retry logic for external APIs - simple try-catch (AI_PROMPT.md:L719)
- [ ] DO NOT add circuit breaker - over-engineering (AI_PROMPT.md:L719)

**Security Guardrails:**
- [ ] NEVER process files without authentication
- [ ] DO NOT expose internal error details - log and return generic message

## 2. CONTEXT CHAIN

### Priority 0 - LEGACY REFERENCE:
None - no legacy systems configured

### Priority 1 - READ FIRST (Required):
- `../AI_PROMPT.md` - TTS requirements (§4), async pattern (§3)
- AI_PROMPT.md:L468-479 for TTS acceptance criteria
- AI_PROMPT.md:L291-295 for TTS API endpoints

### Priority 2 - READ BEFORE CODING:
- AI_PROMPT.md:L206-228 for tts_jobs and tts_results schemas
- AI_PROMPT.md:L344-361 for async processing pattern
- AI_PROMPT.md:L596-610 for TTS implementation guidance

### Priority 3 - REFERENCE IF NEEDED:
- Context7 for Spring @Async configuration
- AI_PROMPT.md:L853-856 for audio storage path

### Inherited From Dependencies:
- TASK3: FileService (get file info, extract pages)
- TASK1: Auth (user session)
- TASK0: schema.sql, AsyncConfig placeholder

## 3. EXECUTION CONTRACT

### 3.1 Pre-Conditions (VERIFY BEFORE ANY CODE):
| Check | Command | Expected |
|-------|---------|----------|
| TASK3 FileService exists | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/FileService.java` | File exists |
| Schema has tts_jobs table | `grep -q "CREATE TABLE tts_jobs" /home/stress/projects/cbr_viewer/backend/src/main/resources/schema.sql` | Exit 0 |
| Schema has tts_results table | `grep -q "CREATE TABLE tts_results" /home/stress/projects/cbr_viewer/backend/src/main/resources/schema.sql` | Exit 0 |

**HARD STOP:** If ANY check fails -> status: blocked

### 3.2 Success Criteria (VERIFY AFTER COMPLETE):
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Backend compiles | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/backend && ./gradlew build -x test --quiet` | - |
| TtsJob entity exists | AI_PROMPT.md:L88 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/model/TtsJob.java` | - |
| TtsResult entity exists | AI_PROMPT.md:L89 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/model/TtsResult.java` | - |
| TtsController exists | AI_PROMPT.md:L66 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/controller/TtsController.java` | - |
| Async annotation present | AI_PROMPT.md:L344 | AUTO | `grep -q "@Async" /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/TtsService.java` | - |
| Job status endpoint | AI_PROMPT.md:L293 | AUTO | `grep -q "status" /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/controller/TtsController.java` | - |

### 3.3 Output Artifacts:
| Artifact | Type | Path | Verification |
|----------|------|------|--------------|
| TtsJob entity | CREATE | backend/src/main/java/com/cbrviewer/model/TtsJob.java | `test -f` |
| TtsResult entity | CREATE | backend/src/main/java/com/cbrviewer/model/TtsResult.java | `test -f` |
| TtsJobRepository | CREATE | backend/src/main/java/com/cbrviewer/repository/TtsJobRepository.java | `test -f` |
| TtsResultRepository | CREATE | backend/src/main/java/com/cbrviewer/repository/TtsResultRepository.java | `test -f` |
| TtsService | CREATE | backend/src/main/java/com/cbrviewer/service/TtsService.java | `test -f` |
| TtsController | CREATE | backend/src/main/java/com/cbrviewer/controller/TtsController.java | `test -f` |
| AsyncConfig | CREATE | backend/src/main/java/com/cbrviewer/config/AsyncConfig.java | `test -f` |
| DTOs | CREATE | backend/src/main/java/com/cbrviewer/dto/TtsJobDto.java, TtsRequestDto.java | `test -f` |

## 4. IMPLEMENTATION STRATEGY

### Phase 1: Preparation
1. Read AI_PROMPT.md TTS infrastructure requirements
2. Verify TASK3 FileService exists
3. Review tts_jobs and tts_results schemas

**Gate:** All pre-conditions verified, schemas understood

### Phase 2: Model Layer
1. Create TtsJob.java entity with: id, fileId, requestedBy, status (PENDING/PROCESSING/COMPLETED/FAILED), progress (0-100), currentPage, errorMessage, createdAt, updatedAt
2. Create TtsResult.java entity with: id, fileId, pageNumber, ocrText, speakersJson, audioFilePath, createdAt
3. Create TtsJobRepository with: findById, findByFileId, save
4. Create TtsResultRepository with: findByFileIdAndPageNumber, findAllByFileId, save
5. Create TtsJobDto and TtsRequestDto

**Gate:** Model classes compile, match schemas

### Phase 3: Async Configuration
1. Create AsyncConfig.java:
   - @EnableAsync annotation
   - Configure thread pool executor (core: 2, max: 5)
   - Configure async exception handler

**Gate:** Async config compiles

### Phase 4: Service Layer
1. Create TtsService.java:
   - requestTts(fileId, userId) - create job with PENDING status, return jobId
   - @Async processFile(jobId) - orchestrate processing:
     - Update status to PROCESSING
     - For each page: call OCR (placeholder), call TTS (placeholder), save result
     - Update progress as each page completes
     - Update status to COMPLETED or FAILED
   - getJobStatus(jobId) - return job with progress
   - getResults(fileId) - return all results for file
   - getAudio(fileId, page) - return audio file bytes
2. Add placeholder methods for OCR and TTS that TASK9 and TASK10 will implement

**Gate:** Service compiles with placeholders

### Phase 5: Controller Layer
1. Create TtsController.java:
   - POST /api/tts/request/{fileId} - create TTS job, start async processing
   - GET /api/tts/status/{jobId} - return job status and progress
   - GET /api/tts/result/{fileId} - return all results for file
   - GET /api/tts/audio/{fileId}/{page} - stream audio file
2. Return appropriate status codes

**Gate:** Endpoints defined, follow API spec

### Phase 6: Validation
1. Verify backend compiles: `./gradlew build -x test`
2. Verify all files created
3. Verify @Async annotation present
4. Verify job status flow implemented
5. Mark task complete

## 5. UNCERTAINTY LOG

| ID | Topic | Assumption | Confidence | Evidence |
|----|-------|------------|------------|----------|
| U1 | Thread pool size | 2-5 threads for async processing | MEDIUM | May need adjustment |
| U2 | Progress calculation | progress = (currentPage / totalPages) * 100 | HIGH | Standard approach |
| U3 | OCR/TTS placeholders | Return mock data until TASK9/TASK10 complete | HIGH | Allows independent testing |

### Stop Rule:
LOW confidence on critical decision -> BLOCKED (do not proceed with guesses)

## 6. INTEGRATION IMPACT

### Files Modified:
None - all files are new

### Files Created:
| File | Imports From | Exports |
|------|--------------|---------|
| TtsJob.java | - | TtsJob entity |
| TtsResult.java | - | TtsResult entity |
| TtsJobRepository.java | TtsJob | Data access |
| TtsResultRepository.java | TtsResult | Data access |
| TtsService.java | Repositories, FileService | TTS orchestration |
| TtsController.java | TtsService | REST endpoints |
| AsyncConfig.java | - | Async configuration |
| TtsJobDto.java | - | DTO |
| TtsRequestDto.java | - | DTO |

### Breaking Changes:
None - new endpoints only
