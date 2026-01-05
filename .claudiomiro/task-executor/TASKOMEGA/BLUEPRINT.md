<!-- BLUEPRINT: Read-only after creation -->
@dependencies [TASK0, TASK1, TASK2, TASK3, TASK4, TASK5, TASK6, TASK7, TASK8, TASK9, TASK10, TASK11, TASK12, TASK13, TASK14, TASK15]
@difficulty hard
@files []

# BLUEPRINT: TASK OMEGA - Final Integration Validation

## 1. IDENTITY

### This Task IS:
- Verifying ALL components integrate correctly end-to-end
- Running complete backend test suite and verifying pass
- Running complete frontend test suite and verifying pass
- Verifying database schema is complete and correct
- Verifying all API endpoints are accessible with correct auth
- Verifying frontend routes work and navigate correctly
- Verifying TTS pipeline works end-to-end (file -> OCR -> TTS -> audio)
- Verifying admin panel functions work for admin users
- Creating final README.md with setup instructions
- Performing final security checks (no exposed secrets, auth enforced)

### This Task IS NOT:
- Writing new feature code
- Adding new tests
- Modifying existing implementations
- Deploying to production

### Anti-Hallucination Anchors:
- If verification fails -> Report specific failure, do not proceed
- If any requirement missing -> Reference AI_PROMPT.md and trace to task
- If integration issue found -> Document for fix, do not auto-fix

### Guardrails (Prohibitions):
**Scope Guardrails:**
- [ ] DO NOT add new features - validation only
- [ ] DO NOT modify existing code - only report issues

**Architecture Guardrails:**
- [ ] DO NOT change architecture decisions
- [ ] DO NOT add deployment configuration

**Quality Guardrails:**
- [ ] DO NOT skip any verification step
- [ ] DO NOT approve with known failures

**Security Guardrails:**
- [ ] NEVER approve if credentials exposed
- [ ] NEVER approve if auth can be bypassed

## 2. CONTEXT CHAIN

### Priority 0 - LEGACY REFERENCE:
None - no legacy systems configured

### Priority 1 - READ FIRST (Required):
- `../AI_PROMPT.md` - All acceptance criteria (§4), deliverables (§10)
- All TASK*/BLUEPRINT.md files for expected artifacts

### Priority 2 - READ BEFORE VALIDATION:
- AI_PROMPT.md:L427-501 for complete acceptance criteria checklist
- AI_PROMPT.md:L685-708 for verification requirements
- AI_PROMPT.md:L869-882 for deliverables checklist

### Priority 3 - REFERENCE IF NEEDED:
- Individual task BLUEPRINTs for specific success criteria

### Inherited From Dependencies:
- All previous tasks: Complete implementations

## 3. EXECUTION CONTRACT

### 3.1 Pre-Conditions (VERIFY BEFORE ANY VALIDATION):
| Check | Command | Expected |
|-------|---------|----------|
| All tasks completed | `ls -la /home/stress/projects/cbr_viewer/.claudiomiro/task-executor/TASK*/` | All directories exist |
| Backend exists | `test -d /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer` | Directory exists |
| Frontend exists | `test -d /home/stress/projects/cbr_viewer/frontend/src` | Directory exists |

**HARD STOP:** If ANY check fails -> status: blocked

### 3.2 Success Criteria (COMPREHENSIVE VERIFICATION):

#### Authentication & Authorization (R1-R7)
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Register endpoint works | AI_PROMPT.md:L430 | BOTH | `curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{"username":"test","email":"test@test.com","password":"test123"}'` | Verify user created in database |
| Login returns session cookie | AI_PROMPT.md:L431 | AUTO | `curl -c - -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"test","password":"test123"}' 2>&1 | grep -i "set-cookie"` | - |
| Session is HTTP-only | AI_PROMPT.md:L432 | AUTO | `curl -c - -X POST http://localhost:8080/api/auth/login 2>&1 | grep -i "httponly"` | - |
| Unauthorized returns 401 | AI_PROMPT.md:L434 | AUTO | `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/files` | Output: 401 |
| Admin endpoint returns 403 for user | AI_PROMPT.md:L435 | MANUAL | - | Login as non-admin, access /api/admin/users, verify 403 |
| Logout invalidates session | AI_PROMPT.md:L436 | MANUAL | - | Login, logout, verify /api/auth/me returns 401 |

