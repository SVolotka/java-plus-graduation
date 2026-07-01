package ru.practicum.comment.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.comment.dto.CommentResponseDto;
import ru.practicum.comment.dto.ReactionResponseDto;
import ru.practicum.comment.entity.Reaction;
import ru.practicum.user.dto.UserShortDto;
import ru.practicum.user.mapper.UserMapper;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class ReactionMapper {

    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ReactionResponseDto toReactionResponseDto(Reaction reaction) {
        UserShortDto userShortDto = userMapper.toUserShortDto(reaction.getEvaluator());
        CommentResponseDto commentResponseDto = commentMapper.toCommentResponseDto(reaction.getComment());

        ReactionResponseDto reactionResponseDto = ReactionResponseDto.builder()
                .id(reaction.getId())
                .voteType(reaction.getVoteType())
                .evaluator(userShortDto)
                .commentResponseDto(commentResponseDto)
                .created(formatter.format(reaction.getCreated()))
                .build();

        if (reaction.getUpdated() != null) {
            reactionResponseDto.setUpdated(formatter.format(reaction.getUpdated()));
        }

        return reactionResponseDto;
    }
}
