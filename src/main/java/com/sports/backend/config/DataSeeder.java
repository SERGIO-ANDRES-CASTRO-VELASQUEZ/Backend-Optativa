package com.sports.backend.config;

import com.sports.backend.model.Category;
import com.sports.backend.model.PaymentMethod;
import com.sports.backend.model.Product;
import com.sports.backend.model.ProductImage;
import com.sports.backend.model.ProductSpec;
import com.sports.backend.model.Rental;
import com.sports.backend.model.RentalItem;
import com.sports.backend.model.RentalStatus;
import com.sports.backend.model.Role;
import com.sports.backend.model.User;
import com.sports.backend.repository.CategoryRepository;
import com.sports.backend.repository.ProductRepository;
import com.sports.backend.repository.RentalRepository;
import com.sports.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Siembra datos iniciales en la base de datos si no existen.
 * Se ejecuta una sola vez al arrancar la aplicación.
 *
 * Fase 1: admin inicial.
 * Fase 2: 7 categorías + 12 productos demo (añadidos Fútbol x2, Boxeo x2).
 * Fase 3: cliente demo para pruebas de alquileres.
 * Fase 4: usuarios extra + alquileres demo para probar el panel admin y dashboard.
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
    private final RentalRepository   rentalRepository;   // ← NUEVO: para alquileres demo
    private final PasswordEncoder    passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      CategoryRepository categoryRepository,
                      ProductRepository productRepository,
                      RentalRepository rentalRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository     = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository  = productRepository;
        this.rentalRepository   = rentalRepository;
        this.passwordEncoder    = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedClientDemo();
        seedExtraUsersForFase4();
        seedCategoriesAndProducts();
        seedRentalsDemo();           // ← NUEVO: alquileres demo
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
                List.of("https://images.unsplash.com/photo-1544191696-102dbdaeeaa0?w=600&q=80"),
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
                List.of("https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=600&q=80"),
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
                List.of("https://images.unsplash.com/photo-1571068316344-75bc76f77890?w=600&q=80"),
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
                List.of("https://images.unsplash.com/photo-1506953823976-52e1fdc0149a?w=600&q=80"),
                List.of(
                        spec("Capacidad", "120 kg"),
                        spec("Longitud", "3.5 m"),
                        spec("Material", "Polietileno HDPE"),
                        spec("Incluye", "Remo + chaleco salvavidas")
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
                List.of("https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?w=600&q=80"),
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
                List.of("https://images.unsplash.com/photo-1622279457486-62dcc4a431d6?w=600&q=80"),
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
                List.of("https://images.unsplash.com/photo-1598520106830-8c45c2035460?w=600&q=80"),
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
                List.of("https://images.unsplash.com/photo-1517963628607-235ccdd5476c?w=600&q=80"),
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
                List.of(
                        "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=600&q=80",
                        "https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?w=600&q=80"
                ),
                List.of(
                        spec("Rango de peso", "2 – 20 kg c/u"),
                        spec("Material", "Acero cromado"),
                        spec("Ajuste", "Cada 2 kg"),
                        spec("Cantidad", "Par (x2)")
                )
        );

        // ---- 11. Balón de Fútbol Profesional --------------------------------
        buildProduct(
                "Balón de Fútbol Profesional",
                "Balón de fútbol Nike Flight talla 5, cubierto en cuero sintético de alta durabilidad " +
                        "con cámara de butilo para retención óptima del aire. " +
                        "Certificado FIFA Quality Pro. Ideal para partidos oficiales y entrenamiento.",
                cats.get("futbol"),
                new BigDecimal("12000"),
                12,
                List.of("https://images.unsplash.com/photo-1614632537190-23e4146777db?w=600&q=80"),
                List.of(
                        spec("Talla", "5 (reglamentaria adultos)"),
                        spec("Material exterior", "Cuero sintético TPU"),
                        spec("Interior", "Cámara de butilo"),
                        spec("Certificación", "FIFA Quality Pro")
                )
        );

        // ---- 12. Set de Espinilleras y Medias -------------------------------
        buildProduct(
                "Set Espinilleras y Medias de Fútbol",
                "Kit completo de espinilleras adulto con tobillera integrada y medias a juego. " +
                        "Espinilleras de polipropileno de alto impacto con relleno EVA. " +
                        "Disponible en tallas S, M y L.",
                cats.get("futbol"),
                new BigDecimal("8000"),
                20,
                List.of("https://images.unsplash.com/photo-1553778263-73a83bab9b0c?w=600&q=80"),
                List.of(
                        spec("Material espinillera", "Polipropileno + EVA"),
                        spec("Tallas", "S / M / L"),
                        spec("Incluye", "Espinilleras + medias"),
                        spec("Uso", "Adulto")
                )
        );

        // ---- 13. Guantes de Boxeo Everlast ----------------------------------
        buildProduct(
                "Guantes de Boxeo Everlast 16 oz",
                "Guantes de boxeo Everlast Pro Style de 16 oz, fabricados en cuero sintético " +
                        "con relleno de espuma de alta densidad. " +
                        "Velcro de cierre rápido. Aptos para saco, sparring y manoplas.",
                cats.get("boxeo"),
                new BigDecimal("18000"),
                10,
                List.of("https://images.unsplash.com/photo-1591117207239-788bf8de6c3b?w=600&q=80"),
                List.of(
                        spec("Peso", "16 oz"),
                        spec("Material", "Cuero sintético"),
                        spec("Cierre", "Velcro"),
                        spec("Uso", "Saco / sparring / manoplas")
                )
        );

        // ---- 14. Saco de Boxeo con Soporte ----------------------------------
        buildProduct(
                "Saco de Boxeo 70 kg con Cadena",
                "Saco de boxeo relleno de arena y tela, peso aproximado 70 kg. " +
                        "Cubierta exterior de cuero sintético resistente a golpes. " +
                        "Incluye cadena de suspensión y mosquetones de acero. " +
                        "Para instalación en techo o marco fijo.",
                cats.get("boxeo"),
                new BigDecimal("35000"),
                3,
                List.of("https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=600&q=80"),
                List.of(
                        spec("Peso", "~70 kg"),
                        spec("Altura", "120 cm"),
                        spec("Diámetro", "40 cm"),
                        spec("Material", "Cuero sintético"),
                        spec("Incluye", "Cadena + mosquetones de acero")
                )
        );
    }

    // =========================================================================
    // NUEVO — Fase 4: Alquileres demo para panel admin y dashboard
    // =========================================================================
    // Guard: no siembra si ya existen alquileres.
    // Los alquileres se crean directamente (sin pasar por RentalService) para
    // poder controlar el estado y las fechas con libertad.
    // =========================================================================

    private void seedRentalsDemo() {
        if (rentalRepository.count() > 0) return;

        User cliente  = userRepository.findByEmail(DEFAULT_CLIENT_EMAIL).orElse(null);
        User maria    = userRepository.findByEmail(CLIENT2_EMAIL).orElse(null);
        User juan     = userRepository.findByEmail(CLIENT3_EMAIL).orElse(null);
        User admin    = userRepository.findByEmail(DEFAULT_ADMIN_EMAIL).orElse(null);

        if (cliente == null || maria == null || juan == null || admin == null) {
            log.warn("==> No se pudieron sembrar alquileres demo: faltan usuarios.");
            return;
        }

        List<Product> products = productRepository.findAll();
        if (products.size() < 4) {
            log.warn("==> No se pudieron sembrar alquileres demo: faltan productos.");
            return;
        }

        Product p1 = products.get(0);  // Bicicleta de Montaña Trek
        Product p2 = products.get(1);  // Bicicleta de Ruta Colnago
        Product p3 = products.get(6);  // Raqueta de Tenis
        Product p4 = products.get(3);  // Kayak Individual

        LocalDate today = LocalDate.now();

        // ── 1. Alquiler PENDIENTE (futuro, cancelable desde el cliente) ───────
        buildRental(
                cliente, null,
                today.plusDays(5), today.plusDays(10),
                RentalStatus.PENDIENTE, PaymentMethod.TARJETA,
                List.of(item(p1, 1, 5))
        );

        // ── 2. Alquiler ACTIVO (en curso hoy — contribuye al KPI activeRentalsToday) ──
        buildRental(
                maria, null,
                today.minusDays(1), today.plusDays(4),
                RentalStatus.ACTIVO, PaymentMethod.PAYPAL,
                List.of(item(p3, 2, 5))
        );

        // ── 3. Alquiler ACTIVO extendible (cliente para testear extend) ──────
        buildRental(
                cliente, null,
                today, today.plusDays(7),
                RentalStatus.ACTIVO, PaymentMethod.EFECTIVO,
                List.of(item(p2, 1, 7), item(p3, 1, 7))
        );

        // ── 4. Alquiler FINALIZADO este mes (contribuye a finishedThisMonth y revenueThisMonth) ──
        buildRental(
                juan, null,
                today.minusDays(10), today.minusDays(3),
                RentalStatus.FINALIZADO, PaymentMethod.TARJETA,
                List.of(item(p4, 1, 7))
        );

        // ── 5. Alquiler FINALIZADO — creado por admin en mostrador ───────────
        buildRental(
                maria, admin,
                today.minusDays(15), today.minusDays(8),
                RentalStatus.FINALIZADO, PaymentMethod.EFECTIVO,
                List.of(item(p1, 1, 7))
        );

        // ── 6. Alquiler VENCIDO (contribuye al KPI overdueRentals) ────────────
        buildRental(
                juan, null,
                today.minusDays(20), today.minusDays(5),
                RentalStatus.VENCIDO, PaymentMethod.PAYPAL,
                List.of(item(p2, 1, 15))
        );

        // ── 7. Alquiler CANCELADO (muestra flujo completo en el panel) ────────
        buildRental(
                cliente, null,
                today.plusDays(2), today.plusDays(6),
                RentalStatus.CANCELADO, PaymentMethod.TARJETA,
                List.of(item(p3, 1, 4))
        );

        log.info("==> {} alquileres demo sembrados", rentalRepository.count());
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

    /**
     * Construye y persiste un alquiler con sus ítems.
     *
     * @param user        cliente dueño del alquiler
     * @param createdBy   admin que lo creó en mostrador (null si lo creó el cliente)
     * @param startDate   fecha de inicio
     * @param endDate     fecha de fin
     * @param status      estado inicial (PENDIENTE, ACTIVO, FINALIZADO, CANCELADO, VENCIDO)
     * @param payment     método de pago
     * @param rentalItems ítems del alquiler (ya construidos con quantity, days, unitPrice)
     */
    private void buildRental(User user, User createdBy,
                             LocalDate startDate, LocalDate endDate,
                             RentalStatus status, PaymentMethod payment,
                             List<RentalItem> rentalItems) {

        // Calcular totales
        BigDecimal subtotal = rentalItems.stream()
                .map(RentalItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Generar código único SR-YYYY-NNNNN
        int year  = startDate.getYear();
        long count = rentalRepository.countByCodeStartingWith("SR-" + year + "-");
        String code = "SR-" + year + "-" + String.format("%05d", count + 1);

        Rental rental = Rental.builder()
                .code(code)
                .user(user)
                .createdBy(createdBy)
                .startDate(startDate)
                .endDate(endDate)
                .status(status)
                .paymentMethod(payment)
                .subtotal(subtotal)
                .deposit(BigDecimal.ZERO)
                .total(subtotal)
                .build();

        // Asociar ítems al alquiler
        for (RentalItem ri : rentalItems) {
            ri.setRental(rental);
            rental.getItems().add(ri);
        }

        rentalRepository.save(rental);
        log.debug("==> Alquiler demo sembrado: {} | {} | {} – {}", code, status, startDate, endDate);
    }

    /**
     * Crea un RentalItem (sin persistir) con snapshot del precio del producto.
     */
    private static RentalItem item(Product product, int quantity, int days) {
        BigDecimal unitPrice  = product.getPricePerDay();
        BigDecimal lineTotal  = unitPrice.multiply(BigDecimal.valueOf((long) quantity * days));

        return RentalItem.builder()
                .product(product)
                .quantity(quantity)
                .days(days)
                .unitPrice(unitPrice)
                .lineTotal(lineTotal)
                .build();
    }

    private static ProductSpec spec(String key, String value) {
        return ProductSpec.builder().key(key).value(value).build();
    }
}
