package com.cbrviewer.service;

import com.cbrviewer.dto.TtsJobDto;
import com.cbrviewer.model.ComicFile;
import com.cbrviewer.model.TtsJob;
import com.cbrviewer.repository.ComicFileRepository;
import com.cbrviewer.repository.TtsJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for managing TTS job requests and status queries.
 * Handles job creation, validation, and status retrieval.
 */
@Service
public class TtsService {

    private static final Logger logger = LoggerFactory.getLogger(TtsService.class);

    private final TtsJobRepository ttsJobRepository;
    private final ComicFileRepository comicFileRepository;
    private final TtsJobProcessor ttsJobProcessor;

    public TtsService(
        TtsJobRepository ttsJobRepository,
        ComicFileRepository comicFileRepository,
        TtsJobProcessor ttsJobProcessor
    ) {
        this.ttsJobRepository = ttsJobRepository;
        this.comicFileRepository = comicFileRepository;
        this.ttsJobProcessor = ttsJobProcessor;
    }

    /**
     * Requests TTS processing for a comic file.
     * Creates job record and submits to async processor.
     *
     * @param fileId comic file ID
     * @param userId user who initiated request
     * @return TtsJobDto with job ID and initial status
     * @throws IllegalArgumentException if file not found
     * @throws IllegalStateException if active job already exists
     */
    public TtsJobDto requestTtsProcessing(Long fileId, Long userId) {
        // 1. Validate file exists
        ComicFile comicFile = comicFileRepository.findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        // 2. Check for active jobs (PENDING or PROCESSING)
        Optional<TtsJob> pendingJob = ttsJobRepository.findByFileIdAndStatus(fileId, "PENDING");
        Optional<TtsJob> processingJob = ttsJobRepository.findByFileIdAndStatus(fileId, "PROCESSING");

        if (pendingJob.isPresent() || processingJob.isPresent()) {
            TtsJob existingJob = pendingJob.orElse(processingJob.get());
            logger.warn("Active TTS job already exists: jobId={}, status={}",
                       existingJob.getId(), existingJob.getStatus());
            throw new IllegalStateException("Active TTS job already exists for file " + fileId);
        }

        // 3. Create new job
        TtsJob job = new TtsJob(fileId, userId);
        job.setStatus("PENDING");
        job.setProgress(0);
        job.setCurrentPage(0);
        job = ttsJobRepository.save(job);

        logger.info("TTS job created: jobId={}, fileId={}, userId={}", job.getId(), fileId, userId);

        // 4. Submit to async processor (fire and forget)
        ttsJobProcessor.processJobAsync(job.getId());

        return mapToDto(job, comicFile.getPageCount());
    }

    /**
     * Retrieves current status of a TTS job.
     *
     * @param jobId job ID
     * @return TtsJobDto with current status and progress
     * @throws IllegalArgumentException if job not found
     */
    public TtsJobDto getJobStatus(Long jobId) {
        TtsJob job = ttsJobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        ComicFile comicFile = comicFileRepository.findById(job.getFileId())
            .orElseThrow(() -> new IllegalArgumentException("File not found: " + job.getFileId()));

        return mapToDto(job, comicFile.getPageCount());
    }

    /**
     * Maps TtsJob entity to TtsJobDto for API responses.
     *
     * @param job the TtsJob entity
     * @param totalPages total page count from ComicFile
     * @return TtsJobDto for API response
     */
    private TtsJobDto mapToDto(TtsJob job, int totalPages) {
        return new TtsJobDto(
            job.getId(),
            job.getFileId(),
            job.getStatus(),
            job.getProgress(),
            job.getCurrentPage(),
            totalPages,
            job.getErrorMessage(),
            job.getCreatedAt(),
            job.getUpdatedAt()
        );
    }
}
