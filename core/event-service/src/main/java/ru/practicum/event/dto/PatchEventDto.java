package ru.practicum.event.dto;

import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Length;
import ru.practicum.event.annotations.FutureOrPresent;
import ru.practicum.event.entity.Location;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PatchEventDto {

    @Length(min = 20, max = 2000, message = "Минимальная длина аннотации 20 символов, максимальная 2000 символов.")
    String annotation;

    @Positive
    Long category;

    @Length(min = 20, max = 7000, message = "Минимальная длина описания 20 символов, максимальная 7000 символов.")
    String description;

    @FutureOrPresent
    String eventDate;

    Location location;
    Boolean paid;

    @Positive
    Integer participantLimit;

    Boolean requestModeration;
    String stateAction;

    @Length(min = 3, max = 120, message = "Минимальная длина заголовка 3 символа, максимальная 120 символов.")
    String title;
}
