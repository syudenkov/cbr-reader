package com.cbrviewer.repository;

import com.cbrviewer.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserIdAndFileId(Long userId, Long fileId);

    List<Rating> findByFileId(Long fileId);
}
