package ru.yandex.practicum.common.requestService.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventRequestStatusUpdateRequest {

    @NotNull(message = "Список идентификаторов заявок не может быть null")
    @NotEmpty(message = "Список идентификаторов заявок не может быть пустым")
    List<Long> requestIds;

    @NotNull(message = "Статус не может быть null")
    String status;
}