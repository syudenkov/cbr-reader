<!-- BLUEPRINT: Read-only after creation -->
@dependencies [TASK1]
@difficulty medium
@files [frontend/src/store/store.ts, frontend/src/store/hooks.ts, frontend/src/store/slices/authSlice.ts, frontend/src/services/api.ts, frontend/src/components/auth/LoginForm.tsx, frontend/src/components/auth/RegisterForm.tsx, frontend/src/pages/LoginPage.tsx, frontend/src/types/index.ts]

# BLUEPRINT: TASK2

## 1. IDENTITY

### This Task IS:
- Setting up Redux Toolkit store with typed hooks
- Creating authSlice with login, logout, register async thunks
- Creating api.ts service with Axios instance configured for session cookies
- Creating LoginForm.tsx component with MUI inputs
- Creating RegisterForm.tsx component with MUI inputs
- Creating LoginPage.tsx page with toggle between login/register
- Updating App.tsx with routing (react-router-dom)
- Handling session state (isAuthenticated, user, loading, error)

### This Task IS NOT:
- Implementing file browsing UI (TASK4)
- Implementing viewer components (TASK5)
- Implementing admin UI (TASK13)
- Backend auth logic (TASK1 - already complete)

### Anti-Hallucination Anchors:
- If Redux pattern unclear -> Follow AI_PROMPT.md:L367-404 exactly
- If component structure unclear -> Follow AI_PROMPT.md:L130-138 paths
- If API service unclear -> Use Axios with withCredentials: true for cookies

### Guardrails (Prohibitions):
**Scope Guardrails:**
- [ ] DO NOT implement file browsing components - belongs to TASK4
- [ ] DO NOT implement viewer components - belongs to TASK5
- [ ] DO NOT implement admin components - belongs to TASK13

**Architecture Guardrails:**
- [ ] DO NOT use React Context for state - Redux Toolkit was chosen (AI_PROMPT.md:L517)
- [ ] DO NOT store JWT tokens - session cookies are used (AI_PROMPT.md:L515)
- [ ] DO NOT use fetch - Axios was specified (AI_PROMPT.md:L42)

**Quality Guardrails:**
- [ ] DO NOT add complex form validation library - basic validation is sufficient
- [ ] DO NOT create unused utility functions

**Security Guardrails:**
- [ ] NEVER store credentials in localStorage - use session cookies only
- [ ] DO NOT expose password in state - only store user info after login

## 2. CONTEXT CHAIN

### Priority 0 - LEGACY REFERENCE:
None - no legacy systems configured

### Priority 1 - READ FIRST (Required):
- `../AI_PROMPT.md` - Redux patterns (§3), component structure (§2)
- AI_PROMPT.md:L367-404 for Redux slice pattern
- AI_PROMPT.md:L130-138 for component file paths

### Priority 2 - READ BEFORE CODING:
- AI_PROMPT.md:L105-113 for store structure
- AI_PROMPT.md:L267-272 for auth API endpoints
- AI_PROMPT.md:L407-423 for MUI theme integration

### Priority 3 - REFERENCE IF NEEDED:
- Context7 for Redux Toolkit async thunk patterns
- Context7 for MUI form components

### Inherited From Dependencies:
- TASK0: React project setup, MUI theme, vite.config.ts
- TASK1: Backend auth endpoints (login, logout, register, me)

## 3. EXECUTION CONTRACT

### 3.1 Pre-Conditions (VERIFY BEFORE ANY CODE):
| Check | Command | Expected |
|-------|---------|----------|
| TASK0 frontend exists | `test -f /home/stress/projects/cbr_viewer/frontend/package.json` | File exists |
| TASK1 auth endpoints exist | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/controller/AuthController.java` | File exists |
| Redux Toolkit in package.json | `grep -q "@reduxjs/toolkit" /home/stress/projects/cbr_viewer/frontend/package.json` | Exit 0 |
| React Router in package.json | `grep -q "react-router-dom" /home/stress/projects/cbr_viewer/frontend/package.json` | Exit 0 |

**HARD STOP:** If ANY check fails -> status: blocked

### 3.2 Success Criteria (VERIFY AFTER COMPLETE):
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Frontend compiles | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/frontend && npm run build --silent` | - |
| Store configured | AI_PROMPT.md:L105 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/store/store.ts` | - |
| authSlice exists | AI_PROMPT.md:L108 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/store/slices/authSlice.ts` | - |
| LoginForm exists | AI_PROMPT.md:L131 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/components/auth/LoginForm.tsx` | - |
| RegisterForm exists | AI_PROMPT.md:L132 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/components/auth/RegisterForm.tsx` | - |
| API service uses cookies | AI_PROMPT.md:L432 | AUTO | `grep -q "withCredentials" /home/stress/projects/cbr_viewer/frontend/src/services/api.ts` | - |

