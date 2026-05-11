package com.sports.backend.repository;

import com.sports.backend.model.Favorite;
import com.sports.backend.model.FavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

    /** Conteo de favoritos para un único producto. */
    long countByIdProductId(Long productId);

    /** Conteo masivo de favoritos para una lista de productos (evita N+1 en listados). */
    @Query("SELECT f.id.productId, COUNT(f) FROM Favorite f " +
           "WHERE f.id.productId IN :productIds GROUP BY f.id.productId")
    List<Object[]> countByProductIds(@Param("productIds") Collection<Long> productIds);

    /** Verifica si un usuario ya marcó un producto como favorito. */
    boolean existsByIdUserIdAndIdProductId(Long userId, Long productId);

    /** Lista todos los favoritos de un usuario (para "mis favoritos"). */
    @Query("SELECT f FROM Favorite f JOIN FETCH f.product p JOIN FETCH p.category " +
           "WHERE f.id.userId = :userId ORDER BY f.createdAt DESC")
    List<Favorite> findByUserIdWithProduct(@Param("userId") Long userId);

    /** Elimina un favorito por usuario y producto. */
    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.id.userId = :userId AND f.id.productId = :productId")
    void deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
}
