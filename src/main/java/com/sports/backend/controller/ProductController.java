package com.sports.backend.controller;

import com.sports.backend.dto.ProductDetailDto;
import com.sports.backend.dto.ProductSummaryDto;
import com.sports.backend.repository.UserRepository;
import com.sports.backend.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Productos", description = "Catálogo de productos deportivos")
public class ProductController {

    private final ProductService productService;
    private final UserRepository userRepository;

    public ProductController(ProductService productService, UserRepository userRepository) {
        this.productService = productService;
        this.userRepository = userRepository;
    }

    // -------------------------------------------------------------------------
    // GET /api/products
    // -------------------------------------------------------------------------

    @GetMapping
    @Operation(
            summary = "Listar productos con filtros opcionales",
            description = "Público. Soporta filtro por categoría, búsqueda en nombre, rango de precio y paginación.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Page<ProductSummaryDto>> list(
            @Parameter(description = "ID de la categoría para filtrar") @RequestParam(required = false) Long category,
            @Parameter(description = "Búsqueda libre en el nombre del producto") @RequestParam(required = false) String q,
            @Parameter(description = "Precio mínimo por día (COP)") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Precio máximo por día (COP)") @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(productService.list(category, q, minPrice, maxPrice, pageable));
    }

    // -------------------------------------------------------------------------
    // GET /api/products/{id}
    // -------------------------------------------------------------------------

    @GetMapping("/{id}")
    @Operation(
            summary = "Detalle de un producto",
            description = "Público. Si el usuario está autenticado, devuelve isFavorite=true si ya lo marcó.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ProductDetailDto> detail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long currentUserId = resolveUserId(userDetails);
        return ResponseEntity.ok(productService.getById(id, currentUserId));
    }

    // -------------------------------------------------------------------------
    // Helper: resuelve userId a partir del email del UserDetails
    // -------------------------------------------------------------------------

    private Long resolveUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        return userRepository.findByEmail(userDetails.getUsername())
                .map(u -> u.getId())
                .orElse(null);
    }
}
