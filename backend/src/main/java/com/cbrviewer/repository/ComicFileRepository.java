package com.cbrviewer.repository;

import com.cbrviewer.model.ComicFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComicFileRepository extends JpaRepository<ComicFile, Long> {

    List<ComicFile> findByUploadedBy(Long userId);

    Optional<ComicFile> findByFilename(String filename);
}
