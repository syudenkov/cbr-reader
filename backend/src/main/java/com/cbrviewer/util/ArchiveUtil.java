package com.cbrviewer.util;

import com.cbrviewer.exception.InvalidArchiveException;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
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

    /**
     * Extract a specific page from an archive
     * @param archiveFile The archive file (CBZ or CBR)
     * @param fileType The file type ("CBZ" or "CBR")
     * @param pageIndex The page index (0-based)
     * @return byte array containing the image data
     * @throws InvalidArchiveException if extraction fails or page index is invalid
     */
    public static byte[] extractPage(File archiveFile, String fileType, int pageIndex) throws InvalidArchiveException {
        if ("CBZ".equalsIgnoreCase(fileType)) {
            return extractPageFromZip(archiveFile, pageIndex);
        } else if ("CBR".equalsIgnoreCase(fileType)) {
            return extractPageFromRar(archiveFile, pageIndex);
        } else {
            throw new InvalidArchiveException("Unsupported file type: " + fileType);
        }
    }

    /**
     * Extract a page from a ZIP (CBZ) archive
     */
    private static byte[] extractPageFromZip(File archiveFile, int pageIndex) throws InvalidArchiveException {
        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            // 1. List all image entries
            List<ZipEntry> imageEntries = new ArrayList<>();
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && isImageFile(entry.getName())) {
                    imageEntries.add(entry);
                }
            }

            // 2. Sort alphabetically (comics are typically numbered like 001.jpg, 002.jpg)
            imageEntries.sort(Comparator.comparing(ZipEntry::getName));

            // 3. Validate page index
            if (pageIndex < 0 || pageIndex >= imageEntries.size()) {
                throw new InvalidArchiveException("Page index out of bounds: " + pageIndex + " (total pages: " + imageEntries.size() + ")");
            }

            // 4. Extract page bytes
            ZipEntry pageEntry = imageEntries.get(pageIndex);
            try (InputStream inputStream = zipFile.getInputStream(pageEntry)) {
                return inputStream.readAllBytes();
            }
        } catch (ZipException e) {
            throw new InvalidArchiveException("Failed to extract page from ZIP: " + e.getMessage());
        } catch (IOException e) {
            throw new InvalidArchiveException("Failed to read page from ZIP: " + e.getMessage());
        }
    }

    /**
     * Extract a page from a RAR (CBR) archive
     */
    private static byte[] extractPageFromRar(File archiveFile, int pageIndex) throws InvalidArchiveException {
        try (Archive rarArchive = new Archive(archiveFile)) {
            // 1. List all image file headers
            List<FileHeader> imageHeaders = new ArrayList<>();
            for (FileHeader header : rarArchive.getFileHeaders()) {
                if (!header.isDirectory() && isImageFile(header.getFileName())) {
                    imageHeaders.add(header);
                }
            }

            // 2. Sort alphabetically
            imageHeaders.sort(Comparator.comparing(FileHeader::getFileName));

            // 3. Validate page index
            if (pageIndex < 0 || pageIndex >= imageHeaders.size()) {
                throw new InvalidArchiveException("Page index out of bounds: " + pageIndex + " (total pages: " + imageHeaders.size() + ")");
            }

            // 4. Extract page bytes
            FileHeader pageHeader = imageHeaders.get(pageIndex);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            rarArchive.extractFile(pageHeader, outputStream);
            return outputStream.toByteArray();
        } catch (RarException e) {
            throw new InvalidArchiveException("Failed to extract page from RAR: " + e.getMessage());
        } catch (IOException e) {
            throw new InvalidArchiveException("Failed to read page from RAR: " + e.getMessage());
        }
    }

    /**
     * Detect image file extension from byte array using magic bytes
     * @param imageBytes The image byte array
     * @return The file extension (e.g., ".jpg", ".png")
     */
    public static String detectImageExtension(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length < 4) {
            return ".jpg"; // default
        }

        // JPEG magic bytes: FF D8 FF
        if (imageBytes.length >= 3 &&
            (imageBytes[0] & 0xFF) == 0xFF &&
            (imageBytes[1] & 0xFF) == 0xD8 &&
            (imageBytes[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }

        // PNG magic bytes: 89 50 4E 47 0D 0A 1A 0A
        if (imageBytes.length >= 8 &&
            (imageBytes[0] & 0xFF) == 0x89 &&
            (imageBytes[1] & 0xFF) == 0x50 &&
            (imageBytes[2] & 0xFF) == 0x4E &&
            (imageBytes[3] & 0xFF) == 0x47) {
            return ".png";
        }

        // GIF magic bytes: 47 49 46 38 (GIF8)
        if (imageBytes.length >= 4 &&
            (imageBytes[0] & 0xFF) == 0x47 &&
            (imageBytes[1] & 0xFF) == 0x49 &&
            (imageBytes[2] & 0xFF) == 0x46 &&
            (imageBytes[3] & 0xFF) == 0x38) {
            return ".gif";
        }

        // WebP magic bytes: 52 49 46 46 ... 57 45 42 50 (RIFF...WEBP)
        if (imageBytes.length >= 12 &&
            (imageBytes[0] & 0xFF) == 0x52 &&
            (imageBytes[1] & 0xFF) == 0x49 &&
            (imageBytes[8] & 0xFF) == 0x57 &&
            (imageBytes[9] & 0xFF) == 0x45 &&
            (imageBytes[10] & 0xFF) == 0x42 &&
            (imageBytes[11] & 0xFF) == 0x50) {
            return ".webp";
        }

        // BMP magic bytes: 42 4D (BM)
        if (imageBytes.length >= 2 &&
            (imageBytes[0] & 0xFF) == 0x42 &&
            (imageBytes[1] & 0xFF) == 0x4D) {
            return ".bmp";
        }

        // Default to .jpg if unknown
        return ".jpg";
    }
}
