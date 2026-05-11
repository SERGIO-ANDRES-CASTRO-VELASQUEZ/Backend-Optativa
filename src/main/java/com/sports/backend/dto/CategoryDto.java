package com.sports.backend.dto;

import com.sports.backend.model.Category;

public record CategoryDto(Long id, String name, String icon) {

    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getIcon());
    }
}
