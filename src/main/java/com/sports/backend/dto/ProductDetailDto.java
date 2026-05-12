package com.sports.backend.dto;

import com.sports.backend.model.Product;
import com.sports.backend.service.util.StarRating;

import java.math.BigDecimal;
import java.util.List;


public record ProductDetailDto(
        Long id,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        BigDecimal pricePerDay,
        Integer stock,
        boolean active,
        List<ProductImageDto> images,
        List<ProductSpecDto> specs,
        long favoriteCount,
        int stars,
        boolean isFavorite
) {

    public static ProductDetailDto from(Product product, long favoriteCount, boolean isFavorite) {
        return new ProductDetailDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getPricePerDay(),
                product.getStock(),
                product.isActive(),
                product.getImages().stream().map(ProductImageDto::from).toList(),
                product.getSpecs().stream().map(ProductSpecDto::from).toList(),
                favoriteCount,
                StarRating.calculate(favoriteCount),
                isFavorite
        );
    }
}
