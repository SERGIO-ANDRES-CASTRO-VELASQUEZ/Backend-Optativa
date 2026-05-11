package com.sports.backend.dto;

import com.sports.backend.model.ProductSpec;

public record ProductSpecDto(Long id, String key, String value) {

    public static ProductSpecDto from(ProductSpec ps) {
        return new ProductSpecDto(ps.getId(), ps.getKey(), ps.getValue());
    }
}
