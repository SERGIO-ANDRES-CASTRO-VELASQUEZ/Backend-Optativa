package com.sports.backend.dto;

import com.sports.backend.model.Rental;
import com.sports.backend.model.RentalItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO liviano de alquiler para el listado del panel admin.
 *
 * <p>A diferencia de {@link RentalSummaryDto} (vista cliente), incluye:
 * <ul>
 *   <li>Datos del cliente ({@code userId}, {@code userFullName}, {@code userEmail}).</li>
 *   <li>Admin que creó el alquiler en mostrador ({@code createdByName}); null si fue el propio cliente.</li>
 * </ul>
 */
public record AdminRentalSummaryDto(
        Long id,
        String code,
        Long userId,
        String userFullName,
        String userEmail,
        String createdByName,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String paymentMethod,
        BigDecimal total,
        int itemCount,
        String firstProductName,
        String firstProductImageUrl,
        OffsetDateTime createdAt
) {
    /**
     * Llamar solo dentro de una transacción activa (accede a colecciones lazy).
     */
    public static AdminRentalSummaryDto from(Rental r) {
        RentalItem first = (r.getItems() == null || r.getItems().isEmpty())
                ? null
                : r.getItems().get(0);

        String createdByName = r.getCreatedBy() != null
                ? r.getCreatedBy().getFullName()
                : null;

        String firstImg = null;
        if (first != null
                && first.getProduct().getImages() != null
                && !first.getProduct().getImages().isEmpty()) {
            firstImg = first.getProduct().getImages().get(0).getUrl();
        }

        return new AdminRentalSummaryDto(
                r.getId(),
                r.getCode(),
                r.getUser().getId(),
                r.getUser().getFullName(),
                r.getUser().getEmail(),
                createdByName,
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
