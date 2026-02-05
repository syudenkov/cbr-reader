package com.cbrviewer.service;

import com.cbrviewer.dto.ReadingProgressResponse;
import com.cbrviewer.model.ReadingProgress;
import com.cbrviewer.repository.ReadingProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private ReadingProgressRepository progressRepository;

    @InjectMocks
    private ProgressService progressService;

    private ReadingProgress existingProgress;

    @BeforeEach
    void setUp() {
        existingProgress = new ReadingProgress(1L, 1L, 10);
        existingProgress.setId(1L);
    }

    @Nested
    @DisplayName("Get All User Progress")
    class GetAllUserProgressTests {

        @Test
        @DisplayName("should return empty list when user has no progress")
        void shouldReturnEmptyListWhenNoProgress() {
            when(progressRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

            List<ReadingProgressResponse> result = progressService.getAllUserProgress(1L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return all progress for user")
        void shouldReturnAllProgressForUser() {
            ReadingProgress progress1 = new ReadingProgress(1L, 1L, 10);
            ReadingProgress progress2 = new ReadingProgress(1L, 2L, 20);

            when(progressRepository.findByUserId(1L)).thenReturn(Arrays.asList(progress1, progress2));

            List<ReadingProgressResponse> result = progressService.getAllUserProgress(1L);

            assertNotNull(result);
            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("Get Progress")
    class GetProgressTests {

        @Test
        @DisplayName("should return existing progress when found")
        void shouldReturnExistingProgress() {
            when(progressRepository.findByUserIdAndFileId(1L, 1L)).thenReturn(Optional.of(existingProgress));

            ReadingProgressResponse result = progressService.getProgress(1L, 1L);

            assertNotNull(result);
            assertEquals(1L, result.getFileId());
            assertEquals(10, result.getCurrentPage());
        }

        @Test
        @DisplayName("should return default progress (page 1) when no record exists")
        void shouldReturnDefaultProgressWhenNotFound() {
            when(progressRepository.findByUserIdAndFileId(1L, 1L)).thenReturn(Optional.empty());

            ReadingProgressResponse result = progressService.getProgress(1L, 1L);

            assertNotNull(result);
            assertEquals(1L, result.getFileId());
            assertEquals(1, result.getCurrentPage());
        }
    }

    @Nested
    @DisplayName("Update Progress")
    class UpdateProgressTests {

        @Test
        @DisplayName("should update existing progress")
        void shouldUpdateExistingProgress() {
            when(progressRepository.findByUserIdAndFileId(1L, 1L)).thenReturn(Optional.of(existingProgress));
            when(progressRepository.save(any(ReadingProgress.class))).thenAnswer(invocation -> {
                ReadingProgress saved = invocation.getArgument(0);
                return saved;
            });

            ReadingProgressResponse result = progressService.updateProgress(1L, 1L, 15);

            assertNotNull(result);
            assertEquals(15, result.getCurrentPage());

            verify(progressRepository).save(existingProgress);
            assertEquals(15, existingProgress.getCurrentPage());
        }

        @Test
        @DisplayName("should create new progress when not exists")
        void shouldCreateNewProgressWhenNotExists() {
            when(progressRepository.findByUserIdAndFileId(1L, 2L)).thenReturn(Optional.empty());
            when(progressRepository.save(any(ReadingProgress.class))).thenAnswer(invocation -> {
                ReadingProgress saved = invocation.getArgument(0);
                saved.setId(2L);
                return saved;
            });

            ReadingProgressResponse result = progressService.updateProgress(1L, 2L, 5);

            assertNotNull(result);
            assertEquals(2L, result.getFileId());
            assertEquals(5, result.getCurrentPage());

            verify(progressRepository).save(any(ReadingProgress.class));
        }

        @Test
        @DisplayName("should handle page 1 progress update")
        void shouldHandlePageOneProgress() {
            when(progressRepository.findByUserIdAndFileId(1L, 1L)).thenReturn(Optional.empty());
            when(progressRepository.save(any(ReadingProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ReadingProgressResponse result = progressService.updateProgress(1L, 1L, 1);

            assertNotNull(result);
            assertEquals(1, result.getCurrentPage());
        }

        @Test
        @DisplayName("should handle large page numbers")
        void shouldHandleLargePageNumbers() {
            when(progressRepository.findByUserIdAndFileId(1L, 1L)).thenReturn(Optional.of(existingProgress));
            when(progressRepository.save(any(ReadingProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ReadingProgressResponse result = progressService.updateProgress(1L, 1L, 500);

            assertNotNull(result);
            assertEquals(500, result.getCurrentPage());
        }
    }
}
