package com.sports.backend.dto;

import com.sports.backend.model.Rental;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record RentalDto(
        Long id,
        String code,
        Long userId,
        String userFullName,
        String userEmail,
        LocalDate startDate,
        LocalDate endDate,
        String status,           // RentalStatus.name()
        String paymentMethod,    // PaymentMethod.name()
        BigDecimal subtotal,
        BigDecimal deposit,
        BigDecimal total,
        List<RentalItemDto> items,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static RentalDto from(Rental r) {
        List<RentalItemDto> itemDtos = r.getItems() == null
                ? List.of()
                : r.getItems().stream().map(RentalItemDto::from).toList();

        return new RentalDto(
                r.getId(),
                r.getCode(),
                r.getUser().getId(),
                r.getUser().getFullName(),
                r.getUser().getEmail(),
                r.getStartDate(),
                r.getEndDate(),
                r.getStatus().name(),
                r.getPaymentMethod().name(),
                r.getSubtotal(),
                r.getDeposit(),
                r.getTotal(),
                itemDtos,
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
