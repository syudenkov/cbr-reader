package com.cbrviewer.service;

import com.cbrviewer.exception.InvalidArchiveException;
import com.cbrviewer.model.ComicFile;
import com.cbrviewer.repository.ComicFileRepository;
import com.cbrviewer.util.ArchiveUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ArchiveService {

    private static final Logger logger = LoggerFactory.getLogger(ArchiveService.class);

    private final ComicFileRepository comicFileRepository;

    @Value("${app.storage.cache-path}")
    private String cacheBasePath;

    public ArchiveService(ComicFileRepository comicFileRepository) {
        this.comicFileRepository = comicFileRepository;
    }

    /**
     * Get a page from the archive, using cache if available
     * @param fileId The file ID
     * @param pageNumber The page number (1-indexed)
     * @return byte array containing the image data
     * @throws IOException if file operations fail
     */
    public byte[] getPage(Long fileId, int pageNumber) throws IOException {
        // 1. Get file metadata
        ComicFile comicFile = comicFileRepository.findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found with id: " + fileId));

        // 2. Validate page number (1-indexed in API)
        if (pageNumber < 1 || pageNumber > comicFile.getPageCount()) {
            throw new IllegalArgumentException("Invalid page number: " + pageNumber + " (valid range: 1-" + comicFile.getPageCount() + ")");
        }

        // Convert to 0-indexed for internal use
        int pageIndex = pageNumber - 1;

        // 3. Check cache first (we need to check all possible extensions)
        byte[] cachedPage = checkCache(fileId, pageNumber);
        if (cachedPage != null) {
            logger.debug("Cache hit for file {} page {}", fileId, pageNumber);
            return cachedPage;
        }

        // 4. Cache miss - extract from archive
        logger.debug("Cache miss for file {} page {} - extracting from archive", fileId, pageNumber);
        File archiveFile = new File(comicFile.getFilePath());
        if (!archiveFile.exists()) {
            throw new IOException("Archive file not found: " + comicFile.getFilePath());
        }

        byte[] pageBytes = ArchiveUtil.extractPage(archiveFile, comicFile.getFileType(), pageIndex);

        // 5. Write to cache
        writeCacheFile(fileId, pageNumber, pageBytes);

        return pageBytes;
    }

    /**
     * Check if a page exists in cache and return it
     * @param fileId The file ID
     * @param pageNumber The page number (1-indexed)
     * @return The cached page bytes, or null if not found
     */
    private byte[] checkCache(Long fileId, int pageNumber) throws IOException {
        // Try all possible image extensions
        String[] extensions = {".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"};

        for (String ext : extensions) {
            Path cachePath = getCachePath(fileId, pageNumber, ext);
            File cacheFile = cachePath.toFile();

            if (cacheFile.exists()) {
                // Update last modified time for LRU tracking
                cacheFile.setLastModified(System.currentTimeMillis());
                // Read and return cached bytes
                return Files.readAllBytes(cachePath);
            }
        }

        return null; // Cache miss
    }

    /**
     * Write page bytes to cache
     * @param fileId The file ID
     * @param pageNumber The page number (1-indexed)
     * @param pageBytes The image bytes
     */
    private void writeCacheFile(Long fileId, int pageNumber, byte[] pageBytes) throws IOException {
        // Detect the actual image format
        String extension = ArchiveUtil.detectImageExtension(pageBytes);

        // Get cache path
        Path cachePath = getCachePath(fileId, pageNumber, extension);

        // Create parent directories if they don't exist
        Files.createDirectories(cachePath.getParent());

        // Write bytes to cache file
        Files.write(cachePath, pageBytes);

        logger.debug("Cached file {} page {} at {}", fileId, pageNumber, cachePath);
    }

    /**
     * Get the cache file path for a specific page
     * @param fileId The file ID
     * @param pageNumber The page number (1-indexed)
     * @param extension The file extension (e.g., ".jpg")
     * @return The cache file path
     */
    private Path getCachePath(Long fileId, int pageNumber, String extension) {
        String filename = pageNumber + extension;
        return Paths.get(cacheBasePath, fileId.toString(), filename);
    }

    /**
     * Get the content type for an image extension
     * @param extension The file extension (e.g., ".jpg")
     * @return The MIME type
     */
    public String getContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".bmp" -> "image/bmp";
            default -> "application/octet-stream";
        };
    }
}
