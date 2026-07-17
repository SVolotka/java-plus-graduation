package ru.yandex.practicum.common.feignClient.fallback;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.common.feignClient.UserClient;
import ru.yandex.practicum.common.userService.dto.UserDto;
import ru.yandex.practicum.common.userService.dto.UserShortDto;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserClientFallback implements UserClient {

    @Override
    public UserDto getById(Long id) {
        return UserDto.builder()
                .id(id)
                .name("unknown")
                .email("unknown@example.com")
                .build();
    }

    @Override
    public List<UserShortDto> getUsersByIds(List<Long> ids) {
        return ids.stream()
                .map(id -> new UserShortDto(id, "unknown"))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(Long id) {
        return false;
    }
}