#### File Management (R8-R14)
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| CBZ upload works | AI_PROMPT.md:L439 | MANUAL | - | Upload CBZ via admin, verify success |
| Page count extracted | AI_PROMPT.md:L440 | AUTO | `sqlite3 backend/data/cbr_viewer.db "SELECT page_count FROM comic_files LIMIT 1"` | - |
| Cover image extracted | AI_PROMPT.md:L441 | AUTO | `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/files/1/cover` | Output: 200 |
| UUID filename | AI_PROMPT.md:L442 | AUTO | `ls storage/comics/ | head -1 | grep -E "^[a-f0-9-]{36}"` | - |
| File list paginated | AI_PROMPT.md:L500 | AUTO | `curl -s "http://localhost:8080/api/files?page=0&size=20" | grep -q "content"` | - |

#### Viewing (R15-R20)
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Page image served | AI_PROMPT.md:L448 | AUTO | `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/files/1/page/1` | Output: 200 |
| Frontend viewer loads | AI_PROMPT.md:L448 | MANUAL | - | Navigate to /viewer/1, verify page displays |
| Fullscreen works | AI_PROMPT.md:L450 | MANUAL | - | Click fullscreen button, verify fullscreen mode |
| Keyboard navigation | AI_PROMPT.md:L451 | MANUAL | - | Press arrow keys, verify page changes |
| Scroll mode works | AI_PROMPT.md:L449 | MANUAL | - | Switch to scroll mode, verify pages display |

#### Reading Progress (R21-R23)
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Progress saved | AI_PROMPT.md:L456 | AUTO | `sqlite3 backend/data/cbr_viewer.db "SELECT COUNT(*) FROM reading_progress"` | Output > 0 after navigation |
| Progress unique per user/file | AI_PROMPT.md:L457 | AUTO | `sqlite3 backend/data/cbr_viewer.db ".schema reading_progress" | grep -q "UNIQUE"` | - |
| Resume from last page | AI_PROMPT.md:L458 | MANUAL | - | Close viewer, reopen, verify same page |

#### Ratings (R24-R26)
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Rating saved | AI_PROMPT.md:L462 | AUTO | `curl -X POST "http://localhost:8080/api/ratings/1" -H "Content-Type: application/json" -d '{"rating":5}'` | - |
| Average rating displayed | AI_PROMPT.md:L464 | MANUAL | - | View file card, verify stars displayed |

#### TTS Processing (R27-R38)
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| TTS job created | AI_PROMPT.md:L468 | AUTO | `curl -X POST "http://localhost:8080/api/tts/request/1"` | Returns job ID |
| Job status available | AI_PROMPT.md:L472 | AUTO | `curl "http://localhost:8080/api/tts/status/1"` | Returns status JSON |
| Results stored | AI_PROMPT.md:L476 | AUTO | `sqlite3 backend/data/cbr_viewer.db "SELECT COUNT(*) FROM tts_results"` | Output > 0 after processing |
| Audio playable | AI_PROMPT.md:L478 | MANUAL | - | Complete TTS, play audio in viewer |

#### Admin Panel (R39-R45)
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Admin-only access | AI_PROMPT.md:L482 | MANUAL | - | Non-admin cannot access /admin |
| User management works | AI_PROMPT.md:L483 | MANUAL | - | Create, edit, delete user in admin |
| Logs viewable | AI_PROMPT.md:L485 | MANUAL | - | View system logs in admin panel |
| LLM config editable | AI_PROMPT.md:L487 | MANUAL | - | Update API key, verify saved |
| API keys encrypted | AI_PROMPT.md:L488 | AUTO | `sqlite3 backend/data/cbr_viewer.db "SELECT api_key_encrypted FROM llm_config LIMIT 1" | grep -v "sk-"` | Should NOT show plaintext |

#### Error Handling (R46-R50)
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Invalid file returns 400 | AI_PROMPT.md:L491 | AUTO | Upload .txt file, verify 400 response | - |
| Not found returns 404 | AI_PROMPT.md:L492 | AUTO | `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/files/99999` | Output: 404 |
| Server errors logged | AI_PROMPT.md:L495 | AUTO | `sqlite3 backend/data/cbr_viewer.db "SELECT COUNT(*) FROM system_logs WHERE level='ERROR'"` | - |

#### Performance (R51-R54)
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| SQLite indexes exist | AI_PROMPT.md:L501 | AUTO | `sqlite3 backend/data/cbr_viewer.db ".indexes"` | Shows indexes |

