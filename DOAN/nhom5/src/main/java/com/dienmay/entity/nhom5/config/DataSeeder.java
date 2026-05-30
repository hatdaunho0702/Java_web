package com.dienmay.entity.nhom5.config;

import com.dienmay.entity.nhom5.entity.Brand;
import com.dienmay.entity.nhom5.entity.Category;
import com.dienmay.entity.nhom5.entity.Coupon;
import com.dienmay.entity.nhom5.entity.CouponDiscountType;
import com.dienmay.entity.nhom5.entity.Product;
import com.dienmay.entity.nhom5.entity.ProductImage;
import com.dienmay.entity.nhom5.entity.ProductSpec;
import com.dienmay.entity.nhom5.entity.Role;
import com.dienmay.entity.nhom5.entity.User;
import com.dienmay.entity.nhom5.repository.BrandRepository;
import com.dienmay.entity.nhom5.repository.CategoryRepository;
import com.dienmay.entity.nhom5.repository.CouponRepository;
import com.dienmay.entity.nhom5.repository.ProductImageRepository;
import com.dienmay.entity.nhom5.repository.ProductRepository;
import com.dienmay.entity.nhom5.repository.ProductSpecRepository;
import com.dienmay.entity.nhom5.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductSpecRepository productSpecRepository;
    private final CouponRepository couponRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (brandRepository.count() > 0) {
            log.info("Skip seeding: dữ liệu đã tồn tại");
            return;
        }

        log.info("Bắt đầu seed dữ liệu mẫu...");
        Map<String, Brand> brandsByName = seedBrands();
        Map<String, Category> categoriesBySlug = seedCategories();
        seedAdminUser();
        seedProducts(brandsByName, categoriesBySlug);
        seedCoupons();
        log.info("Seed dữ liệu mẫu hoàn tất");
    }

    private Map<String, Brand> seedBrands() {
        List<String> brandNames = List.of(
                "Samsung", "Apple", "LG", "Sony", "Panasonic",
                "Xiaomi", "ASUS", "HP", "Dell", "Toshiba", "JBL", "Tecno"
        );

        Map<String, Brand> result = new HashMap<>();
        for (String brandName : brandNames) {
            Brand brand = Brand.builder()
                    .name(brandName)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            brand = brandRepository.save(brand);
            result.put(brandName, brand);
        }
        return result;
    }

    private Map<String, Category> seedCategories() {
        Map<String, Category> categories = new HashMap<>();

        List<CategorySeed> roots = List.of(
                new CategorySeed("Điện thoại & Phụ kiện", "dien-thoai-may-tinh-bang", null),
                new CategorySeed("Laptop", "laptop-may-tinh", null),
                new CategorySeed("Tivi", "tivi", null),
                new CategorySeed("Điện lạnh & Gia dụng", "dien-lanh", null),
                new CategorySeed("Thiết bị âm thanh (Audio)", "am-thanh", null)
        );

        for (CategorySeed seed : roots) {
            Category category = Category.builder()
                    .name(seed.name())
                    .slug(seed.slug())
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            category = categoryRepository.save(category);
            categories.put(seed.slug(), category);
        }

        List<CategorySeed> children = List.of(
                new CategorySeed("Điện thoại", "dien-thoai", "dien-thoai-may-tinh-bang"),
                new CategorySeed("Phụ kiện", "phu-kien", "dien-thoai-may-tinh-bang"),
                new CategorySeed("Laptop", "laptop", "laptop-may-tinh"),
                new CategorySeed("Tủ lạnh", "tu-lanh", "dien-lanh"),
                new CategorySeed("Máy giặt", "may-giat", "dien-lanh"),
                new CategorySeed("Máy giặt cửa trước", "may-giat-cua-truoc", "may-giat"),
                new CategorySeed("Loa Bluetooth", "loa-bluetooth", "am-thanh"),
                new CategorySeed("Tai nghe", "tai-nghe", "am-thanh")
        );

        for (CategorySeed seed : children) {
            Category parent = categories.get(seed.parentSlug());
            Category child = Category.builder()
                    .name(seed.name())
                    .slug(seed.slug())
                    .parent(parent)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            child = categoryRepository.save(child);
            categories.put(seed.slug(), child);
        }

        return categories;
    }

    private void seedAdminUser() {
        User admin = User.builder()
                .uid("admin-seed-uid")
                .fullName("Admin Hệ Thống")
                .email("admin@dienmaydemo.vn")
                .phone("0901234567")
                .role(Role.ADMIN)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(admin);
    }

    private void seedProducts(Map<String, Brand> brands, Map<String, Category> categories) {
        // 1. Máy giặt Samsung Inverter EcoBubble
        Product p1 = saveProduct(
                "Máy giặt Samsung",
                "may-giat-samsung",
                categories.get("may-giat-cua-truoc"),
                brands.get("Samsung"),
                bd("12490000"),
                bd("9890000"),
                20,
                true,
                "/uploads/images/maygiat/maygiat.jpg"
        );
        saveSpecs(p1, List.of(
                spec("Loại máy giặt", "Cửa trước (Lồng ngang)"),
                spec("Động cơ", "Digital Inverter (Tiết kiệm điện)"),
                spec("Công nghệ nổi bật", "Giặt bong bóng EcoBubble / Ecobubble AI"),
                spec("Bảng điều khiển", "Giao diện AI Control tự động ghi nhớ"),
                spec("Tốc độ quay vắt", "1400 vòng/phút"),
                spec("Hiệu suất năng lượng", "5 sao (Siêu tiết kiệm điện)")
        ));
        saveImages(p1, List.of("/uploads/images/maygiat/maygiat.jpg", "/uploads/images/maygiat/maygiat1.jpg"));

        // 2. Tủ lạnh Samsung Inverter 382L
        Product p2 = saveProduct(
                "Tủ lạnh Samsung Inverter 382L",
                "tu-lanh-samsung-inverter-382l",
                categories.get("tu-lanh"),
                brands.get("Samsung"),
                bd("13990000"),
                bd("11490000"),
                15,
                true,
                "/uploads/images/seed/tu-lanh-samsung-inverter-382l.jpg"
        );
        saveSpecs(p2, List.of(
                spec("Kiểu tủ", "Ngăn đá trên / 2 cửa"),
                spec("Công nghệ làm lạnh", "Twin Cooling Plus (2 dàn lạnh độc lập)"),
                spec("Động cơ", "Digital Inverter"),
                spec("Ngăn đông mềm", "Optimal Fresh+ (Giữ thịt cá tươi ngon không đông đá)"),
                spec("Kháng khuẩn khử mùi", "Bộ lọc than hoạt tính Deodorizer"),
                spec("Chất liệu cửa", "Thép không gỉ cao cấp")
        ));
        saveImages(p2, List.of("/uploads/images/seed/tu-lanh-samsung-inverter-382l.jpg"));

        // 3. Tivi LG OLED 55 inch 4K
        Product p3 = saveProduct(
                "Tivi LG OLED 55 inch 4K",
                "tivi-lg-oled-55-inch-4k",
                categories.get("tivi"),
                brands.get("LG"),
                bd("22990000"),
                bd("19990000"),
                12,
                true,
                "/uploads/images/seed/tivi-lg-oled-55-inch-4k.jpg"
        );
        saveSpecs(p3, List.of(
                spec("Kích thước & Loại màn hình", "55 inch OLED 4K"),
                spec("Bộ xử lý hình ảnh (CPU)", "α7 Gen 6 AI 4K"),
                spec("Hệ điều hành", "webOS 23"),
                spec("Tần số quét (Refresh rate)", "120 Hz"),
                spec("Công nghệ âm thanh", "Dolby Atmos & AI Sound Pro"),
                spec("Cổng kết nối", "4 x HDMI (Hỗ trợ eARC), 2 x USB")
        ));
        saveImages(p3, List.of("/uploads/images/seed/tivi-lg-oled-55-inch-4k.jpg"));

        // 4. MacBook Air M3 13 inch
        Product p4 = saveProduct(
                "MacBook Air M3 13 inch",
                "macbook-air-m3-13-inch",
                categories.get("laptop"),
                brands.get("Apple"),
                bd("32990000"),
                bd("27990000"),
                25,
                true,
                "/uploads/images/seed/macbook-air-m3-13-inch.jpg"
        );
        saveSpecs(p4, List.of(
                spec("Kích thước màn hình", "13.6 inch Liquid Retina"),
                spec("Bộ vi xử lý (CPU)", "Apple M3 (8-core CPU)"),
                spec("Bộ xử lý đồ họa (GPU)", "8-core / 10-core GPU"),
                spec("Hệ điều hành", "macOS Sonoma"),
                spec("Thời lượng pin", "Lên đến 18 giờ liên tục"),
                spec("Trọng lượng", "1.24 kg")
        ));
        saveImages(p4, List.of("/uploads/images/seed/macbook-air-m3-13-inch.jpg"));

        // 5. iPhone 14 Pro Max
        Product p5 = saveProduct(
                "iPhone 14 Pro Max",
                "iphone-14-pro-max",
                categories.get("dien-thoai"),
                brands.get("Apple"),
                bd("27990000"),
                bd("24990000"),
                35,
                true,
                "/uploads/images/seed/iphone.jpg"
        );
        saveSpecs(p5, List.of(
                spec("Màn hình", "6.7 inch Super Retina XDR OLED, 120Hz"),
                spec("Bộ vi xử lý (CPU)", "Apple A16 Bionic"),
                spec("Số nhân CPU", "6 nhân"),
                spec("Camera sau", "Chính 48 MP & 2 camera phụ 12 MP"),
                spec("Camera trước", "12 MP"),
                spec("Dung lượng pin", "4323 mAh")
        ));
        saveImages(p5, List.of("/uploads/images/seed/iphone.jpg"));

        // 6. Samsung Galaxy S24 Ultra
        Product p6 = saveProduct(
                "Samsung Galaxy S24 Ultra",
                "samsung-galaxy-s24-ultra",
                categories.get("dien-thoai"),
                brands.get("Samsung"),
                bd("31990000"),
                bd("26990000"),
                28,
                true,
                "/uploads/images/seed/samsung-galaxy-s24-ultra.jpg"
        );
        saveSpecs(p6, List.of(
                spec("Màn hình", "6.8 inch Dynamic AMOLED 2X, QHD+, 120Hz"),
                spec("Bộ vi xử lý (CPU)", "Snapdragon 8 Gen 3 for Galaxy"),
                spec("Số nhân CPU", "8 nhân"),
                spec("Camera sau", "200 MP + 50 MP + 12 MP + 10 MP"),
                spec("Camera trước", "12 MP"),
                spec("Dung lượng pin", "5000 mAh")
        ));
        saveImages(p6, List.of("/uploads/images/seed/samsung-galaxy-s24-ultra.jpg"));

        // 7. Samsung Galaxy S26 Ultra (Mockup/Tương lai)
        Product p7 = saveProduct(
                "Samsung Galaxy S26 Ultra",
                "samsung-galaxy-s26-ultra",
                categories.get("dien-thoai"),
                brands.get("Samsung"),
                bd("34990000"),
                null,
                10,
                true,
                "/uploads/images/seed/samsung-galaxy-s26-ultra.jpg"
        );
        saveSpecs(p7, List.of(
                spec("Màn hình", "6.9 inch Dynamic AMOLED 3X, 144Hz"),
                spec("Bộ vi xử lý (CPU)", "Snapdragon 8 Gen 5 / Exynos 2600"),
                spec("Số nhân CPU", "8 nhân"),
                spec("Camera sau", "320 MP + 50 MP + 50 MP + 12 MP"),
                spec("Camera trước", "24 MP"),
                spec("Dung lượng pin", "5500 mAh")
        ));
        saveImages(p7, List.of("/uploads/images/seed/samsung-galaxy-s26-ultra.jpg"));

        // 8. Xiaomi Redmi Note 10
        Product p8 = saveProduct(
                "Xiaomi Redmi Note 10",
                "xiaomi-redmi-note-10",
                categories.get("dien-thoai"),
                brands.get("Xiaomi"),
                bd("4590000"),
                bd("3890000"),
                40,
                true,
                "/uploads/images/seed/Redminote10.jpg"
        );
        saveSpecs(p8, List.of(
                spec("Màn hình", "6.43 inch AMOLED, Full HD+"),
                spec("Bộ vi xử lý (CPU)", "Snapdragon 678"),
                spec("Số nhân CPU", "8 nhân"),
                spec("Camera sau", "48 MP + 8 MP + 2 MP + 2 MP"),
                spec("Camera trước", "13 MP"),
                spec("Dung lượng pin", "5000 mAh")
        ));
        saveImages(p8, List.of("/uploads/images/seed/Redminote10.jpg"));

        // 9. Redmi Note 13 Pro+ 5G
        Product p9 = saveProduct(
                "Redmi Note 13 Pro+ 5G",
                "redmi-note-13-pro-5g",
                categories.get("dien-thoai"),
                brands.get("Xiaomi"),
                bd("10990000"),
                bd("9290000"),
                30,
                true,
                "/uploads/images/seed/xiaomi.jpg"
        );
        saveSpecs(p9, List.of(
                spec("Màn hình", "6.67 inch AMOLED, 1.5K, 120Hz"),
                spec("Bộ vi xử lý (CPU)", "MediaTek Dimensity 7200-Ultra"),
                spec("Số nhân CPU", "8 nhân"),
                spec("Camera sau", "200 MP + 8 MP + 2 MP"),
                spec("Camera trước", "16 MP"),
                spec("Dung lượng pin", "5000 mAh")
        ));
        saveImages(p9, List.of("/uploads/images/seed/xiaomi.jpg"));

        // 10. Ốp lưng chống sốc iPhone
        Product p10 = saveProduct(
                "Ốp lưng chống sốc iPhone",
                "op-lung-chong-soc-iphone",
                categories.get("phu-kien"),
                brands.get("Apple"),
                bd("200000"),
                bd("150000"),
                100,
                true,
                "/uploads/images/seed/iphone17.jpg"
        );
        saveSpecs(p10, List.of(
                spec("Loại sản phẩm", "Ốp lưng bảo vệ máy"),
                spec("Chất liệu", "Nhựa dẻo TPU kết hợp PC cứng chống ố vàng"),
                spec("Tính năng 1", "Thiết kế chống sốc 4 góc chịu lực va đập tốt"),
                spec("Tính năng 2", "Viền nhô cao bảo vệ tuyệt đối cụm camera"),
                spec("Tính năng 3", "Hỗ trợ sạc không dây ổn định"),
                spec("Trọng lượng", "~35g")
        ));
        saveImages(p10, List.of("/uploads/images/seed/iphone17.jpg"));

        // 11. Loa JBL Charge 5
        Product p11 = saveProduct(
                "Loa JBL Charge 5",
                "loa-jbl-charge-5",
                categories.get("loa-bluetooth"),
                brands.get("JBL"),
                bd("4290000"),
                bd("3850000"),
                50,
                true,
                "/uploads/images/seed/loa-jbl-charge-5.jpg"
        );
        saveSpecs(p11, List.of(
                spec("Công suất tổng", "40W RMS (Củ loa woofer 30W + Tweeter 10W)"),
                spec("Kết nối", "Bluetooth 5.1"),
                spec("Chuẩn kháng nước", "IP67 (Chống nước và cát bụi hoàn toàn)"),
                spec("Thời lượng pin", "Lên đến 20 giờ chơi nhạc liên tục"),
                spec("Dung lượng pin", "7500 mAh (Tích hợp sạc ngược Powerbank cho điện thoại)"),
                spec("Công nghệ đặc biệt", "JBL PartyBoost kết nối nhiều loa")
        ));
        saveImages(p11, List.of("/uploads/images/seed/loa-jbl-charge-5.jpg"));

        // 12. Tai nghe Tecno Buds
        Product p12 = saveProduct(
                "Tai nghe Tecno Buds",
                "tai-nghe-tecno-buds",
                categories.get("tai-nghe"),
                brands.get("Tecno"),
                bd("600000"),
                bd("450000"),
                80,
                true,
                "/uploads/images/seed/anc_airport.jpg"
        );
        saveSpecs(p12, List.of(
                spec("Kết nối không dây", "Bluetooth 5.3 ổn định"),
                spec("Kích thước driver", "10 mm Dynamic Driver"),
                spec("Kháng nước bụi", "IPX4 (Chống mồ hôi)"),
                spec("Chống ồn", "Khử tiếng ồn môi trường khi đàm thoại (ENC)"),
                spec("Thời lượng pin tai nghe", "~5 giờ sử dụng độc lập"),
                spec("Tổng thời lượng kèm dock", "Lên đến 25 giờ sử dụng")
        ));
        saveImages(p12, List.of("/uploads/images/seed/anc_airport.jpg"));

        // 13. Tai nghe chụp tai
        Product p13 = saveProduct(
                "Tai nghe chụp tai",
                "tai-nghe-chup-tai",
                categories.get("tai-nghe"),
                brands.get("Sony"),
                bd("1690000"),
                bd("1250000"),
                60,
                true,
                "/uploads/images/seed/headphone.jpg"
        );
        saveSpecs(p13, List.of(
                spec("Kiểu tai nghe", "Over-ear (Chụp tai không dây)"),
                spec("Kết nối", "Bluetooth 5.2 / Jack cắm Aux 3.5mm"),
                spec("Kích thước driver", "40 mm cho dải âm trầm sâu"),
                spec("Chống ồn", "Chủ động chống ồn kỹ thuật số (ANC)"),
                spec("Thời lượng pin", "Lên đến 40 giờ (Tắt ANC)"),
                spec("Cổng sạc", "USB Type-C hỗ trợ sạc nhanh")
        ));
        saveImages(p13, List.of("/uploads/images/seed/headphone.jpg"));

        // 14. Tai nghe TWS JBL
        Product p14 = saveProduct(
                "Tai nghe TWS JBL",
                "tai-nghe-tws-jbl",
                categories.get("tai-nghe"),
                brands.get("JBL"),
                bd("2490000"),
                bd("1990000"),
                40,
                true,
                "/uploads/images/seed/jbp.jpg"
        );
        saveSpecs(p14, List.of(
                spec("Kết nối không dây", "Bluetooth 5.3"),
                spec("Công nghệ âm thanh", "JBL Pure Bass Sound đặc trưng"),
                spec("Chống ồn", "Chống ồn chủ động (ANC) + Xuyên âm (Smart Ambient)"),
                spec("Kháng nước", "IP54 (Kháng nước nhẹ và bụi bẩn tốt)"),
                spec("Thời lượng pin tai nghe", "~8 giờ sử dụng liên tục"),
                spec("Tổng thời lượng kèm dock", "Lên đến 32 giờ sạc")
        ));
        saveImages(p14, List.of("/uploads/images/seed/jbp.jpg"));

        // 15. Tai nghe Redmi Buds
        Product p15 = saveProduct(
                "Tai nghe Redmi Buds",
                "tai-nghe-redmi-buds",
                categories.get("tai-nghe"),
                brands.get("Xiaomi"),
                bd("990000"),
                bd("750000"),
                90,
                true,
                "/uploads/images/seed/redmi.jpg"
        );
        saveSpecs(p15, List.of(
                spec("Kết nối không dây", "Bluetooth 5.3 độ trễ thấp"),
                spec("Kích thước driver", "12.4 mm màng loa mạ titan cực chất"),
                spec("Chống ồn", "Khử tiếng ồn chủ động thông minh bằng AI"),
                spec("Kháng nước bụi", "IP54 chuẩn thể thao"),
                spec("Thời lượng pin tai nghe", "~7 giờ nghe liên tục"),
                spec("Tổng thời lượng kèm dock", "Lên đến 38 giờ sử dụng")
        ));
        saveImages(p15, List.of("/uploads/images/seed/redmi.jpg"));
    }

    private Product saveProduct(
            String name,
            String slug,
            Category category,
            Brand brand,
            BigDecimal originalPrice,
            BigDecimal salePrice,
            int stockQty,
            boolean isFeatured,
            String thumbnailUrl
    ) {
        Product product = Product.builder()
                .name(name)
                .slug(slug)
                .description(name)
                .category(category)
                .brand(brand)
                .originalPrice(originalPrice)
                .salePrice(salePrice)
                .stockQty(stockQty)
                .soldQty(0)
                .isActive(true)
                .isFeatured(isFeatured)
                .thumbnailUrl(thumbnailUrl)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return productRepository.save(product);
    }

    private void saveSpecs(Product product, List<SpecSeed> specs) {
        List<ProductSpec> entities = new ArrayList<>();
        for (SpecSeed seed : specs) {
            entities.add(ProductSpec.builder()
                    .product(product)
                    .specKey(seed.key())
                    .specValue(seed.value())
                    .build());
        }
        productSpecRepository.saveAll(entities);
    }

    private void saveImages(Product product, List<String> urls) {
        List<ProductImage> entities = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            entities.add(ProductImage.builder()
                    .product(product)
                    .imageUrl(urls.get(i))
                    .sortOrder(i)
                    .build());
        }
        productImageRepository.saveAll(entities);
    }

    private void seedCoupons() {
        LocalDateTime now = LocalDateTime.now();

        Coupon welcome10 = Coupon.builder()
                .code("WELCOME10")
                .description("Giảm 10% cho khách hàng mới")
                .discountType(CouponDiscountType.PERCENT)
                .discountValue(bd("10"))
                .minOrderValue(BigDecimal.ZERO)
                .maxDiscount(bd("500000"))
                .usageLimit(100)
                .usedCount(0)
                .startDate(now)
                .endDate(now.plusYears(1))
                .isActive(true)
                .createdAt(now)
                .build();

        Coupon sale500k = Coupon.builder()
                .code("SALE500K")
                .description("Giảm 500K cho đơn từ 5 triệu")
                .discountType(CouponDiscountType.FIXED)
                .discountValue(bd("500000"))
                .minOrderValue(bd("5000000"))
                .maxDiscount(null)
                .usageLimit(50)
                .usedCount(0)
                .startDate(now)
                .endDate(now.plusMonths(6))
                .isActive(true)
                .createdAt(now)
                .build();

        couponRepository.save(welcome10);
        couponRepository.save(sale500k);
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private SpecSeed spec(String key, String value) {
        return new SpecSeed(key, value);
    }

    private record CategorySeed(String name, String slug, String parentSlug) {
    }

    private record SpecSeed(String key, String value) {
    }
}
