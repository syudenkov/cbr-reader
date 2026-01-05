<!-- BLUEPRINT: Read-only after creation -->
@dependencies [TASK8]
@difficulty hard
@files [backend/src/main/java/com/cbrviewer/service/OcrService.java, backend/src/main/java/com/cbrviewer/service/OpenAiOcrClient.java, backend/src/main/java/com/cbrviewer/service/ClaudeOcrClient.java, backend/src/main/java/com/cbrviewer/dto/OcrSegment.java, backend/src/main/java/com/cbrviewer/dto/OcrResponse.java]

# BLUEPRINT: TASK9

## 1. IDENTITY

### This Task IS:
- Creating OcrService that orchestrates OCR with fallback (OpenAI primary, Claude fallback)
- Creating OpenAiOcrClient for OpenAI Vision API integration
- Creating ClaudeOcrClient for Claude Vision API integration (fallback)
- Implementing prompt engineering for dialogue/speaker extraction
- Parsing LLM JSON responses into structured OcrSegment objects
- Integrating OcrService into TtsService.processFile()

### This Task IS NOT:
- Implementing TTS generation (TASK10)
- Implementing TTS frontend (TASK11)
- Managing API keys (TASK12 - uses LlmConfig from DB)
- Creating TTS job infrastructure (TASK8 complete)

### Anti-Hallucination Anchors:
- If OpenAI API format unclear -> Follow AI_PROMPT.md:L742-767 exactly
- If Claude API format unclear -> Follow AI_PROMPT.md:L769-801 exactly
- If OCR prompt unclear -> Use AI_PROMPT.md:L755-757 prompt structure
- If response parsing fails -> Return error, mark job FAILED

### Guardrails (Prohibitions):
**Scope Guardrails:**
- [ ] DO NOT implement TTS audio generation - belongs to TASK10
- [ ] DO NOT implement admin API key UI - belongs to TASK12/TASK13

**Architecture Guardrails:**
- [ ] DO NOT add circuit breaker - simple try-catch fallback (AI_PROMPT.md:L719)
- [ ] DO NOT cache OCR results externally - store in tts_results table

**Quality Guardrails:**
- [ ] DO NOT over-engineer retry logic - single retry per provider is sufficient
- [ ] DO NOT add complex prompt templating - inline prompts are fine

**Security Guardrails:**
- [ ] NEVER log API keys - use masked logging
- [ ] NEVER expose API errors to frontend - generic error message only
- [ ] DO NOT hardcode API keys - read from encrypted DB (LlmConfig)

## 2. CONTEXT CHAIN

### Priority 0 - LEGACY REFERENCE:
None - no legacy systems configured

### Priority 1 - READ FIRST (Required):
- `../AI_PROMPT.md` - OCR requirements (§4), API formats (§8)
- AI_PROMPT.md:L473-474 for OCR acceptance criteria
- AI_PROMPT.md:L742-801 for API request formats

### Priority 2 - READ BEFORE CODING:
- AI_PROMPT.md:L599-604 for OCR service implementation guidance
- AI_PROMPT.md:L755-757 for OCR prompt example
- AI_PROMPT.md:L231-240 for LlmConfig table (API key storage)

### Priority 3 - REFERENCE IF NEEDED:
- Context7 for HTTP client usage in Spring Boot
- AI_PROMPT.md:L33-35 for API references

### Inherited From Dependencies:
- TASK8: TtsService with placeholder for OCR, TtsResult for storing OCR text
- TASK3: ArchiveService for extracting page images
- TASK12 will provide: LlmConfigService for decrypted API keys

## 3. EXECUTION CONTRACT

### 3.1 Pre-Conditions (VERIFY BEFORE ANY CODE):
| Check | Command | Expected |
|-------|---------|----------|
| TASK8 TtsService exists | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/TtsService.java` | File exists |
| ArchiveService exists | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/ArchiveService.java` | File exists |

**HARD STOP:** If ANY check fails -> status: blocked

### 3.2 Success Criteria (VERIFY AFTER COMPLETE):
| Criterion | Source | Testable? | Command | Manual Check |
|-----------|--------|-----------|---------|--------------|
| Backend compiles | AI_PROMPT.md:§10 | AUTO | `cd /home/stress/projects/cbr_viewer/backend && ./gradlew build -x test --quiet` | - |
| OcrService exists | AI_PROMPT.md:L72 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/OcrService.java` | - |
| OpenAI client exists | AI_PROMPT.md:L33 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/OpenAiOcrClient.java` | - |
| Claude client exists | AI_PROMPT.md:L34 | AUTO | `test -f /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/ClaudeOcrClient.java` | - |
| Fallback implemented | AI_PROMPT.md:L473 | AUTO | `grep -q "catch" /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/OcrService.java` | - |
| OCR prompt present | AI_PROMPT.md:L755 | AUTO | `grep -q "dialogue" /home/stress/projects/cbr_viewer/backend/src/main/java/com/cbrviewer/service/OpenAiOcrClient.java` | - |

