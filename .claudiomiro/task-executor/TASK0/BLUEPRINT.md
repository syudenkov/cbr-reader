<!-- BLUEPRINT: Read-only after creation -->
@dependencies []
@difficulty medium
@files [backend/build.gradle, backend/settings.gradle, backend/src/main/java/com/cbrviewer/CbrViewerApplication.java, backend/src/main/resources/application.yml, backend/src/main/resources/schema.sql, frontend/package.json, frontend/tsconfig.json, frontend/vite.config.ts, frontend/index.html, frontend/src/main.tsx, frontend/src/App.tsx, frontend/src/theme/theme.ts]

# BLUEPRINT: TASK0

## 1. IDENTITY

### This Task IS:
- Initializing Spring Boot 4 backend project with Gradle
- Initializing React 18 + Vite + TypeScript frontend project
- Creating SQLite database schema with all tables from AI_PROMPT.md
- Configuring application.yml with datasource, session, CORS, and storage settings
- Setting up MUI theme for dark mode
- Establishing monorepo structure as defined in AI_PROMPT.md:§2

### This Task IS NOT:
- Implementing authentication logic (TASK1)
- Implementing file management (TASK3)
- Implementing any business logic or API endpoints
- Creating React components beyond App.tsx scaffold
- Implementing Redux store (TASK2/TASK4)

### Anti-Hallucination Anchors:
- If Spring Boot version unclear -> Use Spring Boot 4.x as specified in AI_PROMPT.md:L24
- If SQLite schema differs from AI_PROMPT.md:L158-262 -> Follow schema exactly
- If frontend structure unclear -> Follow AI_PROMPT.md:L101-148 exactly

### Guardrails (Prohibitions):
**Scope Guardrails:**
- [ ] DO NOT implement controllers, services, or repositories - only project structure
- [ ] DO NOT create Redux store or slices - only theme and App scaffold

**Architecture Guardrails:**
- [ ] DO NOT use JWT or Spring Security config - session-based auth is TASK1
- [ ] DO NOT use H2 or any other database - SQLite only (AI_PROMPT.md:L26)

**Quality Guardrails:**
- [ ] DO NOT add dependencies not listed in AI_PROMPT.md:L22-47
- [ ] DO NOT create documentation files - README is separate deliverable

**Security Guardrails:**
- [ ] NEVER include hardcoded credentials in application.yml
- [ ] DO NOT disable any security defaults prematurely

## 2. CONTEXT CHAIN

### Priority 0 - LEGACY REFERENCE:
None - no legacy systems configured

### Priority 1 - READ FIRST (Required):
- `../AI_PROMPT.md` - Full tech stack (§2), project structure (§2:L49-154), database schema (§2:L156-262), configuration (§9)
- AI_PROMPT.md:L49-154 for exact directory structure

### Priority 2 - READ BEFORE CODING:
- AI_PROMPT.md:L158-262 for complete SQLite schema
- AI_PROMPT.md:L829-859 for application.yml configuration
- AI_PROMPT.md:L407-423 for MUI theme pattern

### Priority 3 - REFERENCE IF NEEDED:
- AI_PROMPT.md:L861-866 for frontend environment variables
- Context7 for Spring Boot 4 + SQLite configuration if needed

### Inherited From Dependencies:
None - this is Layer 0

## 3. EXECUTION CONTRACT

### 3.1 Pre-Conditions (VERIFY BEFORE ANY CODE):
| Check | Command | Expected |
|-------|---------|----------|
| Working directory exists | `test -d /home/stress/projects/cbr_viewer` | Directory exists |
| Java 21 available | `java -version 2>&1 | grep -q "21"` | Exit 0 |
| Node.js available | `node --version` | Version output |
| npm available | `npm --version` | Version output |

**HARD STOP:** If ANY check fails -> status: blocked

### 3.2 Success Criteria (VERIFY AFTER COMPLETE):
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Backend compiles | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/backend && ./gradlew build -x test --quiet` | - |
| Frontend compiles | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/frontend && npm run build --silent` | - |
| SQLite schema created | AI_PROMPT.md:§2 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/resources/schema.sql` | - |
| All 8 tables defined | AI_PROMPT.md:L158-262 | AUTO | `grep -c "CREATE TABLE" /home/stress/projects/cbr_viewer/backend/src/main/resources/schema.sql` | Output should be 8 |
| Storage directories configured | AI_PROMPT.md:§9 | AUTO | `grep -q "comics-path" /home/stress/projects/cbr_viewer/backend/src/main/resources/application.yml` | - |
| MUI theme configured | AI_PROMPT.md:L407-423 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/theme/theme.ts` | - |

