package ru.yandex.practicum.core.commentService.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "reactions")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@ToString
@EqualsAndHashCode(of = "id")
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Pattern(regexp = "^(LIKE|DISLIKE)$")
    @Column(name = "vote_type", nullable = false)
    String voteType;

    @Column(name = "comment_id", nullable = false)
    Long commentId;

    @Column(name = "evaluator_id", nullable = false)
    Long evaluatorId;

    @CreationTimestamp
    @Column(name = "created_time")
    LocalDateTime created;

    @UpdateTimestamp
    @Column(name = "updated_time")
    LocalDateTime updated;
}