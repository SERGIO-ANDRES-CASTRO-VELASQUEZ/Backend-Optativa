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

    // ── Consultas del cliente ────────────────────────────────────────────────

    /**
     * Lista paginada de alquileres de un usuario, más reciente primero.
     * Los items NO se cargan aquí (lazy). Para el listado se usa RentalSummaryDto.
     */
    Page<Rental> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Carga un alquiler verificando que pertenece al usuario.
     * Devuelve empty si el id no existe O si pertenece a otro usuario.
     */
    Optional<Rental> findByIdAndUserId(Long id, Long userId);

    /**
     * Búsqueda por código único (para ticket y panel admin).
     */
    Optional<Rental> findByCode(String code);

    /**
     * Cuenta los códigos que empiezan por el prefijo dado.
     * Se usa para generar el número secuencial: SR-YYYY-NNNNN.
     * Ejemplo: countByCodeStartingWith("SR-2026-") → 5 → próximo = SR-2026-00006
     */
    long countByCodeStartingWith(String prefix);

    // ── Validación de stock ──────────────────────────────────────────────────

    /**
     * Cuenta las unidades del producto que están comprometidas en alquileres
     * PENDIENTE o ACTIVO que se solapan con el rango [startDate, endDate].
     *
     * Dos rangos se solapan si: start1 <= end2 AND end1 >= start2
     *
     * Se usa para calcular: disponible = product.stock - countOccupiedStock(...)
     */
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

    // ── Detalle completo con items ───────────────────────────────────────────

    /**
     * Carga un alquiler con todos sus items y el producto de cada item.
     * Se usa en findById (detalle) y en extend (necesita recalcular totales).
     *
     * NOTA: no hace JOIN FETCH de product.images aquí para evitar
     * MultipleBagFetchException. Las imágenes se resuelven lazy dentro
     * de la transacción del servicio.
     */
    @Query("""
            SELECT DISTINCT r FROM Rental r
            LEFT JOIN FETCH r.items ri
            LEFT JOIN FETCH ri.product p
            LEFT JOIN FETCH p.category
            WHERE r.id = :id
            """)
    Optional<Rental> findByIdWithItems(@Param("id") Long id);

    /**
     * Carga los alquileres de un usuario con items y productos en una sola query.
     * Se usa en findMine para evitar N+1 al construir RentalSummaryDto.
     */
    /**
     * Carga los alquileres de un usuario con items y productos.
     * Las imágenes se cargan lazy dentro de la transacción del servicio.
     * Se evita JOIN FETCH p.images para no causar MultipleBagFetchException
     * (dos @OneToMany a la vez: r.items y p.images).
     */
    @Query("""
            SELECT DISTINCT r FROM Rental r
            LEFT JOIN FETCH r.items ri
            LEFT JOIN FETCH ri.product p
            LEFT JOIN FETCH p.category
            WHERE r.user.id = :userId
            ORDER BY r.createdAt DESC
            """)
    java.util.List<Rental> findByUserIdWithItemsAndProducts(@Param("userId") Long userId);

    // ── Queries para Fase 4 (panel admin) — se implementan en Fase 4 ─────────
    //
    // Page<Rental> searchAdmin(RentalStatus status, String q, Pageable pageable)

    /** Verifica si un usuario tiene alquileres (para bloquear hard delete). */
    boolean existsByUserId(Long userId);

    // ── Dashboard KPIs ────────────────────────────────────────────────────────

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
