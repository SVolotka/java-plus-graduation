package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.entityparam.PublicEventParam;
import ru.practicum.event.service.EventService;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicEventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventShortDto>> findEventsBy(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "0") Integer from,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            HttpServletRequest httpServletRequest
            ) {
        PublicEventParam param = new PublicEventParam(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);
        return ResponseEntity.ok(eventService.findEventsBy(param, httpServletRequest));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventFullDto> findEventById(@PathVariable @Min(1) Long id, HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(eventService.findEventById(id, httpServletRequest));
    }
}
