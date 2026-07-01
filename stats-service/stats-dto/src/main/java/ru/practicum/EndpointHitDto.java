package ru.practicum;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHitDto {
    @Min(1)
    Long id;

    @NotBlank(message = "Идентификатор сервиса не может быть пустым.")
    String app;

    @NotBlank(message = "URI для которого осуществлен запрос не может быть пустым.")
    String uri;

    @NotBlank(message = "IP-адрес пользователя, осуществившего запрос, не может быть пустым.")
    String ip;

    @Pattern(
            regexp = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])[T ]\\d{2}:\\d{2}:\\d{2}$",
            message = "Дата и время должны быть в формате: YYYY-MM-DD HH:MM:SS или YYYY-MM-DDTHH:MM:SS")
    @NotBlank(message = "Дата и время, когда был совершен запрос к эндпоинту, не может быть пустым.")
    String timestamp;
}
