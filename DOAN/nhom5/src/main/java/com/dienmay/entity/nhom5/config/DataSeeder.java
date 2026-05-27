package com.dienmay.entity.nhom5.config;

import com.dienmay.entity.nhom5.entity.Brand;
import com.dienmay.entity.nhom5.entity.Category;
import com.dienmay.entity.nhom5.entity.Coupon;
import com.dienmay.entity.nhom5.entity.CouponDiscountType;
import com.dienmay.entity.nhom5.entity.Product;
import com.dienmay.entity.nhom5.entity.ProductSpec;
import com.dienmay.entity.nhom5.entity.Role;
import com.dienmay.entity.nhom5.entity.User;
import com.dienmay.entity.nhom5.repository.BrandRepository;
import com.dienmay.entity.nhom5.repository.CategoryRepository;
import com.dienmay.entity.nhom5.repository.CouponRepository;
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
                "Xiaomi", "ASUS", "HP", "Dell", "Toshiba"
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
                new CategorySeed("Điện thoại & Máy tính bảng", "dien-thoai-may-tinh-bang", null),
                new CategorySeed("Laptop & Máy tính", "laptop-may-tinh", null),
                new CategorySeed("Tivi", "tivi", null),
                new CategorySeed("Điện lạnh", "dien-lanh", null),
                new CategorySeed("Máy giặt", "may-giat", null),
                new CategorySeed("Âm thanh", "am-thanh", null)
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
                new CategorySeed("Máy tính bảng", "may-tinh-bang", "dien-thoai-may-tinh-bang"),
                new CategorySeed("Laptop", "laptop", "laptop-may-tinh"),
                new CategorySeed("Máy tính bàn", "may-tinh-ban", "laptop-may-tinh"),
                new CategorySeed("Tủ lạnh", "tu-lanh", "dien-lanh"),
                new CategorySeed("Điều hòa", "dieu-hoa", "dien-lanh"),
                new CategorySeed("Máy giặt cửa trước", "may-giat-cua-truoc", "may-giat"),
                new CategorySeed("Loa Bluetooth", "loa-bluetooth", "am-thanh")
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
        Product p1 = saveProduct(
                "Samsung Galaxy S24 Ultra",
                "samsung-galaxy-s24-ultra",
                categories.get("dien-thoai"),
                brands.get("Samsung"),
                bd("31990000"),
                bd("28990000"),
                50,
                true
        );
        saveSpecs(p1, List.of(
                spec("RAM", "12GB"),
                spec("Bộ nhớ", "256GB"),
                spec("Màn hình", "6.8 inch Dynamic AMOLED"),
                spec("Pin", "5000mAh"),
                spec("Camera", "200MP")
        ));

        Product p2 = saveProduct(
                "MacBook Air M3 13 inch",
                "macbook-air-m3-13-inch",
                categories.get("laptop"),
                brands.get("Apple"),
                bd("28990000"),
                null,
                30,
                true
        );
        saveSpecs(p2, List.of(
                spec("CPU", "Apple M3"),
                spec("RAM", "8GB"),
                spec("Ổ cứng", "256GB SSD"),
                spec("Màn hình", "13.6 inch Liquid Retina"),
                spec("Pin", "18 giờ")
        ));

        Product p3 = saveProduct(
                "Tivi LG OLED 55 inch 4K",
                "tivi-lg-oled-55-inch-4k",
                categories.get("tivi"),
                brands.get("LG"),
                bd("22990000"),
                bd("19990000"),
                15,
                false
        );
        saveSpecs(p3, List.of(
                spec("Màn hình", "55 inch OLED 4K"),
                spec("Hệ điều hành", "webOS 23"),
                spec("Kết nối", "WiFi, Bluetooth 5.0"),
                spec("Âm thanh", "2.2ch 60W")
        ));

        Product p4 = saveProduct(
                "Tủ lạnh Samsung Inverter 382L",
                "tu-lanh-samsung-inverter-382l",
                categories.get("tu-lanh"),
                brands.get("Samsung"),
                bd("12990000"),
                bd("10990000"),
                4,
                false
        );
        saveSpecs(p4, List.of(
                spec("Dung tích", "382 lít"),
                spec("Công nghệ", "Digital Inverter"),
                spec("Loại", "2 cánh"),
                spec("Điện năng", "36 kWh/tháng")
        ));

        Product p5 = saveProduct(
                "Loa JBL Charge 5",
                "loa-jbl-charge-5",
                categories.get("loa-bluetooth"),
                brands.get("Sony"),
                bd("3990000"),
                bd("3490000"),
                100,
                false
        );
        saveSpecs(p5, List.of(
                spec("Công suất", "40W"),
                spec("Pin", "20 giờ"),
                spec("Kháng nước", "IP67"),
                spec("Kết nối", "Bluetooth 5.1")
        ));
    }

    private Product saveProduct(
            String name,
            String slug,
            Category category,
            Brand brand,
            BigDecimal originalPrice,
            BigDecimal salePrice,
            int stockQty,
            boolean isFeatured
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
                .thumbnailUrl("/uploads/images/seed/" + slug + ".jpg")
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
