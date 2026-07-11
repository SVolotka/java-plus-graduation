package ru.yandex.practicum.common.feignClient.fallback;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.common.feignClient.UserClient;
import ru.yandex.practicum.common.userService.dto.UserDto;
import ru.yandex.practicum.common.userService.dto.UserShortDto;

import java.util.Collections;
import java.util.List;

@Component
public class UserClientFallback implements UserClient {
    @Override
    public UserDto getById(Long id) {
        return null;
    }

    @Override
    public List<UserShortDto> getUsersByIds(List<Long> ids) {
        return Collections.emptyList();
    }

    @Override
    public boolean existsById(Long id) {
        return false;
    }
}
