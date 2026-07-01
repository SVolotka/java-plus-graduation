package ru.practicum.comment.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentStatsResponse {
    Long commentId;
    Long likesCount;
    Long dislikesCount;
}