### 3.3 Output Artifacts:
| Artifact | Type | Path | Verification |
|----------|------|------|--------------|
| OcrService | CREATE | backend/src/main/java/com/cbrviewer/service/OcrService.java | `test -f` |
| OpenAiOcrClient | CREATE | backend/src/main/java/com/cbrviewer/service/OpenAiOcrClient.java | `test -f` |
| ClaudeOcrClient | CREATE | backend/src/main/java/com/cbrviewer/service/ClaudeOcrClient.java | `test -f` |
| OcrSegment | CREATE | backend/src/main/java/com/cbrviewer/dto/OcrSegment.java | `test -f` |
| OcrResponse | CREATE | backend/src/main/java/com/cbrviewer/dto/OcrResponse.java | `test -f` |

## 4. IMPLEMENTATION STRATEGY

### Phase 1: Preparation
1. Read AI_PROMPT.md OCR requirements and API formats
2. Verify TASK8 TtsService exists
3. Review API request/response formats

**Gate:** All pre-conditions verified, API formats understood

### Phase 2: DTOs
1. Create OcrSegment.java with: type (dialogue/narration), speaker (String), text (String)
2. Create OcrResponse.java with: segments (List<OcrSegment>), rawText (String)

**Gate:** DTOs compile

### Phase 3: OpenAI OCR Client
1. Create OpenAiOcrClient.java:
   - processImage(byte[] imageData, String apiKey) returning OcrResponse
   - Build request body following AI_PROMPT.md:L748-766 format
   - Use gpt-4o model as specified
   - Include prompt for dialogue/speaker extraction (AI_PROMPT.md:L755-757)
   - Parse JSON response from LLM
   - Handle API errors gracefully
   - Use RestTemplate or WebClient for HTTP calls

**Gate:** OpenAI client compiles, follows API format

### Phase 4: Claude OCR Client
1. Create ClaudeOcrClient.java:
   - processImage(byte[] imageData, String apiKey) returning OcrResponse
   - Build request body following AI_PROMPT.md:L772-801 format
   - Use claude-3-5-sonnet model as specified
   - Same prompt as OpenAI for consistency
   - Parse JSON response from LLM
   - Handle API errors gracefully

**Gate:** Claude client compiles, follows API format

### Phase 5: OCR Service Orchestration
1. Create OcrService.java:
   - processPage(byte[] imageData) returning OcrResponse
   - Get active LLM configs from database (will integrate with TASK12)
   - Try OpenAI first (primary)
   - On failure, fall back to Claude
   - Log which provider was used
   - Throw if both fail
2. Integrate into TtsService.processFile():
   - Replace placeholder OCR call with OcrService.processPage()
   - Store ocrText and speakersJson in TtsResult

**Gate:** OcrService compiles with fallback logic

### Phase 6: Validation
1. Verify backend compiles: `./gradlew build -x test`
2. Verify all files created
3. Verify fallback logic (try-catch pattern)
4. Verify prompt includes dialogue/speaker keywords
5. Mark task complete

## 5. UNCERTAINTY LOG

| ID | Topic | Assumption | Confidence | Evidence |
|----|-------|------------|------------|----------|
| U1 | Image encoding | Using Base64 encoding for image data | HIGH | AI_PROMPT.md:L764, L791 |
| U2 | JSON parsing | LLM returns valid JSON most of the time | MEDIUM | May need error handling |
| U3 | API key retrieval | Will use placeholder until TASK12 done | HIGH | Can mock for testing |
| U4 | HTTP client choice | Using RestTemplate (simpler than WebClient) | MEDIUM | Either works |

### Stop Rule:
LOW confidence on critical decision -> BLOCKED (do not proceed with guesses)

## 6. INTEGRATION IMPACT

### Files Modified:
| File | Modification | Who Imports | Impact |
|------|--------------|-------------|--------|
| TtsService.java | Replace OCR placeholder with OcrService call | - | OCR integration |

### Files Created:
| File | Imports From | Exports |
|------|--------------|---------|
| OcrService.java | OpenAiOcrClient, ClaudeOcrClient, LlmConfigService | OCR orchestration |
| OpenAiOcrClient.java | RestTemplate, OcrResponse | OpenAI API calls |
| ClaudeOcrClient.java | RestTemplate, OcrResponse | Claude API calls |
| OcrSegment.java | - | DTO |
| OcrResponse.java | OcrSegment | DTO |

### Breaking Changes:
None - integrates with existing TtsService
