package com.cbrviewer.controller;

import com.cbrviewer.dto.AverageRatingResponse;
import com.cbrviewer.dto.RatingRequest;
import com.cbrviewer.dto.RatingResponse;
import com.cbrviewer.exception.InvalidCredentialsException;
import com.cbrviewer.model.Rating;
import com.cbrviewer.service.RatingService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {
    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping("/{fileId}")
    public ResponseEntity<RatingResponse> submitRating(
            @PathVariable Long fileId,
            @Valid @RequestBody RatingRequest request,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new InvalidCredentialsException("User not authenticated");
        }

        Rating rating = ratingService.submitRating(userId, fileId, request.getRating());
        return ResponseEntity.ok(RatingResponse.fromEntity(rating));
    }

    @GetMapping("/{fileId}/average")
    public ResponseEntity<AverageRatingResponse> getAverageRating(@PathVariable Long fileId) {
        AverageRatingResponse response = ratingService.getAverageRating(fileId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<RatingResponse> getUserRating(
            @PathVariable Long fileId,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new InvalidCredentialsException("User not authenticated");
        }

        return ratingService.getUserRating(userId, fileId)
            .map(rating -> ResponseEntity.ok(RatingResponse.fromEntity(rating)))
            .orElse(ResponseEntity.ok(null));
    }
}
