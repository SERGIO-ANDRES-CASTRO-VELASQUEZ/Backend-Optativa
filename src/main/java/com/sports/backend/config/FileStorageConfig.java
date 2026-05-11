package com.sports.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configura el handler de recursos estáticos para servir las imágenes
 * subidas por el admin desde el sistema de archivos local.
 *
 * <h2>Mapping</h2>
 * <pre>
 *   URL:  GET /files/products/abc123.jpg
 *   Ruta: ${app.uploads.dir}/products/abc123.jpg
 * </pre>
 *
 * <h2>Seguridad</h2>
 * El patrón {@code /files/**} ya está marcado como público en
 * {@link SecurityConfig} ({@code .requestMatchers("/files/**").permitAll()}).
 * No se requiere autenticación para ver imágenes.
 *
 * <h2>Producción</h2>
 * En un entorno de producción real se reemplazaría este handler por un
 * CDN o un servidor de archivos dedicado (Nginx, S3, etc.). Para el
 * alcance académico del proyecto, este handler es suficiente.
 */
@Configuration
public class FileStorageConfig implements WebMvcConfigurer {

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    @Value("${app.uploads.public-base-url:/files}")
    private String publicBaseUrl;

    /**
     * Mapea las peticiones {@code /files/**} al directorio de uploads en disco.
     *
     * <p>La URL del recurso de Spring requiere el prefijo {@code file:///} y
     * terminar en {@code /} para que funcione correctamente como directorio.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Convertir a ruta absoluta para que funcione independientemente del
        // directorio de trabajo (útil cuando Spring Boot corre desde IntelliJ vs CLI)
        Path absoluteUploadsDir = Paths.get(uploadsDir).toAbsolutePath().normalize();

        registry.addResourceHandler(publicBaseUrl + "/**")
                .addResourceLocations("file:///" + absoluteUploadsDir + "/");
    }
}
