package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.model.Similarity;

import java.util.List;

public interface SimilarityRepository extends JpaRepository<Similarity, Long> {
    @Query("SELECT s FROM Similarity s WHERE s.event1 = :eventId OR s.event2 = :eventId")
    List<Similarity> findByEventId(@Param("eventId") Long eventId);

    @Modifying
    @Query("UPDATE Similarity s SET s.similarity = :similarity, s.timestamp = :timestamp WHERE s.event1 = :event1 AND s.event2 = :event2")
    int updateSimilarity(@Param("event1") Long event1, @Param("event2") Long event2,
                         @Param("similarity") Float similarity, @Param("timestamp") java.time.Instant timestamp);
}