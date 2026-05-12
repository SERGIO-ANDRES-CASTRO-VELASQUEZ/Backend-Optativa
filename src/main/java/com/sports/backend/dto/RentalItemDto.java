package com.sports.backend.dto;

import com.sports.backend.model.RentalItem;

import java.math.BigDecimal;

public record RentalItemDto(
        Long id,
        Long productId,
        String productName,
        String productMainImageUrl,   // null si el producto no tiene imágenes
        int quantity,
        int days,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {

    public static RentalItemDto from(RentalItem ri) {
        String imgUrl = (ri.getProduct().getImages() == null || ri.getProduct().getImages().isEmpty())
                ? null
                : ri.getProduct().getImages().get(0).getUrl();

        return new RentalItemDto(
                ri.getId(),
                ri.getProduct().getId(),
                ri.getProduct().getName(),
                imgUrl,
                ri.getQuantity(),
                ri.getDays(),
                ri.getUnitPrice(),
                ri.getLineTotal()
        );
    }
}
