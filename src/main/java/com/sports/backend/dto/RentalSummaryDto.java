package com.sports.backend.dto;

import com.sports.backend.model.Rental;
import com.sports.backend.model.RentalItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO liviano para el listado de alquileres (GET /api/rentals/mine).
 * No incluye la lista completa de ítems para no sobrecargar el listado.
 * El detalle completo se obtiene con GET /api/rentals/{id} → RentalDto.
 */
public record RentalSummaryDto(
        Long id,
        String code,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String paymentMethod,
        BigDecimal total,
        int itemCount,
        String firstProductName,
        String firstProductImageUrl,  // null si no hay imágenes
        OffsetDateTime createdAt
) {
    public static RentalSummaryDto from(Rental r) {
        RentalItem first = (r.getItems() == null || r.getItems().isEmpty())
                ? null
                : r.getItems().get(0);

        String firstImg = null;
        if (first != null
                && first.getProduct().getImages() != null
                && !first.getProduct().getImages().isEmpty()) {
            firstImg = first.getProduct().getImages().get(0).getUrl();
        }

        return new RentalSummaryDto(
                r.getId(),
                r.getCode(),
                r.getStartDate(),
                r.getEndDate(),
                r.getStatus().name(),
                r.getPaymentMethod().name(),
                r.getTotal(),
                r.getItems() == null ? 0 : r.getItems().size(),
                first != null ? first.getProduct().getName() : null,
                firstImg,
                r.getCreatedAt()
        );
    }
}
