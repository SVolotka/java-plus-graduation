package ru.yandex.practicum.core.requestService.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.common.requestService.dto.ParticipationRequestDto;
import ru.yandex.practicum.common.requestService.enums.RequestStatus;
import ru.yandex.practicum.core.requestService.entity.ParticipationRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RequestMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public ParticipationRequest toEntity(Long requesterId, Long eventId, RequestStatus status) {
        return ParticipationRequest.builder()
                .requesterId(requesterId)
                .eventId(eventId)
                .status(status)
                .created(LocalDateTime.now())
                .build();
    }

    public ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .requester(request.getRequesterId())
                .event(request.getEventId())
                .status(request.getStatus().name())
                .created(request.getCreated() != null ? request.getCreated().format(FORMATTER) : null)
                .build();
    }

    public List<ParticipationRequestDto> toDtoList(List<ParticipationRequest> requests) {
        return requests.stream().map(this::toDto).collect(Collectors.toList());
    }
}