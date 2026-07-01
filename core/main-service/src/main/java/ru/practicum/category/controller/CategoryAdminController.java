package ru.practicum.category.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.CategoryRequestDto;
import ru.practicum.category.service.CategoryAdminService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin/categories")
public class CategoryAdminController {

    private final CategoryAdminService categoryAdminService;

    @PostMapping
    public ResponseEntity<CategoryDto> create(@Valid @RequestBody CategoryRequestDto categoryRequestDto) {
        log.info("запрос на создание категории: CategoryController");
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(categoryAdminService.create(categoryRequestDto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CategoryDto> update(@PathVariable Long id,
                                              @Valid @RequestBody CategoryRequestDto categoryRequestDto) {
        log.info("запрос на обновление категории с id = {}", id);
        return ResponseEntity.ok(categoryAdminService.update(id, categoryRequestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("запрос на удаление пользователя с id = {}", id);
        categoryAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
