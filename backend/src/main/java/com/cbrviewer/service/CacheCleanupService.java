package com.cbrviewer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class CacheCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(CacheCleanupService.class);

    @Value("${app.cache.max-size-mb}")
    private int maxSizeMb;

    @Value("${app.cache.ttl-days}")
    private int ttlDays;

    @Value("${app.storage.cache-path}")
    private String cacheBasePath;

    /**
     * Scheduled task to clean up cache directory
     * Runs daily at 2:00 AM by default
     */
    @Scheduled(cron = "${app.cache.cleanup-cron}")
    public void cleanupCache() {
        logger.info("Starting cache cleanup task");

        try {
            // Calculate limits
            long maxSizeBytes = maxSizeMb * 1024L * 1024L;
            long ttlMillis = ttlDays * 24L * 60L * 60L * 1000L;
            long cutoffTime = System.currentTimeMillis() - ttlMillis;

            // 1. Scan all cache files
            List<CacheFileInfo> cacheFiles = scanCacheDirectory();

            if (cacheFiles.isEmpty()) {
                logger.info("Cache directory is empty - no cleanup needed");
                return;
            }

            // 2. Delete expired files (TTL eviction)
            int expiredDeleted = deleteExpiredFiles(cacheFiles, cutoffTime);

            // 3. Calculate total size after TTL cleanup
            List<CacheFileInfo> remainingFiles = cacheFiles.stream()
                .filter(f -> f.lastModified >= cutoffTime)
                .toList();

            long totalSize = remainingFiles.stream()
                .mapToLong(f -> f.size)
                .sum();

            // 4. Delete oldest files if over size limit (LRU eviction)
            int lruDeleted = 0;
            if (totalSize > maxSizeBytes) {
                lruDeleted = evictLRU(remainingFiles, totalSize, maxSizeBytes);
            }

            long finalSize = Math.min(totalSize, maxSizeBytes);
            logger.info("Cache cleanup complete: {} expired files deleted, {} LRU evicted, final cache size: {} MB",
                expiredDeleted, lruDeleted, finalSize / (1024 * 1024));

        } catch (Exception e) {
            logger.error("Cache cleanup failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Scan cache directory and collect file metadata
     * @return List of cache file information
     */
    private List<CacheFileInfo> scanCacheDirectory() {
        List<CacheFileInfo> cacheFiles = new ArrayList<>();
        Path cacheDir = Paths.get(cacheBasePath);

        if (!Files.exists(cacheDir)) {
            logger.warn("Cache directory does not exist: {}", cacheBasePath);
            return cacheFiles;
        }

        try (Stream<Path> paths = Files.walk(cacheDir)) {
            paths.filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        File file = path.toFile();
                        cacheFiles.add(new CacheFileInfo(
                            path,
                            file.length(),
                            file.lastModified()
                        ));
                    } catch (Exception e) {
                        logger.warn("Failed to read file metadata: {}", path, e);
                    }
                });
        } catch (IOException e) {
            logger.error("Failed to scan cache directory: {}", e.getMessage(), e);
        }

        return cacheFiles;
    }

    /**
     * Delete files older than cutoff time
     * @param files List of cache files
     * @param cutoffTime Cutoff timestamp (files older than this will be deleted)
     * @return Number of files deleted
     */
    private int deleteExpiredFiles(List<CacheFileInfo> files, long cutoffTime) {
        int deletedCount = 0;
        long freedSpace = 0;

        for (CacheFileInfo fileInfo : files) {
            if (fileInfo.lastModified < cutoffTime) {
                try {
                    Files.delete(fileInfo.path);
                    deletedCount++;
                    freedSpace += fileInfo.size;
                    logger.debug("Deleted expired cache file: {}", fileInfo.path);
                } catch (IOException e) {
                    logger.warn("Failed to delete expired file: {}", fileInfo.path, e);
                }
            }
        }

        if (deletedCount > 0) {
            logger.info("TTL cleanup: {} files deleted, {} MB freed", deletedCount, freedSpace / (1024 * 1024));
        }

        return deletedCount;
    }

    /**
     * Evict oldest files until total size is under the limit
     * @param files List of cache files
     * @param currentSize Current total size
     * @param maxSize Maximum allowed size
     * @return Number of files deleted
     */
    private int evictLRU(List<CacheFileInfo> files, long currentSize, long maxSize) {
        int deletedCount = 0;
        long freedSpace = 0;

        // Sort by last modified time (oldest first)
        List<CacheFileInfo> sortedFiles = new ArrayList<>(files);
        sortedFiles.sort(Comparator.comparingLong(f -> f.lastModified));

        long size = currentSize;
        for (CacheFileInfo fileInfo : sortedFiles) {
            if (size <= maxSize) {
                break;
            }

            try {
                Files.delete(fileInfo.path);
                deletedCount++;
                freedSpace += fileInfo.size;
                size -= fileInfo.size;
                logger.debug("Deleted LRU cache file: {}", fileInfo.path);
            } catch (IOException e) {
                logger.warn("Failed to delete LRU file: {}", fileInfo.path, e);
            }
        }

        if (deletedCount > 0) {
            logger.info("LRU cleanup: {} files deleted, {} MB freed", deletedCount, freedSpace / (1024 * 1024));
        }

        return deletedCount;
    }

    /**
     * Internal class to hold cache file information
     */
    private static class CacheFileInfo {
        final Path path;
        final long size;
        final long lastModified;

        CacheFileInfo(Path path, long size, long lastModified) {
            this.path = path;
            this.size = size;
            this.lastModified = lastModified;
        }
    }
}
