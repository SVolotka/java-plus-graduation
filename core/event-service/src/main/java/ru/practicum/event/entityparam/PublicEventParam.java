package ru.practicum.event.entityparam;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PublicEventParam {
    String text;
    List<Long> categories;
    Boolean paid;
    String rangeStart;
    String rangeEnd;
    Boolean onlyAvailable;
    String sort;
    Integer from;
    Integer size;
}
