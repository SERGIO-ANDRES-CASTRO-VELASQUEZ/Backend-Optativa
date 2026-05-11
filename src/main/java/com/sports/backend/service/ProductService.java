package com.sports.backend.service;

import com.sports.backend.dto.ProductDetailDto;
import com.sports.backend.dto.ProductSummaryDto;
import com.sports.backend.exception.ApiException;
import com.sports.backend.model.Product;
import com.sports.backend.repository.FavoriteRepository;
import com.sports.backend.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final FavoriteRepository favoriteRepository;

    public ProductService(ProductRepository productRepository,
                          FavoriteRepository favoriteRepository) {
        this.productRepository = productRepository;
        this.favoriteRepository = favoriteRepository;
    }

    // -------------------------------------------------------------------------
    // Listado con filtros
    // -------------------------------------------------------------------------

    /**
     * Lista productos activos con filtros opcionales combinables.
     *
     * @param categoryId filtra por categoría (null = todas)
     * @param q          búsqueda libre en el nombre (null o blank = sin filtro)
     * @param minPrice   precio mínimo por día (null = sin límite inferior)
     * @param maxPrice   precio máximo por día (null = sin límite superior)
     * @param pageable   paginación y ordenación (default page=0, size=20, sort=name,asc)
     */
    public Page<ProductSummaryDto> list(Long categoryId, String q,
                                        BigDecimal minPrice, BigDecimal maxPrice,
                                        Pageable pageable) {
        Specification<Product> spec = buildSpec(categoryId, q, minPrice, maxPrice);
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        // Conteo masivo de favoritos en una sola query (evita N+1)
        List<Long> ids = productPage.getContent().stream().map(Product::getId).toList();
        Map<Long, Long> favCounts = batchFavoriteCounts(ids);

        return productPage.map(p ->
                ProductSummaryDto.from(p, favCounts.getOrDefault(p.getId(), 0L)));
    }

    // -------------------------------------------------------------------------
    // Detalle
    // -------------------------------------------------------------------------

    /**
     * Devuelve el detalle completo de un producto activo.
     *
     * @param id            id del producto
     * @param currentUserId id del usuario autenticado (null si es anónimo)
     */
    public ProductDetailDto getById(Long id, Long currentUserId) {
        Product product = productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));

        long favCount = favoriteRepository.countByIdProductId(id);
        boolean isFavorite = currentUserId != null
                && favoriteRepository.existsByIdUserIdAndIdProductId(currentUserId, id);

        return ProductDetailDto.from(product, favCount, isFavorite);
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private Specification<Product> buildSpec(Long categoryId, String q,
                                              BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Solo productos activos (siempre)
            predicates.add(cb.isTrue(root.get("active")));

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (q != null && !q.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + q.trim().toLowerCase() + "%"
                ));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("pricePerDay"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("pricePerDay"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Convierte el resultado de la query batch en un Map<productId, count>. */
    private Map<Long, Long> batchFavoriteCounts(List<Long> productIds) {
        Map<Long, Long> map = new HashMap<>();
        if (productIds.isEmpty()) return map;
        for (Object[] row : favoriteRepository.countByProductIds(productIds)) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }
}
