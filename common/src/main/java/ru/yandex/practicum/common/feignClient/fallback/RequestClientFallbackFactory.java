package ru.yandex.practicum.common.feignClient.fallback;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.common.feignClient.RequestClient;
import ru.yandex.practicum.common.requestService.dto.EventRequestStatusUpdateRequest;
import ru.yandex.practicum.common.requestService.dto.EventRequestStatusUpdateResult;
import ru.yandex.practicum.common.requestService.dto.ParticipationRequestDto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RequestClientFallbackFactory implements FallbackFactory<RequestClient> {

    @Override
    public RequestClient create(Throwable cause) {
        if (cause instanceof FeignException) {
            throw (FeignException) cause;
        }

        log.warn("RequestClient fallback triggered due to: {}", cause.getMessage());
        return new RequestClient() {
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

            @Override
            public boolean isUserConfirmed(Long userId, Long eventId) {
                return false;
            }
        };
    }
}