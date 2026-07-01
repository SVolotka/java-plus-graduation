package ru.practicum.comment.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.user.dto.UserShortDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReactionResponseDto {

    Long id;
    String voteType;
    UserShortDto evaluator;
    CommentResponseDto commentResponseDto;
    String created;
    String updated;
}
