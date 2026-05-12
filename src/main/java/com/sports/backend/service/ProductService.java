package com.sports.backend.service;

import com.sports.backend.dto.AdminProductDto;
import com.sports.backend.dto.CreateProductRequest;
import com.sports.backend.dto.ProductDetailDto;
import com.sports.backend.dto.ProductSummaryDto;
import com.sports.backend.dto.UpdateProductRequest;
import com.sports.backend.exception.ApiException;
import com.sports.backend.model.Category;
import com.sports.backend.model.Product;
import com.sports.backend.model.ProductImage;
import com.sports.backend.model.ProductSpec;
import com.sports.backend.repository.CategoryRepository;
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

    private final ProductRepository  productRepository;
    private final FavoriteRepository favoriteRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository,
                          FavoriteRepository favoriteRepository,
                          CategoryRepository categoryRepository) {
        this.productRepository  = productRepository;
        this.favoriteRepository = favoriteRepository;
        this.categoryRepository = categoryRepository;
    }

    // =========================================================================
    // Vista pública (cliente)
    // =========================================================================

   public Page<ProductSummaryDto> list(Long categoryId, String q,
                                        BigDecimal minPrice, BigDecimal maxPrice,
                                        Pageable pageable) {
        Specification<Product> spec = buildSpec(true, categoryId, q, minPrice, maxPrice);
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        List<Long> ids = productPage.getContent().stream().map(Product::getId).toList();
        Map<Long, Long> favCounts = batchFavoriteCounts(ids);

        return productPage.map(p ->
                ProductSummaryDto.from(p, favCounts.getOrDefault(p.getId(), 0L)));
    }

    public ProductDetailDto getById(Long id, Long currentUserId) {
        Product product = productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));

        long favCount = favoriteRepository.countByIdProductId(id);
        boolean isFavorite = currentUserId != null
                && favoriteRepository.existsByIdUserIdAndIdProductId(currentUserId, id);

        return ProductDetailDto.from(product, favCount, isFavorite);
    }

    // =========================================================================
    // Vista admin — solo lectura
    // =========================================================================

    public Page<ProductSummaryDto> listAdmin(Long categoryId, String q, Pageable pageable) {
        Specification<Product> spec = buildSpec(false, categoryId, q, null, null);
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        List<Long> ids = productPage.getContent().stream().map(Product::getId).toList();
        Map<Long, Long> favCounts = batchFavoriteCounts(ids);

        return productPage.map(p ->
                ProductSummaryDto.from(p, favCounts.getOrDefault(p.getId(), 0L)));
    }

    public AdminProductDto getByIdAdmin(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));
        long favCount = favoriteRepository.countByIdProductId(id);
        return AdminProductDto.from(product, favCount);
    }

    // =========================================================================
    // Vista admin — escritura
    // =========================================================================

    @Transactional
    public AdminProductDto create(CreateProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> ApiException.notFound(
                        "Categoría no encontrada: id=" + request.categoryId()));

        Product product = Product.builder()
                .name(request.name().trim())
                .description(request.description().trim())
                .category(category)
                .pricePerDay(request.pricePerDay())
                .stock(request.stock())
                .active(request.active() == null || request.active())
                .build();

        // Añadir imágenes por URL (opcionales)
        if (request.imageUrls() != null) {
            for (int i = 0; i < request.imageUrls().size(); i++) {
                String url = request.imageUrls().get(i);
                if (url != null && !url.isBlank()) {
                    product.getImages().add(ProductImage.builder()
                            .product(product)
                            .url(url.trim())
                            .orderIndex(i)
                            .build());
                }
            }
        }

        // Añadir specs (opcionales)
        if (request.specs() != null) {
            for (CreateProductRequest.SpecRequest sr : request.specs()) {
                product.getSpecs().add(ProductSpec.builder()
                        .product(product)
                        .key(sr.key().trim())
                        .value(sr.value().trim())
                        .build());
            }
        }

        productRepository.save(product);
        return AdminProductDto.from(product, 0L); // recién creado: 0 favoritos
    }

    @Transactional
    public AdminProductDto update(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));

        if (request.name() != null) {
            product.setName(request.name().trim());
        }
        if (request.description() != null) {
            product.setDescription(request.description().trim());
        }
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> ApiException.notFound(
                            "Categoría no encontrada: id=" + request.categoryId()));
            product.setCategory(category);
        }
        if (request.pricePerDay() != null) {
            product.setPricePerDay(request.pricePerDay());
        }
        if (request.stock() != null) {
            product.setStock(request.stock());
        }
        if (request.active() != null) {
            product.setActive(request.active());
        }

        // Reemplazo completo de specs si se envía la lista (incluso vacía)
        if (request.specs() != null) {
            product.getSpecs().clear();
            for (CreateProductRequest.SpecRequest sr : request.specs()) {
                product.getSpecs().add(ProductSpec.builder()
                        .product(product)
                        .key(sr.key().trim())
                        .value(sr.value().trim())
                        .build());
            }
        }

        productRepository.save(product);
        long favCount = favoriteRepository.countByIdProductId(id);
        return AdminProductDto.from(product, favCount);
    }

    @Transactional
    public void deactivate(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional
    public AdminProductDto addImage(Long productId, String imageUrl) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));

        int nextOrder = product.getImages().stream()
                .mapToInt(ProductImage::getOrderIndex)
                .max()
                .orElse(-1) + 1;

        product.getImages().add(ProductImage.builder()
                .product(product)
                .url(imageUrl)
                .orderIndex(nextOrder)
                .build());

        productRepository.save(product); // cascade ALL persiste la imagen
        long favCount = favoriteRepository.countByIdProductId(productId);
        return AdminProductDto.from(product, favCount);
    }

    @Transactional
    public String removeImage(Long productId, Long imageId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));

        ProductImage image = product.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound(
                        "Imagen no encontrada en el producto: imageId=" + imageId));

        String removedUrl = image.getUrl();
        product.getImages().remove(image); // orphanRemoval = true borra de BD
        productRepository.save(product);

        return removedUrl;
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    private Specification<Product> buildSpec(boolean onlyActive,
                                              Long categoryId, String q,
                                              BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (onlyActive) {
                predicates.add(cb.isTrue(root.get("active")));
            }
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

    private Map<Long, Long> batchFavoriteCounts(List<Long> productIds) {
        Map<Long, Long> map = new HashMap<>();
        if (productIds.isEmpty()) return map;
        for (Object[] row : favoriteRepository.countByProductIds(productIds)) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }
}
