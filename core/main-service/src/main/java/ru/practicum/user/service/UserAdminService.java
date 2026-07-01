package ru.practicum.user.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserRequestDto;

import java.util.List;

public interface UserAdminService {

    UserDto create(UserRequestDto userRequestDto);

    List<UserDto> getAllUsers(List<Long> ids, Pageable pageable);

    UserDto getUserById(Long id);

    void deleteUser(Long id);
}