### 3.3 Deliverables Check:
| Deliverable | Source | Command | Expected |
|-------------|--------|---------|----------|
| Backend application | AI_PROMPT.md:L871 | `test -f backend/build.gradle` | Exists |
| Frontend application | AI_PROMPT.md:L872 | `test -f frontend/package.json` | Exists |
| SQLite database | AI_PROMPT.md:L873 | `test -f backend/data/cbr_viewer.db` | Exists |
| Working auth | AI_PROMPT.md:L874 | Login test | Works |
| File upload/viewing | AI_PROMPT.md:L875 | Upload + view test | Works |
| Reading progress | AI_PROMPT.md:L876 | Progress test | Works |
| Rating system | AI_PROMPT.md:L877 | Rating test | Works |
| TTS pipeline | AI_PROMPT.md:L878 | TTS test | Works |
| Admin panel | AI_PROMPT.md:L879 | Admin test | Works |
| Unit tests | AI_PROMPT.md:L880 | `./gradlew test && npm test` | Pass |
| Integration tests | AI_PROMPT.md:L881 | MockMvc tests | Pass |
| README | AI_PROMPT.md:L882 | `test -f README.md` | Exists |

## 4. IMPLEMENTATION STRATEGY

### Phase 1: Environment Verification
1. Verify all task directories exist
2. Verify backend compiles: `./gradlew build`
3. Verify frontend compiles: `npm run build`
4. Verify database schema created

**Gate:** All builds pass

### Phase 2: Backend Integration Tests
1. Run all backend tests: `./gradlew test`
2. Verify all tests pass
3. Check coverage meets targets

**Gate:** All tests pass, coverage met

### Phase 3: Frontend Integration Tests
1. Run all frontend tests: `npm test`
2. Verify all tests pass
3. Check coverage meets targets

**Gate:** All tests pass, coverage met

### Phase 4: Manual Integration Testing
1. Start backend: `./gradlew bootRun`
2. Start frontend: `npm run dev`
3. Execute all MANUAL verification criteria
4. Document any failures

**Gate:** All manual tests pass

### Phase 5: Security Verification
1. Check no hardcoded credentials in code
2. Verify API keys encrypted in database
3. Verify auth enforced on protected endpoints
4. Verify admin-only access control

**Gate:** Security requirements met

### Phase 6: Documentation
1. Create README.md with:
   - Project overview
   - Tech stack
   - Prerequisites (Java 21, Node.js)
   - Setup instructions (backend + frontend)
   - Default admin credentials
   - Environment configuration
   - Running tests
2. Verify README is complete

**Gate:** README created and complete

### Phase 7: Final Sign-off
1. All automated tests pass
2. All manual tests pass
3. All deliverables present
4. README complete
5. No security issues

**Gate:** System approved for completion

## 5. UNCERTAINTY LOG

| ID | Topic | Assumption | Confidence | Evidence |
|----|-------|------------|------------|----------|
| U1 | TTS API keys | Need real API keys for full TTS test | LOW | May test with mocks only |
| U2 | Audio generation | Minimax API may behave differently | LOW | Documented in TASK10 |
| U3 | Browser compatibility | Testing in Chrome primarily | MEDIUM | May have issues in Safari |

### Stop Rule:
If ANY critical verification fails -> Document issue, do not approve

## 6. INTEGRATION IMPACT

### Files Modified:
None - validation only

### Files Created:
| File | Imports From | Exports |
|------|--------------|---------|
| README.md | - | Documentation |

### Breaking Changes:
None - validation task only

## 7. FINAL VALIDATION CHECKLIST

Before marking complete, verify ALL items:

- [ ] Backend compiles without errors
- [ ] Frontend compiles without errors
- [ ] All backend tests pass
- [ ] All frontend tests pass
- [ ] Authentication flow works end-to-end
- [ ] File upload and viewing works
- [ ] Reading progress saves and restores
- [ ] Ratings can be submitted and displayed
- [ ] TTS processing completes (or gracefully handles missing API keys)
- [ ] Admin panel accessible only to admins
- [ ] No exposed credentials in code or config
- [ ] API keys encrypted in database
- [ ] README with setup instructions created
- [ ] All acceptance criteria from AI_PROMPT.md verified

**ONLY mark task complete when ALL items are checked.**
