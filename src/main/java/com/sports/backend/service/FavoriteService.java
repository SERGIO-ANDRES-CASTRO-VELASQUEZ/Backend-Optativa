package com.sports.backend.service;

import com.sports.backend.dto.ProductSummaryDto;
import com.sports.backend.exception.ApiException;
import com.sports.backend.model.Favorite;
import com.sports.backend.model.FavoriteId;
import com.sports.backend.model.Product;
import com.sports.backend.model.User;
import com.sports.backend.repository.FavoriteRepository;
import com.sports.backend.repository.ProductRepository;
import com.sports.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository) {
        this.favoriteRepository = favoriteRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    // -------------------------------------------------------------------------
    // Marcar favorito
    // -------------------------------------------------------------------------

    @Transactional
    public void addFavorite(Long userId, Long productId) {
        FavoriteId favoriteId = new FavoriteId(userId, productId);

        if (favoriteRepository.existsById(favoriteId)) {
            throw ApiException.conflict("El producto ya está en tus favoritos");
        }

        Product product = productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

        favoriteRepository.save(
                Favorite.builder()
                        .id(favoriteId)
                        .user(user)
                        .product(product)
                        .build()
        );
    }

    // -------------------------------------------------------------------------
    // Desmarcar favorito
    // -------------------------------------------------------------------------

    @Transactional
    public void removeFavorite(Long userId, Long productId) {
        if (!favoriteRepository.existsById(new FavoriteId(userId, productId))) {
            throw ApiException.notFound("El producto no está en tus favoritos");
        }
        favoriteRepository.deleteByUserIdAndProductId(userId, productId);
    }

    // -------------------------------------------------------------------------
    // Mis favoritos
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ProductSummaryDto> getMyFavorites(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserIdWithProduct(userId);

        List<Long> productIds = favorites.stream()
                .map(f -> f.getProduct().getId())
                .toList();

        // Conteo batch
        java.util.Map<Long, Long> favCounts = new java.util.HashMap<>();
        if (!productIds.isEmpty()) {
            for (Object[] row : favoriteRepository.countByProductIds(productIds)) {
                favCounts.put((Long) row[0], (Long) row[1]);
            }
        }

        return favorites.stream()
                .map(f -> ProductSummaryDto.from(
                        f.getProduct(),
                        favCounts.getOrDefault(f.getProduct().getId(), 0L)))
                .toList();
    }
}
