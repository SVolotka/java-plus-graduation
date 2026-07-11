package ru.yandex.practicum.common.feignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.common.feignClient.fallback.RequestClientFallback;
import ru.yandex.practicum.common.requestService.dto.EventRequestStatusUpdateRequest;
import ru.yandex.practicum.common.requestService.dto.EventRequestStatusUpdateResult;
import ru.yandex.practicum.common.requestService.dto.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service", fallback = RequestClientFallback.class)
public interface RequestClient {

    @GetMapping("/internal/requests/count")
    Map<Long, Long> getConfirmedRequestsForEvents(@RequestParam("eventIds") List<Long> eventIds);

    @GetMapping("/users/{userId}/events/{eventId}/requests")
    List<ParticipationRequestDto> getEventParticipants(@PathVariable Long userId, @PathVariable Long eventId);

    @PatchMapping("/users/{userId}/events/{eventId}/requests")
    EventRequestStatusUpdateResult changeRequestStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody EventRequestStatusUpdateRequest request);
}