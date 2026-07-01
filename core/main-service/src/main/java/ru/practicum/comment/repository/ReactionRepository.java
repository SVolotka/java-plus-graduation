package ru.practicum.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.comment.entity.Reaction;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    @Query("SELECT r.voteType, COUNT(r) " +
            "FROM Reaction r " +
            "WHERE r.comment.id = :commentId " +
            "GROUP BY r.voteType")
    List<Object[]> getLikesAndDislikesCount(Long commentId);

    @Query("SELECT r " +
            "FROM Reaction r " +
            "WHERE r.evaluator.id = :evaluatorId AND r.comment.id = :commentId")
    Optional<Reaction> existByUserAndComment(Long evaluatorId, Long commentId);
}
