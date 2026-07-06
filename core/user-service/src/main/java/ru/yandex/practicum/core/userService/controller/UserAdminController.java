package ru.yandex.practicum.core.userService.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.common.userService.dto.UserDto;
import ru.yandex.practicum.core.userService.dto.UserRequestDto;
import ru.yandex.practicum.core.userService.service.UserAdminService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody UserRequestDto userRequestDto) {
        log.info("запрос на создание пользователя: UserController");
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userAdminService.create(userRequestDto));
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers(@RequestParam(required = false) List<Long> ids,
                                                     @RequestParam(defaultValue = "0") int from,
                                                     @RequestParam(defaultValue = "10") int size) {
        log.info("запрос на вывод всех пользователей: UserController");
        return ResponseEntity.ok(userAdminService.getAllUsers(ids, PageRequest.of(from / size, size)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("запрос на удаление пользователя: UserController");
        userAdminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        log.info("запрос на получение пользователя по id = {}", id);
        return ResponseEntity.ok(userAdminService.getUserById(id));
    }
}
