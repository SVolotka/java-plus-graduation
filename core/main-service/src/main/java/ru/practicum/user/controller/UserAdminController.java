package ru.practicum.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserRequestDto;
import ru.practicum.user.service.UserAdminService;

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
}
