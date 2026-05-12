package com.sports.backend.repository;

import com.sports.backend.model.Rental;
import com.sports.backend.model.RentalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, Long>,
        JpaSpecificationExecutor<Rental> {


    Page<Rental> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Rental> findByIdAndUserId(Long id, Long userId);

    Optional<Rental> findByCode(String code);

    long countByCodeStartingWith(String prefix);

    @Query("""
            SELECT COALESCE(SUM(ri.quantity), 0)
            FROM RentalItem ri
            JOIN ri.rental r
            WHERE ri.product.id = :productId
              AND r.status IN ('PENDIENTE', 'ACTIVO')
              AND r.startDate <= :endDate
              AND r.endDate   >= :startDate
            """)
    int countOccupiedStock(
            @Param("productId") Long productId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate
    );

    @Query("""
            SELECT DISTINCT r FROM Rental r
            LEFT JOIN FETCH r.items ri
            LEFT JOIN FETCH ri.product p
            LEFT JOIN FETCH p.category
            WHERE r.id = :id
            """)
    Optional<Rental> findByIdWithItems(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT r FROM Rental r
            LEFT JOIN FETCH r.items ri
            LEFT JOIN FETCH ri.product p
            LEFT JOIN FETCH p.category
            WHERE r.user.id = :userId
            ORDER BY r.createdAt DESC
            """)
    java.util.List<Rental> findByUserIdWithItemsAndProducts(@Param("userId") Long userId);

    boolean existsByUserId(Long userId);

    @Query("SELECT COUNT(r) FROM Rental r WHERE r.status = 'ACTIVO' " +
           "AND r.startDate <= :today AND r.endDate >= :today")
    long countActiveToday(@Param("today") LocalDate today);

    @Query("SELECT COUNT(r) FROM Rental r WHERE r.status = 'ACTIVO' AND r.endDate < :today")
    long countOverdue(@Param("today") LocalDate today);

    @Query("SELECT COUNT(r) FROM Rental r WHERE r.status = 'FINALIZADO' " +
           "AND MONTH(r.createdAt) = :month AND YEAR(r.createdAt) = :year")
    long countFinishedThisMonth(@Param("month") int month, @Param("year") int year);

    @Query("SELECT COALESCE(SUM(r.total), 0) FROM Rental r WHERE r.status <> 'CANCELADO' " +
           "AND MONTH(r.createdAt) = :month AND YEAR(r.createdAt) = :year")
    BigDecimal revenueThisMonth(@Param("month") int month, @Param("year") int year);

    @Query("SELECT ri.product.id, COUNT(ri) FROM RentalItem ri " +
           "GROUP BY ri.product.id ORDER BY COUNT(ri) DESC")
    List<Object[]> findTopRentedProductIds(Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE MONTH(u.createdAt) = :month AND YEAR(u.createdAt) = :year")
    long countNewUsersThisMonth(@Param("month") int month, @Param("year") int year);
}
