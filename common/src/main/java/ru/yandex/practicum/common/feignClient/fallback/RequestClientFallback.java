package ru.yandex.practicum.common.feignClient.fallback;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.common.feignClient.RequestClient;
import ru.yandex.practicum.common.requestService.dto.EventRequestStatusUpdateRequest;
import ru.yandex.practicum.common.requestService.dto.EventRequestStatusUpdateResult;
import ru.yandex.practicum.common.requestService.dto.ParticipationRequestDto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class RequestClientFallback implements RequestClient {
    @Override
    public Map<Long, Long> getConfirmedRequestsForEvents(List<Long> eventIds) {
        return Collections.emptyMap();
    }

    @Override
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        return Collections.emptyList();
    }

    @Override
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest request) {
        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(Collections.emptyList())
                .rejectedRequests(Collections.emptyList())
                .build();
    }
}
