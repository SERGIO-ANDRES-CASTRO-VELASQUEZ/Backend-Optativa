package com.sports.backend.service;

import com.sports.backend.dto.CategoryDto;
import com.sports.backend.model.Category;
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


    public List<CategoryDto> listAll() {
        return categoryRepository.findAll(Sort.by("name"))
                .stream()
                .map(CategoryDto::from)
                .toList();
    }


    @Transactional
    public CategoryDto create(CategoryDto dto) {
        Category category = Category.builder()
                .name(dto.name())
                .icon(dto.icon())
                .build();
        Category saved = categoryRepository.save(category);
        return CategoryDto.from(saved);
    }
}
