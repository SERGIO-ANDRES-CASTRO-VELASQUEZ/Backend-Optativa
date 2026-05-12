package com.sports.backend.dto;

import com.sports.backend.model.Product;
import com.sports.backend.service.util.StarRating;

import java.math.BigDecimal;

public record ProductSummaryDto(
        Long id,
        String name,
        Long categoryId,
        String categoryName,
        BigDecimal pricePerDay,
        Integer stock,
        String mainImageUrl,
        long favoriteCount,
        int stars
) {

    public static ProductSummaryDto from(Product product, long favoriteCount) {
        String mainImageUrl = product.getImages().stream()
                .filter(img -> img.getOrderIndex() == 0)
                .map(img -> img.getUrl())
                .findFirst()
                .orElse(product.getImages().isEmpty() ? null
                        : product.getImages().get(0).getUrl());

        return new ProductSummaryDto(
                product.getId(),
                product.getName(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getPricePerDay(),
                product.getStock(),
                mainImageUrl,
                favoriteCount,
                StarRating.calculate(favoriteCount)
        );
    }
}
