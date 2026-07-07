package ru.yandex.practicum.core.requestService.controller;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.common.requestService.dto.EventRequestStatusUpdateRequest;
import ru.yandex.practicum.common.requestService.dto.EventRequestStatusUpdateResult;
import ru.yandex.practicum.common.requestService.dto.ParticipationRequestDto;
import ru.yandex.practicum.core.requestService.service.RequestService;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events/{eventId}/requests")
public class EventRequestController {

    private final RequestService requestService;

    @GetMapping
    public ResponseEntity<List<ParticipationRequestDto>> getEventParticipants(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long eventId) {
        return ResponseEntity.ok(requestService.getEventParticipants(userId, eventId));
    }

    @PatchMapping
    public ResponseEntity<EventRequestStatusUpdateResult> changeRequestStatus(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long eventId,
            @RequestBody EventRequestStatusUpdateRequest request) {
        return ResponseEntity.ok(requestService.changeRequestStatus(userId, eventId, request));
    }
}