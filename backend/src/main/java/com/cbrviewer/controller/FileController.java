package com.cbrviewer.controller;

import com.cbrviewer.dto.ComicFileResponse;
import com.cbrviewer.model.ComicFile;
import com.cbrviewer.service.FileService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping
    public ResponseEntity<ComicFileResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            HttpSession session) throws IOException {

        // 1. Get current user from session
        Long userId = (Long) session.getAttribute("userId");
        String role = (String) session.getAttribute("role");

        // 2. Check authentication
        if (userId == null || role == null) {
            throw new AccessDeniedException("Authentication required");
        }

        // 3. Check admin authorization
        if (!"ADMIN".equals(role)) {
            throw new AccessDeniedException("Admin role required for file upload");
        }

        // 4. Delegate to service
        ComicFile savedFile = fileService.uploadFile(file, userId);

        // 5. Convert to DTO and return
        ComicFileResponse response = ComicFileResponse.fromEntity(savedFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ComicFileResponse>> listFiles() {
        // Get all files sorted by created date DESC
        List<ComicFile> files = fileService.listFiles();

        // Convert to DTOs
        List<ComicFileResponse> response = files.stream()
                .map(ComicFileResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComicFileResponse> getFile(@PathVariable Long id) {
        ComicFile file = fileService.getFile(id);
        ComicFileResponse response = ComicFileResponse.fromEntity(file);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteFile(
            @PathVariable Long id,
            HttpSession session) throws IOException {

        // 1. Get role from session
        String role = (String) session.getAttribute("role");

        // 2. Check authentication
        if (role == null) {
            throw new AccessDeniedException("Authentication required");
        }

        // 3. Check admin authorization
        if (!"ADMIN".equals(role)) {
            throw new AccessDeniedException("Admin role required for file deletion");
        }

        // 4. Delegate to service
        fileService.deleteFile(id);

        // 5. Return success message
        Map<String, String> response = new HashMap<>();
        response.put("message", "File deleted successfully");
        return ResponseEntity.ok(response);
    }
}