### 3.3 Output Artifacts:
| Artifact | Type | Path | Verification |
|----------|------|------|--------------|
| Redux store | CREATE | frontend/src/store/store.ts | `test -f` |
| Store hooks | CREATE | frontend/src/store/hooks.ts | `test -f` |
| authSlice | CREATE | frontend/src/store/slices/authSlice.ts | `test -f` |
| API service | CREATE | frontend/src/services/api.ts | `test -f` |
| LoginForm | CREATE | frontend/src/components/auth/LoginForm.tsx | `test -f` |
| RegisterForm | CREATE | frontend/src/components/auth/RegisterForm.tsx | `test -f` |
| LoginPage | CREATE | frontend/src/pages/LoginPage.tsx | `test -f` |
| Types | CREATE | frontend/src/types/index.ts | `test -f` |

## 4. IMPLEMENTATION STRATEGY

### Phase 1: Preparation
1. Read AI_PROMPT.md Redux patterns and component structure
2. Verify TASK0 and TASK1 artifacts exist
3. Review auth API endpoints from AI_PROMPT.md:L267-272

**Gate:** All pre-conditions verified, patterns understood

### Phase 2: Foundation Setup
1. Create types/index.ts with User, LoginRequest, RegisterRequest, AuthState types
2. Create services/api.ts with Axios instance:
   - baseURL from VITE_API_BASE_URL env var
   - withCredentials: true for session cookies
   - Interceptors for error handling

**Gate:** Types and API service compile

### Phase 3: Redux Store
1. Create store/store.ts with configureStore
2. Create store/hooks.ts with typed useAppDispatch, useAppSelector
3. Create store/slices/authSlice.ts with:
   - State: user, isAuthenticated, status, error
   - Async thunks: login, logout, register, checkAuth
   - Reducers for status transitions
   - Follow AI_PROMPT.md:L367-404 pattern exactly

**Gate:** Store compiles, exports typed hooks

### Phase 4: Auth Components
1. Create components/auth/LoginForm.tsx:
   - MUI TextField for username and password
   - MUI Button for submit
   - Error display with MUI Alert
   - Loading state handling
2. Create components/auth/RegisterForm.tsx:
   - MUI TextField for username, email, password
   - MUI Button for submit
   - Error display and validation feedback

**Gate:** Components compile, use MUI correctly

### Phase 5: Pages and Routing
1. Create pages/LoginPage.tsx:
   - Toggle between LoginForm and RegisterForm
   - Redirect to home on successful auth
2. Update App.tsx:
   - Add Redux Provider
   - Add BrowserRouter
   - Add routes for login and protected routes
   - Add auth check on app load

**Gate:** Frontend builds, routing works

### Phase 6: Validation
1. Verify frontend compiles: `npm run build`
2. Verify all files created
3. Verify API service uses withCredentials
4. Verify Redux slice follows pattern
5. Mark task complete

## 5. UNCERTAINTY LOG

| ID | Topic | Assumption | Confidence | Evidence |
|----|-------|------------|------------|----------|
| U1 | CSRF handling | Backend will set CSRF token in cookie, frontend reads it | MEDIUM | Standard Spring Security pattern |
| U2 | Protected route handling | Using custom ProtectedRoute component | HIGH | Standard React Router pattern |
| U3 | Session check on load | Call /api/auth/me on app mount to check session | HIGH | AI_PROMPT.md:L271 |

### Stop Rule:
LOW confidence on critical decision -> BLOCKED (do not proceed with guesses)

## 6. INTEGRATION IMPACT

### Files Modified:
| File | Modification | Who Imports | Impact |
|------|--------------|-------------|--------|
| App.tsx | Add Provider, Router, routes | main.tsx | App structure |
| main.tsx | Import store | - | Store setup |

### Files Created:
| File | Imports From | Exports |
|------|--------------|---------|
| store/store.ts | authSlice | store, RootState, AppDispatch |
| store/hooks.ts | store | useAppDispatch, useAppSelector |
| store/slices/authSlice.ts | api, types | authSlice, async thunks |
| services/api.ts | axios | api instance |
| components/auth/LoginForm.tsx | MUI, authSlice | LoginForm |
| components/auth/RegisterForm.tsx | MUI, authSlice | RegisterForm |
| pages/LoginPage.tsx | LoginForm, RegisterForm | LoginPage |
| types/index.ts | - | User, LoginRequest, etc. |

### Breaking Changes:
None - App.tsx modification adds functionality
