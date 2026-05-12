package com.sports.backend.repository;

import com.sports.backend.model.Favorite;
import com.sports.backend.model.FavoriteId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {


    long countByIdProductId(Long productId);


    @Query("SELECT f.id.productId, COUNT(f) FROM Favorite f " +
           "WHERE f.id.productId IN :productIds GROUP BY f.id.productId")
    List<Object[]> countByProductIds(@Param("productIds") Collection<Long> productIds);


    boolean existsByIdUserIdAndIdProductId(Long userId, Long productId);


    @Query("SELECT f FROM Favorite f JOIN FETCH f.product p JOIN FETCH p.category " +
           "WHERE f.id.userId = :userId ORDER BY f.createdAt DESC")
    List<Favorite> findByUserIdWithProduct(@Param("userId") Long userId);


    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.id.userId = :userId AND f.id.productId = :productId")
    void deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    // =========================================================================
    // Fase 4 — Dashboard
    // =========================================================================


    @Query("""
        SELECT f.id.productId, COUNT(f)
        FROM Favorite f
        GROUP BY f.id.productId
        ORDER BY COUNT(f) DESC
        """)
    List<Object[]> findTopFavoriteProductIds(Pageable pageable);
}
