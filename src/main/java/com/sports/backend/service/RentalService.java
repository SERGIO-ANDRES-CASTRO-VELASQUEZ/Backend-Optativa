package com.sports.backend.service;

import com.sports.backend.dto.AdminRentalSummaryDto;
import com.sports.backend.dto.CreateRentalRequest;
import com.sports.backend.dto.ExtendRentalRequest;
import com.sports.backend.dto.RentalDto;
import com.sports.backend.dto.RentalItemRequest;
import com.sports.backend.dto.RentalSummaryDto;
import com.sports.backend.exception.ApiException;
import com.sports.backend.model.PaymentMethod;
import com.sports.backend.model.Product;
import com.sports.backend.model.Rental;
import com.sports.backend.model.RentalItem;
import com.sports.backend.model.RentalStatus;
import com.sports.backend.model.User;
import com.sports.backend.repository.ProductRepository;
import com.sports.backend.repository.RentalRepository;
import com.sports.backend.repository.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalService {

    private final RentalRepository    rentalRepository;
    private final ProductRepository   productRepository;
    private final UserRepository      userRepository;

    // =========================================================================
    // POST /api/rentals  — crear alquiler
    // =========================================================================

    @Transactional
    public RentalDto create(Long userId, CreateRentalRequest req) {

        // ── 1. Validar fechas ────────────────────────────────────────────────
        LocalDate today = LocalDate.now();
        if (req.startDate().isBefore(today)) {
            throw ApiException.badRequest("La fecha de inicio no puede ser anterior a hoy");
        }
        if (!req.endDate().isAfter(req.startDate())) {
            throw ApiException.badRequest("La fecha de fin debe ser posterior a la fecha de inicio");
        }

        int days = (int) ChronoUnit.DAYS.between(req.startDate(), req.endDate());

        // ── 2. Cargar usuario ────────────────────────────────────────────────
        var user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

        // ── 3. Validar ítems y construir líneas ──────────────────────────────
        List<RentalItem> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (RentalItemRequest itemReq : req.items()) {
            Product product = productRepository.findById(itemReq.productId())
                    .filter(Product::isActive)
                    .orElseThrow(() -> ApiException.notFound(
                            "Producto no encontrado o inactivo: id=" + itemReq.productId()));

            // ── Stock disponible en el rango pedido ──
            int occupied  = rentalRepository.countOccupiedStock(
                    product.getId(), req.startDate(), req.endDate());
            int available = product.getStock() - occupied;

            if (available < itemReq.quantity()) {
                throw ApiException.badRequest(
                        "Stock insuficiente para \"" + product.getName() + "\". " +
                        "Disponible: " + available + ", solicitado: " + itemReq.quantity());
            }

            // Snapshot del precio al momento de crear el alquiler
            BigDecimal unitPrice = product.getPricePerDay();
            BigDecimal lineTotal = unitPrice
                    .multiply(BigDecimal.valueOf(itemReq.quantity()))
                    .multiply(BigDecimal.valueOf(days));

            lines.add(RentalItem.builder()
                    .product(product)
                    .quantity(itemReq.quantity())
                    .days(days)
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .build());

            subtotal = subtotal.add(lineTotal);
        }

        // ── 4. Generar código único SR-YYYY-NNNNN ────────────────────────────
        String yearPrefix = "SR-" + req.startDate().getYear() + "-";
        long count = rentalRepository.countByCodeStartingWith(yearPrefix);
        String code = yearPrefix + String.format("%05d", count + 1);

        // ── 5. Construir y persistir el alquiler ─────────────────────────────
        Rental rental = Rental.builder()
                .code(code)
                .user(user)
                .startDate(req.startDate())
                .endDate(req.endDate())
                .status(RentalStatus.PENDIENTE)
                .paymentMethod(req.paymentMethod())
                .subtotal(subtotal)
                .total(subtotal)   // deposit = 0 por defecto en Fase 3
                .build();

        // Asociar líneas al rental antes de persistir (cascade ALL)
        for (RentalItem line : lines) {
            line.setRental(rental);
            rental.getItems().add(line);
        }

        rentalRepository.save(rental);
        return RentalDto.from(rental);
    }

    // =========================================================================
    // GET /api/rentals/mine  — mis alquileres
    // =========================================================================

    @Transactional(readOnly = true)
    public List<RentalSummaryDto> findMine(Long userId) {
        return rentalRepository.findByUserIdWithItemsAndProducts(userId)
                .stream()
                .map(RentalSummaryDto::from)
                .toList();
    }

    // =========================================================================
    // GET /api/rentals/{id}  — detalle de un alquiler
    // =========================================================================

    @Transactional(readOnly = true)
    public RentalDto findById(Long userId, Long id) {
        Rental rental = rentalRepository.findByIdWithItems(id)
                .orElseThrow(() -> ApiException.notFound("Alquiler no encontrado"));

        if (!rental.getUser().getId().equals(userId)) {
            // Devolver 404 en lugar de 403 para no revelar si el recurso existe
            throw ApiException.notFound("Alquiler no encontrado");
        }

        return RentalDto.from(rental);
    }

    // =========================================================================
    // POST /api/rentals/{id}/extend  — extender fecha de devolución
    // =========================================================================

    @Transactional
    public RentalDto extend(Long userId, Long id, ExtendRentalRequest req) {

        Rental rental = rentalRepository.findByIdWithItems(id)
                .orElseThrow(() -> ApiException.notFound("Alquiler no encontrado"));

        if (!rental.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Alquiler no encontrado");
        }

        // ── Validar estado ───────────────────────────────────────────────────
        if (rental.getStatus() != RentalStatus.PENDIENTE
                && rental.getStatus() != RentalStatus.ACTIVO) {
            throw ApiException.badRequest(
                    "Solo se pueden extender alquileres en estado PENDIENTE o ACTIVO. " +
                    "Estado actual: " + rental.getStatus().name());
        }

        // ── Validar nueva fecha ──────────────────────────────────────────────
        LocalDate oldEndDate = rental.getEndDate();
        if (!req.newEndDate().isAfter(oldEndDate)) {
            throw ApiException.badRequest(
                    "La nueva fecha de fin debe ser posterior a la actual (" + oldEndDate + ")");
        }

        // ── Validar stock para el período de extensión ───────────────────────
        // Período: día siguiente al fin actual → nueva fecha de fin
        LocalDate extensionStart = oldEndDate.plusDays(1);

        for (RentalItem item : rental.getItems()) {
            int occupied  = rentalRepository.countOccupiedStock(
                    item.getProduct().getId(), extensionStart, req.newEndDate());
            int available = item.getProduct().getStock() - occupied;

            if (available < item.getQuantity()) {
                throw ApiException.badRequest(
                        "Stock insuficiente para extender \"" + item.getProduct().getName() + "\". " +
                        "Disponible en el período de extensión: " + available +
                        ", necesario: " + item.getQuantity());
            }
        }

        // ── Actualizar rental y recalcular totales ───────────────────────────
        rental.setEndDate(req.newEndDate());

        int newDays = (int) ChronoUnit.DAYS.between(rental.getStartDate(), req.newEndDate());
        BigDecimal newSubtotal = BigDecimal.ZERO;

        for (RentalItem item : rental.getItems()) {
            item.setDays(newDays);
            BigDecimal newLineTotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .multiply(BigDecimal.valueOf(newDays));
            item.setLineTotal(newLineTotal);
            newSubtotal = newSubtotal.add(newLineTotal);
        }

        rental.setSubtotal(newSubtotal);
        rental.setTotal(newSubtotal.add(rental.getDeposit()));

        rentalRepository.save(rental);
        return RentalDto.from(rental);
    }

    // =========================================================================
    // POST /api/rentals/{id}/cancel  — cancelar alquiler
    // =========================================================================

    @Transactional
    public void cancel(Long userId, Long id) {
        Rental rental = rentalRepository.findByIdWithItems(id)
                .orElseThrow(() -> ApiException.notFound("Alquiler no encontrado"));

        if (!rental.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Alquiler no encontrado");
        }

        if (rental.getStatus() != RentalStatus.PENDIENTE) {
            throw ApiException.badRequest(
                    "Solo se pueden cancelar alquileres en estado PENDIENTE. " +
                    "Estado actual: " + rental.getStatus().name());
        }

        rental.setStatus(RentalStatus.CANCELADO);
        rentalRepository.save(rental);
    }

    // =========================================================================
    // Fase 4 — Panel admin
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<AdminRentalSummaryDto> findAllAdmin(RentalStatus status, String q, Pageable pageable) {

        Specification<Rental> spec = (root, query, cb) -> {
            // Fetch joins para evitar N+1 en el mapeo a DTO (solo en SELECT, no en COUNT)
            Join<Object, Object> userPath;
            if (query != null && Long.class != query.getResultType()) {
                userPath = (Join<Object, Object>) root.fetch("user", JoinType.LEFT);
                root.fetch("createdBy", JoinType.LEFT);
                root.fetch("items", JoinType.LEFT).fetch("product", JoinType.LEFT);
            } else {
                userPath = root.join("user", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(userPath.get("email")), pattern),
                        cb.like(cb.lower(userPath.get("fullName")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return rentalRepository.findAll(spec, pageable).map(AdminRentalSummaryDto::from);
    }

    @Transactional(readOnly = true)
    public RentalDto findByIdAdmin(Long id) {
        Rental rental = rentalRepository.findByIdWithItems(id)
                .orElseThrow(() -> ApiException.notFound("Alquiler no encontrado: id=" + id));
        return RentalDto.from(rental);
    }

    @Transactional
    public RentalDto createForClient(Long adminId, Long clientId, CreateRentalRequest req) {

        // ── 1. Validar fechas ────────────────────────────────────────────────
        if (!req.endDate().isAfter(req.startDate())) {
            throw ApiException.badRequest("La fecha de fin debe ser posterior a la de inicio");
        }
        int days = (int) ChronoUnit.DAYS.between(req.startDate(), req.endDate());

        // ── 2. Cargar admin y cliente ────────────────────────────────────────
        var admin  = userRepository.findById(adminId)
                .orElseThrow(() -> ApiException.notFound("Admin no encontrado"));
        var client = userRepository.findById(clientId)
                .orElseThrow(() -> ApiException.notFound("Cliente no encontrado: id=" + clientId));

        // ── 3. Validar ítems ─────────────────────────────────────────────────
        List<RentalItem> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (RentalItemRequest itemReq : req.items()) {
            Product product = productRepository.findById(itemReq.productId())
                    .filter(Product::isActive)
                    .orElseThrow(() -> ApiException.notFound(
                            "Producto no encontrado o inactivo: id=" + itemReq.productId()));

            int occupied  = rentalRepository.countOccupiedStock(
                    product.getId(), req.startDate(), req.endDate());
            int available = product.getStock() - occupied;

            if (available < itemReq.quantity()) {
                throw ApiException.badRequest(
                        "Stock insuficiente para \"" + product.getName() + "\". " +
                        "Disponible: " + available + ", solicitado: " + itemReq.quantity());
            }

            BigDecimal unitPrice = product.getPricePerDay();
            BigDecimal lineTotal = unitPrice
                    .multiply(BigDecimal.valueOf(itemReq.quantity()))
                    .multiply(BigDecimal.valueOf(days));

            lines.add(RentalItem.builder()
                    .product(product)
                    .quantity(itemReq.quantity())
                    .days(days)
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .build());

            subtotal = subtotal.add(lineTotal);
        }

        // ── 4. Código único ──────────────────────────────────────────────────
        String yearPrefix = "SR-" + req.startDate().getYear() + "-";
        long count = rentalRepository.countByCodeStartingWith(yearPrefix);
        String code = yearPrefix + String.format("%05d", count + 1);

        // ── 5. Construir y persistir ─────────────────────────────────────────
        Rental rental = Rental.builder()
                .code(code)
                .user(client)
                .createdBy(admin)
                .startDate(req.startDate())
                .endDate(req.endDate())
                .status(RentalStatus.PENDIENTE)
                .paymentMethod(req.paymentMethod())
                .subtotal(subtotal)
                .total(subtotal)
                .build();

        for (RentalItem line : lines) {
            line.setRental(rental);
            rental.getItems().add(line);
        }

        rentalRepository.save(rental);
        return RentalDto.from(rental);
    }

    @Transactional
    public RentalDto forceChangeStatus(Long rentalId, RentalStatus newStatus) {
        Rental rental = rentalRepository.findByIdWithItems(rentalId)
                .orElseThrow(() -> ApiException.notFound("Alquiler no encontrado: id=" + rentalId));
        rental.setStatus(newStatus);
        rentalRepository.save(rental);
        return RentalDto.from(rental);
    }
}
