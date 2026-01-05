<!-- BLUEPRINT: Read-only after creation -->
@dependencies [TASK4]
@difficulty fast
@files [backend/src/main/java/com/cbrviewer/model/Rating.java, backend/src/main/java/com/cbrviewer/repository/RatingRepository.java, backend/src/main/java/com/cbrviewer/service/RatingService.java, backend/src/main/java/com/cbrviewer/controller/RatingController.java, backend/src/main/java/com/cbrviewer/dto/RatingDto.java, frontend/src/components/library/RatingStars.tsx]

# BLUEPRINT: TASK7

## 1. IDENTITY

### This Task IS:
- Creating Rating entity matching ratings table schema
- Creating RatingRepository with findByUserIdAndFileId, getAverageRating methods
- Creating RatingService for rate and getAverage operations
- Creating RatingController with GET /api/ratings/{fileId}, POST /api/ratings/{fileId}, GET /api/ratings/{fileId}/avg endpoints
- Creating RatingStars.tsx component for displaying and submitting ratings
- Updating FileCard.tsx to display average rating (read-only)

### This Task IS NOT:
- Implementing file browsing (TASK4 complete)
- Implementing viewer (TASK5 complete)
- Implementing TTS (TASK8-11)

### Anti-Hallucination Anchors:
- If Rating schema unclear -> Follow AI_PROMPT.md:L196-203 exactly
- If endpoint paths unclear -> Follow AI_PROMPT.md:L287-289 API spec
- If rating validation unclear -> Enforce 1-5 range (AI_PROMPT.md:L200)

### Guardrails (Prohibitions):
**Scope Guardrails:**
- [ ] DO NOT implement comments or reviews - only star ratings
- [ ] DO NOT implement rating history - only current rating

**Architecture Guardrails:**
- [ ] DO NOT cache ratings client-side long-term - fetch fresh on view
- [ ] DO NOT use floating point for ratings - integer 1-5 only

**Quality Guardrails:**
- [ ] DO NOT add half-star support - whole stars only
- [ ] DO NOT add rating breakdown charts

**Security Guardrails:**
- [ ] NEVER allow rating without authentication (AI_PROMPT.md:L465)
- [ ] NEVER allow rating values outside 1-5 range

## 2. CONTEXT CHAIN

### Priority 0 - LEGACY REFERENCE:
None - no legacy systems configured

### Priority 1 - READ FIRST (Required):
- `../AI_PROMPT.md` - Ratings requirements (§4)
- AI_PROMPT.md:L462-465 for ratings acceptance criteria
- AI_PROMPT.md:L287-289 for ratings API endpoints

### Priority 2 - READ BEFORE CODING:
- AI_PROMPT.md:L196-203 for ratings table schema
- AI_PROMPT.md:L124 for RatingStars component path
- AI_PROMPT.md:L316-340 for REST controller pattern

### Priority 3 - REFERENCE IF NEEDED:
- AI_PROMPT.md:L583-588 for ratings implementation guidance
- Context7 for MUI Rating component

### Inherited From Dependencies:
- TASK1: Auth (authenticated users only)
- TASK4: FileCard (display average rating)
- TASK0: schema.sql with ratings table

## 3. EXECUTION CONTRACT

### 3.1 Pre-Conditions (VERIFY BEFORE ANY CODE):
| Check | Command | Expected |
|-------|---------|----------|
| TASK4 FileCard exists | `test -f /home/stress/projects/cbr_viewer/frontend/src/components/library/FileCard.tsx` | File exists |
| Schema has ratings table | `grep -q "CREATE TABLE ratings" /home/stress/projects/cbr_viewer/backend/src/main/resources/schema.sql` | Exit 0 |
| TASK1 auth exists | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/AuthService.java` | File exists |

**HARD STOP:** If ANY check fails -> status: blocked

### 3.2 Success Criteria (VERIFY AFTER COMPLETE):
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Backend compiles | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/backend && ./gradlew build -x test --quiet` | - |
| Frontend compiles | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/frontend && npm run build --silent` | - |
| Rating entity exists | AI_PROMPT.md:L86 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/model/Rating.java` | - |
| RatingController exists | AI_PROMPT.md:L287 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/controller/RatingController.java` | - |
| RatingStars exists | AI_PROMPT.md:L124 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/components/library/RatingStars.tsx` | - |
| Rating validation 1-5 | AI_PROMPT.md:L200 | AUTO | `grep -q "rating >= 1" /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/RatingService.java` | - |

