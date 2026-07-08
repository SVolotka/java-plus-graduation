package ru.practicum.event.entityparam;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminEventParam {
    List<Long> users;
    List<String> states;
    List<Long> categories;
    String rangeStart;
    String rangeEnd;
    Integer from;
    Integer size;
}
