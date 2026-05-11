package com.sports.backend.dto;

import com.sports.backend.model.Product;
import com.sports.backend.service.util.StarRating;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO de respuesta del panel de administración para un producto.
 *
 * <p>Diferencias con {@link ProductDetailDto} (vista cliente):
 * <ul>
 *   <li>Incluye {@code active} siempre (el cliente nunca ve productos inactivos).</li>
 *   <li>Incluye {@code createdAt} y {@code updatedAt} para auditoría.</li>
 *   <li>No incluye {@code isFavorite} (no es relevante para el admin).</li>
 *   <li>Incluye {@code favoriteCount} y {@code stars} para referencia.</li>
 * </ul>
 *
 * <p>Usado como respuesta de:
 * <ul>
 *   <li>{@code POST   /api/admin/products}</li>
 *   <li>{@code PUT    /api/admin/products/{id}}</li>
 *   <li>{@code GET    /api/admin/products/{id}}</li>
 *   <li>{@code POST   /api/admin/products/{id}/images}</li>
 *   <li>{@code DELETE /api/admin/products/{id}/images/{imageId}}</li>
 * </ul>
 */
public record AdminProductDto(
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
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * Convierte una entidad {@link Product} a este DTO.
     *
     * <p>Llamar solo dentro de una transacción activa (accede a imágenes y specs lazy).
     *
     * @param product       entidad cargada con imágenes y specs.
     * @param favoriteCount conteo actual de favoritos para el producto.
     */
    public static AdminProductDto from(Product product, long favoriteCount) {
        return new AdminProductDto(
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
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
