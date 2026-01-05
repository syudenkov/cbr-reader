<!-- BLUEPRINT: Read-only after creation -->
@dependencies [TASK5]
@difficulty fast
@files [backend/src/main/java/com/cbrviewer/model/ReadingProgress.java, backend/src/main/java/com/cbrviewer/repository/ReadingProgressRepository.java, backend/src/main/java/com/cbrviewer/service/ReadingProgressService.java, backend/src/main/java/com/cbrviewer/controller/ProgressController.java, backend/src/main/java/com/cbrviewer/dto/ProgressDto.java, frontend/src/hooks/useReadingProgress.ts]

# BLUEPRINT: TASK6

## 1. IDENTITY

### This Task IS:
- Creating ReadingProgress entity matching reading_progress table schema
- Creating ReadingProgressRepository with findByUserIdAndFileId, save methods
- Creating ReadingProgressService for get/update progress operations
- Creating ProgressController with GET /api/progress/{fileId}, PUT /api/progress/{fileId} endpoints
- Creating useReadingProgress hook for auto-save on page change
- Integrating progress loading into ViewerPage on file open
- Implementing auto-save when user navigates pages (debounced)

### This Task IS NOT:
- Modifying viewer navigation logic (TASK5 complete)
- Implementing ratings (TASK7)
- Implementing TTS (TASK8-11)
- Scroll position tracking - only page number (AI_PROMPT.md:L459)

### Anti-Hallucination Anchors:
- If ReadingProgress schema unclear -> Follow AI_PROMPT.md:L186-193 exactly
- If endpoint paths unclear -> Follow AI_PROMPT.md:L283-285 API spec
- If save behavior unclear -> Save only page number, not scroll position (AI_PROMPT.md:L459)

### Guardrails (Prohibitions):
**Scope Guardrails:**
- [ ] DO NOT implement scroll position saving - only page number (AI_PROMPT.md:L459)
- [ ] DO NOT modify viewer components - only add progress hook

**Architecture Guardrails:**
- [ ] DO NOT store progress client-side - always sync with server
- [ ] DO NOT add complex conflict resolution - last write wins

**Quality Guardrails:**
- [ ] DO NOT debounce more than 1 second - responsive feel needed
- [ ] DO NOT add undo/history for progress

**Security Guardrails:**
- [ ] NEVER allow users to update other users' progress - validate user ownership

## 2. CONTEXT CHAIN

### Priority 0 - LEGACY REFERENCE:
None - no legacy systems configured

### Priority 1 - READ FIRST (Required):
- `../AI_PROMPT.md` - Reading progress requirements (§4)
- AI_PROMPT.md:L456-459 for progress acceptance criteria
- AI_PROMPT.md:L283-285 for progress API endpoints

### Priority 2 - READ BEFORE CODING:
- AI_PROMPT.md:L186-193 for reading_progress table schema
- AI_PROMPT.md:L316-340 for REST controller pattern
- TASK5 ViewerPage.tsx for integration point

### Priority 3 - REFERENCE IF NEEDED:
- AI_PROMPT.md:L578-581 for reading progress implementation guidance

### Inherited From Dependencies:
- TASK1: Auth (user session for ownership)
- TASK5: ViewerPage (integration point for progress)
- TASK0: schema.sql with reading_progress table

## 3. EXECUTION CONTRACT

### 3.1 Pre-Conditions (VERIFY BEFORE ANY CODE):
| Check | Command | Expected |
|-------|---------|----------|
| TASK5 ViewerPage exists | `test -f /home/stress/projects/cbr_viewer/frontend/src/pages/ViewerPage.tsx` | File exists |
| Schema has reading_progress table | `grep -q "CREATE TABLE reading_progress" /home/stress/projects/cbr_viewer/backend/src/main/resources/schema.sql` | Exit 0 |
| TASK1 auth exists | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/AuthService.java` | File exists |

**HARD STOP:** If ANY check fails -> status: blocked

### 3.2 Success Criteria (VERIFY AFTER COMPLETE):
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Backend compiles | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/backend && ./gradlew build -x test --quiet` | - |
| Frontend compiles | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/frontend && npm run build --silent` | - |
| ReadingProgress entity exists | AI_PROMPT.md:L87 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/model/ReadingProgress.java` | - |
| ProgressController exists | AI_PROMPT.md:L283 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/controller/ProgressController.java` | - |
| GET progress endpoint | AI_PROMPT.md:L284 | AUTO | `grep -q "GET.*progress" /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/controller/ProgressController.java` | - |
| PUT progress endpoint | AI_PROMPT.md:L285 | AUTO | `grep -q "PUT.*progress" /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/controller/ProgressController.java` | - |
| useReadingProgress hook | AI_PROMPT.md:L456 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/hooks/useReadingProgress.ts` | - |

