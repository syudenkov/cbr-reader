<!-- BLUEPRINT: Read-only after creation -->
@dependencies [TASK10, TASK5]
@difficulty medium
@files [frontend/src/store/slices/ttsSlice.ts, frontend/src/components/viewer/TtsPlayer.tsx, frontend/src/components/viewer/TtsRequestButton.tsx, frontend/src/components/viewer/TtsProgressIndicator.tsx]

# BLUEPRINT: TASK11

## 1. IDENTITY

### This Task IS:
- Creating ttsSlice with async thunks for requestTts, pollStatus, fetchResults
- Creating TtsRequestButton.tsx for triggering TTS generation
- Creating TtsProgressIndicator.tsx for showing job progress (polling)
- Creating TtsPlayer.tsx for playing page audio
- Integrating TTS components into ViewerPage.tsx
- Implementing polling mechanism for job status updates

### This Task IS NOT:
- Implementing backend TTS logic (TASK8-10 complete)
- Modifying backend endpoints (TASK8 complete)
- Implementing admin LLM config UI (TASK13)

### Anti-Hallucination Anchors:
- If ttsSlice pattern unclear -> Follow AI_PROMPT.md:L367-404 slice pattern
- If API endpoints unclear -> Follow AI_PROMPT.md:L291-295 spec
- If polling interval unclear -> Use 2-3 seconds between polls

### Guardrails (Prohibitions):
**Scope Guardrails:**
- [ ] DO NOT implement WebSocket for progress - polling only (AI_PROMPT.md:L511)
- [ ] DO NOT modify viewer navigation - only add TTS controls

**Architecture Guardrails:**
- [ ] DO NOT use React Context - Redux Toolkit only
- [ ] DO NOT implement custom audio player - use HTML5 audio element

**Quality Guardrails:**
- [ ] DO NOT poll indefinitely - stop after job completes or fails
- [ ] DO NOT add complex audio controls - play/pause/seek is sufficient

**Security Guardrails:**
- [ ] DO NOT expose audio file paths - use API endpoints only

## 2. CONTEXT CHAIN

### Priority 0 - LEGACY REFERENCE:
None - no legacy systems configured

### Priority 1 - READ FIRST (Required):
- `../AI_PROMPT.md` - TTS frontend requirements (§4), Redux patterns (§3)
- AI_PROMPT.md:L478 for audio playback requirement
- AI_PROMPT.md:L611-616 for TTS frontend implementation guidance

### Priority 2 - READ BEFORE CODING:
- AI_PROMPT.md:L291-295 for TTS API endpoints
- AI_PROMPT.md:L111 for ttsSlice path
- AI_PROMPT.md:L120 for TtsPlayer component path

### Priority 3 - REFERENCE IF NEEDED:
- Context7 for Redux async thunk polling patterns
- MDN for HTML5 Audio element

### Inherited From Dependencies:
- TASK5: ViewerPage (integration point), viewerSlice (current page)
- TASK8: TTS API endpoints (request, status, result, audio)
- TASK2: Redux store, api.ts

## 3. EXECUTION CONTRACT

