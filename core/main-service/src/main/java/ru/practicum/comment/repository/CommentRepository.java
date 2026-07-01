package ru.practicum.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.comment.entity.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findAllByEventId(Long eventId, Pageable pageable);

    @Query("SELECT r.comment " +
            "FROM Reaction r " +
            "WHERE r.voteType = :voteType " +
            "GROUP BY r.comment " +
            "ORDER BY COUNT(r) ASC")
    Slice<Comment> getCommentsByAsc(String voteType, Pageable pageable);

    @Query("SELECT r.comment " +
            "FROM Reaction r " +
            "WHERE r.voteType = :voteType " +
            "GROUP BY r.comment " +
            "ORDER BY COUNT(r) DESC")
    Slice<Comment> getCommentsByDesc(String voteType, Pageable pageable);
}
