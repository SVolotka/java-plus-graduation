package ru.yandex.practicum.common.comment.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.common.userService.dto.UserShortDto;

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
