<!-- BLUEPRINT: Read-only after creation -->
@dependencies [TASK2, TASK12]
@difficulty medium
@files [frontend/src/store/slices/adminSlice.ts, frontend/src/pages/AdminPage.tsx, frontend/src/components/admin/UserManagement.tsx, frontend/src/components/admin/FileManagement.tsx, frontend/src/components/admin/LogsView.tsx, frontend/src/components/admin/LlmConfigPanel.tsx, frontend/src/components/common/AdminRoute.tsx]

# BLUEPRINT: TASK13

## 1. IDENTITY

### This Task IS:
- Creating adminSlice with async thunks for users, logs, LLM config
- Creating AdminPage.tsx with tabbed interface for admin sections
- Creating UserManagement.tsx for user CRUD (list, create, edit, delete)
- Creating FileManagement.tsx for file list with delete capability
- Creating LogsView.tsx for viewing system and audit logs with filters
- Creating LlmConfigPanel.tsx for managing API keys
- Creating AdminRoute.tsx component for admin-only route protection

### This Task IS NOT:
- Implementing backend admin logic (TASK12 complete)
- Implementing file upload (TASK4 - already has upload dialog)
- Implementing audit log writing (TASK15)

### Anti-Hallucination Anchors:
- If adminSlice pattern unclear -> Follow AI_PROMPT.md:L367-404 slice pattern
- If admin endpoints unclear -> Follow AI_PROMPT.md:L297-306 spec
- If component structure unclear -> Follow AI_PROMPT.md:L125-129 paths

### Guardrails (Prohibitions):
**Scope Guardrails:**
- [ ] DO NOT implement backend admin logic - TASK12 complete
- [ ] DO NOT modify existing file upload - only add admin file list

**Architecture Guardrails:**
- [ ] DO NOT use React Context - Redux Toolkit only
- [ ] DO NOT create separate admin store - use main store

**Quality Guardrails:**
- [ ] DO NOT add complex data tables - MUI Table is sufficient
- [ ] DO NOT add export functionality - not in requirements

**Security Guardrails:**
- [ ] NEVER display full API keys - show masked version
- [ ] NEVER allow admin actions without confirmation dialogs

## 2. CONTEXT CHAIN

### Priority 0 - LEGACY REFERENCE:
None - no legacy systems configured

### Priority 1 - READ FIRST (Required):
- `../AI_PROMPT.md` - Admin UI requirements (§4), Redux patterns (§3)
- AI_PROMPT.md:L482-488 for admin acceptance criteria
- AI_PROMPT.md:L125-129 for admin component paths

### Priority 2 - READ BEFORE CODING:
- AI_PROMPT.md:L297-306 for admin API endpoints
- AI_PROMPT.md:L112 for adminSlice path
- AI_PROMPT.md:L627-634 for admin frontend implementation guidance

### Priority 3 - REFERENCE IF NEEDED:
- Context7 for MUI Table, Tabs, Dialog components
- TASK12 for API response structures

### Inherited From Dependencies:
- TASK2: Redux store, api.ts, authSlice (user role check)
- TASK12: Admin API endpoints (users, logs, llm-config)

## 3. EXECUTION CONTRACT

### 3.1 Pre-Conditions (VERIFY BEFORE ANY CODE):
| Check | Command | Expected |
|-------|---------|----------|
| TASK2 store exists | `test -f /home/stress/projects/cbr_viewer/frontend/src/store/store.ts` | File exists |
| TASK12 AdminController exists | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/controller/AdminController.java` | File exists |
| authSlice has role | `grep -q "role" /home/stress/projects/cbr_viewer/frontend/src/store/slices/authSlice.ts` | Exit 0 |

**HARD STOP:** If ANY check fails -> status: blocked

### 3.2 Success Criteria (VERIFY AFTER COMPLETE):
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Frontend compiles | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/frontend && npm run build --silent` | - |
| adminSlice exists | AI_PROMPT.md:L112 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/store/slices/adminSlice.ts` | - |
| AdminPage exists | AI_PROMPT.md:L137 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/pages/AdminPage.tsx` | - |
| UserManagement exists | AI_PROMPT.md:L126 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/components/admin/UserManagement.tsx` | - |
| LogsView exists | AI_PROMPT.md:L128 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/components/admin/LogsView.tsx` | - |
| LlmConfigPanel exists | AI_PROMPT.md:L129 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/components/admin/LlmConfigPanel.tsx` | - |
| Admin route protection | AI_PROMPT.md:L482 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/components/common/AdminRoute.tsx` | - |

