<!-- BLUEPRINT: Read-only after creation -->
@dependencies [TASK2, TASK3]
@difficulty medium
@files [frontend/src/store/slices/filesSlice.ts, frontend/src/components/library/FileGrid.tsx, frontend/src/components/library/FileCard.tsx, frontend/src/pages/LibraryPage.tsx, frontend/src/pages/HomePage.tsx, frontend/src/components/library/FileUploadDialog.tsx]

# BLUEPRINT: TASK4

## 1. IDENTITY

### This Task IS:
- Creating filesSlice with async thunks for fetchFiles, uploadFile, deleteFile
- Creating FileGrid.tsx component displaying files in MUI grid layout
- Creating FileCard.tsx component with cover image, title, average rating display
- Creating LibraryPage.tsx page with file grid and pagination
- Creating HomePage.tsx with redirect to library or login
- Creating FileUploadDialog.tsx for admin file upload (MUI Dialog)
- Integrating with file API endpoints from TASK3

### This Task IS NOT:
- Implementing the viewer component (TASK5)
- Implementing reading progress tracking (TASK6)
- Implementing rating interaction (TASK7 - only display here)
- Backend file management (TASK3 - already complete)

### Anti-Hallucination Anchors:
- If filesSlice pattern unclear -> Follow AI_PROMPT.md:L367-404 slice pattern exactly
- If FileCard layout unclear -> Use MUI Card component
- If pagination unclear -> Use MUI Pagination component
- If file list API response unclear -> Check TASK3 FileDto structure

### Guardrails (Prohibitions):
**Scope Guardrails:**
- [ ] DO NOT implement viewer functionality - belongs to TASK5
- [ ] DO NOT implement rating submission - belongs to TASK7 (only display average)
- [ ] DO NOT implement reading progress - belongs to TASK6

**Architecture Guardrails:**
- [ ] DO NOT use React Context for state - Redux Toolkit only (AI_PROMPT.md:L517)
- [ ] DO NOT implement SSR - client-side rendering only

**Quality Guardrails:**
- [ ] DO NOT add complex filtering/sorting yet - basic list is sufficient for MVP
- [ ] DO NOT create unused components

**Security Guardrails:**
- [ ] DO NOT show upload button to non-admin users
- [ ] NEVER expose file paths to users - display names only

## 2. CONTEXT CHAIN

### Priority 0 - LEGACY REFERENCE:
None - no legacy systems configured

### Priority 1 - READ FIRST (Required):
- `../AI_PROMPT.md` - Redux patterns (§3), component structure (§2)
- AI_PROMPT.md:L367-404 for Redux slice pattern
- AI_PROMPT.md:L121-124 for library component paths

### Priority 2 - READ BEFORE CODING:
- AI_PROMPT.md:L274-280 for file API endpoints
- AI_PROMPT.md:L109 for filesSlice path
- TASK3 FileDto structure for API response types

### Priority 3 - REFERENCE IF NEEDED:
- Context7 for MUI Card, Grid, Pagination components
- AI_PROMPT.md:L500 for pagination default (20 per page)

### Inherited From Dependencies:
- TASK2: Redux store, hooks, api.ts service, types
- TASK3: Backend file endpoints (GET /api/files, GET /api/files/{id}/cover, POST /api/files)

## 3. EXECUTION CONTRACT

### 3.1 Pre-Conditions (VERIFY BEFORE ANY CODE):
| Check | Command | Expected |
|-------|---------|----------|
| TASK2 store exists | `test -f /home/stress/projects/cbr_viewer/frontend/src/store/store.ts` | File exists |
| TASK3 FileController exists | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/controller/FileController.java` | File exists |
| API service exists | `test -f /home/stress/projects/cbr_viewer/frontend/src/services/api.ts` | File exists |

**HARD STOP:** If ANY check fails -> status: blocked

### 3.2 Success Criteria (VERIFY AFTER COMPLETE):
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Frontend compiles | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/frontend && npm run build --silent` | - |
| filesSlice exists | AI_PROMPT.md:L109 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/store/slices/filesSlice.ts` | - |
| FileGrid exists | AI_PROMPT.md:L122 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/components/library/FileGrid.tsx` | - |
| FileCard exists | AI_PROMPT.md:L123 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/components/library/FileCard.tsx` | - |
| LibraryPage exists | AI_PROMPT.md:L135 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/pages/LibraryPage.tsx` | - |
| Pagination implemented | AI_PROMPT.md:L500 | AUTO | `grep -q "Pagination" /home/stress/projects/cbr_viewer/frontend/src/pages/LibraryPage.tsx` | - |

