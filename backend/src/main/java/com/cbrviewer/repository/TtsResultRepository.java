package com.cbrviewer.repository;

import com.cbrviewer.model.TtsResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TtsResultRepository extends JpaRepository<TtsResult, Long> {

    Optional<TtsResult> findByFileIdAndPageNumber(Long fileId, Integer pageNumber);

    List<TtsResult> findByFileId(Long fileId);
}
