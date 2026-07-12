package ru.practicum.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Length;
import ru.practicum.event.annotations.DateTimeStart;
import ru.practicum.event.entity.Location;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewEventDto {

    @NotBlank(message = "Краткое описание события должно быть указано.")
    @Length(min = 20, max = 2000, message = "Минимальная длина аннотации 20 символов, максимальная 2000 символов.")
    String annotation;

    @NotNull(message = "id категории, к которой относится событие, должно быть указано.")
    Long category;

    @NotBlank(message = "Полное описание события должно быть указано.")
    @Length(min = 20, max = 7000, message = "Минимальная длина описания 20 символов, максимальная 7000 символов.")
    String description;

    @NotNull(message = "Дата и время на которые намечено событие должны быть указаны")
    @DateTimeStart(value = 2, message = "Дата и время на которые намечено событие не может быть раньше, чем через два часа от текущего момента")
    String eventDate;

    @NotNull(message = "Широта и долгота места проведения события должны быть указаны.")
    Location location;

    Boolean paid;

    @PositiveOrZero(message = "Количество участников должно быть неотрицательным числом.")
    Integer participantLimit;

    Boolean requestModeration;

    @NotBlank(message = "Заголовок события должен быть указан.")
    @Length(min = 3, max = 120, message = "Минимальная длина заголовка 3 символа, максимальная 120 символов.")
    String title;
}
