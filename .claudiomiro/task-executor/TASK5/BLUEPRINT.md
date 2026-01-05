<!-- BLUEPRINT: Read-only after creation -->
@dependencies [TASK4]
@difficulty medium
@files [frontend/src/store/slices/viewerSlice.ts, frontend/src/components/viewer/PageViewer.tsx, frontend/src/components/viewer/ScrollViewer.tsx, frontend/src/components/viewer/ViewerControls.tsx, frontend/src/pages/ViewerPage.tsx, frontend/src/hooks/useKeyboardNav.ts]

# BLUEPRINT: TASK5

## 1. IDENTITY

### This Task IS:
- Creating viewerSlice with state for currentPage, viewMode, isFullscreen
- Creating PageViewer.tsx for page-by-page viewing with prev/next navigation
- Creating ScrollViewer.tsx for vertical scroll viewing with lazy loading
- Creating ViewerControls.tsx for mode toggle, fullscreen toggle, page indicator
- Creating ViewerPage.tsx that orchestrates viewer components
- Implementing keyboard navigation (arrow keys for prev/next, ESC for exit fullscreen)
- Implementing fullscreen mode using Fullscreen API
- Implementing responsive image sizing to fit viewport

### This Task IS NOT:
- Implementing reading progress saving (TASK6)
- Implementing ratings (TASK7)
- Implementing TTS player (TASK11)
- Backend page serving (TASK3 - already complete)

### Anti-Hallucination Anchors:
- If viewerSlice pattern unclear -> Follow AI_PROMPT.md:L367-404 slice pattern
- If page image URL unclear -> Use /api/files/{id}/page/{n} from TASK3
- If fullscreen API unclear -> Use standard browser requestFullscreen()
- If lazy loading unclear -> Use IntersectionObserver for scroll mode

### Guardrails (Prohibitions):
**Scope Guardrails:**
- [ ] DO NOT implement reading progress API calls - belongs to TASK6
- [ ] DO NOT implement TTS player - belongs to TASK11
- [ ] DO NOT implement rating UI - belongs to TASK7

**Architecture Guardrails:**
- [ ] DO NOT use React Context for viewer state - Redux Toolkit only
- [ ] DO NOT implement zoom via image processing - CSS transform only
- [ ] DO NOT pre-fetch all pages - lazy loading required (AI_PROMPT.md:L499)

**Quality Guardrails:**
- [ ] DO NOT add complex gesture handling - keyboard and click only for MVP
- [ ] DO NOT implement page thumbnails - not in requirements

**Security Guardrails:**
- [ ] DO NOT expose file system paths - use API endpoints only

## 2. CONTEXT CHAIN

### Priority 0 - LEGACY REFERENCE:
None - no legacy systems configured

### Priority 1 - READ FIRST (Required):
- `../AI_PROMPT.md` - Viewing requirements (§4), component structure (§2)
- AI_PROMPT.md:L448-453 for viewing acceptance criteria
- AI_PROMPT.md:L117-120 for viewer component paths

### Priority 2 - READ BEFORE CODING:
- AI_PROMPT.md:L276 for page image endpoint
- AI_PROMPT.md:L110 for viewerSlice path
- AI_PROMPT.md:L499 for lazy loading requirement

### Priority 3 - REFERENCE IF NEEDED:
- Context7 for IntersectionObserver usage
- MDN for Fullscreen API

### Inherited From Dependencies:
- TASK4: LibraryPage (navigation to viewer), filesSlice (file metadata)
- TASK3: Backend page endpoint (GET /api/files/{id}/page/{n})
- TASK2: Redux store, api.ts

## 3. EXECUTION CONTRACT

### 3.1 Pre-Conditions (VERIFY BEFORE ANY CODE):
| Check | Command | Expected |
|-------|---------|----------|
| TASK4 LibraryPage exists | `test -f /home/stress/projects/cbr_viewer/frontend/src/pages/LibraryPage.tsx` | File exists |
| Store exists | `test -f /home/stress/projects/cbr_viewer/frontend/src/store/store.ts` | File exists |
| API service exists | `test -f /home/stress/projects/cbr_viewer/frontend/src/services/api.ts` | File exists |

**HARD STOP:** If ANY check fails -> status: blocked

### 3.2 Success Criteria (VERIFY AFTER COMPLETE):
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Frontend compiles | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/frontend && npm run build --silent` | - |
| viewerSlice exists | AI_PROMPT.md:L110 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/store/slices/viewerSlice.ts` | - |
| PageViewer exists | AI_PROMPT.md:L117 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/components/viewer/PageViewer.tsx` | - |
| ScrollViewer exists | AI_PROMPT.md:L118 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/components/viewer/ScrollViewer.tsx` | - |
| ViewerControls exists | AI_PROMPT.md:L119 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/components/viewer/ViewerControls.tsx` | - |
| ViewerPage exists | AI_PROMPT.md:L136 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/pages/ViewerPage.tsx` | - |
| Keyboard navigation | AI_PROMPT.md:L451 | AUTO | `grep -q "useEffect" /home/stress/projects/cbr_viewer/frontend/src/hooks/useKeyboardNav.ts` | - |
| Fullscreen support | AI_PROMPT.md:L450 | AUTO | `grep -q "requestFullscreen" /home/stress/projects/cbr_viewer/frontend/src/components/viewer/ViewerControls.tsx` | - |

