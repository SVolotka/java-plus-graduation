package ru.yandex.practicum.common.eventService.compilation.dto;

import lombok.Data;
import ru.yandex.practicum.common.eventService.event.dto.EventShortDto;

import java.util.List;

@Data
public class CompilationDto {
    private List<EventShortDto> events;
    private Long id;
    private Boolean pinned;
    private String title;
}