### 3.3 Output Artifacts:
| Artifact | Type | Path | Verification |
|----------|------|------|--------------|
| adminSlice | CREATE | frontend/src/store/slices/adminSlice.ts | `test -f` |
| AdminPage | CREATE | frontend/src/pages/AdminPage.tsx | `test -f` |
| UserManagement | CREATE | frontend/src/components/admin/UserManagement.tsx | `test -f` |
| FileManagement | CREATE | frontend/src/components/admin/FileManagement.tsx | `test -f` |
| LogsView | CREATE | frontend/src/components/admin/LogsView.tsx | `test -f` |
| LlmConfigPanel | CREATE | frontend/src/components/admin/LlmConfigPanel.tsx | `test -f` |
| AdminRoute | CREATE | frontend/src/components/common/AdminRoute.tsx | `test -f` |

## 4. IMPLEMENTATION STRATEGY

### Phase 1: Preparation
1. Read AI_PROMPT.md admin UI requirements
2. Verify TASK2 and TASK12 artifacts exist
3. Review admin API endpoints

**Gate:** All pre-conditions verified, requirements understood

### Phase 2: Types and Slice
1. Add admin types to types/index.ts: AdminUser, LlmConfigItem, LogEntry, AdminState
2. Create adminSlice.ts with:
   - State: users, files, systemLogs, auditLogs, llmConfigs, status, error
   - Async thunks: fetchUsers, createUser, updateUser, deleteUser
   - Async thunks: fetchLogs, fetchAuditLogs
   - Async thunks: fetchLlmConfigs, updateLlmConfig
3. Add adminSlice to store.ts

**Gate:** adminSlice compiles, added to store

### Phase 3: Admin Route Protection
1. Create AdminRoute.tsx:
   - Wrapper component that checks user.role === 'ADMIN'
   - Redirects to home if not admin
   - Shows loading while checking auth

**Gate:** AdminRoute compiles

### Phase 4: Admin Components - Users
1. Create UserManagement.tsx:
   - MUI Table with columns: username, email, role, created, actions
   - Create button opens dialog with form
   - Edit button opens dialog with pre-filled form
   - Delete button with confirmation dialog
   - Role dropdown (USER, ADMIN)

**Gate:** UserManagement compiles

### Phase 5: Admin Components - Files and Logs
1. Create FileManagement.tsx:
   - MUI Table with columns: filename, type, size, pages, uploaded by, actions
   - Delete button with confirmation
   - Use existing filesSlice for data
2. Create LogsView.tsx:
   - Tabs: System Logs, Audit Logs
   - Filter by level (INFO, WARN, ERROR)
   - Filter by date range
   - MUI Table with timestamp, level, message/action
   - Pagination

**Gate:** Components compile, display data

### Phase 6: Admin Components - LLM Config
1. Create LlmConfigPanel.tsx:
   - List of configured providers (OpenAI, Claude, Minimax)
   - Show masked API key (****...****last4)
   - Edit button to update API key
   - Toggle for active/inactive
   - Priority number for fallback order
   - Save button with confirmation

**Gate:** LlmConfigPanel compiles

### Phase 7: Admin Page
1. Create AdminPage.tsx:
   - MUI Tabs: Users, Files, Logs, LLM Config
   - Render appropriate component based on tab
   - Fetch data on tab change
2. Update App.tsx:
   - Add /admin route wrapped in AdminRoute

**Gate:** AdminPage compiles, routing works

### Phase 8: Validation
1. Verify frontend compiles: `npm run build`
2. Verify all files created
3. Verify admin route protection
4. Verify role check in AdminRoute
5. Mark task complete

## 5. UNCERTAINTY LOG

| ID | Topic | Assumption | Confidence | Evidence |
|----|-------|------------|------------|----------|
| U1 | API key masking | Backend returns masked key, frontend shows as-is | HIGH | TASK12 masks in response |
| U2 | File management data | Reuse filesSlice or create separate endpoint | MEDIUM | May need admin-specific endpoint |
| U3 | Log pagination | Default 50 entries per page | MEDIUM | Not specified |

### Stop Rule:
LOW confidence on critical decision -> BLOCKED (do not proceed with guesses)

## 6. INTEGRATION IMPACT

### Files Modified:
| File | Modification | Who Imports | Impact |
|------|--------------|-------------|--------|
| store/store.ts | Add adminSlice | Admin components | State management |
| App.tsx | Add admin route | - | Routing |
| types/index.ts | Add admin types | adminSlice, components | Type definitions |

### Files Created:
| File | Imports From | Exports |
|------|--------------|---------|
| store/slices/adminSlice.ts | api, types | adminSlice, async thunks |
| pages/AdminPage.tsx | All admin components, MUI Tabs | AdminPage |
| components/admin/UserManagement.tsx | adminSlice, MUI | UserManagement |
| components/admin/FileManagement.tsx | filesSlice, MUI | FileManagement |
| components/admin/LogsView.tsx | adminSlice, MUI | LogsView |
| components/admin/LlmConfigPanel.tsx | adminSlice, MUI | LlmConfigPanel |
| components/common/AdminRoute.tsx | authSlice, react-router | AdminRoute |

### Breaking Changes:
None - new components and routes
