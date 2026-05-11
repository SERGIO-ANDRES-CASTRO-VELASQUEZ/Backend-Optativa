package com.sports.backend.dto;

import com.sports.backend.model.ProductImage;

public record ProductImageDto(Long id, String url, int orderIndex) {

    public static ProductImageDto from(ProductImage pi) {
        return new ProductImageDto(pi.getId(), pi.getUrl(), pi.getOrderIndex());
    }
}