### 3.3 Output Artifacts:
| Artifact | Type | Path | Verification |
|----------|------|------|--------------|
| ReadingProgress entity | CREATE | backend/src/main/java/com/cbrviewer/model/ReadingProgress.java | `test -f` |
| ReadingProgressRepository | CREATE | backend/src/main/java/com/cbrviewer/repository/ReadingProgressRepository.java | `test -f` |
| ReadingProgressService | CREATE | backend/src/main/java/com/cbrviewer/service/ReadingProgressService.java | `test -f` |
| ProgressController | CREATE | backend/src/main/java/com/cbrviewer/controller/ProgressController.java | `test -f` |
| ProgressDto | CREATE | backend/src/main/java/com/cbrviewer/dto/ProgressDto.java | `test -f` |
| useReadingProgress | CREATE | frontend/src/hooks/useReadingProgress.ts | `test -f` |

## 4. IMPLEMENTATION STRATEGY

### Phase 1: Preparation
1. Read AI_PROMPT.md progress requirements
2. Verify TASK5 ViewerPage exists
3. Review reading_progress schema

**Gate:** All pre-conditions verified, schema understood

### Phase 2: Backend Model Layer
1. Create ReadingProgress.java entity with: id, userId, fileId, currentPage, updatedAt
2. Create ReadingProgressRepository.java with:
   - findByUserIdAndFileId(userId, fileId) returning Optional
   - save(ReadingProgress) with upsert behavior
3. Create ProgressDto.java with: fileId, currentPage

**Gate:** Model classes compile, match schema

### Phase 3: Backend Service Layer
1. Create ReadingProgressService.java:
   - getProgress(userId, fileId) - return current page or 1 if none
   - updateProgress(userId, fileId, page) - save/update progress
   - Validate page number is positive and within range

**Gate:** Service compiles, handles edge cases

### Phase 4: Backend Controller Layer
1. Create ProgressController.java:
   - GET /api/progress/{fileId} - return user's progress for file
   - PUT /api/progress/{fileId} - update progress (body: {currentPage})
   - Get userId from session
   - Return 404 if file doesn't exist

**Gate:** Endpoints defined, follow API spec

### Phase 5: Frontend Integration
1. Create useReadingProgress.ts hook:
   - loadProgress(fileId) - fetch current page on mount
   - saveProgress(fileId, page) - debounced save (500ms)
   - Return { currentPage, isLoading, saveProgress }
2. Update ViewerPage.tsx:
   - Call loadProgress on mount to set initial page
   - Call saveProgress when currentPage changes

**Gate:** Hook compiles, integrates with ViewerPage

### Phase 6: Validation
1. Verify backend compiles: `./gradlew build -x test`
2. Verify frontend compiles: `npm run build`
3. Verify all files created
4. Verify GET and PUT endpoints exist
5. Mark task complete

## 5. UNCERTAINTY LOG

| ID | Topic | Assumption | Confidence | Evidence |
|----|-------|------------|------------|----------|
| U1 | Upsert behavior | Use ON CONFLICT for SQLite upsert | HIGH | Standard SQLite pattern |
| U2 | Debounce timing | 500ms debounce is responsive enough | MEDIUM | May need adjustment |
| U3 | Initial page | Default to page 1 if no progress | HIGH | AI_PROMPT.md:L458 implies resume |

### Stop Rule:
LOW confidence on critical decision -> BLOCKED (do not proceed with guesses)

## 6. INTEGRATION IMPACT

### Files Modified:
| File | Modification | Who Imports | Impact |
|------|--------------|-------------|--------|
| ViewerPage.tsx | Use useReadingProgress hook | - | Auto-save progress |

### Files Created:
| File | Imports From | Exports |
|------|--------------|---------|
| ReadingProgress.java | - | ReadingProgress entity |
| ReadingProgressRepository.java | ReadingProgress | Data access |
| ReadingProgressService.java | ReadingProgressRepository | Progress logic |
| ProgressController.java | ReadingProgressService | REST endpoints |
| ProgressDto.java | - | DTO |
| useReadingProgress.ts | api | useReadingProgress hook |

### Breaking Changes:
None - additive changes only
