package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.model.Interaction;

import java.util.List;
import java.util.Optional;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {
    List<Interaction> findByUserIdOrderByTimestampDesc(Long userId);

    @Query("SELECT i.eventId FROM Interaction i WHERE i.userId = :userId")
    List<Long> findEventIdsByUserId(@Param("userId") Long userId);

    Optional<Interaction> findByUserIdAndEventId(long userId, long eventId);
}