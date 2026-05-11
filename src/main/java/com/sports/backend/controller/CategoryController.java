package com.sports.backend.controller;

import com.sports.backend.dto.CategoryDto;
import com.sports.backend.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categorías", description = "Listado de categorías de productos deportivos")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "Listar todas las categorías", description = "Público. Devuelve la lista de categorías ordenada por nombre.")
    public ResponseEntity<List<CategoryDto>> list() {
        return ResponseEntity.ok(categoryService.listAll());
    }
}
