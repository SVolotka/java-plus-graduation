package ru.yandex.practicum.common.feignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.common.feignClient.fallback.UserClientFallback;
import ru.yandex.practicum.common.userService.dto.UserDto;
import ru.yandex.practicum.common.userService.dto.UserShortDto;

import java.util.List;

@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient {

    @GetMapping("/admin/users/{id}")
    UserDto getById(@PathVariable Long id);

    @GetMapping("/internal/users/batch")
    List<UserShortDto> getUsersByIds(@RequestParam("ids") List<Long> ids);

    @GetMapping("/internal/users/{id}/exists")
    boolean existsById(@PathVariable Long id);
}