package ru.yandex.practicum.core.userService.service;

import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.common.userService.dto.UserDto;
import ru.yandex.practicum.core.userService.dto.UserRequestDto;
import ru.yandex.practicum.core.userService.entity.User;

import java.util.List;

public interface UserAdminService {

    UserDto create(UserRequestDto userRequestDto);

    List<UserDto> getAllUsers(List<Long> ids, Pageable pageable);

    UserDto getUserById(Long id);

    void deleteUser(Long id);

    List<User> getUsersByIds(List<Long> ids);

    boolean existsById(Long id);
}
