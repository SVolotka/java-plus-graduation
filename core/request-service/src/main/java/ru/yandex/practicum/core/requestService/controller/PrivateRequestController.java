package ru.yandex.practicum.core.requestService.controller;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.common.requestService.dto.ParticipationRequestDto;
import ru.yandex.practicum.core.requestService.service.RequestService;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/requests")
public class PrivateRequestController {

    private final RequestService requestService;

    @GetMapping
    public ResponseEntity<List<ParticipationRequestDto>> getUserRequests(
            @PathVariable @Min(1) Long userId) {
        return ResponseEntity.ok(requestService.getUserRequests(userId));
    }

    @PostMapping
    public ResponseEntity<ParticipationRequestDto> addParticipationRequest(
            @PathVariable @Min(1) Long userId,
            @RequestParam @Min(1) Long eventId) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(requestService.addParticipationRequest(userId, eventId));
    }

    @PatchMapping("/{requestId}/cancel")
    public ResponseEntity<ParticipationRequestDto> cancelRequest(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long requestId) {
        return ResponseEntity.ok(requestService.cancelRequest(userId, requestId));
    }
}