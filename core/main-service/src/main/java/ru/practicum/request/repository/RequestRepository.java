package ru.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.request.entity.ParticipationRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findAllByRequesterId(Long userId);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    Optional<ParticipationRequest> findByRequesterIdAndEventId(Long userId, Long eventId);

    boolean existsByRequesterIdAndEventId(Long userId, Long eventId);

    @Query("SELECT COUNT(pr) FROM ParticipationRequest pr " +
            "WHERE pr.event.id = :eventId AND pr.status = 'CONFIRMED'")
    Long countConfirmedRequestsByEventId(@Param("eventId") Long eventId);

    @Query("SELECT e.id, COUNT(pr) " +
            "FROM Event e " +
            "LEFT JOIN ParticipationRequest pr ON pr.event.id = e.id " +
            "WHERE e.id IN :events AND pr.status = 'CONFIRMED' " +
            "GROUP BY e.id")
    List<Object[]> countConfirmedRequestsForEvents(List<Long> events);

    List<ParticipationRequest> findAllByIdInAndEventId(List<Long> ids, Long eventId);

    @Modifying
    @Query("UPDATE ParticipationRequest pr SET pr.status = 'REJECTED' " +
            "WHERE pr.event.id = :eventId AND pr.status = 'PENDING'")
    void rejectAllPendingRequestsByEventId(@Param("eventId") Long eventId);
}