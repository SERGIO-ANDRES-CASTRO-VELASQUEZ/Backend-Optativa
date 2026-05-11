package com.sports.backend.dto;

import com.sports.backend.model.Product;
import com.sports.backend.service.util.StarRating;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO completo para la vista de detalle de un producto.
 * Incluye imágenes, specs, conteo de favoritos, estrellas y si el usuario actual lo marcó.
 */
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
    /**
     * @param product       entidad Product (images y specs lazy-cargadas dentro de transacción)
     * @param favoriteCount conteo total de favoritos del producto
     * @param isFavorite    true si el usuario autenticado ya lo marcó como favorito
     */
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
