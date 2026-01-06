package com.cbrviewer.util;

import com.cbrviewer.exception.InvalidArchiveException;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ArchiveUtil {

    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    ));

    /**
     * Check if file bytes match CBZ (ZIP) magic bytes
     * ZIP magic bytes: 50 4B (PK)
     */
    public static boolean isCBZ(byte[] fileBytes) {
        return fileBytes.length >= 2 &&
                (fileBytes[0] & 0xFF) == 0x50 &&
                (fileBytes[1] & 0xFF) == 0x4B;
    }

    /**
     * Check if file bytes match CBR (RAR) magic bytes
     * RAR magic bytes: 52 61 72 21 1A 07 (Rar!)
     */
    public static boolean isCBR(byte[] fileBytes) {
        return fileBytes.length >= 6 &&
                (fileBytes[0] & 0xFF) == 0x52 &&
                (fileBytes[1] & 0xFF) == 0x61 &&
                (fileBytes[2] & 0xFF) == 0x72 &&
                (fileBytes[3] & 0xFF) == 0x21 &&
                (fileBytes[4] & 0xFF) == 0x1A &&
                (fileBytes[5] & 0xFF) == 0x07;
    }

    /**
     * Get the number of image pages in an archive
     */
    public static int getPageCount(File archiveFile, String fileType) throws InvalidArchiveException {
        if ("CBZ".equalsIgnoreCase(fileType)) {
            return getPageCountFromZip(archiveFile);
        } else if ("CBR".equalsIgnoreCase(fileType)) {
            return getPageCountFromRar(archiveFile);
        } else {
            throw new InvalidArchiveException("Unsupported file type: " + fileType);
        }
    }

    /**
     * Count image files in a ZIP archive
     */
    private static int getPageCountFromZip(File archiveFile) throws InvalidArchiveException {
        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            int count = 0;
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && isImageFile(entry.getName())) {
                    count++;
                }
            }
            return count;
        } catch (ZipException e) {
            throw new InvalidArchiveException("Corrupt ZIP archive: " + e.getMessage());
        } catch (IOException e) {
            throw new InvalidArchiveException("Failed to read ZIP archive: " + e.getMessage());
        }
    }

    /**
     * Count image files in a RAR archive
     */
    private static int getPageCountFromRar(File archiveFile) throws InvalidArchiveException {
        try (Archive rarArchive = new Archive(archiveFile)) {
            int count = 0;
            for (FileHeader fileHeader : rarArchive.getFileHeaders()) {
                if (!fileHeader.isDirectory() && isImageFile(fileHeader.getFileName())) {
                    count++;
                }
            }
            return count;
        } catch (RarException e) {
            throw new InvalidArchiveException("Corrupt RAR archive: " + e.getMessage());
        } catch (IOException e) {
            throw new InvalidArchiveException("Failed to read RAR archive: " + e.getMessage());
        }
    }

    /**
     * Validate that the archive can be opened and is not corrupt
     */
    public static void validateArchive(File archiveFile, String fileType) throws InvalidArchiveException {
        if ("CBZ".equalsIgnoreCase(fileType)) {
            validateZipArchive(archiveFile);
        } else if ("CBR".equalsIgnoreCase(fileType)) {
            validateRarArchive(archiveFile);
        } else {
            throw new InvalidArchiveException("Unsupported file type: " + fileType);
        }
    }

    /**
     * Validate ZIP archive can be opened
     */
    private static void validateZipArchive(File archiveFile) throws InvalidArchiveException {
        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            // Just opening it is enough to validate basic structure
            if (zipFile.size() == 0) {
                throw new InvalidArchiveException("ZIP archive is empty");
            }
        } catch (ZipException e) {
            throw new InvalidArchiveException("Corrupt ZIP archive: " + e.getMessage());
        } catch (IOException e) {
            throw new InvalidArchiveException("Failed to read ZIP archive: " + e.getMessage());
        }
    }

    /**
     * Validate RAR archive can be opened
     */
    private static void validateRarArchive(File archiveFile) throws InvalidArchiveException {
        try (Archive rarArchive = new Archive(archiveFile)) {
            // Just opening it is enough to validate basic structure
            if (rarArchive.getFileHeaders().isEmpty()) {
                throw new InvalidArchiveException("RAR archive is empty");
            }
        } catch (RarException e) {
            throw new InvalidArchiveException("Corrupt RAR archive: " + e.getMessage());
        } catch (IOException e) {
            throw new InvalidArchiveException("Failed to read RAR archive: " + e.getMessage());
        }
    }

    /**
     * Check if filename has an image extension
     */
    private static boolean isImageFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        String lowerFilename = filename.toLowerCase();
        for (String ext : IMAGE_EXTENSIONS) {
            if (lowerFilename.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
