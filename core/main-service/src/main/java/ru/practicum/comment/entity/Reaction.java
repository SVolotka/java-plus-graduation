package ru.practicum.comment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ru.practicum.user.entity.User;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "reactions")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Pattern(regexp = "^(LIKE|DISLIKE)$", message = "Допустимы только значения LIKE или DISLIKE")
    @Column(name = "vote_type", nullable = false)
    String voteType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id", nullable = false)
    User evaluator;

    @CreationTimestamp
    @Column(name = "created_time")
    LocalDateTime created;

    @UpdateTimestamp
    @Column(name = "updated_time")
    LocalDateTime updated;
}
