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

/**
 * Controlador del panel de administración para la gestión de productos.
 *
 * <p>Todos los endpoints requieren rol {@code ADMIN} (garantizado en
 * {@link com.sports.backend.config.SecurityConfig} con
 * {@code .requestMatchers("/api/admin/**").hasRole("ADMIN")}).
 *
 * <pre>
 *   GET    /api/admin/products              → listar (activos + inactivos)
 *   GET    /api/admin/products/{id}         → detalle (incluye inactivos)
 *   POST   /api/admin/products              → crear producto
 *   PUT    /api/admin/products/{id}         → editar producto
 *   DELETE /api/admin/products/{id}         → desactivar (soft delete)
 *   POST   /api/admin/products/{id}/images  → subir imagen (multipart)
 *   DELETE /api/admin/products/{id}/images/{imageId} → eliminar imagen
 * </pre>
 *
 * <h2>Flujo de imágenes multipart</h2>
 * <ol>
 *   <li>El controller recibe el {@code MultipartFile}.</li>
 *   <li>Delega en {@link ImageStorageService#store(MultipartFile)} para
 *       validar y guardar en disco. Obtiene la URL pública.</li>
 *   <li>Llama a {@link ProductService#addImage(Long, String)} para
 *       registrar la imagen en BD y devolver el DTO actualizado.</li>
 * </ol>
 */
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

    /**
     * Lista todos los productos (activos e inactivos) con filtros opcionales.
     *
     * <p>A diferencia del listado público, incluye productos desactivados para
     * que el admin pueda gestionarlos.
     *
     * @param category filtra por id de categoría (opcional)
     * @param q        búsqueda libre en el nombre (opcional)
     * @param pageable paginación; default: page=0, size=20, sort=name,asc
     * @return página de {@link ProductSummaryDto}
     */
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

    /**
     * Devuelve el detalle completo de un producto, incluyendo si está inactivo.
     *
     * @param id id del producto
     * @return {@link AdminProductDto} con imágenes, specs, favoriteCount y timestamps
     */
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

    /**
     * Crea un producto nuevo.
     *
     * <p>Si {@code active} es {@code null} en el body, el producto se crea activo.
     * Las imágenes se pueden enviar como URLs externas en el body o subir después
     * via multipart con {@code POST /api/admin/products/{id}/images}.
     *
     * @param request datos del nuevo producto
     * @return 201 Created con el {@link AdminProductDto} del producto creado
     */
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

    /**
     * Edita un producto existente.
     *
     * <p>Semántica patch: solo actualiza los campos no-null del body.
     * Para las specs, si se envía la lista (incluso vacía), reemplaza todas.
     * Si no se envía la lista ({@code null}), las specs no se tocan.
     *
     * @param id      id del producto a editar
     * @param request campos a actualizar
     * @return 200 OK con el {@link AdminProductDto} actualizado
     */
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

    /**
     * Desactiva un producto (soft delete).
     *
     * <p>El producto queda oculto en el catálogo público pero no se elimina
     * de la BD. Los alquileres existentes no se ven afectados.
     * Para reactivar, usar {@code PUT /{id}} con {@code "active": true}.
     *
     * @param id id del producto a desactivar
     * @return 204 No Content
     */
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

    /**
     * Sube una imagen para un producto y la asocia en BD.
     *
     * <p>La imagen se envía como {@code multipart/form-data} con el campo
     * {@code file}. Se valida: tamaño ≤ 5 MB, tipos JPEG/PNG/WEBP.
     *
     * <p>La imagen se guarda en {@code uploads/products/{uuid}.ext} y se sirve
     * públicamente desde {@code /files/products/{uuid}.ext}.
     *
     * <p>Ejemplo con curl:
     * <pre>
     *   curl -X POST http://localhost:8080/api/admin/products/1/images \
     *     -H "Authorization: Bearer TOKEN" \
     *     -F "file=@/ruta/imagen.jpg"
     * </pre>
     *
     * @param id   id del producto
     * @param file archivo de imagen (campo {@code file} del form-data)
     * @return 201 Created con el {@link AdminProductDto} actualizado (incluye la nueva imagen)
     */
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

    /**
     * Elimina una imagen de un producto.
     *
     * <p>Si la imagen fue subida localmente (URL empieza por {@code /files/}),
     * también borra el archivo físico del disco.
     * Si es una URL externa (http://...), solo borra el registro de BD.
     *
     * @param id      id del producto
     * @param imageId id de la imagen a eliminar
     * @return 204 No Content
     */
    // =========================================================================
    // POST /api/admin/products/{id}/images/url  — añadir imagen por URL
    // =========================================================================

    /**
     * Añade una imagen externa (URL) a un producto sin necesidad de subir archivo.
     *
     * @param id   id del producto
     * @param body JSON con campo {@code url}
     * @return 201 Created con el {@link AdminProductDto} actualizado
     */
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
