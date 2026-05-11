package com.sports.backend.service;

import com.sports.backend.exception.ApiException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio para almacenar imágenes de productos subidas como multipart.
 *
 * <h2>Cómo funciona</h2>
 * <ol>
 *   <li>El archivo se guarda en {@code ${app.uploads.dir}/products/{uuid}.{ext}}
 *       en el sistema de archivos local.</li>
 *   <li>El método devuelve la URL pública: {@code /files/products/{uuid}.{ext}}.</li>
 *   <li>Spring sirve ese directorio gracias a
 *       que mapea {@code /files/**} a {@code ${app.uploads.dir}}.</li>
 * </ol>
 *
 * <h2>Restricciones validadas</h2>
 * <ul>
 *   <li>Tamaño máximo: 5 MB (también configurado en {@code application.yml}).</li>
 *   <li>Tipos MIME permitidos: {@code image/jpeg}, {@code image/png}, {@code image/webp}.</li>
 * </ul>
 *
 * <h2>Uso en AdminProductController</h2>
 * <pre>
 *   String publicUrl = imageStorageService.store(file);
 *   // publicUrl = "/files/products/a1b2c3d4-....jpg"
 * </pre>
 *
 * <h2>Eliminar una imagen almacenada</h2>
 * <pre>
 *   imageStorageService.delete("/files/products/a1b2c3d4-....jpg");
 * </pre>
 */
@Service
public class ImageStorageService {

    /** Tipos MIME de imagen aceptados. */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    /** Tamaño máximo en bytes (5 MB). */
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    /** Carpeta raíz de uploads, leída de {@code app.uploads.dir}. Default: {@code uploads}. */
    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    /** Prefijo de la URL pública, leída de {@code app.uploads.public-base-url}. Default: {@code /files}. */
    @Value("${app.uploads.public-base-url:/files}")
    private String publicBaseUrl;

    /** Ruta absoluta al subdirectorio de imágenes de productos. */
    private Path productsDir;

    /**
     * Crea el directorio {@code uploads/products/} al arrancar la aplicación
     * si no existe todavía.
     */
    @PostConstruct
    void init() {
        productsDir = Paths.get(uploadsDir).resolve("products").toAbsolutePath().normalize();
        try {
            Files.createDirectories(productsDir);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo crear el directorio de uploads: " + productsDir, e);
        }
    }

    /**
     * Almacena un archivo multipart en disco y devuelve su URL pública.
     *
     * @param file archivo recibido desde el controller (multipart/form-data).
     * @return URL pública relativa, p. ej. {@code /files/products/abc123.jpg}.
     * @throws ApiException 400 si el archivo está vacío, supera el tamaño o el tipo no está permitido.
     */
    public String store(MultipartFile file) {
        // ── Validaciones ─────────────────────────────────────────────────────
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("El archivo de imagen no puede estar vacío");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw ApiException.badRequest(
                    "El archivo supera el tamaño máximo permitido de 5 MB. " +
                    "Tamaño recibido: " + (file.getSize() / 1024 / 1024) + " MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw ApiException.badRequest(
                    "Tipo de archivo no permitido: '" + contentType + "'. " +
                    "Se aceptan: JPEG, PNG, WEBP");
        }

        // ── Generar nombre único ──────────────────────────────────────────────
        String ext = extractExtension(file.getOriginalFilename(), contentType);
        String filename = UUID.randomUUID() + "." + ext;
        Path destination = productsDir.resolve(filename).normalize();

        // Seguridad: verificar que el destino está dentro del directorio esperado
        if (!destination.startsWith(productsDir)) {
            throw ApiException.badRequest("Nombre de archivo no permitido");
        }

        // ── Guardar en disco ──────────────────────────────────────────────────
        try {
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la imagen en disco: " + e.getMessage(), e);
        }

        return publicBaseUrl + "/products/" + filename;
    }

    /**
     * Elimina del disco la imagen asociada a la URL pública dada.
     *
     * <p>Si el archivo no existe, la operación es silenciosa (no lanza excepción).
     * Solo elimina archivos que estén dentro de {@code productsDir} (seguridad).
     *
     * @param publicUrl URL pública almacenada en {@code product_images.url},
     *                  p. ej. {@code /files/products/abc123.jpg}.
     */
    public void delete(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(publicBaseUrl + "/products/")) {
            // URL externa (http://...) o ruta no gestionada por este servicio → ignorar
            return;
        }
        String filename = publicUrl.substring((publicBaseUrl + "/products/").length());
        Path target = productsDir.resolve(filename).normalize();

        // Seguridad: solo borrar archivos dentro de productsDir
        if (!target.startsWith(productsDir)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            // No fatal: loguear y continuar (la BD ya borrará el registro)
            // En un proyecto real usaríamos un Logger
            System.err.println("[ImageStorageService] No se pudo eliminar archivo: " + target);
        }
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    /**
     * Extrae la extensión del nombre original del archivo.
     * Si no tiene extensión válida, la infiere del content-type.
     */
    private String extractExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                    .toLowerCase().trim();
            if (Set.of("jpg", "jpeg", "png", "webp").contains(ext)) {
                return ext.equals("jpeg") ? "jpg" : ext;
            }
        }
        // Inferir del content-type
        return switch (contentType.toLowerCase()) {
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            default           -> "jpg"; // jpeg / image/jpg
        };
    }
}
