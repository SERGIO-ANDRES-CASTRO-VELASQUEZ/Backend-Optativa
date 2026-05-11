package com.sports.backend.repository;

import com.sports.backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {
    // Consultas derivadas y Specifications se resuelven automáticamente.
    // El CRUD admin de Fase 4 también usará este repositorio.
}