### 3.3 Output Artifacts:
| Artifact | Type | Path | Verification |
|----------|------|------|--------------|
| Rating entity | CREATE | backend/src/main/java/com/cbrviewer/model/Rating.java | `test -f` |
| RatingRepository | CREATE | backend/src/main/java/com/cbrviewer/repository/RatingRepository.java | `test -f` |
| RatingService | CREATE | backend/src/main/java/com/cbrviewer/service/RatingService.java | `test -f` |
| RatingController | CREATE | backend/src/main/java/com/cbrviewer/controller/RatingController.java | `test -f` |
| RatingDto | CREATE | backend/src/main/java/com/cbrviewer/dto/RatingDto.java | `test -f` |
| RatingStars | CREATE | frontend/src/components/library/RatingStars.tsx | `test -f` |

## 4. IMPLEMENTATION STRATEGY

### Phase 1: Preparation
1. Read AI_PROMPT.md ratings requirements
2. Verify TASK4 FileCard exists
3. Review ratings schema

**Gate:** All pre-conditions verified, schema understood

### Phase 2: Backend Model Layer
1. Create Rating.java entity with: id, userId, fileId, rating (1-5), createdAt
2. Create RatingRepository.java with:
   - findByUserIdAndFileId(userId, fileId) returning Optional
   - getAverageByFileId(fileId) returning Double
   - save(Rating) with upsert behavior (update if exists)
3. Create RatingDto.java with: fileId, rating, averageRating

**Gate:** Model classes compile, match schema

### Phase 3: Backend Service Layer
1. Create RatingService.java:
   - getUserRating(userId, fileId) - return user's rating or null
   - rateFile(userId, fileId, rating) - save/update rating, validate 1-5
   - getAverageRating(fileId) - return average or null if no ratings
   - Validate rating is between 1 and 5

**Gate:** Service compiles, validates input

### Phase 4: Backend Controller Layer
1. Create RatingController.java:
   - GET /api/ratings/{fileId} - return user's rating for file
   - POST /api/ratings/{fileId} - rate file (body: {rating})
   - GET /api/ratings/{fileId}/avg - return average rating
   - Get userId from session
   - Return 400 for invalid rating value

**Gate:** Endpoints defined, follow API spec

### Phase 5: Frontend Component
1. Create RatingStars.tsx:
   - Use MUI Rating component
   - Props: fileId, initialRating?, readOnly?, size?
   - When not readOnly: submit rating on change
   - Display loading state during submission
   - Show success/error feedback
2. Update FileCard.tsx:
   - Add RatingStars in read-only mode for average rating
   - Fetch average rating with file data

**Gate:** Component compiles, uses MUI Rating

### Phase 6: Validation
1. Verify backend compiles: `./gradlew build -x test`
2. Verify frontend compiles: `npm run build`
3. Verify all files created
4. Verify rating validation (1-5 range)
5. Mark task complete

## 5. UNCERTAINTY LOG

| ID | Topic | Assumption | Confidence | Evidence |
|----|-------|------------|------------|----------|
| U1 | Upsert behavior | Use ON CONFLICT for SQLite upsert | HIGH | Standard SQLite pattern |
| U2 | Average calculation | Use AVG() in SQL | HIGH | Standard SQL |
| U3 | MUI Rating precision | Using precision=1 for whole stars | HIGH | AI_PROMPT.md doesn't specify half-stars |

### Stop Rule:
LOW confidence on critical decision -> BLOCKED (do not proceed with guesses)

## 6. INTEGRATION IMPACT

### Files Modified:
| File | Modification | Who Imports | Impact |
|------|--------------|-------------|--------|
| FileCard.tsx | Add RatingStars for average display | - | Shows ratings |
| filesSlice.ts | Include averageRating in file data | FileCard | Rating display |

### Files Created:
| File | Imports From | Exports |
|------|--------------|---------|
| Rating.java | - | Rating entity |
| RatingRepository.java | Rating | Data access |
| RatingService.java | RatingRepository | Rating logic |
| RatingController.java | RatingService | REST endpoints |
| RatingDto.java | - | DTO |
| RatingStars.tsx | MUI Rating, api | RatingStars component |

### Breaking Changes:
None - additive changes only
