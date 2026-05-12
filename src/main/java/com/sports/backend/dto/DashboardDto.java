package com.sports.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardDto(
        long activeRentalsToday,
        long overdueRentals,
        long finishedThisMonth,
        BigDecimal revenueThisMonth,
        java.util.List<ProductSummaryDto> topRentedProducts,
        java.util.List<ProductSummaryDto> topFavoriteProducts,
        long newUsersThisMonth,
        List<CategoryOccupationDto> categoryOccupation
) {

    public record CategoryOccupationDto(
            Long categoryId,
            String categoryName,
            long activeUnits
    ) {}
}
