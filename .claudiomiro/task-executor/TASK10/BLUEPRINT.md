<!-- BLUEPRINT: Read-only after creation -->
@dependencies [TASK9]
@difficulty medium
@files [backend/src/main/java/com/cbrviewer/service/TtsGenerationService.java, backend/src/main/java/com/cbrviewer/service/MinimaxTtsClient.java, backend/src/main/java/com/cbrviewer/dto/TtsSegmentResult.java, backend/src/main/java/com/cbrviewer/util/AudioCombiner.java]

# BLUEPRINT: TASK10

## 1. IDENTITY

### This Task IS:
- Creating MinimaxTtsClient for Minimax TTS API integration
- Creating TtsGenerationService that generates audio per speaker segment
- Implementing speaker-to-voice mapping (narrator, character voices)
- Storing generated audio files on disk
- Combining per-segment audio into per-page audio file
- Updating TtsResult with audio file paths
- Integrating into TtsService.processFile() after OCR

### This Task IS NOT:
- Implementing OCR (TASK9 complete)
- Implementing TTS frontend (TASK11)
- Managing API keys (TASK12)
- Job orchestration (TASK8 complete)

### Anti-Hallucination Anchors:
- If Minimax API format unclear -> Follow AI_PROMPT.md:L804-820 structure
- If audio storage path unclear -> Follow AI_PROMPT.md:L855 (audio-path)
- If speaker mapping unclear -> Use simple mapping (narrator=voice1, default=voice2)

### Guardrails (Prohibitions):
**Scope Guardrails:**
- [ ] DO NOT implement OCR - belongs to TASK9
- [ ] DO NOT implement frontend audio player - belongs to TASK11
- [ ] DO NOT implement admin voice config - not in MVP scope

**Architecture Guardrails:**
- [ ] DO NOT stream audio during generation - store complete files
- [ ] DO NOT use external audio processing libraries - basic concatenation only

**Quality Guardrails:**
- [ ] DO NOT add complex voice selection - simple mapping is sufficient
- [ ] DO NOT implement audio format conversion - use format from API

**Security Guardrails:**
- [ ] NEVER log API keys - use masked logging
- [ ] DO NOT expose audio file paths - use API endpoints only

## 2. CONTEXT CHAIN

### Priority 0 - LEGACY REFERENCE:
None - no legacy systems configured

### Priority 1 - READ FIRST (Required):
- `../AI_PROMPT.md` - TTS requirements (§4), Minimax API (§8)
- AI_PROMPT.md:L475-477 for TTS acceptance criteria
- AI_PROMPT.md:L804-820 for Minimax API format

### Priority 2 - READ BEFORE CODING:
- AI_PROMPT.md:L605-610 for TTS generation implementation guidance
- AI_PROMPT.md:L855 for audio storage path
- TASK9 OcrSegment for input format

### Priority 3 - REFERENCE IF NEEDED:
- https://www.minimax.io/ for detailed API documentation
- Context7 for file I/O patterns

### Inherited From Dependencies:
- TASK9: OcrService returns OcrResponse with segments
- TASK8: TtsService orchestration, TtsResult for storing paths
- TASK12 will provide: LlmConfigService for Minimax API key

## 3. EXECUTION CONTRACT

### 3.1 Pre-Conditions (VERIFY BEFORE ANY CODE):
| Check | Command | Expected |
|-------|---------|----------|
| TASK9 OcrService exists | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/OcrService.java` | File exists |
| OcrSegment exists | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/dto/OcrSegment.java` | File exists |
| Audio path configured | `grep -q "audio-path" /home/stress/projects/cbr_viewer/backend/src/main/resources/application.yml` | Exit 0 |

**HARD STOP:** If ANY check fails -> status: blocked

