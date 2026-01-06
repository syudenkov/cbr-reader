package com.cbrviewer.service;

import com.cbrviewer.dto.ReadingProgressResponse;
import com.cbrviewer.model.ReadingProgress;
import com.cbrviewer.repository.ReadingProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProgressService {

    private final ReadingProgressRepository progressRepository;

    public ProgressService(ReadingProgressRepository progressRepository) {
        this.progressRepository = progressRepository;
    }

    public List<ReadingProgressResponse> getAllUserProgress(Long userId) {
        List<ReadingProgress> progressList = progressRepository.findByUserId(userId);
        return progressList.stream()
            .map(ReadingProgressResponse::fromEntity)
            .collect(Collectors.toList());
    }

    public ReadingProgressResponse getProgress(Long userId, Long fileId) {
        Optional<ReadingProgress> progressOpt = progressRepository.findByUserIdAndFileId(userId, fileId);

        if (progressOpt.isPresent()) {
            return ReadingProgressResponse.fromEntity(progressOpt.get());
        } else {
            // Return default progress (page 1) if no record exists
            return new ReadingProgressResponse(fileId, 1, null);
        }
    }

    @Transactional
    public ReadingProgressResponse updateProgress(Long userId, Long fileId, Integer currentPage) {
        Optional<ReadingProgress> progressOpt = progressRepository.findByUserIdAndFileId(userId, fileId);

        ReadingProgress progress;
        if (progressOpt.isPresent()) {
            // Update existing progress
            progress = progressOpt.get();
            progress.setCurrentPage(currentPage);
        } else {
            // Create new progress record
            progress = new ReadingProgress(userId, fileId, currentPage);
        }

        ReadingProgress savedProgress = progressRepository.save(progress);
        return ReadingProgressResponse.fromEntity(savedProgress);
    }
}
