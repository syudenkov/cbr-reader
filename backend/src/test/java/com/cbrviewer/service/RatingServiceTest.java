package com.cbrviewer.service;

import com.cbrviewer.dto.AverageRatingResponse;
import com.cbrviewer.model.Rating;
import com.cbrviewer.repository.RatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @InjectMocks
    private RatingService ratingService;

    private Rating existingRating;

    @BeforeEach
    void setUp() {
        existingRating = new Rating(1L, 1L, 4);
        existingRating.setId(1L);
    }

    @Nested
    @DisplayName("Submit Rating")
    class SubmitRatingTests {

        @Test
        @DisplayName("should create new rating when user has not rated before")
        void shouldCreateNewRating() {
            when(ratingRepository.findByUserIdAndFileId(1L, 1L)).thenReturn(Optional.empty());
            when(ratingRepository.save(any(Rating.class))).thenAnswer(invocation -> {
                Rating rating = invocation.getArgument(0);
                rating.setId(1L);
                return rating;
            });

            Rating result = ratingService.submitRating(1L, 1L, 5);

            assertNotNull(result);
            assertEquals(5, result.getRating());
            assertEquals(1L, result.getUserId());
            assertEquals(1L, result.getFileId());

            verify(ratingRepository).save(any(Rating.class));
        }

        @Test
        @DisplayName("should update existing rating when user has already rated")
        void shouldUpdateExistingRating() {
            when(ratingRepository.findByUserIdAndFileId(1L, 1L)).thenReturn(Optional.of(existingRating));
            when(ratingRepository.save(any(Rating.class))).thenReturn(existingRating);

            Rating result = ratingService.submitRating(1L, 1L, 5);

            assertNotNull(result);
            assertEquals(5, result.getRating());

            verify(ratingRepository).save(existingRating);
        }

        @Test
        @DisplayName("should allow rating values from 1 to 5")
        void shouldAllowValidRatingValues() {
            when(ratingRepository.findByUserIdAndFileId(anyLong(), anyLong())).thenReturn(Optional.empty());
            when(ratingRepository.save(any(Rating.class))).thenAnswer(invocation -> invocation.getArgument(0));

            for (int i = 1; i <= 5; i++) {
                Rating result = ratingService.submitRating(1L, 1L, i);
                assertEquals(i, result.getRating());
            }
        }
    }

    @Nested
    @DisplayName("Get Average Rating")
    class GetAverageRatingTests {

        @Test
        @DisplayName("should return null average when no ratings exist")
        void shouldReturnNullAverageWhenNoRatings() {
            when(ratingRepository.findByFileId(1L)).thenReturn(Collections.emptyList());

            AverageRatingResponse result = ratingService.getAverageRating(1L);

            assertNotNull(result);
            assertNull(result.getAverageRating());
            assertEquals(0, result.getTotalRatings());
            assertEquals(1L, result.getFileId());
        }

        @Test
        @DisplayName("should calculate correct average for single rating")
        void shouldCalculateAverageForSingleRating() {
            Rating rating = new Rating(1L, 1L, 5);
            when(ratingRepository.findByFileId(1L)).thenReturn(Collections.singletonList(rating));

            AverageRatingResponse result = ratingService.getAverageRating(1L);

            assertNotNull(result);
            assertEquals(5.0, result.getAverageRating());
            assertEquals(1, result.getTotalRatings());
        }

        @Test
        @DisplayName("should calculate correct average for multiple ratings")
        void shouldCalculateAverageForMultipleRatings() {
            Rating rating1 = new Rating(1L, 1L, 5);
            Rating rating2 = new Rating(2L, 1L, 4);
            Rating rating3 = new Rating(3L, 1L, 3);

            when(ratingRepository.findByFileId(1L)).thenReturn(Arrays.asList(rating1, rating2, rating3));

            AverageRatingResponse result = ratingService.getAverageRating(1L);

            assertNotNull(result);
            assertEquals(4.0, result.getAverageRating()); // (5+4+3)/3 = 4.0
            assertEquals(3, result.getTotalRatings());
        }

        @Test
        @DisplayName("should handle decimal averages correctly")
        void shouldHandleDecimalAverages() {
            Rating rating1 = new Rating(1L, 1L, 5);
            Rating rating2 = new Rating(2L, 1L, 4);

            when(ratingRepository.findByFileId(1L)).thenReturn(Arrays.asList(rating1, rating2));

            AverageRatingResponse result = ratingService.getAverageRating(1L);

            assertNotNull(result);
            assertEquals(4.5, result.getAverageRating()); // (5+4)/2 = 4.5
            assertEquals(2, result.getTotalRatings());
        }
    }

    @Nested
    @DisplayName("Get User Rating")
    class GetUserRatingTests {

        @Test
        @DisplayName("should return user rating when it exists")
        void shouldReturnUserRatingWhenExists() {
            when(ratingRepository.findByUserIdAndFileId(1L, 1L)).thenReturn(Optional.of(existingRating));

            Optional<Rating> result = ratingService.getUserRating(1L, 1L);

            assertTrue(result.isPresent());
            assertEquals(4, result.get().getRating());
        }

        @Test
        @DisplayName("should return empty when user has not rated")
        void shouldReturnEmptyWhenUserHasNotRated() {
            when(ratingRepository.findByUserIdAndFileId(1L, 1L)).thenReturn(Optional.empty());

            Optional<Rating> result = ratingService.getUserRating(1L, 1L);

            assertFalse(result.isPresent());
        }
    }
}
