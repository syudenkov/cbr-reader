<!-- BLUEPRINT: Read-only after creation -->
@dependencies [TASK1, TASK2, TASK3, TASK4, TASK5, TASK6, TASK7, TASK8, TASK9, TASK10, TASK11, TASK12, TASK13]
@difficulty medium
@files [backend/src/test/java/com/cbrviewer/service/AuthServiceTest.java, backend/src/test/java/com/cbrviewer/service/FileServiceTest.java, backend/src/test/java/com/cbrviewer/service/ArchiveServiceTest.java, backend/src/test/java/com/cbrviewer/service/TtsServiceTest.java, backend/src/test/java/com/cbrviewer/controller/AuthControllerTest.java, backend/src/test/java/com/cbrviewer/controller/FileControllerTest.java, backend/src/test/java/com/cbrviewer/controller/AdminControllerTest.java, frontend/src/store/slices/authSlice.test.ts, frontend/src/store/slices/filesSlice.test.ts, frontend/src/components/viewer/PageViewer.test.tsx, frontend/src/components/library/RatingStars.test.tsx, frontend/src/components/auth/LoginForm.test.tsx]

# BLUEPRINT: TASK14

## 1. IDENTITY

### This Task IS:
- Creating JUnit 5 unit tests for backend services (AuthService, FileService, ArchiveService, TtsService)
- Creating MockMvc integration tests for controllers (AuthController, FileController, AdminController)
- Creating Jest unit tests for Redux slices (authSlice, filesSlice)
- Creating React Testing Library tests for key components (PageViewer, RatingStars, LoginForm)
- Verifying security configuration (auth required, admin-only endpoints)
- Testing archive extraction with sample CBZ/CBR test files

### This Task IS NOT:
- Writing new feature code (all features complete)
- Creating E2E tests (AI_PROMPT.md:L47, L512)
- Achieving 100% coverage (targets: services 80%, controllers 70%, slices 90%, components 60%)

### Anti-Hallucination Anchors:
- If test structure unclear -> Follow standard JUnit/Jest conventions
- If coverage targets unclear -> Follow AI_PROMPT.md:L664-680 exactly
- If test file paths unclear -> Follow AI_PROMPT.md:§2 project structure

### Guardrails (Prohibitions):
**Scope Guardrails:**
- [ ] DO NOT write E2E tests - only unit + integration (AI_PROMPT.md:L512)
- [ ] DO NOT write tests for utilities - focus on services and controllers

**Architecture Guardrails:**
- [ ] DO NOT use real database in unit tests - mock repositories
- [ ] DO NOT call external APIs in tests - mock HTTP clients

**Quality Guardrails:**
- [ ] DO NOT aim for 100% coverage - follow specified targets
- [ ] DO NOT write fragile tests dependent on implementation details

**Security Guardrails:**
- [ ] DO NOT include real API keys in tests - use mock values
- [ ] DO NOT skip security tests - verify auth required endpoints

## 2. CONTEXT CHAIN

### Priority 0 - LEGACY REFERENCE:
None - no legacy systems configured

### Priority 1 - READ FIRST (Required):
- `../AI_PROMPT.md` - Testing guidance (§5.1)
- AI_PROMPT.md:L649-680 for complete testing requirements
- AI_PROMPT.md:L44-47 for testing tech stack

### Priority 2 - READ BEFORE CODING:
- AI_PROMPT.md:L651-665 for backend testing guidance
- AI_PROMPT.md:L667-679 for frontend testing guidance
- All TASK files for component/service implementations

### Priority 3 - REFERENCE IF NEEDED:
- Context7 for JUnit 5 + MockMvc patterns
- Context7 for Jest + React Testing Library patterns

### Inherited From Dependencies:
- All previous tasks: Complete implementations to test

## 3. EXECUTION CONTRACT

### 3.1 Pre-Conditions (VERIFY BEFORE ANY CODE):
| Check | Command | Expected |
|-------|---------|----------|
| All services exist | `ls /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/` | Multiple .java files |
| All controllers exist | `ls /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/controller/` | Multiple .java files |
| All slices exist | `ls /home/stress/projects/cbr_viewer/frontend/src/store/slices/` | Multiple .ts files |
| Test dependencies in build.gradle | `grep -q "junit" /home/stress/projects/cbr_viewer/backend/build.gradle` | Exit 0 |
| Jest in package.json | `grep -q "jest" /home/stress/projects/cbr_viewer/frontend/package.json` | Exit 0 |

**HARD STOP:** If ANY check fails -> status: blocked

### 3.2 Success Criteria (VERIFY AFTER COMPLETE):
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Backend tests pass | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/backend && ./gradlew test --quiet` | - |
| Frontend tests pass | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/frontend && npm test -- --watchAll=false --silent` | - |
| AuthServiceTest exists | AI_PROMPT.md:L652 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/test/java/com/cbrviewer/service/AuthServiceTest.java` | - |
| FileServiceTest exists | AI_PROMPT.md:L653 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/test/java/com/cbrviewer/service/FileServiceTest.java` | - |
| authSlice.test.ts exists | AI_PROMPT.md:L669 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/store/slices/authSlice.test.ts` | - |
| Component tests exist | AI_PROMPT.md:L673 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/components/viewer/PageViewer.test.tsx` | - |

