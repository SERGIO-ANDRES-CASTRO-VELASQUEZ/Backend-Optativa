package com.sports.backend.service;

import com.sports.backend.dto.CategoryDto;
import com.sports.backend.repository.CategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /** Devuelve todas las categorías ordenadas por nombre. */
    public List<CategoryDto> listAll() {
        return categoryRepository.findAll(Sort.by("name"))
                .stream()
                .map(CategoryDto::from)
                .toList();
    }
}
