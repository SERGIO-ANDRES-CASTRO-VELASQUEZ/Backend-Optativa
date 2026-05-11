package com.sports.backend.config;

import com.sports.backend.model.Category;
import com.sports.backend.model.Product;
import com.sports.backend.model.ProductImage;
import com.sports.backend.model.ProductSpec;
import com.sports.backend.model.Role;
import com.sports.backend.model.User;
import com.sports.backend.repository.CategoryRepository;
import com.sports.backend.repository.ProductRepository;
import com.sports.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Siembra datos iniciales en la base de datos si no existen.
 * Se ejecuta una sola vez al arrancar la aplicación.
 *
 * Fase 1: admin inicial.
 * Fase 2: 7 categorías + 10 productos demo.
 * Fase 3: cliente demo para pruebas de alquileres.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String DEFAULT_ADMIN_EMAIL    = "admin@sports.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin1234";

    private static final String DEFAULT_CLIENT_EMAIL    = "cliente@sports.com";
    private static final String DEFAULT_CLIENT_PASSWORD = "Cliente1234";

    // Usuarios extra para pruebas de Fase 4
    private static final String CLIENT2_EMAIL    = "maria@sports.com";
    private static final String CLIENT2_PASSWORD = "Cliente1234";

    private static final String CLIENT3_EMAIL    = "juan@sports.com";
    private static final String CLIENT3_PASSWORD = "Cliente1234";

    private static final String ADMIN2_EMAIL    = "supervisor@sports.com";
    private static final String ADMIN2_PASSWORD = "Admin1234";

    private final UserRepository     userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository  productRepository;
    private final PasswordEncoder    passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      CategoryRepository categoryRepository,
                      ProductRepository productRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository     = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository  = productRepository;
        this.passwordEncoder    = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedClientDemo();
        seedExtraUsersForFase4();
        seedCategoriesAndProducts();
    }

    // =========================================================================
    // Fase 1 — Admin inicial
    // =========================================================================

    private void seedAdmin() {
        if (userRepository.existsByEmail(DEFAULT_ADMIN_EMAIL)) return;

        User admin = User.builder()
                .fullName("Administrador SportRent")
                .username("admin")
                .email(DEFAULT_ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
                .role(Role.ADMIN)
                .active(true)
                .build();

        userRepository.save(admin);
        log.info("==> Admin inicial creado: {} / {} (cambialo en produccion)",
                DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_PASSWORD);
    }

    // =========================================================================
    // Fase 3 — Cliente demo para pruebas de alquileres
    // =========================================================================

    private void seedClientDemo() {
        if (userRepository.existsByEmail(DEFAULT_CLIENT_EMAIL)) return;

        User client = User.builder()
                .fullName("Carlos Cliente Demo")
                .username("cliente")
                .email(DEFAULT_CLIENT_EMAIL)
                .passwordHash(passwordEncoder.encode(DEFAULT_CLIENT_PASSWORD))
                .phone("3001234567")
                .idDocument("1234567890")
                .role(Role.CLIENT)
                .active(true)
                .build();

        userRepository.save(client);
        log.info("==> Cliente demo creado: {} / {}", DEFAULT_CLIENT_EMAIL, DEFAULT_CLIENT_PASSWORD);
    }

    // =========================================================================
    // Fase 4 — Usuarios extra para pruebas del panel admin
    // =========================================================================

    private void seedExtraUsersForFase4() {

        if (!userRepository.existsByEmail(CLIENT2_EMAIL)) {
            userRepository.save(User.builder()
                    .fullName("María García López")
                    .username("maria.garcia")
                    .email(CLIENT2_EMAIL)
                    .passwordHash(passwordEncoder.encode(CLIENT2_PASSWORD))
                    .phone("3109876543")
                    .idDocument("9876543210")
                    .role(Role.CLIENT)
                    .active(true)
                    .build());
            log.info("==> Cliente 2 creado: {} / {}", CLIENT2_EMAIL, CLIENT2_PASSWORD);
        }

        if (!userRepository.existsByEmail(CLIENT3_EMAIL)) {
            userRepository.save(User.builder()
                    .fullName("Juan Pérez Martínez")
                    .username("juan.perez")
                    .email(CLIENT3_EMAIL)
                    .passwordHash(passwordEncoder.encode(CLIENT3_PASSWORD))
                    .phone("3157654321")
                    .idDocument("1122334455")
                    .role(Role.CLIENT)
                    .active(true)
                    .build());
            log.info("==> Cliente 3 creado: {} / {}", CLIENT3_EMAIL, CLIENT3_PASSWORD);
        }

        if (!userRepository.existsByEmail(ADMIN2_EMAIL)) {
            userRepository.save(User.builder()
                    .fullName("Supervisor SportRent")
                    .username("supervisor")
                    .email(ADMIN2_EMAIL)
                    .passwordHash(passwordEncoder.encode(ADMIN2_PASSWORD))
                    .role(Role.ADMIN)
                    .active(true)
                    .build());
            log.info("==> Admin 2 creado: {} / {}", ADMIN2_EMAIL, ADMIN2_PASSWORD);
        }
    }

    // =========================================================================
    // Fase 2 — Categorías y productos demo
    // =========================================================================

    private void seedCategoriesAndProducts() {
        if (categoryRepository.count() > 0) return;   // ya sembrado

        // --- Categorías ---
        Map<String, Category> cats = seedCategories();

        // --- Productos ---
        seedProducts(cats);

        log.info("==> Datos demo sembrados: {} categorías, {} productos",
                categoryRepository.count(), productRepository.count());
    }

    private Map<String, Category> seedCategories() {
        Category ciclismo  = save(new Category(null, "Ciclismo",   "bx-cycling"));
        Category futbol    = save(new Category(null, "Fútbol",     "bx-football"));
        Category tenis     = save(new Category(null, "Tenis",      "bxs-tennis-ball"));
        Category boxeo     = save(new Category(null, "Boxeo",      "bx-dumbbell"));
        Category fitness   = save(new Category(null, "Fitness",    "bx-run"));
        Category camping   = save(new Category(null, "Camping",    "bx-home-smile"));
        Category acuaticos = save(new Category(null, "Acuáticos",  "bx-swim"));

        return Map.of(
                "ciclismo",  ciclismo,
                "futbol",    futbol,
                "tenis",     tenis,
                "boxeo",     boxeo,
                "fitness",   fitness,
                "camping",   camping,
                "acuaticos", acuaticos
        );
    }

    private Category save(Category c) {
        return categoryRepository.save(c);
    }

    private void seedProducts(Map<String, Category> cats) {

        // ---- 1. Bicicleta de Montaña Trek ----------------------------------
        buildProduct(
                "Bicicleta de Montaña Trek",
                "Bicicleta de montaña Trek Marlin 7 con cuadro de aluminio ligero, " +
                        "suspensión delantera RockShox, 21 velocidades Shimano y frenos hidráulicos. " +
                        "Ideal para terrenos irregulares y senderos de montaña.",
                cats.get("ciclismo"),
                new BigDecimal("45000"),
                5,
                List.of("/img/bike-mountain.jpg"),
                List.of(
                        spec("Material", "Aluminio 6061"),
                        spec("Velocidades", "21 Shimano"),
                        spec("Frenos", "Hidráulicos"),
                        spec("Tallas disponibles", "S / M / L")
                )
        );

        // ---- 2. Bicicleta de Ruta Colnago -----------------------------------
        buildProduct(
                "Bicicleta de Ruta Colnago",
                "Bicicleta de ruta Colnago de carbono de alta performance, " +
                        "diseñada para recorridos largos en carretera. Transmisión Shimano 105, " +
                        "peso total 8.2 kg y geometría aerodinámica.",
                cats.get("ciclismo"),
                new BigDecimal("55000"),
                3,
                List.of("/img/bike-route.jpg"),
                List.of(
                        spec("Material", "Fibra de carbono"),
                        spec("Peso", "8.2 kg"),
                        spec("Transmisión", "Shimano 105 22v"),
                        spec("Frenos", "Pinza de doble pivote")
                )
        );

        // ---- 3. Bicicleta Trekking ------------------------------------------
        buildProduct(
                "Bicicleta Trekking Híbrida",
                "Bicicleta híbrida ideal para recorridos urbanos y caminos de tierra. " +
                        "Cuadro de aluminio, horquilla rígida, manubrio plano y portaequipaje trasero incluido.",
                cats.get("ciclismo"),
                new BigDecimal("35000"),
                6,
                List.of("/img/bike-trek.jpg"),
                List.of(
                        spec("Material", "Aluminio"),
                        spec("Velocidades", "7 Shimano"),
                        spec("Frenos", "V-Brake"),
                        spec("Portaequipaje", "Incluido")
                )
        );

        // ---- 4. Kayak Individual --------------------------------------------
        buildProduct(
                "Kayak Individual Plástico",
                "Kayak de travesía para una persona, fabricado en polietileno de alta densidad. " +
                        "Estable y fácil de maniobrar, apto para ríos tranquilos y lagos. " +
                        "Incluye remo y chaleco salvavidas.",
                cats.get("acuaticos"),
                new BigDecimal("80000"),
                4,
                List.of("/img/kayak.jpg"),
                List.of(
                        spec("Capacidad", "120 kg"),
                        spec("Longitud", "3.5 m"),
                        spec("Material", "Polietileno HDPE"),
                        spec("Incluye", "Remo + chaleco salvavidas")
                )
        );

        // ---- 5. Tabla de Surf 7' -------------------------------------------
        buildProduct(
                "Tabla de Surf 7 Pies Epoxy",
                "Tabla de surf longboard de 7 pies en material epoxy, perfecta para principiantes " +
                        "y olas medianas. Mayor flotabilidad y estabilidad que las tablas de poliuretano. " +
                        "Diseño clásico con tres quillas (thruster).",
                cats.get("acuaticos"),
                new BigDecimal("60000"),
                3,
                List.of("/img/surfboard.jpg"),
                List.of(
                        spec("Longitud", "7 pies (213 cm)"),
                        spec("Material", "Epoxy"),
                        spec("Volumen", "52 L"),
                        spec("Quillas", "Thruster (3)")
                )
        );

        // ---- 6. Carpa para 4 personas ----------------------------------------
        buildProduct(
                "Carpa Camping 4 Personas",
                "Carpa doble capa impermeable con capacidad para 4 personas. " +
                        "Estructura de varillas de fibra de vidrio, suelo cosido y mosquitero incluido. " +
                        "Montaje rápido de 10 minutos. Ideal para camping familiar y senderismo.",
                cats.get("camping"),
                new BigDecimal("50000"),
                5,
                List.of("/img/camping-tent.jpg"),
                List.of(
                        spec("Capacidad", "4 personas"),
                        spec("Impermeable", "3000 mm HH"),
                        spec("Varillas", "Fibra de vidrio"),
                        spec("Peso", "3.8 kg"),
                        spec("Montaje", "~10 minutos")
                )
        );

        // ---- 7. Raqueta de Tenis Wilson -------------------------------------
        buildProduct(
                "Raqueta de Tenis Wilson Pro Staff",
                "Raqueta de tenis Wilson Pro Staff 97 RF, diseñada para jugadores intermedios " +
                        "y avanzados. Marco de grafito Braided con cuerda incluida. " +
                        "Ofrece control y potencia equilibrados en cancha.",
                cats.get("tenis"),
                new BigDecimal("25000"),
                8,
                List.of("/img/tennis-wilson.jpg"),
                List.of(
                        spec("Peso", "340 g"),
                        spec("Balance", "Neutro (32 cm)"),
                        spec("Cabeza", "97 pulgadas²"),
                        spec("Material", "Grafito"),
                        spec("Cuerda", "Incluida")
                )
        );

        // ---- 8. Patineta Eléctrica ------------------------------------------
        buildProduct(
                "Patineta Eléctrica 25 km/h",
                "Scooter eléctrico plegable con autonomía de hasta 25 km por carga. " +
                        "Motor de 350 W, frenos eléctrico y mecánico de disco, " +
                        "pantalla LED y luz delantera integrada. Apto para pavimento urbano.",
                cats.get("fitness"),
                new BigDecimal("70000"),
                4,
                List.of("/img/scooter.jpg"),
                List.of(
                        spec("Autonomía", "25 km"),
                        spec("Velocidad máxima", "25 km/h"),
                        spec("Motor", "350 W"),
                        spec("Carga máx.", "120 kg"),
                        spec("Carga batería", "4-5 h")
                )
        );

        // ---- 9. Set de Pesas 20 kg ------------------------------------------
        buildProduct(
                "Set de Pesas 20 kg",
                "Set completo de pesas de hierro fundido de 20 kg para entrenamiento de fuerza. " +
                        "Incluye barra olímpica de 1.2 m y collares de seguridad. " +
                        "Distribuido en discos de 1.25, 2.5 y 5 kg.",
                cats.get("fitness"),
                new BigDecimal("30000"),
                6,
                List.of("/img/weights-20kg.jpg"),
                List.of(
                        spec("Peso total", "20 kg"),
                        spec("Material", "Hierro fundido"),
                        spec("Incluye", "Barra 1.2 m + collares"),
                        spec("Discos", "1.25 / 2.5 / 5 kg")
                )
        );

        // ---- 10. Mancuernas Ajustables --------------------------------------
        buildProduct(
                "Mancuernas Ajustables 2–20 kg",
                "Par de mancuernas ajustables de acero con sistema de carga rápida. " +
                        "Rango de 2 a 20 kg por unidad (ajuste en incrementos de 2 kg). " +
                        "Ideales para rutinas de fuerza en casa o gimnasio.",
                cats.get("fitness"),
                new BigDecimal("40000"),
                4,
                List.of("/img/weights-1.jpg", "/img/weights-2.jpg"),   // 2 imágenes
                List.of(
                        spec("Rango de peso", "2 – 20 kg c/u"),
                        spec("Material", "Acero cromado"),
                        spec("Ajuste", "Cada 2 kg"),
                        spec("Cantidad", "Par (x2)")
                )
        );
    }

    // =========================================================================
    // Builders de utilidad
    // =========================================================================

    private void buildProduct(String name, String description, Category category,
                              BigDecimal pricePerDay, int stock,
                              List<String> imageUrls, List<ProductSpec> rawSpecs) {

        Product product = Product.builder()
                .name(name)
                .description(description)
                .category(category)
                .pricePerDay(pricePerDay)
                .stock(stock)
                .active(true)
                .build();

        // Imágenes
        for (int i = 0; i < imageUrls.size(); i++) {
            ProductImage img = ProductImage.builder()
                    .product(product)
                    .url(imageUrls.get(i))
                    .orderIndex(i)
                    .build();
            product.getImages().add(img);
        }

        // Specs
        for (ProductSpec s : rawSpecs) {
            s.setProduct(product);
            product.getSpecs().add(s);
        }

        productRepository.save(product);
    }

    private static ProductSpec spec(String key, String value) {
        return ProductSpec.builder().key(key).value(value).build();
    }
}