package ru.practicum.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.PatchEventDto;
import ru.practicum.event.entityparam.AdminEventParam;
import ru.practicum.event.service.EventService;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/events")
@Slf4j
public class AdminEventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventFullDto>> findEventsBy(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<String> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(required = false,  defaultValue = "0") Integer from,
            @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        AdminEventParam param = new AdminEventParam(users, states, categories, rangeStart, rangeEnd, from, size);
        return ResponseEntity.ok(eventService.findEventsBy(param));
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> patchEvent(
            @PathVariable("eventId") @Min(1) Long id,
            @Valid @RequestBody @NotNull PatchEventDto patchEventDto) {
        return ResponseEntity.ok(eventService.patchEvent(id, patchEventDto));
    }
}
