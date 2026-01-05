<!-- BLUEPRINT: Read-only after creation -->
@dependencies [TASK0]
@difficulty medium
@files [backend/src/main/java/com/cbrviewer/config/SecurityConfig.java, backend/src/main/java/com/cbrviewer/config/CorsConfig.java, backend/src/main/java/com/cbrviewer/controller/AuthController.java, backend/src/main/java/com/cbrviewer/service/AuthService.java, backend/src/main/java/com/cbrviewer/model/User.java, backend/src/main/java/com/cbrviewer/repository/UserRepository.java, backend/src/main/java/com/cbrviewer/dto/LoginRequest.java, backend/src/main/java/com/cbrviewer/dto/RegisterRequest.java, backend/src/main/java/com/cbrviewer/dto/UserDto.java, backend/src/main/java/com/cbrviewer/exception/GlobalExceptionHandler.java]

# BLUEPRINT: TASK1

## 1. IDENTITY

### This Task IS:
- Implementing Spring Security configuration for session-based authentication
- Creating User entity with id, username, email, password_hash, role, timestamps
- Creating UserRepository with Spring Data JDBC
- Creating AuthService with login, logout, register, getCurrentUser methods
- Creating AuthController with POST /api/auth/login, POST /api/auth/logout, POST /api/auth/register, GET /api/auth/me endpoints
- Implementing BCrypt password hashing
- Configuring session timeout (24 hours) and HTTP-only secure cookies
- Implementing CORS configuration for frontend origin

### This Task IS NOT:
- Creating frontend auth UI (TASK2)
- Creating admin user management endpoints (TASK12)
- Implementing file management endpoints (TASK3)
- Implementing audit logging (TASK15)

### Anti-Hallucination Anchors:
- If session configuration unclear -> Follow AI_PROMPT.md:L832-843 exactly
- If User entity fields unclear -> Follow AI_PROMPT.md:L159-168 schema
- If endpoint paths unclear -> Follow AI_PROMPT.md:L267-272 API spec
- If JWT mentioned anywhere -> REJECT - session-based auth only (AI_PROMPT.md:L515)

### Guardrails (Prohibitions):
**Scope Guardrails:**
- [ ] DO NOT implement admin user CRUD endpoints - belongs to TASK12
- [ ] DO NOT implement frontend auth components - belongs to TASK2
- [ ] DO NOT implement audit logging - belongs to TASK15

**Architecture Guardrails:**
- [ ] DO NOT use JWT tokens - session-based auth was explicitly chosen (AI_PROMPT.md:L515)
- [ ] DO NOT disable CSRF protection for session-based auth (AI_PROMPT.md:L532)
- [ ] DO NOT use in-memory user store - use SQLite via repository

**Quality Guardrails:**
- [ ] DO NOT over-engineer password validation - basic BCrypt is sufficient
- [ ] DO NOT add complex role hierarchy - only USER and ADMIN roles

**Security Guardrails:**
- [ ] NEVER store passwords in plain text - use BCrypt (AI_PROMPT.md:L531)
- [ ] NEVER expose stack traces to frontend - log and return generic error (AI_PROMPT.md:L529)
- [ ] NEVER trust user input - validate all inputs (AI_PROMPT.md:L530)

## 2. CONTEXT CHAIN

### Priority 0 - LEGACY REFERENCE:
None - no legacy systems configured

### Priority 1 - READ FIRST (Required):
- `../AI_PROMPT.md` - Full tech stack, security requirements
- AI_PROMPT.md:L429-436 for authentication acceptance criteria
- AI_PROMPT.md:L267-272 for auth API endpoints

### Priority 2 - READ BEFORE CODING:
- AI_PROMPT.md:L316-340 for REST controller pattern
- AI_PROMPT.md:L159-168 for User table schema
- AI_PROMPT.md:L832-843 for session configuration

### Priority 3 - REFERENCE IF NEEDED:
- AI_PROMPT.md:L505-533 for security guardrails
- Context7 for Spring Security 6.x session configuration

### Inherited From Dependencies:
- TASK0: Project structure, build.gradle with Spring Security dependency, application.yml base config, schema.sql with users table

## 3. EXECUTION CONTRACT

### 3.1 Pre-Conditions (VERIFY BEFORE ANY CODE):
| Check | Command | Expected |
|-------|---------|----------|
| TASK0 completed | `test -f /home/stress/projects/cbr_viewer/backend/build.gradle` | File exists |
| Schema has users table | `grep -q "CREATE TABLE users" /home/stress/projects/cbr_viewer/backend/src/main/resources/schema.sql` | Exit 0 |
| Spring Security in build.gradle | `grep -q "spring-boot-starter-security" /home/stress/projects/cbr_viewer/backend/build.gradle` | Exit 0 |

**HARD STOP:** If ANY check fails -> status: blocked