### 3.2 Success Criteria (VERIFY AFTER COMPLETE):
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Backend compiles | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/backend && ./gradlew build -x test --quiet` | - |
| TtsGenerationService exists | AI_PROMPT.md:L605 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/TtsGenerationService.java` | - |
| MinimaxTtsClient exists | AI_PROMPT.md:L35 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/MinimaxTtsClient.java` | - |
| Audio storage integration | AI_PROMPT.md:L477 | AUTO | `grep -q "audioFilePath" /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/TtsGenerationService.java` | - |
| Minimax API call | AI_PROMPT.md:L808 | AUTO | `grep -q "minimax" /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/MinimaxTtsClient.java` | - |

### 3.3 Output Artifacts:
| Artifact | Type | Path | Verification |
|----------|------|------|--------------|
| TtsGenerationService | CREATE | backend/src/main/java/com/cbrviewer/service/TtsGenerationService.java | `test -f` |
| MinimaxTtsClient | CREATE | backend/src/main/java/com/cbrviewer/service/MinimaxTtsClient.java | `test -f` |
| TtsSegmentResult | CREATE | backend/src/main/java/com/cbrviewer/dto/TtsSegmentResult.java | `test -f` |
| AudioCombiner | CREATE | backend/src/main/java/com/cbrviewer/util/AudioCombiner.java | `test -f` |

## 4. IMPLEMENTATION STRATEGY

### Phase 1: Preparation
1. Read AI_PROMPT.md TTS generation requirements
2. Verify TASK9 OcrService and OcrSegment exist
3. Review Minimax API format

**Gate:** All pre-conditions verified, API format understood

### Phase 2: DTOs and Utilities
1. Create TtsSegmentResult.java with: speaker, text, audioBytes, audioPath
2. Create AudioCombiner.java utility:
   - combine(List<byte[]> audioChunks) - concatenate audio files
   - saveToFile(byte[] audio, String path) - write to disk

**Gate:** Utilities compile

### Phase 3: Minimax TTS Client
1. Create MinimaxTtsClient.java:
   - generateSpeech(String text, String voiceId, String apiKey) returning byte[]
   - Build request body following AI_PROMPT.md:L808-820 format
   - Map speaker types to voice IDs (narrator=voice1, characters=voice2, etc.)
   - Handle API errors gracefully
   - Return audio bytes

**Gate:** Minimax client compiles, follows API format

### Phase 4: TTS Generation Service
1. Create TtsGenerationService.java:
   - generatePageAudio(Long fileId, int pageNumber, List<OcrSegment> segments) returning String (audio path)
   - For each segment:
     - Map speaker to voice ID
     - Call MinimaxTtsClient.generateSpeech()
     - Store segment audio temporarily
   - Combine all segment audio using AudioCombiner
   - Save combined audio to disk at: {audio-path}/{fileId}/page_{pageNumber}.mp3
   - Return audio file path
2. Handle errors: if any segment fails, mark with error but continue others

**Gate:** Service compiles, generates audio files

### Phase 5: Integration with TtsService
1. Update TtsService.processFile():
   - After OCR: call TtsGenerationService.generatePageAudio()
   - Update TtsResult with audioFilePath
   - Update TtsResult with speakersJson (serialize OcrSegments)
   - Handle generation errors

**Gate:** TtsService integrates with TTS generation

### Phase 6: Validation
1. Verify backend compiles: `./gradlew build -x test`
2. Verify all files created
3. Verify audio path in TtsGenerationService
4. Verify Minimax API call structure
5. Mark task complete

## 5. UNCERTAINTY LOG

| ID | Topic | Assumption | Confidence | Evidence |
|----|-------|------------|------------|----------|
| U1 | Minimax API response | Returns audio bytes or URL to download | MEDIUM | Need to check actual API |
| U2 | Audio format | MP3 format from Minimax | MEDIUM | AI_PROMPT.md:L817 suggests mp3 |
| U3 | Voice IDs | Will use placeholder IDs until confirmed | LOW | Need Minimax docs |
| U4 | Audio concatenation | Simple byte concatenation works for MP3 | LOW | May need audio library |

### Stop Rule:
LOW confidence on critical decision -> BLOCKED (do not proceed with guesses)

## 6. INTEGRATION IMPACT

### Files Modified:
| File | Modification | Who Imports | Impact |
|------|--------------|-------------|--------|
| TtsService.java | Add TtsGenerationService call after OCR | - | Audio generation |

### Files Created:
| File | Imports From | Exports |
|------|--------------|---------|
| TtsGenerationService.java | MinimaxTtsClient, AudioCombiner, StorageConfig | Audio generation |
| MinimaxTtsClient.java | RestTemplate | Minimax API calls |
| TtsSegmentResult.java | - | DTO |
| AudioCombiner.java | - | Audio utility |

### Breaking Changes:
None - integrates with existing TtsService
