package com.cbrviewer.repository;

import com.cbrviewer.model.TtsJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TtsJobRepository extends JpaRepository<TtsJob, Long> {

    List<TtsJob> findByStatus(String status);

    List<TtsJob> findByFileId(Long fileId);

    Optional<TtsJob> findByFileIdAndStatus(Long fileId, String status);
}
