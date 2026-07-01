package ru.practicum.category.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.service.CategoryPublicService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/categories")
public class CategoryPublicController {

    private final CategoryPublicService categoryPublicService;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getListCategories(@RequestParam(defaultValue = "0") int from,
                                                               @RequestParam(defaultValue = "10") int size) {
        log.info("запрос на получения списка категорий");
        return ResponseEntity.ok(categoryPublicService.getListCategories(PageRequest.of(from / size, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable Long id) {
        log.info("запрос на получение категории с id = {}", id);
        return ResponseEntity.ok(categoryPublicService.getCategoryById(id));
    }
}
