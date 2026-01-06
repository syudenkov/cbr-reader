package com.cbrviewer.service;

import com.cbrviewer.exception.InvalidArchiveException;
import com.cbrviewer.model.ComicFile;
import com.cbrviewer.repository.ComicFileRepository;
import com.cbrviewer.util.ArchiveUtil;
import com.cbrviewer.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    private final ComicFileRepository comicFileRepository;

    @Value("${app.storage.comics-path}")
    private String comicsBasePath;

    private static final long MAX_FILE_SIZE = 500L * 1024 * 1024; // 500 MB

    public FileService(ComicFileRepository comicFileRepository) {
        this.comicFileRepository = comicFileRepository;
    }

    @Transactional
    public ComicFile uploadFile(MultipartFile file, Long userId) throws IOException {
        // 1. Validate file
        validateFile(file);

        // 2. Validate file type using magic bytes
        byte[] fileBytes = file.getBytes();
        String fileType = determineFileType(fileBytes, file.getOriginalFilename());

        // 3. Generate UUID for storage filename
        String fileExtension = FileUtil.getFileExtension(file.getOriginalFilename());
        String uuid = UUID.randomUUID().toString();
        String storageFilename = uuid + fileExtension;

        // 4. Create storage directory
        String userDirectory = comicsBasePath + File.separator + userId;
        String filePath = FileUtil.saveFile(file, userDirectory, storageFilename);

        // 5. Validate archive integrity
        File savedFile = new File(filePath);
        ArchiveUtil.validateArchive(savedFile, fileType);

        // 6. Calculate page count
        int pageCount = ArchiveUtil.getPageCount(savedFile, fileType);

        // 7. Create ComicFile entity
        ComicFile comicFile = new ComicFile(
            storageFilename,
            file.getOriginalFilename(),
            filePath,
            fileType,
            file.getSize(),
            pageCount,
            userId
        );

        // 8. Save to database
        return comicFileRepository.save(comicFile);
    }

    public List<ComicFile> listFiles() {
        // Return all files sorted by createdAt DESC
        return comicFileRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public ComicFile getFile(Long fileId) {
        return comicFileRepository.findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found with id: " + fileId));
    }

    @Transactional
    public void deleteFile(Long fileId) throws IOException {
        // 1. Find file by ID
        ComicFile comicFile = comicFileRepository.findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found with id: " + fileId));

        // 2. Delete physical file from disk
        FileUtil.deleteFile(comicFile.getFilePath());

        // 3. Delete database record
        comicFileRepository.delete(comicFile);
    }

    private void validateFile(MultipartFile file) {
        // Check if file is null or empty
        if (file == null || file.isEmpty()) {
            throw new InvalidArchiveException("File is empty or not provided");
        }

        // Check original filename for path traversal
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !FileUtil.isSafeFilename(originalFilename)) {
            throw new InvalidArchiveException("Invalid filename");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidArchiveException("File size exceeds maximum limit of 500MB");
        }

        // Check if file size is 0
        if (file.getSize() == 0) {
            throw new InvalidArchiveException("File is empty");
        }
    }

    private String determineFileType(byte[] fileBytes, String filename) {
        // Check magic bytes for CBZ (ZIP)
        if (ArchiveUtil.isCBZ(fileBytes)) {
            return "CBZ";
        }

        // Check magic bytes for CBR (RAR)
        if (ArchiveUtil.isCBR(fileBytes)) {
            return "CBR";
        }

        // If magic bytes don't match, throw exception
        throw new InvalidArchiveException("Invalid archive format. Only CBR and CBZ files are supported.");
    }
}