### 3.3 Output Artifacts:
| Artifact | Type | Path | Verification |
|----------|------|------|--------------|
| filesSlice | CREATE | frontend/src/store/slices/filesSlice.ts | `test -f` |
| FileGrid | CREATE | frontend/src/components/library/FileGrid.tsx | `test -f` |
| FileCard | CREATE | frontend/src/components/library/FileCard.tsx | `test -f` |
| LibraryPage | CREATE | frontend/src/pages/LibraryPage.tsx | `test -f` |
| HomePage | CREATE | frontend/src/pages/HomePage.tsx | `test -f` |
| FileUploadDialog | CREATE | frontend/src/components/library/FileUploadDialog.tsx | `test -f` |

## 4. IMPLEMENTATION STRATEGY

### Phase 1: Preparation
1. Read AI_PROMPT.md Redux patterns and component structure
2. Verify TASK2 and TASK3 artifacts exist
3. Review file API endpoints from AI_PROMPT.md:L274-280

**Gate:** All pre-conditions verified, API understood

### Phase 2: Types and Slice
1. Add file-related types to types/index.ts: ComicFile, FilesState
2. Create filesSlice.ts with:
   - State: items, totalPages, currentPage, status, error
   - Async thunks: fetchFiles(page), uploadFile(FormData), deleteFile(id)
   - Follow AI_PROMPT.md:L367-404 pattern exactly
3. Add filesSlice to store.ts

**Gate:** filesSlice compiles, added to store

### Phase 3: File Components
1. Create FileCard.tsx:
   - MUI Card with CardMedia for cover image
   - CardContent with title, file type, page count
   - Display average rating if available (read-only stars)
   - onClick handler to navigate to viewer
2. Create FileGrid.tsx:
   - MUI Grid container with responsive columns
   - Map files to FileCard components
   - Loading skeleton when status is 'loading'

**Gate:** Components compile, use MUI correctly

### Phase 4: Upload Dialog
1. Create FileUploadDialog.tsx:
   - MUI Dialog with file input
   - Accept only .cbr, .cbz files
   - Upload progress indicator
   - Success/error feedback
   - Only rendered for admin users

**Gate:** Dialog compiles, handles file selection

### Phase 5: Pages
1. Create LibraryPage.tsx:
   - Use FileGrid to display files
   - MUI Pagination component at bottom
   - Fetch files on mount and page change
   - Show upload button (FAB) for admin users
2. Create HomePage.tsx:
   - Redirect to LibraryPage if authenticated
   - Redirect to LoginPage if not authenticated
3. Update App.tsx with new routes

**Gate:** Pages compile, routing works

### Phase 6: Validation
1. Verify frontend compiles: `npm run build`
2. Verify all files created
3. Verify pagination component used
4. Verify admin-only upload button logic
5. Mark task complete

## 5. UNCERTAINTY LOG

| ID | Topic | Assumption | Confidence | Evidence |
|----|-------|------------|------------|----------|
| U1 | Cover image URL | Using /api/files/{id}/cover endpoint | HIGH | AI_PROMPT.md:L277 |
| U2 | Pagination response | API returns { content, totalPages, number } | MEDIUM | Standard Spring Page response |
| U3 | Admin role check | Using user.role from authSlice | HIGH | TASK2 stores user with role |

### Stop Rule:
LOW confidence on critical decision -> BLOCKED (do not proceed with guesses)

## 6. INTEGRATION IMPACT

### Files Modified:
| File | Modification | Who Imports | Impact |
|------|--------------|-------------|--------|
| store/store.ts | Add filesSlice | All components using files | State management |
| App.tsx | Add LibraryPage, HomePage routes | - | Routing |
| types/index.ts | Add file types | filesSlice, components | Type definitions |

### Files Created:
| File | Imports From | Exports |
|------|--------------|---------|
| store/slices/filesSlice.ts | api, types | filesSlice, async thunks |
| components/library/FileGrid.tsx | FileCard, filesSlice | FileGrid |
| components/library/FileCard.tsx | MUI, types | FileCard |
| components/library/FileUploadDialog.tsx | MUI, filesSlice | FileUploadDialog |
| pages/LibraryPage.tsx | FileGrid, FileUploadDialog, MUI | LibraryPage |
| pages/HomePage.tsx | authSlice, react-router | HomePage |

### Breaking Changes:
None - new components and routes