### 3.3 Output Artifacts:
| Artifact | Type | Path | Verification |
|----------|------|------|--------------|
| Backend project | CREATE | backend/ | `test -d backend/src/main/java/com/cbrviewer` |
| Frontend project | CREATE | frontend/ | `test -d frontend/src` |
| SQLite schema | CREATE | backend/src/main/resources/schema.sql | `test -f` |
| Application config | CREATE | backend/src/main/resources/application.yml | `test -f` |
| MUI theme | CREATE | frontend/src/theme/theme.ts | `test -f` |
| Vite config | CREATE | frontend/vite.config.ts | `test -f` |

## 4. IMPLEMENTATION STRATEGY

### Phase 1: Preparation
1. Read AI_PROMPT.md sections §2, §9 for exact structure and config
2. Verify Java 21 and Node.js are available
3. Plan directory structure based on AI_PROMPT.md:L49-154

**Gate:** All pre-conditions verified, structure understood

### Phase 2: Backend Foundation
1. Create backend directory structure: src/main/java/com/cbrviewer/, src/main/resources/
2. Create build.gradle with Spring Boot 4.x, SQLite, junrar dependencies
3. Create settings.gradle with project name
4. Create CbrViewerApplication.java with @SpringBootApplication
5. Create application.yml following AI_PROMPT.md:L829-859 pattern
6. Create schema.sql with all 8 tables from AI_PROMPT.md:L158-262

**Gate:** Backend compiles with `./gradlew build -x test`

### Phase 3: Frontend Foundation
1. Create frontend directory with Vite + React + TypeScript
2. Create package.json with React 18, MUI, Redux Toolkit, Axios, Vite dependencies
3. Create tsconfig.json with strict mode
4. Create vite.config.ts with API proxy to backend
5. Create index.html with root div
6. Create main.tsx with React root render
7. Create App.tsx with basic MUI ThemeProvider wrapper
8. Create theme/theme.ts with dark mode theme (AI_PROMPT.md:L407-423)

**Gate:** Frontend compiles with `npm run build`

### Phase 4: Storage Structure
1. Create storage/comics/ directory
2. Create storage/audio/ directory
3. Ensure application.yml references these paths

**Gate:** Storage directories exist

### Phase 5: Validation
1. Verify backend compiles: `./gradlew build -x test`
2. Verify frontend compiles: `npm run build`
3. Verify schema.sql has all 8 tables
4. Verify application.yml has all required config
5. Mark task complete

## 5. UNCERTAINTY LOG

| ID | Topic | Assumption | Confidence | Evidence |
|----|-------|------------|------------|----------|
| U1 | Spring Boot 4 release | Assuming Spring Boot 4.x is available | MEDIUM | AI_PROMPT.md:L24 specifies 4.x, may need 3.x if 4.x not stable |
| U2 | SQLite JDBC driver | Using org.xerial:sqlite-jdbc | HIGH | Standard SQLite JDBC driver |
| U3 | Gradle version | Using Gradle 8.x wrapper | HIGH | Standard for Spring Boot 4 |

### Stop Rule:
LOW confidence on critical decision -> BLOCKED (do not proceed with guesses)

## 6. INTEGRATION IMPACT

### Files Modified:
None - all files are new

### Files Created:
| File | Imports From | Exports |
|------|--------------|---------|
| backend/build.gradle | - | Project dependencies |
| backend/settings.gradle | - | Project name |
| backend/src/main/java/com/cbrviewer/CbrViewerApplication.java | Spring Boot | Main class |
| backend/src/main/resources/application.yml | - | Configuration |
| backend/src/main/resources/schema.sql | - | Database schema |
| frontend/package.json | - | Project dependencies |
| frontend/tsconfig.json | - | TypeScript config |
| frontend/vite.config.ts | - | Vite config |
| frontend/index.html | - | HTML entry |
| frontend/src/main.tsx | React, App, theme | React root |
| frontend/src/App.tsx | MUI, theme | App component |
| frontend/src/theme/theme.ts | MUI | Theme object |

### Breaking Changes:
None - greenfield project
