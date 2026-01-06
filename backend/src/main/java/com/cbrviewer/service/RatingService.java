package com.cbrviewer.service;

import com.cbrviewer.dto.AverageRatingResponse;
import com.cbrviewer.model.Rating;
import com.cbrviewer.repository.RatingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RatingService {
    private final RatingRepository ratingRepository;

    public RatingService(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    @Transactional
    public Rating submitRating(Long userId, Long fileId, Integer ratingValue) {
        // Check if user has already rated this file
        Optional<Rating> existingRating = ratingRepository.findByUserIdAndFileId(userId, fileId);

        if (existingRating.isPresent()) {
            // Update existing rating
            Rating rating = existingRating.get();
            rating.setRating(ratingValue);
            return ratingRepository.save(rating);
        } else {
            // Create new rating
            Rating rating = new Rating();
            rating.setUserId(userId);
            rating.setFileId(fileId);
            rating.setRating(ratingValue);
            return ratingRepository.save(rating);
        }
    }

    public AverageRatingResponse getAverageRating(Long fileId) {
        List<Rating> ratings = ratingRepository.findByFileId(fileId);

        if (ratings.isEmpty()) {
            return new AverageRatingResponse(fileId, null, 0);
        }

        double sum = ratings.stream()
            .mapToInt(Rating::getRating)
            .sum();
        double average = sum / ratings.size();

        return new AverageRatingResponse(fileId, average, ratings.size());
    }

    public Optional<Rating> getUserRating(Long userId, Long fileId) {
        return ratingRepository.findByUserIdAndFileId(userId, fileId);
    }
}
