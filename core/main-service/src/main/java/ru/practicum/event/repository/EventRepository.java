package ru.practicum.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.event.entity.Event;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    @Query("SELECT e FROM Event e WHERE e.id = :eventId AND e.state = 'PUBLISHED'")
    Optional<Event> findPublishedEventById(Long eventId);

    boolean existsByCategoryId(Long categoryId);

    boolean existsById(Long id);
}
