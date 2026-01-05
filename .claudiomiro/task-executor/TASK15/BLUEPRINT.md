<!-- BLUEPRINT: Read-only after creation -->
@dependencies [TASK12]
@difficulty fast
@files [backend/src/main/java/com/cbrviewer/aspect/AuditLogAspect.java]

# BLUEPRINT: TASK15

## 1. IDENTITY

### This Task IS:
- Creating AuditLogAspect.java for AOP-based audit logging
- Logging user actions: login, logout, file upload, file delete, user create/update/delete
- Capturing action details: userId, action type, entity type, entity ID, IP address
- Integrating with AuditLogService from TASK12
- Applying to relevant controller methods via annotations or pointcuts

### This Task IS NOT:
- Creating AuditLogService (TASK12 complete)
- Creating admin audit log UI (TASK13 complete)
- Implementing system error logging (already in GlobalExceptionHandler)

### Anti-Hallucination Anchors:
- If audit log schema unclear -> Follow AI_PROMPT.md:L244-251 exactly
- If AOP pattern unclear -> Use @Aspect with @AfterReturning
- If action types unclear -> Use: LOGIN, LOGOUT, UPLOAD, DELETE, USER_CREATE, USER_UPDATE, USER_DELETE, CONFIG_UPDATE

### Guardrails (Prohibitions):
**Scope Guardrails:**
- [ ] DO NOT log every API call - only significant actions
- [ ] DO NOT log read-only operations - only mutations

**Architecture Guardrails:**
- [ ] DO NOT use interceptors - use Spring AOP @Aspect
- [ ] DO NOT block request processing - async logging preferred

**Quality Guardrails:**
- [ ] DO NOT log sensitive data (passwords, API keys)
- [ ] DO NOT over-complicate action categorization

**Security Guardrails:**
- [ ] NEVER log passwords or credentials
- [ ] NEVER log full API keys - only action occurred

## 2. CONTEXT CHAIN

### Priority 0 - LEGACY REFERENCE:
None - no legacy systems configured

### Priority 1 - READ FIRST (Required):
- `../AI_PROMPT.md` - Audit log requirements (§4)
- AI_PROMPT.md:L486 for audit log acceptance criteria
- AI_PROMPT.md:L641-645 for audit logging implementation guidance

### Priority 2 - READ BEFORE CODING:
- AI_PROMPT.md:L244-251 for audit_logs table schema
- TASK12 AuditLogService for logging method
- Existing controllers for method signatures

### Priority 3 - REFERENCE IF NEEDED:
- Context7 for Spring AOP patterns

### Inherited From Dependencies:
- TASK12: AuditLogService.logAction() method
- TASK1: AuthController methods to audit
- TASK3: FileController methods to audit
- TASK12: AdminController methods to audit

## 3. EXECUTION CONTRACT

### 3.1 Pre-Conditions (VERIFY BEFORE ANY CODE):
| Check | Command | Expected |
|-------|---------|----------|
| TASK12 AuditLogService exists | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/AuditLogService.java` | File exists |
| AuthController exists | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/controller/AuthController.java` | File exists |
| FileController exists | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/controller/FileController.java` | File exists |

**HARD STOP:** If ANY check fails -> status: blocked

### 3.2 Success Criteria (VERIFY AFTER COMPLETE):
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Backend compiles | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/backend && ./gradlew build -x test --quiet` | - |
| AuditLogAspect exists | AI_PROMPT.md:L641 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/aspect/AuditLogAspect.java` | - |
| Aspect annotation present | AI_PROMPT.md:L641 | AUTO | `grep -q "@Aspect" /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/aspect/AuditLogAspect.java` | - |
| AuditLogService injected | AI_PROMPT.md:L641 | AUTO | `grep -q "AuditLogService" /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/aspect/AuditLogAspect.java` | - |

### 3.3 Output Artifacts:
| Artifact | Type | Path | Verification |
|----------|------|------|--------------|
| AuditLogAspect | CREATE | backend/src/main/java/com/cbrviewer/aspect/AuditLogAspect.java | `test -f` |

## 4. IMPLEMENTATION STRATEGY

### Phase 1: Preparation
1. Read AI_PROMPT.md audit log requirements
2. Verify TASK12 AuditLogService exists
3. Review controller methods to audit

**Gate:** All pre-conditions verified

### Phase 2: Create Audit Aspect
1. Create aspect/AuditLogAspect.java:
   - @Aspect and @Component annotations
   - Inject AuditLogService
   - Inject HttpServletRequest for IP address
2. Define pointcuts for auditable methods:
   - AuthController: login, logout, register
   - FileController: uploadFile, deleteFile
   - AdminController: createUser, updateUser, deleteUser, updateLlmConfig

**Gate:** Aspect structure compiles

### Phase 3: Implement Audit Methods
1. Create @AfterReturning advice for each action type:
   - LOGIN: after successful login
   - LOGOUT: after logout
   - REGISTER: after user registration
   - FILE_UPLOAD: after file upload
   - FILE_DELETE: after file deletion
   - USER_CREATE: after admin creates user
   - USER_UPDATE: after admin updates user
   - USER_DELETE: after admin deletes user
   - CONFIG_UPDATE: after LLM config update
2. Extract userId from session/SecurityContext
3. Extract entity ID from method arguments or return value
4. Get IP address from HttpServletRequest
5. Call AuditLogService.logAction() with details

**Gate:** Audit methods compile

### Phase 4: Handle Edge Cases
1. Handle null user (system actions)
2. Handle failed actions (use @AfterThrowing if needed)
3. Ensure async logging doesn't block request

**Gate:** Edge cases handled

### Phase 5: Validation
1. Verify backend compiles: `./gradlew build -x test`
2. Verify aspect file created
3. Verify @Aspect annotation present
4. Verify all action types covered
5. Mark task complete

## 5. UNCERTAINTY LOG

| ID | Topic | Assumption | Confidence | Evidence |
|----|-------|------------|------------|----------|
| U1 | User ID extraction | Using SecurityContextHolder.getContext() | HIGH | Standard Spring Security |
| U2 | IP address source | Using HttpServletRequest.getRemoteAddr() | HIGH | Standard approach |
| U3 | Async logging | Using @Async on logAction if needed | MEDIUM | May impact performance |
| U4 | Pointcut expressions | Using execution() with method patterns | HIGH | Standard AOP |

### Stop Rule:
LOW confidence on critical decision -> BLOCKED (do not proceed with guesses)

## 6. INTEGRATION IMPACT

### Files Modified:
None - aspect auto-applies via Spring AOP

### Files Created:
| File | Imports From | Exports |
|------|--------------|---------|
| AuditLogAspect.java | AuditLogService, HttpServletRequest, SecurityContext | Audit logging aspect |

### Breaking Changes:
None - transparent aspect application