### 3.3 Output Artifacts:
| Artifact | Type | Path | Verification |
|----------|------|------|--------------|
| viewerSlice | CREATE | frontend/src/store/slices/viewerSlice.ts | `test -f` |
| PageViewer | CREATE | frontend/src/components/viewer/PageViewer.tsx | `test -f` |
| ScrollViewer | CREATE | frontend/src/components/viewer/ScrollViewer.tsx | `test -f` |
| ViewerControls | CREATE | frontend/src/components/viewer/ViewerControls.tsx | `test -f` |
| ViewerPage | CREATE | frontend/src/pages/ViewerPage.tsx | `test -f` |
| useKeyboardNav | CREATE | frontend/src/hooks/useKeyboardNav.ts | `test -f` |

## 4. IMPLEMENTATION STRATEGY

### Phase 1: Preparation
1. Read AI_PROMPT.md viewing requirements
2. Verify TASK4 artifacts exist
3. Review page endpoint from TASK3

**Gate:** All pre-conditions verified, requirements understood

### Phase 2: Viewer Slice
1. Add viewer types to types/index.ts: ViewMode ('page' | 'scroll'), ViewerState
2. Create viewerSlice.ts with:
   - State: fileId, currentPage, totalPages, viewMode, isFullscreen
   - Actions: setCurrentPage, setViewMode, toggleFullscreen, setFileInfo
   - No async thunks needed (page images loaded via img src)
3. Add viewerSlice to store.ts

**Gate:** viewerSlice compiles, added to store

### Phase 3: Keyboard Navigation Hook
1. Create hooks/useKeyboardNav.ts:
   - Listen for ArrowLeft, ArrowRight for prev/next page
   - Listen for Escape to exit fullscreen
   - Accept callbacks for actions
   - Clean up event listeners on unmount

**Gate:** Hook compiles, handles keyboard events

### Phase 4: Viewer Components
1. Create PageViewer.tsx:
   - Display single page image (img with src=/api/files/{id}/page/{n})
   - Responsive sizing (max-width: 100%, max-height: 100vh)
   - Click zones: left 30% for prev, right 30% for next
   - Loading state while image loads
2. Create ScrollViewer.tsx:
   - Container with all pages stacked vertically
   - Use IntersectionObserver for lazy loading
   - Only render visible pages + buffer (3 above, 3 below)
   - Smooth scrolling
3. Create ViewerControls.tsx:
   - Mode toggle button (page/scroll icons)
   - Fullscreen toggle button
   - Page indicator (current / total)
   - Back to library button

**Gate:** Components compile, layout works

### Phase 5: Viewer Page
1. Create ViewerPage.tsx:
   - Get fileId from URL params (react-router)
   - Fetch file metadata to get page count
   - Render PageViewer or ScrollViewer based on viewMode
   - Render ViewerControls
   - Apply useKeyboardNav hook
   - Handle fullscreen toggle via Fullscreen API
2. Update App.tsx with viewer route: /viewer/:fileId

**Gate:** ViewerPage compiles, routing works

### Phase 6: Validation
1. Verify frontend compiles: `npm run build`
2. Verify all files created
3. Verify keyboard navigation code present
4. Verify fullscreen API usage
5. Verify lazy loading in ScrollViewer
6. Mark task complete

## 5. UNCERTAINTY LOG

| ID | Topic | Assumption | Confidence | Evidence |
|----|-------|------------|------------|----------|
| U1 | Image aspect ratio | Images will fit viewport width, maintain aspect ratio | HIGH | CSS max-width: 100% |
| U2 | IntersectionObserver support | Modern browsers support it | HIGH | >95% browser support |
| U3 | Fullscreen API | Works across browsers | MEDIUM | May need vendor prefixes |
| U4 | Page count source | Get from file metadata via filesSlice | HIGH | FileDto includes pageCount |

### Stop Rule:
LOW confidence on critical decision -> BLOCKED (do not proceed with guesses)

## 6. INTEGRATION IMPACT

### Files Modified:
| File | Modification | Who Imports | Impact |
|------|--------------|-------------|--------|
| store/store.ts | Add viewerSlice | Viewer components | State management |
| App.tsx | Add viewer route | - | Routing |
| types/index.ts | Add viewer types | viewerSlice, components | Type definitions |

### Files Created:
| File | Imports From | Exports |
|------|--------------|---------|
| store/slices/viewerSlice.ts | - | viewerSlice, actions |
| components/viewer/PageViewer.tsx | viewerSlice, api | PageViewer |
| components/viewer/ScrollViewer.tsx | viewerSlice, api | ScrollViewer |
| components/viewer/ViewerControls.tsx | viewerSlice, MUI | ViewerControls |
| pages/ViewerPage.tsx | All viewer components, useKeyboardNav | ViewerPage |
| hooks/useKeyboardNav.ts | - | useKeyboardNav |

### Breaking Changes:
None - new components and routes
