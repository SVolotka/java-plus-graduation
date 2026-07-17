package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.model.Similarity;

import java.util.List;

public interface SimilarityRepository extends JpaRepository<Similarity, Long> {
    @Query("SELECT s FROM Similarity s WHERE s.event1 = :eventId OR s.event2 = :eventId")
    List<Similarity> findByEventId(@Param("eventId") Long eventId);
}