### 3.1 Pre-Conditions (VERIFY BEFORE ANY CODE):
| Check | Command | Expected |
|-------|---------|----------|
| TASK5 ViewerPage exists | `test -f /home/stress/projects/cbr_viewer/frontend/src/pages/ViewerPage.tsx` | File exists |
| TASK10 TtsGenerationService exists | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/TtsGenerationService.java` | File exists |
| Store exists | `test -f /home/stress/projects/cbr_viewer/frontend/src/store/store.ts` | File exists |

**HARD STOP:** If ANY check fails -> status: blocked

### 3.2 Success Criteria (VERIFY AFTER COMPLETE):
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Frontend compiles | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/frontend && npm run build --silent` | - |
| ttsSlice exists | AI_PROMPT.md:L111 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/store/slices/ttsSlice.ts` | - |
| TtsPlayer exists | AI_PROMPT.md:L120 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/components/viewer/TtsPlayer.tsx` | - |
| TtsRequestButton exists | AI_PROMPT.md:L468 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/components/viewer/TtsRequestButton.tsx` | - |
| TtsProgressIndicator exists | AI_PROMPT.md:L471 | AUTO | `test -f /home/stress/projects/cbr_viewer/frontend/src/components/viewer/TtsProgressIndicator.tsx` | - |
| Polling implemented | AI_PROMPT.md:L472 | AUTO | `grep -q "setInterval\|setTimeout" /home/stress/projects/cbr_viewer/frontend/src/store/slices/ttsSlice.ts` | - |

### 3.3 Output Artifacts:
| Artifact | Type | Path | Verification |
|----------|------|------|--------------|
| ttsSlice | CREATE | frontend/src/store/slices/ttsSlice.ts | `test -f` |
| TtsPlayer | CREATE | frontend/src/components/viewer/TtsPlayer.tsx | `test -f` |
| TtsRequestButton | CREATE | frontend/src/components/viewer/TtsRequestButton.tsx | `test -f` |
| TtsProgressIndicator | CREATE | frontend/src/components/viewer/TtsProgressIndicator.tsx | `test -f` |

## 4. IMPLEMENTATION STRATEGY

### Phase 1: Preparation
1. Read AI_PROMPT.md TTS frontend requirements
2. Verify TASK5 ViewerPage and TASK10 backend exist
3. Review TTS API endpoints

**Gate:** All pre-conditions verified, requirements understood

### Phase 2: Types and Slice
1. Add TTS types to types/index.ts: TtsJob, TtsResult, TtsState
2. Create ttsSlice.ts with:
   - State: currentJob, results (by fileId), status, error
   - Async thunks:
     - requestTts(fileId) - POST /api/tts/request/{fileId}
     - pollJobStatus(jobId) - GET /api/tts/status/{jobId}
     - fetchResults(fileId) - GET /api/tts/result/{fileId}
   - Polling logic: poll every 2 seconds until COMPLETED/FAILED
3. Add ttsSlice to store.ts

**Gate:** ttsSlice compiles with polling logic

### Phase 3: TTS Request Button
1. Create TtsRequestButton.tsx:
   - MUI Button with "Generate Audio" label
   - Dispatches requestTts action on click
   - Disabled while job is pending/processing
   - Shows loading spinner during request

**Gate:** Button compiles, dispatches action

### Phase 4: Progress Indicator
1. Create TtsProgressIndicator.tsx:
   - MUI LinearProgress for progress bar
   - Shows current progress percentage (0-100)
   - Shows current page being processed
   - Shows error message if job failed
   - Only visible when job is PENDING or PROCESSING

**Gate:** Progress indicator compiles

### Phase 5: Audio Player
1. Create TtsPlayer.tsx:
   - HTML5 audio element with controls
   - Source: /api/tts/audio/{fileId}/{page}
   - Show when TTS results exist for current page
   - Auto-update src when page changes
   - Play/pause controls via audio element
   - Loading state while audio loads

**Gate:** Audio player compiles, plays audio

### Phase 6: ViewerPage Integration
1. Update ViewerPage.tsx:
   - Add TtsRequestButton to ViewerControls area
   - Add TtsProgressIndicator when job in progress
   - Add TtsPlayer below viewer when audio available
   - Fetch TTS results on mount to check if already processed
2. Update App.tsx if needed for routing

**Gate:** Components integrated in ViewerPage

### Phase 7: Validation
1. Verify frontend compiles: `npm run build`
2. Verify all files created
3. Verify polling logic present
4. Verify audio element integration
5. Mark task complete

## 5. UNCERTAINTY LOG

| ID | Topic | Assumption | Confidence | Evidence |
|----|-------|------------|------------|----------|
| U1 | Polling cleanup | Stop polling on component unmount | HIGH | Standard React pattern |
| U2 | Audio source URL | Using /api/tts/audio/{fileId}/{page} | HIGH | AI_PROMPT.md:L295 |
| U3 | Progress updates | Backend updates progress 0-100 | HIGH | TASK8 implementation |

### Stop Rule:
LOW confidence on critical decision -> BLOCKED (do not proceed with guesses)

## 6. INTEGRATION IMPACT

### Files Modified:
| File | Modification | Who Imports | Impact |
|------|--------------|-------------|--------|
| store/store.ts | Add ttsSlice | TTS components | State management |
| ViewerPage.tsx | Add TTS components | - | Audio playback |
| types/index.ts | Add TTS types | ttsSlice, components | Type definitions |

### Files Created:
| File | Imports From | Exports |
|------|--------------|---------|
| store/slices/ttsSlice.ts | api, types | ttsSlice, async thunks |
| components/viewer/TtsPlayer.tsx | ttsSlice, MUI | TtsPlayer |
| components/viewer/TtsRequestButton.tsx | ttsSlice, MUI | TtsRequestButton |
| components/viewer/TtsProgressIndicator.tsx | ttsSlice, MUI | TtsProgressIndicator |

### Breaking Changes:
None - additive changes to ViewerPage
