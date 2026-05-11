package com.sports.backend.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de respuesta del endpoint {@code GET /api/admin/dashboard}.
 *
 * <h2>KPIs incluidos:</h2>
 * <ul>
 *   <li>{@code activeRentalsToday}   — alquileres ACTIVO cuyo rango cubre hoy.</li>
 *   <li>{@code overdueRentals}       — alquileres ACTIVO con endDate pasado (vencidos sin marcar).</li>
 *   <li>{@code finishedThisMonth}    — alquileres FINALIZADO creados este mes.</li>
 *   <li>{@code revenueThisMonth}     — suma de totales de alquileres no CANCELADO este mes.</li>
 *   <li>{@code topRentedProducts}    — top 5 productos con más líneas de alquiler.</li>
 *   <li>{@code topFavoriteProducts}  — top 5 productos con más favoritos.</li>
 *   <li>{@code newUsersThisMonth}    — usuarios creados este mes.</li>
 *   <li>{@code categoryOccupation}  — resumen de unidades alquiladas por categoría.</li>
 * </ul>
 */
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

    /**
     * Ocupación de una categoría: nombre y unidades actualmente alquiladas.
     */
    public record CategoryOccupationDto(
            Long categoryId,
            String categoryName,
            long activeUnits
    ) {}
}