### 3.3 Output Artifacts:
| Artifact | Type | Path | Verification |
|----------|------|------|--------------|
| AuthServiceTest | CREATE | backend/src/test/java/com/cbrviewer/service/AuthServiceTest.java | `test -f` |
| FileServiceTest | CREATE | backend/src/test/java/com/cbrviewer/service/FileServiceTest.java | `test -f` |
| ArchiveServiceTest | CREATE | backend/src/test/java/com/cbrviewer/service/ArchiveServiceTest.java | `test -f` |
| TtsServiceTest | CREATE | backend/src/test/java/com/cbrviewer/service/TtsServiceTest.java | `test -f` |
| AuthControllerTest | CREATE | backend/src/test/java/com/cbrviewer/controller/AuthControllerTest.java | `test -f` |
| FileControllerTest | CREATE | backend/src/test/java/com/cbrviewer/controller/FileControllerTest.java | `test -f` |
| AdminControllerTest | CREATE | backend/src/test/java/com/cbrviewer/controller/AdminControllerTest.java | `test -f` |
| authSlice.test.ts | CREATE | frontend/src/store/slices/authSlice.test.ts | `test -f` |
| filesSlice.test.ts | CREATE | frontend/src/store/slices/filesSlice.test.ts | `test -f` |
| PageViewer.test.tsx | CREATE | frontend/src/components/viewer/PageViewer.test.tsx | `test -f` |
| RatingStars.test.tsx | CREATE | frontend/src/components/library/RatingStars.test.tsx | `test -f` |
| LoginForm.test.tsx | CREATE | frontend/src/components/auth/LoginForm.test.tsx | `test -f` |

## 4. IMPLEMENTATION STRATEGY

### Phase 1: Preparation
1. Read AI_PROMPT.md testing guidance
2. Verify all implementations exist
3. Ensure test dependencies are configured

**Gate:** All pre-conditions verified

### Phase 2: Backend Service Tests
1. Create AuthServiceTest.java:
   - Test register with valid input
   - Test register with duplicate username (expect error)
   - Test login with correct credentials
   - Test login with wrong password (expect error)
   - Test password hashing (verify BCrypt used)
2. Create FileServiceTest.java:
   - Test uploadFile with valid CBZ
   - Test uploadFile with invalid format (expect error)
   - Test findAll pagination
   - Test deleteFile removes from disk
3. Create ArchiveServiceTest.java:
   - Test extractPage from CBZ (include test CBZ file)
   - Test extractPage from CBR (include test CBR file)
   - Test getPageCount
   - Test extractCover
4. Create TtsServiceTest.java:
   - Test requestTts creates job with PENDING status
   - Test processFile updates progress
   - Test job completion status

**Gate:** Service tests pass

### Phase 3: Backend Controller Tests
1. Create AuthControllerTest.java (MockMvc):
   - Test POST /api/auth/register success
   - Test POST /api/auth/login success
   - Test GET /api/auth/me returns user info
   - Test unauthorized access returns 401
2. Create FileControllerTest.java (MockMvc):
   - Test GET /api/files requires auth
   - Test POST /api/files requires admin
   - Test GET /api/files/{id}/page/{n} returns image
3. Create AdminControllerTest.java (MockMvc):
   - Test admin endpoints return 403 for non-admin
   - Test admin endpoints work for admin user
   - Test user CRUD operations

**Gate:** Controller tests pass

### Phase 4: Frontend Slice Tests
1. Create authSlice.test.ts:
   - Test initial state
   - Test login.pending sets loading
   - Test login.fulfilled sets user and isAuthenticated
   - Test login.rejected sets error
   - Test logout clears state
2. Create filesSlice.test.ts:
   - Test initial state
   - Test fetchFiles.fulfilled sets items
   - Test pagination state updates

**Gate:** Slice tests pass

### Phase 5: Frontend Component Tests
1. Create PageViewer.test.tsx:
   - Test renders image with correct src
   - Test click navigation areas
   - Test loading state
2. Create RatingStars.test.tsx:
   - Test renders correct number of stars
   - Test click updates rating
   - Test read-only mode
3. Create LoginForm.test.tsx:
   - Test renders inputs
   - Test validation errors displayed
   - Test submit dispatches action

**Gate:** Component tests pass

### Phase 6: Validation
1. Run all backend tests: `./gradlew test`
2. Run all frontend tests: `npm test`
3. Verify all test files created
4. Check no tests skipped or failing
5. Mark task complete

## 5. UNCERTAINTY LOG

| ID | Topic | Assumption | Confidence | Evidence |
|----|-------|------------|------------|----------|
| U1 | Test CBZ/CBR files | Will create small test archives | HIGH | Standard testing practice |
| U2 | MockMvc setup | Using @WebMvcTest with security | HIGH | Standard Spring testing |
| U3 | Redux store mock | Using configureStore with preloadedState | HIGH | RTK testing pattern |
| U4 | Coverage reporting | Using built-in JaCoCo and Jest coverage | MEDIUM | May need config |

### Stop Rule:
LOW confidence on critical decision -> BLOCKED (do not proceed with guesses)

## 6. INTEGRATION IMPACT

### Files Modified:
| File | Modification | Who Imports | Impact |
|------|--------------|-------------|--------|
| build.gradle | May add test dependencies | - | Test config |
| package.json | May add test scripts | - | Test config |

### Files Created:
All test files as listed in Output Artifacts

### Breaking Changes:
None - test files only
