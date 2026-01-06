package com.cbrviewer.controller;

import com.cbrviewer.dto.ProgressUpdateRequest;
import com.cbrviewer.dto.ReadingProgressResponse;
import com.cbrviewer.exception.InvalidCredentialsException;
import com.cbrviewer.service.ProgressService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping
    public ResponseEntity<List<ReadingProgressResponse>> getAllProgress(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new InvalidCredentialsException("Not authenticated");
        }

        List<ReadingProgressResponse> progress = progressService.getAllUserProgress(userId);
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<ReadingProgressResponse> getProgress(
            @PathVariable Long fileId,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new InvalidCredentialsException("Not authenticated");
        }

        ReadingProgressResponse progress = progressService.getProgress(userId, fileId);
        return ResponseEntity.ok(progress);
    }

    @PutMapping("/{fileId}")
    public ResponseEntity<ReadingProgressResponse> updateProgress(
            @PathVariable Long fileId,
            @Valid @RequestBody ProgressUpdateRequest request,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new InvalidCredentialsException("Not authenticated");
        }

        ReadingProgressResponse progress = progressService.updateProgress(
            userId,
            fileId,
            request.getCurrentPage()
        );

        return ResponseEntity.ok(progress);
    }
}
