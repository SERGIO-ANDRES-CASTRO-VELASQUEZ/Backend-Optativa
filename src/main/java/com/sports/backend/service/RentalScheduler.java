package com.sports.backend.service;

import com.sports.backend.model.Rental;
import com.sports.backend.model.RentalStatus;
import com.sports.backend.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Tarea programada que marca como {@code VENCIDO} los alquileres
 * {@code ACTIVO} cuya fecha de fin ha pasado.
 *
 * <p>Se ejecuta todos los días a las 02:00 AM servidor.
 * Requiere {@code @EnableScheduling} en {@link com.sports.backend.BackendApplication}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RentalScheduler {

    private final RentalRepository rentalRepository;

    /**
     * Busca alquileres ACTIVO con endDate anterior a hoy y los marca VENCIDO.
     * Cron: segundo=0, minuto=0, hora=2, cualquier día/mes/año.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void markOverdueRentals() {

        LocalDate today = LocalDate.now();

        Specification<Rental> spec = (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("status"), RentalStatus.ACTIVO),
                        cb.lessThan(root.get("endDate"), today)
                );

        List<Rental> overdueList = rentalRepository.findAll(spec);

        if (overdueList.isEmpty()) {
            log.debug("[RentalScheduler] No hay alquileres vencidos para marcar en {}", today);
            return;
        }

        overdueList.forEach(r -> r.setStatus(RentalStatus.VENCIDO));
        rentalRepository.saveAll(overdueList);

        log.info("[RentalScheduler] {} alquiler(es) marcado(s) como VENCIDO en {}",
                overdueList.size(), today);
    }
}
