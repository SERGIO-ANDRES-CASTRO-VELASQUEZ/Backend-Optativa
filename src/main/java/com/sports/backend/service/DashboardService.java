package com.sports.backend.service;

import com.sports.backend.dto.DashboardDto;
import com.sports.backend.dto.DashboardDto.CategoryOccupationDto;
import com.sports.backend.dto.ProductSummaryDto;
import com.sports.backend.model.Product;
import com.sports.backend.repository.FavoriteRepository;
import com.sports.backend.repository.ProductRepository;
import com.sports.backend.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio que calcula los KPIs del dashboard de administración.
 *
 * <p>Todos los cálculos se realizan en una única transacción de solo lectura.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RentalRepository   rentalRepository;
    private final ProductRepository  productRepository;
    private final FavoriteRepository favoriteRepository;

    /**
     * Calcula todos los KPIs del dashboard para la fecha actual.
     *
     * @return {@link DashboardDto} con los indicadores del panel
     */
    @Transactional(readOnly = true)
    public DashboardDto getDashboard() {

        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year  = today.getYear();

        // ── KPIs simples ─────────────────────────────────────────────────────
        long activeRentalsToday = rentalRepository.countActiveToday(today);
        long overdueRentals     = rentalRepository.countOverdue(today);
        long finishedThisMonth  = rentalRepository.countFinishedThisMonth(month, year);
        long newUsersThisMonth  = rentalRepository.countNewUsersThisMonth(month, year);

        BigDecimal revenueThisMonth = rentalRepository.revenueThisMonth(month, year);
        if (revenueThisMonth == null) revenueThisMonth = BigDecimal.ZERO;

        // ── Top 5 productos más alquilados ───────────────────────────────────
        List<ProductSummaryDto> topRented = buildTopProducts(
                rentalRepository.findTopRentedProductIds(PageRequest.of(0, 5)));

        // ── Top 5 productos más marcados como favorito ───────────────────────
        List<ProductSummaryDto> topFavorites = buildTopProducts(
                favoriteRepository.findTopFavoriteProductIds(PageRequest.of(0, 5)));

        // ── Ocupación por categoría: unidades en alquileres ACTIVO hoy ───────
        List<CategoryOccupationDto> categoryOccupation = buildCategoryOccupation(today);

        return new DashboardDto(
                activeRentalsToday,
                overdueRentals,
                finishedThisMonth,
                revenueThisMonth,
                topRented,
                topFavorites,
                newUsersThisMonth,
                categoryOccupation
        );
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    /**
     * Recibe lista de Object[] { productId, count } y construye los DTOs
     * cargando cada producto y calculando su favoriteCount real.
     */
    private List<ProductSummaryDto> buildTopProducts(List<Object[]> rows) {
        if (rows.isEmpty()) return List.of();

        List<Long> ids = rows.stream()
                .map(r -> ((Number) r[0]).longValue())
                .toList();

        // Cargar todos los productos de una vez
        List<Product> products = productRepository.findAllById(ids);
        Map<Long, Product> byId = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // Obtener conteo de favoritos en bulk
        List<Object[]> favCounts = favoriteRepository.countByProductIds(ids);
        Map<Long, Long> favMap = favCounts.stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).longValue()));

        List<ProductSummaryDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long pid = ((Number) row[0]).longValue();
            Product p = byId.get(pid);
            if (p != null) {
                long favCount = favMap.getOrDefault(pid, 0L);
                result.add(ProductSummaryDto.from(p, favCount));
            }
        }
        return result;
    }

    /**
     * Calcula cuántas unidades de cada categoría están en alquileres ACTIVO hoy.
     *
     * <p>Usa una query JPQL en lugar de añadir un nuevo método al repositorio,
     * delegando la query directamente al EntityManager a través de JPA (JPQL inline).
     * Se implementa con una query derivada del tipo que ya hace RentalRepository.
     */
    private List<CategoryOccupationDto> buildCategoryOccupation(LocalDate today) {
        // Query: unidades por categoría en alquileres ACTIVO que cubren hoy
        // Reutiliza la lógica: status=ACTIVO AND startDate <= today AND endDate >= today
        // Agrupado por category
        List<Object[]> rows = productRepository.findAll().stream()
                // Filtramos en memoria para no añadir más queries al repo ahora.
                // En producción con muchos productos conviene moverlo a una @Query en el repositorio.
                .collect(Collectors.groupingBy(p -> p.getCategory()))
                .entrySet().stream()
                .map(e -> new Object[]{
                        e.getKey(),
                        e.getValue().stream()
                                .mapToLong(p -> rentalRepository.countOccupiedStock(
                                        p.getId(), today, today))
                                .sum()
                })
                .filter(r -> ((Long) r[1]) > 0)
                .sorted((a, b) -> Long.compare((Long) b[1], (Long) a[1]))
                .toList();

        return rows.stream()
                .map(r -> {
                    var cat = (com.sports.backend.model.Category) r[0];
                    return new CategoryOccupationDto(cat.getId(), cat.getName(), (Long) r[1]);
                })
                .toList();
    }
}