### 3.2 Success Criteria (VERIFY AFTER COMPLETE):
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Backend compiles | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/backend && ./gradlew build -x test --quiet` | - |
| SecurityConfig exists | AI_PROMPT.md:L59 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/config/SecurityConfig.java` | - |
| AuthController exists | AI_PROMPT.md:L63 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/controller/AuthController.java` | - |
| User entity exists | AI_PROMPT.md:L84 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/model/User.java` | - |
| BCrypt used | AI_PROMPT.md:L531 | AUTO | `grep -q "BCrypt" /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/AuthService.java` | - |
| Session cookie HTTP-only | AI_PROMPT.md:L432 | AUTO | `grep -q "http-only: true" /home/stress/projects/cbr_viewer/backend/src/main/resources/application.yml` | - |
| Login endpoint defined | AI_PROMPT.md:L268 | AUTO | `grep -q "/auth/login" /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/controller/AuthController.java` | - |

### 3.3 Output Artifacts:
| Artifact | Type | Path | Verification |
|----------|------|------|--------------|
| SecurityConfig | CREATE | backend/src/main/java/com/cbrviewer/config/SecurityConfig.java | `test -f` |
| CorsConfig | CREATE | backend/src/main/java/com/cbrviewer/config/CorsConfig.java | `test -f` |
| AuthController | CREATE | backend/src/main/java/com/cbrviewer/controller/AuthController.java | `test -f` |
| AuthService | CREATE | backend/src/main/java/com/cbrviewer/service/AuthService.java | `test -f` |
| User entity | CREATE | backend/src/main/java/com/cbrviewer/model/User.java | `test -f` |
| UserRepository | CREATE | backend/src/main/java/com/cbrviewer/repository/UserRepository.java | `test -f` |
| DTOs | CREATE | backend/src/main/java/com/cbrviewer/dto/*.java | `test -d` |
| GlobalExceptionHandler | CREATE | backend/src/main/java/com/cbrviewer/exception/GlobalExceptionHandler.java | `test -f` |

## 4. IMPLEMENTATION STRATEGY

### Phase 1: Preparation
1. Read AI_PROMPT.md security requirements (§4, §4.1)
2. Verify TASK0 artifacts exist
3. Review User table schema from schema.sql

**Gate:** All pre-conditions verified, security requirements understood

### Phase 2: User Model Layer
1. Create User.java entity with fields: id, username, email, passwordHash, role, createdAt, updatedAt
2. Create UserRepository.java with findByUsername, findByEmail, existsByUsername, existsByEmail methods
3. Create DTOs: LoginRequest (username, password), RegisterRequest (username, email, password), UserDto (id, username, email, role)

**Gate:** Model classes compile, match schema.sql

### Phase 3: Service Layer
1. Create AuthService with:
   - register(RegisterRequest) - validate unique username/email, hash password, save user
   - login(username, password) - verify credentials, return user
   - getCurrentUser(session) - get user from session
   - logout(session) - invalidate session
2. Use BCryptPasswordEncoder for password hashing
3. Throw appropriate exceptions for validation failures

**Gate:** AuthService compiles, password hashing works

### Phase 4: Security Configuration
1. Create SecurityConfig.java:
   - Configure session management (always create session)
   - Set session timeout to 24 hours
   - Configure HTTP-only secure cookies
   - Define public paths: /api/auth/login, /api/auth/register
   - Require authentication for all other paths
   - Configure CSRF (enabled for session auth)
2. Create CorsConfig.java:
   - Allow frontend origin (http://localhost:5173)
   - Allow credentials

**Gate:** Security config compiles, follows AI_PROMPT.md:L832-843

### Phase 5: Controller Layer
1. Create AuthController.java with:
   - POST /api/auth/login - authenticate user, create session
   - POST /api/auth/logout - invalidate session
   - POST /api/auth/register - register new user
   - GET /api/auth/me - return current user info
2. Follow REST controller pattern from AI_PROMPT.md:L316-340
3. Create GlobalExceptionHandler for consistent error responses

**Gate:** All endpoints defined, follow API spec

### Phase 6: Validation
1. Verify backend compiles: `./gradlew build -x test`
2. Verify all files created
3. Verify BCrypt is used for passwords
4. Verify session config matches AI_PROMPT.md
5. Mark task complete

## 5. UNCERTAINTY LOG

| ID | Topic | Assumption | Confidence | Evidence |
|----|-------|------------|------------|----------|
| U1 | Spring Security version | Using Spring Security 6.x with Spring Boot 4 | HIGH | Standard pairing |
| U2 | Session store | Using default in-memory session store for MVP | MEDIUM | AI_PROMPT.md doesn't specify Redis |
| U3 | CSRF token handling | Frontend will include CSRF token from cookie | HIGH | Standard session-based auth pattern |

### Stop Rule:
LOW confidence on critical decision -> BLOCKED (do not proceed with guesses)

## 6. INTEGRATION IMPACT

### Files Modified:
| File | Modification | Who Imports | Impact |
|------|--------------|-------------|--------|
| application.yml | Add session config | Spring Boot | Session behavior |

### Files Created:
| File | Imports From | Exports |
|------|--------------|---------|
| SecurityConfig.java | Spring Security | Security filter chain |
| CorsConfig.java | Spring Web | CORS configuration |
| AuthController.java | AuthService, DTOs | Auth REST endpoints |
| AuthService.java | UserRepository, BCrypt | Auth business logic |
| User.java | - | User entity |
| UserRepository.java | User | Data access |
| DTOs | - | Request/response objects |
| GlobalExceptionHandler.java | - | Error handling |

### Breaking Changes:
None - new endpoints only
