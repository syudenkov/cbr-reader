package com.cbrviewer.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {

    /**
     * Save an uploaded file to the specified directory with the given filename
     * @param file The uploaded file
     * @param directory The directory to save to (will be created if not exists)
     * @param filename The filename to use (including extension)
     * @return The absolute path of the saved file
     * @throws IOException If file operation fails
     */
    public static String saveFile(MultipartFile file, String directory, String filename) throws IOException {
        // Create directory if not exists
        createDirectoryIfNotExists(directory);

        // Construct file path
        Path filePath = Paths.get(directory, filename);

        // Save file to disk
        file.transferTo(filePath.toFile());

        // Return absolute path
        return filePath.toAbsolutePath().toString();
    }

    /**
     * Delete a file from disk
     * @param filePath The absolute path to the file
     * @return true if file was deleted, false if file didn't exist
     * @throws IOException If deletion fails
     */
    public static boolean deleteFile(String filePath) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            Files.delete(path);
            return true;
        }
        return false;
    }

    /**
     * Create directory and all parent directories if they don't exist
     * @param directory The directory path to create
     * @throws IOException If directory creation fails
     */
    public static void createDirectoryIfNotExists(String directory) throws IOException {
        Path dirPath = Paths.get(directory);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
    }

    /**
     * Check if a filename contains path traversal attempts
     * @param filename The filename to check
     * @return true if filename is safe, false if it contains path traversal
     */
    public static boolean isSafeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        // Check for path traversal patterns
        return !filename.contains("..") &&
               !filename.contains("/") &&
               !filename.contains("\\");
    }

    /**
     * Get file extension from filename (including the dot)
     * @param filename The filename
     * @return The extension (e.g., ".cbz") or empty string if no extension
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot);
        }
        return "";
    }
}
