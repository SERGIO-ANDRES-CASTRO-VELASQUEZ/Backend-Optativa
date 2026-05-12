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

@Service
public class ImageStorageService {


    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );


    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    @Value("${app.uploads.public-base-url:/files}")
    private String publicBaseUrl;

    private Path productsDir;

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
