package com.sports.backend.dto;

import com.sports.backend.model.Product;
import com.sports.backend.service.util.StarRating;

import java.math.BigDecimal;

/**
 * DTO compacto para el listado del catálogo.
 * Incluye solo los campos necesarios para pintar una tarjeta de producto.
 */
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
    /**
     * @param product       entidad Product (con images lazy-cargadas dentro de transacción)
     * @param favoriteCount conteo de favoritos pre-calculado
     */
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
