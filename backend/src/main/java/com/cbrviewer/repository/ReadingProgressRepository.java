package com.cbrviewer.repository;

import com.cbrviewer.model.ReadingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Long> {

    Optional<ReadingProgress> findByUserIdAndFileId(Long userId, Long fileId);

    List<ReadingProgress> findByUserId(Long userId);
}
