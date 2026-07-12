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

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ParticipationRequest toEntity(Long requesterId, Long eventId, RequestStatus status) {
        return ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .requesterId(requesterId)
                .eventId(eventId)
                .status(status)
                .build();
    }

    public ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(formatter.format(request.getCreated()))
                .event(request.getEventId())
                .requester(request.getRequesterId())
                .status(request.getStatus().name())
                .build();
    }

    public List<ParticipationRequestDto> toDtoList(List<ParticipationRequest> requests) {
        return requests.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}