package com.cbrviewer.controller;

import com.cbrviewer.dto.TtsJobDto;
import com.cbrviewer.dto.TtsJobRequest;
import com.cbrviewer.model.User;
import com.cbrviewer.service.TtsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for TTS job management.
 * Provides endpoints for job creation and status polling.
 */
@RestController
@RequestMapping("/api/tts")
public class TtsController {

    private static final Logger logger = LoggerFactory.getLogger(TtsController.class);

    private final TtsService ttsService;

    public TtsController(TtsService ttsService) {
        this.ttsService = ttsService;
    }

    /**
     * POST /api/tts/jobs
     * Initiates TTS processing for a comic file.
     *
     * @param request TTS job request containing fileId
     * @param currentUser authenticated user (from session)
     * @return HTTP 202 with TtsJobDto containing job ID and status
     */
    @PostMapping("/jobs")
    public ResponseEntity<TtsJobDto> requestTts(
        @RequestBody TtsJobRequest request,
        @AuthenticationPrincipal User currentUser
    ) {
        logger.info("TTS request received: fileId={}, userId={}", request.getFileId(), currentUser.getId());

        try {
            TtsJobDto job = ttsService.requestTtsProcessing(request.getFileId(), currentUser.getId());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);

        } catch (IllegalStateException e) {
            // Active job already exists (PENDING or PROCESSING)
            logger.warn("TTS request rejected: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        } catch (IllegalArgumentException e) {
            // File not found
            logger.warn("TTS request invalid: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * GET /api/tts/jobs/{id}
     * Retrieves current status of a TTS job.
     *
     * @param id job ID
     * @return HTTP 200 with TtsJobDto containing job progress
     */
    @GetMapping("/jobs/{id}")
    public ResponseEntity<TtsJobDto> getJobStatus(@PathVariable Long id) {
        logger.debug("Job status query: jobId={}", id);

        try {
            TtsJobDto job = ttsService.getJobStatus(id);
            return ResponseEntity.ok(job);

        } catch (IllegalArgumentException e) {
            logger.warn("Job not found: jobId={}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
