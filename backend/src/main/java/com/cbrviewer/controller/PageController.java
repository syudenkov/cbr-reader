package com.cbrviewer.controller;

import com.cbrviewer.model.ComicFile;
import com.cbrviewer.service.ArchiveService;
import com.cbrviewer.service.FileService;
import com.cbrviewer.util.ArchiveUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/files")
public class PageController {

    private static final Logger logger = LoggerFactory.getLogger(PageController.class);

    private final ArchiveService archiveService;
    private final FileService fileService;

    public PageController(ArchiveService archiveService, FileService fileService) {
        this.archiveService = archiveService;
        this.fileService = fileService;
    }

    /**
     * Get a specific page from a comic file
     * @param id The file ID
     * @param pageNumber The page number (1-indexed)
     * @return The image bytes with appropriate Content-Type header
     */
    @GetMapping("/{id}/page/{pageNumber}")
    public ResponseEntity<byte[]> getPage(
            @PathVariable Long id,
            @PathVariable int pageNumber) throws IOException {

        logger.debug("Requesting file {} page {}", id, pageNumber);

        // 1. Validate file exists
        ComicFile file = fileService.getFile(id);

        // 2. Validate page number is in valid range
        if (pageNumber < 1 || pageNumber > file.getPageCount()) {
            throw new IllegalArgumentException("Invalid page number: " + pageNumber + " (valid range: 1-" + file.getPageCount() + ")");
        }

        // 3. Get page bytes from cache or extraction
        byte[] pageBytes = archiveService.getPage(id, pageNumber);

        // 4. Detect image format and set Content-Type
        String extension = ArchiveUtil.detectImageExtension(pageBytes);
        String contentType = archiveService.getContentType(extension);

        // 5. Build response with headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(pageBytes.length);
        // Enable browser caching for 1 hour (reduces server load for repeat views)
        headers.setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic());

        logger.debug("Serving file {} page {} ({} bytes, type: {})", id, pageNumber, pageBytes.length, contentType);

        return ResponseEntity.ok()
            .headers(headers)
            .body(pageBytes);
    }
}
