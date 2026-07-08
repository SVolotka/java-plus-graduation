package ru.yandex.practicum.core.commentService.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.core.commentService.entity.Reaction;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    @Query("SELECT r.voteType, COUNT(r) " +
            "FROM Reaction r " +
            "WHERE r.commentId = :commentId " +
            "GROUP BY r.voteType")
    List<Object[]> getLikesAndDislikesCount(Long commentId);

    @Query("SELECT r FROM Reaction r WHERE r.evaluatorId = :evaluatorId AND r.commentId = :commentId")
    Optional<Reaction> existByUserAndComment(Long evaluatorId, Long commentId);

    @Query("SELECT r.commentId FROM Reaction r WHERE r.voteType = :voteType GROUP BY r.commentId ORDER BY COUNT(r) ASC")
    List<Long> findCommentIdsByVoteTypeAsc(String voteType);

    @Query("SELECT r.commentId FROM Reaction r WHERE r.voteType = :voteType GROUP BY r.commentId ORDER BY COUNT(r) DESC")
    List<Long> findCommentIdsByVoteTypeDesc(String voteType);
}
