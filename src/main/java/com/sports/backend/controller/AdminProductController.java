package com.sports.backend.controller;

import com.sports.backend.dto.AdminProductDto;
import com.sports.backend.dto.CreateProductRequest;
import com.sports.backend.dto.ProductSummaryDto;
import com.sports.backend.dto.UpdateProductRequest;
import com.sports.backend.service.ImageStorageService;
import com.sports.backend.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin – Productos", description = "CRUD de productos para el panel de administración")
@SecurityRequirement(name = "bearerAuth")
public class AdminProductController {

    private final ProductService       productService;
    private final ImageStorageService  imageStorageService;

    // =========================================================================
    // GET /api/admin/products  — listar todos (activos + inactivos)
    // =========================================================================

    @GetMapping
    @Operation(
            summary = "Listar todos los productos (admin)",
            description = "Lista activos e inactivos. Soporta filtro por categoría y búsqueda en nombre."
    )
    public ResponseEntity<Page<ProductSummaryDto>> listAll(
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(productService.listAdmin(category, q, pageable));
    }

    // =========================================================================
    // GET /api/admin/products/{id}  — detalle (incluye inactivos)
    // =========================================================================

    @GetMapping("/{id}")
    @Operation(
            summary = "Detalle de un producto (admin)",
            description = "Devuelve el producto con imágenes, specs y datos de auditoría. " +
                          "Incluye productos inactivos."
    )
    public ResponseEntity<AdminProductDto> detail(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getByIdAdmin(id));
    }

    // =========================================================================
    // POST /api/admin/products  — crear producto
    // =========================================================================

    @PostMapping
    @Operation(
            summary = "Crear producto",
            description = "Crea un producto nuevo. Acepta URLs de imágenes externas y specs técnicas. " +
                          "Para subir imágenes locales usar POST /{id}/images."
    )
    public ResponseEntity<AdminProductDto> create(
            @Valid @RequestBody CreateProductRequest request
    ) {
        AdminProductDto created = productService.create(request);
        return ResponseEntity.status(201).body(created);
    }

    // =========================================================================
    // PUT /api/admin/products/{id}  — editar producto
    // =========================================================================

    @PutMapping("/{id}")
    @Operation(
            summary = "Editar producto",
            description = "Actualiza solo los campos no-null del body (semántica patch). " +
                          "Si se envía 'specs', reemplaza todas las specs existentes."
    )
    public ResponseEntity<AdminProductDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    // =========================================================================
    // DELETE /api/admin/products/{id}  — desactivar (soft delete)
    // =========================================================================

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Desactivar producto (soft delete)",
            description = "Oculta el producto del catálogo sin eliminarlo de la BD. " +
                          "Para reactivar usar PUT /{id} con { \"active\": true }."
    )
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        productService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // POST /api/admin/products/{id}/images  — subir imagen multipart
    // =========================================================================

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Subir imagen de producto",
            description = "Sube una imagen multipart (JPEG/PNG/WEBP, máx. 5 MB) y la asocia al producto. " +
                          "La URL pública queda en /files/products/{uuid}.ext."
    )
    public ResponseEntity<AdminProductDto> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        // 1. Guardar archivo en disco → obtener URL pública
        String publicUrl = imageStorageService.store(file);

        // 2. Registrar en BD
        AdminProductDto updated = productService.addImage(id, publicUrl);

        return ResponseEntity.status(201).body(updated);
    }

    // =========================================================================
    // DELETE /api/admin/products/{id}/images/{imageId}  — eliminar imagen
    // =========================================================================

    // =========================================================================
    // POST /api/admin/products/{id}/images/url  — añadir imagen por URL
    // =========================================================================

    @PostMapping("/{id}/images/url")
    @Operation(summary = "Añadir imagen por URL", description = "Asocia una URL externa de imagen al producto.")
    public ResponseEntity<AdminProductDto> addImageByUrl(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        AdminProductDto updated = productService.addImage(id, url.trim());
        return ResponseEntity.status(201).body(updated);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @Operation(
            summary = "Eliminar imagen de producto",
            description = "Elimina la imagen de BD y, si fue subida localmente, también del disco."
    )
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId
    ) {
        // 1. Borrar de BD → devuelve la URL para poder borrar el archivo
        String removedUrl = productService.removeImage(id, imageId);

        // 2. Intentar borrar el archivo físico (silencioso si es URL externa)
        imageStorageService.delete(removedUrl);

        return ResponseEntity.noContent().build();
    }
}
