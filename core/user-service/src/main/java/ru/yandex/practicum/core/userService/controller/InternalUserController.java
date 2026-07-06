package ru.yandex.practicum.core.userService.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.common.userService.dto.UserShortDto;
import ru.yandex.practicum.core.userService.mapper.UserMapper;
import ru.yandex.practicum.core.userService.service.UserAdminService;

import java.util.List;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {
    private final UserAdminService userAdminService;
    private final UserMapper userMapper;

    @GetMapping("/batch")
    public List<UserShortDto> getUsersByIds(@RequestParam("ids") List<Long> ids) {
        return userAdminService.getUsersByIds(ids).stream()
                .map(userMapper::toUserShortDto)
                .toList();
    }
}
