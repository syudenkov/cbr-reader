package com.cbrviewer.service;

import com.cbrviewer.client.MinimaxClient;
import com.cbrviewer.dto.PageOcrResult;
import com.cbrviewer.exception.MinimaxException;
import com.cbrviewer.model.ComicFile;
import com.cbrviewer.model.TtsJob;
import com.cbrviewer.model.TtsResult;
import com.cbrviewer.repository.ComicFileRepository;
import com.cbrviewer.repository.TtsJobRepository;
import com.cbrviewer.repository.TtsResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Processes TTS jobs asynchronously in background thread.
 * Runs in single-threaded executor (configured in AsyncConfig).
 */
@Service
public class TtsJobProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TtsJobProcessor.class);

    private final TtsJobRepository ttsJobRepository;
    private final ComicFileRepository comicFileRepository;
    private final TtsResultRepository ttsResultRepository;
    private final ArchiveService archiveService;
    private final OcrService ocrService;
    private final VoiceService voiceService;
    private final MinimaxClient minimaxClient;

    @Value("${app.storage.audio-path}")
    private String audioBasePath;

    public TtsJobProcessor(
        TtsJobRepository ttsJobRepository,
        ComicFileRepository comicFileRepository,
        TtsResultRepository ttsResultRepository,
        ArchiveService archiveService,
        OcrService ocrService,
        VoiceService voiceService,
        MinimaxClient minimaxClient
    ) {
        this.ttsJobRepository = ttsJobRepository;
        this.comicFileRepository = comicFileRepository;
        this.ttsResultRepository = ttsResultRepository;
        this.archiveService = archiveService;
        this.ocrService = ocrService;
        this.voiceService = voiceService;
        this.minimaxClient = minimaxClient;
    }

    /**
     * Processes TTS job asynchronously.
     * Runs in "ttsTaskExecutor" thread pool (single-threaded).
     *
     * @param jobId job ID to process
     */
    @Async("ttsTaskExecutor")
    public void processJobAsync(Long jobId) {
        logger.info("Starting async TTS job processing: jobId={}", jobId);

        TtsJob job = ttsJobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        try {
            // Update status to PROCESSING
            job.setStatus("PROCESSING");
            job = ttsJobRepository.save(job);

            // Process all pages
            processAllPages(job);

            // Mark job as COMPLETED
            job.setStatus("COMPLETED");
            job.setProgress(100);
            job = ttsJobRepository.save(job);

            logger.info("TTS job completed successfully: jobId={}", jobId);

        } catch (Exception e) {
            logger.error("TTS job failed: jobId={}, error={}", jobId, e.getMessage(), e);
            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
            ttsJobRepository.save(job);
        }
    }

    /**
     * Processes all pages for the job.
     * Continues processing even if individual pages fail.
     */
    private void processAllPages(TtsJob job) throws Exception {
        ComicFile comicFile = comicFileRepository.findById(job.getFileId())
            .orElseThrow(() -> new IllegalArgumentException("File not found: " + job.getFileId()));

        int pageCount = comicFile.getPageCount();
        logger.info("Processing {} pages for jobId={}", pageCount, job.getId());

        for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
            try {
                processPage(job, comicFile, pageNumber, pageCount);
            } catch (Exception e) {
                logger.error("Failed to process page {}: {}", pageNumber, e.getMessage());
                // Continue to next page (partial completion is acceptable)
            }
        }
    }

    /**
     * Processes a single page: extract image → OCR → TTS → save audio.
     */
    private void processPage(TtsJob job, ComicFile comicFile, int pageNumber, int totalPages) throws Exception {
        logger.info("Processing page {}/{} for jobId={}", pageNumber, totalPages, job.getId());

        // 1. Extract page image from archive
        byte[] imageBytes;
        try {
            imageBytes = archiveService.getPage(comicFile.getId(), pageNumber);
        } catch (IOException e) {
            logger.error("Failed to extract page {}: {}", pageNumber, e.getMessage());
            saveFailedResult(comicFile.getId(), pageNumber);
            updateJobProgress(job, pageNumber, totalPages);
            return;
        }

        // 2. Run OCR (OpenAI → Claude fallback)
        PageOcrResult ocrResult = ocrService.extractPage(imageBytes, pageNumber);

        if (!ocrResult.isSuccess()) {
            logger.error("OCR failed for page {}: {}", pageNumber, ocrResult.getErrorMessage());
            saveFailedResult(comicFile.getId(), pageNumber);
            updateJobProgress(job, pageNumber, totalPages);
            return;
        }

        // 3. Synthesize audio for each speaker segment
        List<byte[]> audioSegments = new ArrayList<>();
        for (PageOcrResult.SpeakerSegment segment : ocrResult.getSegments()) {
            String voiceId = voiceService.mapVoiceCharacteristics(segment.getVoiceCharacteristics());

            try {
                byte[] audioBytes = minimaxClient.synthesize(segment.getText(), voiceId);
                audioSegments.add(audioBytes);
            } catch (MinimaxException e) {
                logger.error("Minimax synthesis failed for segment: {}", e.getMessage());
                // Continue with next segment (partial audio is acceptable)
            }
        }

        // 4. Concatenate audio segments into single MP3
        byte[] pageAudio = concatenateAudioSegments(audioSegments);

        // 5. Save audio file to disk
        String audioFilePath = saveAudioFile(job.getId(), comicFile.getId(), pageNumber, pageAudio);

        // 6. Save TtsResult to database
        TtsResult result = new TtsResult(comicFile.getId(), pageNumber);
        result.setOcrText(extractTextSnippet(ocrResult));
        result.setSpeakersJson(buildSpeakersJson(ocrResult));
        result.setAudioFilePath(audioFilePath);
        ttsResultRepository.save(result);

        // 7. Update job progress
        updateJobProgress(job, pageNumber, totalPages);

        logger.info("Page {}/{} processed successfully", pageNumber, totalPages);
    }

    /**
     * Concatenates multiple MP3 audio segments into single MP3.
     * MVP STUB: Returns first segment if multiple exist.
     * TODO: Implement full MP3 concatenation in future iteration.
     */
    private byte[] concatenateAudioSegments(List<byte[]> segments) {
        if (segments.isEmpty()) {
            return new byte[0];
        }

        if (segments.size() == 1) {
            return segments.get(0);
        }

        // TODO: Implement MP3 concatenation using FFmpeg or Java audio library
        logger.warn("Multiple audio segments ({} total), using first segment only (MVP stub)",
                   segments.size());
        return segments.get(0);
    }

    /**
     * Saves audio bytes to disk at storage/audio/{jobId}/page_{pageNum}.mp3
     *
     * @return relative API path (e.g., "/api/tts/audio/42/1")
     */
    private String saveAudioFile(Long jobId, Long fileId, int pageNumber, byte[] audioBytes) throws IOException {
        // Create directory: storage/audio/{jobId}/
        Path audioDir = Paths.get(audioBasePath, jobId.toString());
        Files.createDirectories(audioDir);

        // Save file: storage/audio/{jobId}/page_{pageNum}.mp3
        Path audioFile = audioDir.resolve("page_" + pageNumber + ".mp3");
        Files.write(audioFile, audioBytes);

        logger.info("Audio saved: {}", audioFile);

        // Return relative API path
        return "/api/tts/audio/" + fileId + "/" + pageNumber;
    }

    /**
     * Saves failed page result with null audio path.
     */
    private void saveFailedResult(Long fileId, int pageNumber) {
        TtsResult failedResult = new TtsResult(fileId, pageNumber);
        failedResult.setOcrText(null);
        failedResult.setSpeakersJson(null);
        failedResult.setAudioFilePath(null);
        ttsResultRepository.save(failedResult);
    }

    /**
     * Updates job progress after page completion.
     */
    private void updateJobProgress(TtsJob job, int completedPage, int totalPages) {
        int progress = (int) ((double) completedPage / totalPages * 100);
        job.setProgress(progress);
        job.setCurrentPage(completedPage);
        ttsJobRepository.save(job);
    }

    /**
     * Extracts text snippet from OCR result for database storage.
     */
    private String extractTextSnippet(PageOcrResult ocrResult) {
        StringBuilder snippet = new StringBuilder();
        for (PageOcrResult.SpeakerSegment segment : ocrResult.getSegments()) {
            snippet.append(segment.getSpeaker()).append(": ").append(segment.getText()).append(" ");
        }
        return snippet.toString().trim();
    }

    /**
     * Builds JSON representation of speakers for database storage.
     */
    private String buildSpeakersJson(PageOcrResult ocrResult) {
        // Simple JSON array format: [{"speaker":"Batman","text":"...","voice":"hero_deep_male"}]
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < ocrResult.getSegments().size(); i++) {
            PageOcrResult.SpeakerSegment segment = ocrResult.getSegments().get(i);
            String voiceId = voiceService.mapVoiceCharacteristics(segment.getVoiceCharacteristics());

            if (i > 0) json.append(",");
            json.append("{\"speaker\":\"").append(escapeJson(segment.getSpeaker())).append("\",");
            json.append("\"text\":\"").append(escapeJson(segment.getText())).append("\",");
            json.append("\"voice\":\"").append(escapeJson(voiceId)).append("\"}");
        }
        json.append("]");
        return json.toString();
    }

    /**
     * Escapes special characters for JSON strings.
     */
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
