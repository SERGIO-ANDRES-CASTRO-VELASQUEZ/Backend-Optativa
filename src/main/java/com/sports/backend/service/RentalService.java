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

    /**
     * Crea un nuevo alquiler para el usuario autenticado.
     *
     * <p>Validaciones que se aplican en orden:
     * <ol>
     *   <li>startDate ≥ hoy</li>
     *   <li>endDate > startDate (mínimo 1 día)</li>
     *   <li>Por cada ítem: producto existe y está activo</li>
     *   <li>Por cada ítem: stock disponible ≥ cantidad solicitada</li>
     * </ol>
     *
     * <p>El código de alquiler se genera como {@code SR-{YYYY}-{NNNNN}}
     * (ej. SR-2026-00003). Es secuencial por año, basado en el conteo de
     * alquileres con ese prefijo. No es 100% atómico (sin lock de BD) pero
     * es suficiente para el alcance del proyecto.
     *
     * <p>El pago es SIMULADO: solo se persiste el método elegido.
     *
     * @param userId ID del cliente autenticado.
     * @param req    Datos del alquiler (fechas, método de pago, ítems).
     * @return RentalDto con el detalle completo del alquiler creado.
     */
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

    /**
     * Devuelve el historial completo de alquileres del usuario, ordenado
     * del más reciente al más antiguo.
     *
     * <p>Usa {@code findByUserIdWithItemsAndProducts} para cargar todo en
     * una sola query y evitar el problema N+1.
     *
     * @param userId ID del cliente autenticado.
     * @return Lista de resúmenes (sin detalle de ítems).
     */
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

    /**
     * Devuelve el detalle completo de un alquiler, verificando que pertenece
     * al usuario solicitante.
     *
     * <p>En Fase 4, el panel de administración usará un método distinto sin
     * la verificación de propiedad.
     *
     * @param userId ID del cliente autenticado.
     * @param id     ID del alquiler.
     * @return RentalDto con todos los ítems.
     * @throws ApiException 404 si no existe o no pertenece al usuario.
     */
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

    /**
     * Extiende la fecha de fin de un alquiler PENDIENTE o ACTIVO.
     *
     * <p>Pasos:
     * <ol>
     *   <li>Verifica propiedad y estado válido (PENDIENTE | ACTIVO).</li>
     *   <li>Valida que {@code newEndDate} es posterior a la fecha actual de fin.</li>
     *   <li>Comprueba stock disponible para el período de extensión
     *       ({@code oldEndDate + 1 día → newEndDate}) por cada ítem.
     *       El alquiler actual NO se cuenta en ese rango porque su endDate
     *       es anterior al inicio del período de extensión.</li>
     *   <li>Recalcula {@code days}, {@code lineTotal} de cada ítem y
     *       {@code subtotal} / {@code total} del alquiler.</li>
     * </ol>
     *
     * @param userId  ID del cliente autenticado.
     * @param id      ID del alquiler a extender.
     * @param req     Contiene la nueva fecha de fin.
     * @return RentalDto actualizado.
     */
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

    /**
     * Cancela un alquiler en estado PENDIENTE.
     *
     * <p>Solo se permite cancelar mientras el alquiler está PENDIENTE.
     * Una vez en ACTIVO, el cliente debe contactar a la tienda (Fase 4).
     *
     * @param userId ID del cliente autenticado.
     * @param id     ID del alquiler a cancelar.
     */
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

    /**
     * Lista TODOS los alquileres con filtros opcionales para el panel admin.
     *
     * <p>Búsqueda {@code q} busca en el código del alquiler y el email del cliente.
     *
     * @param status  filtro por estado (null = todos)
     * @param q       búsqueda libre en código y email del cliente
     * @param pageable paginación
     * @return página de {@link AdminRentalSummaryDto}
     */
    @Transactional(readOnly = true)
    public Page<AdminRentalSummaryDto> findAllAdmin(RentalStatus status, String q, Pageable pageable) {

        Specification<Rental> spec = (root, query, cb) -> {
            // Fetch joins para evitar N+1 en el mapeo a DTO
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("user", JoinType.LEFT);
                root.fetch("createdBy", JoinType.LEFT);
                root.fetch("items", JoinType.LEFT).fetch("product", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.toLowerCase() + "%";
                Join<Rental, User> userJoin = root.join("user", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(userJoin.get("email")), pattern),
                        cb.like(cb.lower(userJoin.get("fullName")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return rentalRepository.findAll(spec, pageable).map(AdminRentalSummaryDto::from);
    }

    /**
     * Devuelve el detalle completo de un alquiler sin verificar propiedad.
     * Para uso exclusivo del panel admin.
     *
     * @param id id del alquiler
     * @throws ApiException 404 si no existe
     */
    @Transactional(readOnly = true)
    public RentalDto findByIdAdmin(Long id) {
        Rental rental = rentalRepository.findByIdWithItems(id)
                .orElseThrow(() -> ApiException.notFound("Alquiler no encontrado: id=" + id));
        return RentalDto.from(rental);
    }

    /**
     * Crea un alquiler desde el mostrador admin.
     *
     * <p>Igual que {@link #create(Long, CreateRentalRequest)} pero:
     * <ul>
     *   <li>El titular del alquiler es {@code clientId}, no el admin.</li>
     *   <li>Se registra {@code createdBy = admin} para auditoría.</li>
     *   <li>La fecha de inicio puede ser hoy (sin restricción de futuro).</li>
     * </ul>
     *
     * @param adminId  id del admin que opera en mostrador
     * @param clientId id del cliente titular del alquiler
     * @param req      datos del alquiler
     */
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

    /**
     * Cambia el estado de un alquiler de forma forzada (sin restricciones de flujo).
     * Solo disponible para admins.
     *
     * @param rentalId  id del alquiler
     * @param newStatus nuevo estado a asignar
     * @throws ApiException 404 si el alquiler no existe
     */
    @Transactional
    public RentalDto forceChangeStatus(Long rentalId, RentalStatus newStatus) {
        Rental rental = rentalRepository.findByIdWithItems(rentalId)
                .orElseThrow(() -> ApiException.notFound("Alquiler no encontrado: id=" + rentalId));
        rental.setStatus(newStatus);
        rentalRepository.save(rental);
        return RentalDto.from(rental);
    }
}
