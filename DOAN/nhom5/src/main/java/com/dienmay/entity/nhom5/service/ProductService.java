package com.dienmay.entity.nhom5.service;

import com.dienmay.entity.nhom5.dto.request.CreateProductRequest;
import com.dienmay.entity.nhom5.dto.request.CreateReviewRequest;
import com.dienmay.entity.nhom5.dto.request.ProductFilterRequest;
import com.dienmay.entity.nhom5.dto.request.ProductSpecRequest;
import com.dienmay.entity.nhom5.dto.response.ProductDetailResponse;
import com.dienmay.entity.nhom5.dto.response.ProductResponse;
import com.dienmay.entity.nhom5.dto.response.ReviewDto;
import com.dienmay.entity.nhom5.dto.response.ProductSpecDto;
import com.dienmay.entity.nhom5.entity.Brand;
import com.dienmay.entity.nhom5.entity.Category;
import com.dienmay.entity.nhom5.entity.OrderItem;
import com.dienmay.entity.nhom5.entity.Product;
import com.dienmay.entity.nhom5.entity.ProductImage;
import com.dienmay.entity.nhom5.entity.ProductSpec;
import com.dienmay.entity.nhom5.entity.Review;
import com.dienmay.entity.nhom5.entity.ReviewStatus;
import com.dienmay.entity.nhom5.entity.User;
import com.dienmay.entity.nhom5.exception.BadRequestException;
import com.dienmay.entity.nhom5.exception.ResourceNotFoundException;
import com.dienmay.entity.nhom5.repository.BrandRepository;
import com.dienmay.entity.nhom5.repository.CategoryRepository;
import com.dienmay.entity.nhom5.repository.OrderItemRepository;
import com.dienmay.entity.nhom5.repository.ProductImageRepository;
import com.dienmay.entity.nhom5.repository.ProductRepository;
import com.dienmay.entity.nhom5.repository.ProductSpecRepository;
import com.dienmay.entity.nhom5.repository.ReviewRepository;
import com.dienmay.entity.nhom5.repository.UserRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductSpecRepository productSpecRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    @Value("${app.storage.product-image-dir:./uploads/products}")
    private String productImageDir;

    public Page<ProductResponse> getProducts(ProductFilterRequest filter, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isActive")));

            if (filter != null) {
                if (filter.getCategoryId() != null) {
                    predicates.add(cb.equal(root.get("category").get("id"), filter.getCategoryId()));
                }
                if (filter.getCategoryIds() != null && !filter.getCategoryIds().isEmpty()) {
                    predicates.add(root.get("category").get("id").in(filter.getCategoryIds()));
                }
                if (filter.getBrandId() != null) {
                    predicates.add(cb.equal(root.get("brand").get("id"), filter.getBrandId()));
                }
                if (filter.getBrandIds() != null && !filter.getBrandIds().isEmpty()) {
                    predicates.add(root.get("brand").get("id").in(filter.getBrandIds()));
                }
                if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                    predicates.add(cb.like(
                            cb.lower(root.get("name")),
                            "%" + filter.getKeyword().trim().toLowerCase(Locale.ROOT) + "%"
                    ));
                }
                if (filter.getMinPrice() != null || filter.getMaxPrice() != null) {
                    Expression<BigDecimal> priceExpr = cb.coalesce(root.get("salePrice"), root.get("originalPrice"));
                    if (filter.getMinPrice() != null) {
                        predicates.add(cb.greaterThanOrEqualTo(priceExpr, filter.getMinPrice()));
                    }
                    if (filter.getMaxPrice() != null) {
                        predicates.add(cb.lessThanOrEqualTo(priceExpr, filter.getMaxPrice()));
                    }
                }

                String sort = filter.getSort() == null ? "newest" : filter.getSort();
                switch (sort) {
                    case "price_asc" -> query.orderBy(cb.asc(cb.coalesce(root.get("salePrice"), root.get("originalPrice"))));
                    case "price_desc" -> query.orderBy(cb.desc(cb.coalesce(root.get("salePrice"), root.get("originalPrice"))));
                    case "popular" -> query.orderBy(cb.desc(root.get("soldQty")), cb.desc(root.get("createdAt")));
                    default -> query.orderBy(cb.desc(root.get("createdAt")));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return productRepository.findAll(spec, pageable).map(this::toProductResponse);
    }

    public Map<String, Object> getProductsPayload(ProductFilterRequest filter, Pageable pageable) {
        Page<ProductResponse> page = getProducts(filter, pageable);

        List<Map<String, Object>> categories = categoryRepository.findAllByIsActiveTrue().stream()
                .map(cat -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", cat.getId());
                    item.put("name", cat.getName());
                    return item;
                })
                .toList();

        List<Map<String, Object>> brands = brandRepository.findAllByIsActiveTrueOrderByNameAsc().stream()
                .map(brand -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", brand.getId());
                    item.put("name", brand.getName());
                    return item;
                })
                .toList();

        BigDecimal minPrice = Objects.requireNonNullElse(productRepository.findMinEffectivePriceForActive(), BigDecimal.ZERO);
        BigDecimal maxPrice = Objects.requireNonNullElse(productRepository.findMaxEffectivePriceForActive(), BigDecimal.ZERO);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("content", page.getContent());
        payload.put("totalElements", page.getTotalElements());
        payload.put("totalPages", page.getTotalPages());
        payload.put("currentPage", page.getNumber());
        payload.put("filters", Map.of(
                "categories", categories,
                "brands", brands,
                "priceRange", Map.of("min", minPrice, "max", maxPrice)
        ));
        return payload;
    }

        public Map<String, Object> getFeaturedProducts() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("newArrivals", productRepository
            .findByIsActiveTrueOrderByCreatedAtDesc(Pageable.ofSize(8))
            .map(this::toProductResponse)
            .getContent());
        response.put("onSale", productRepository
            .findByIsActiveTrueAndSalePriceIsNotNull(Pageable.ofSize(8))
            .map(this::toProductResponse)
            .getContent());
        response.put("bestSeller", productRepository
            .findByIsActiveTrueOrderBySoldQtyDesc(Pageable.ofSize(8))
            .map(this::toProductResponse)
            .getContent());

        List<Map<String, Object>> categories = categoryRepository.findActiveCategoriesHavingActiveProducts()
            .stream()
            .map(cat -> {
                Map<String, Object> c = new LinkedHashMap<>();
                c.put("id", cat.getId());
                c.put("name", cat.getName());
                c.put("slug", cat.getSlug());
                c.put("iconUrl", cat.getIconUrl());
                return c;
            })
            .toList();
        response.put("categories", categories);
        return response;
        }

    public Page<ProductResponse> getAdminProducts(String keyword, Long categoryId, Boolean isActive, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%"
                ));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return productRepository.findAll(spec, pageable).map(this::toProductResponse);
    }

    public ProductDetailResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));
        return toProductDetailResponse(product);
    }

    public ProductDetailResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với slug: " + slug));

        return toProductDetailResponse(product);
    }

    public Page<ReviewDto> getProductReviews(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId);
        }
        return reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(productId, ReviewStatus.APPROVED, pageable)
                .map(this::toReviewDto);
    }

    @Transactional
    public ReviewDto createReview(Long productId, String firebaseUid, CreateReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        List<OrderItem> purchasedItems = orderItemRepository.findPurchasedItemsByUserAndProduct(user.getUid(), productId);
        if (purchasedItems.isEmpty()) {
            throw new BadRequestException("Bạn chỉ có thể đánh giá sau khi mua sản phẩm");
        }

        OrderItem eligibleItem = purchasedItems.stream()
                .filter(item -> !reviewRepository.existsByProductIdAndUser_UidAndOrderId(productId, user.getUid(), item.getOrder().getId()))
                .findFirst()
                .orElse(null);

        if (eligibleItem == null) {
            throw new BadRequestException("Bạn đã đánh giá sản phẩm này trước đó");
        }

        Review review = reviewRepository.save(Review.builder()
                .product(product)
                .user(user)
                .order(eligibleItem.getOrder())
                .rating(request.getRating())
                .comment(request.getComment().trim())
                .status(ReviewStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build());

        return toReviewDto(review);
    }

    private ProductDetailResponse toProductDetailResponse(Product product) {

        List<String> imageUrls = productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId())
                .stream()
                .map(ProductImage::getImageUrl)
                .toList();

        List<ProductSpecDto> specs = productSpecRepository.findByProductId(product.getId())
                .stream()
                .map(spec -> ProductSpecDto.builder()
                        .key(spec.getSpecKey())
                        .value(spec.getSpecValue())
                        .build())
                .toList();

        Double avgRating = reviewRepository.findAverageRatingByProductIdAndStatus(product.getId(), ReviewStatus.APPROVED);
        if (avgRating == null) {
            avgRating = 0.0;
        }

        int reviewCount = (int) reviewRepository.countByProductIdAndStatus(product.getId(), ReviewStatus.APPROVED);
        List<ReviewDto> reviews = reviewRepository
            .findByProductIdAndStatusOrderByCreatedAtDesc(product.getId(), ReviewStatus.APPROVED, PageRequest.of(0, 5))
            .stream()
            .map(this::toReviewDto)
            .toList();
        List<ProductResponse> related = productRepository
            .findByIsActiveTrueAndCategory_IdAndIdNotOrderByCreatedAtDesc(
                product.getCategory().getId(),
                product.getId(),
                PageRequest.of(0, 4)
            )
            .map(this::toProductResponse)
            .getContent();

        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .originalPrice(product.getOriginalPrice())
                .salePrice(product.getSalePrice())
                .stockQty(product.getStockQty())
            .active(Boolean.TRUE.equals(product.getIsActive()))
                .thumbnailUrl(product.getThumbnailUrl())
                .avgRating(avgRating)
            .averageRating(avgRating)
            .reviewCount(reviewCount)
            .categoryName(product.getCategory() == null ? null : product.getCategory().getName())
            .brandName(product.getBrand() == null ? null : product.getBrand().getName())
            .categoryId(product.getCategory() == null ? null : product.getCategory().getId())
            .brandId(product.getBrand() == null ? null : product.getBrand().getId())
                .imageUrls(imageUrls)
                .specs(specs)
            .reviews(reviews)
            .related(related)
                .build();
    }

        private ReviewDto toReviewDto(Review review) {
        return ReviewDto.builder()
            .id(review.getId())
            .rating(review.getRating())
            .comment(review.getComment())
            .userName(review.getUser() == null ? "Khách hàng" : review.getUser().getFullName())
            .userAvatarUrl(review.getUser() == null ? null : review.getUser().getAvatarUrl())
            .createdAt(review.getCreatedAt())
            .build();
        }

    @Transactional
    public ProductDetailResponse createProduct(CreateProductRequest req, MultipartFile[] images) {
        validateProductRequest(req);

        if (productRepository.findBySlug(req.getSlug()).isPresent()) {
            throw new BadRequestException("Slug sản phẩm đã tồn tại trong hệ thống");
        }

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
        Brand brand = brandRepository.findById(req.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu"));

        Product product = Product.builder()
                .name(req.getName().trim())
                .slug(req.getSlug())
                .description(req.getDescription())
                .category(category)
                .brand(brand)
                .originalPrice(req.getOriginalPrice())
                .salePrice(req.getSalePrice())
                .stockQty(req.getStockQty() == null ? 0 : req.getStockQty())
                .soldQty(0)
                .isActive(true)
                .isFeatured(Boolean.TRUE.equals(req.getIsFeatured()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        product = productRepository.save(product);

        saveImages(product, images);
        saveSpecs(product, req.getSpecs());

        return getProductBySlug(product.getSlug());
    }

    @Transactional
    public ProductDetailResponse updateProduct(Long id, CreateProductRequest req, MultipartFile[] newImages) {
        validateProductRequest(req);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        if (productRepository.findBySlug(req.getSlug()).stream().anyMatch(p -> !p.getId().equals(id))) {
            throw new BadRequestException("Slug sản phẩm đã tồn tại trong hệ thống");
        }

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
        Brand brand = brandRepository.findById(req.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu"));

        product.setName(req.getName().trim());
        product.setSlug(req.getSlug());
        product.setDescription(req.getDescription());
        product.setCategory(category);
        product.setBrand(brand);
        product.setOriginalPrice(req.getOriginalPrice());
        product.setSalePrice(req.getSalePrice());
        product.setStockQty(req.getStockQty() == null ? product.getStockQty() : req.getStockQty());
        product.setIsFeatured(Boolean.TRUE.equals(req.getIsFeatured()));
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);

        if (newImages != null && newImages.length > 0) {
            productImageRepository.deleteByProductId(product.getId());
            product.setThumbnailUrl(null);
            saveImages(product, newImages);
        }

        if (req.getSpecs() != null) {
            productSpecRepository.deleteByProductId(product.getId());
            saveSpecs(product, req.getSpecs());
        }

        return getProductBySlug(product.getSlug());
    }

    @Transactional
    public void toggleActive(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        product.setIsActive(!Boolean.TRUE.equals(product.getIsActive()));
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
    }

    private void validateProductRequest(CreateProductRequest req) {
        if (req == null) {
            throw new BadRequestException("Dữ liệu sản phẩm không hợp lệ");
        }
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BadRequestException("Tên sản phẩm không được để trống");
        }
        if (req.getName().trim().length() > 255) {
            throw new BadRequestException("Tên sản phẩm không được vượt quá 255 ký tự");
        }
        if (req.getOriginalPrice() == null || req.getOriginalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Giá gốc phải lớn hơn 0");
        }
        if (req.getSalePrice() != null && req.getSalePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Giá khuyến mãi không được âm");
        }
        if (req.getSalePrice() != null && req.getSalePrice().compareTo(req.getOriginalPrice()) >= 0) {
            throw new BadRequestException("Giá khuyến mãi phải nhỏ hơn giá gốc");
        }
        if (req.getCategoryId() == null || req.getBrandId() == null) {
            throw new BadRequestException("Danh mục và thương hiệu là bắt buộc");
        }
        if (req.getStockQty() != null && req.getStockQty() < 0) {
            throw new BadRequestException("Số lượng tồn kho phải lớn hơn hoặc bằng 0");
        }
    }

    private void saveImages(Product product, MultipartFile[] images) {
        if (images == null || images.length == 0) {
            return;
        }

        List<ProductImage> savedImages = new ArrayList<>();
        Path basePath = Paths.get(productImageDir, String.valueOf(product.getId()));
        try {
            Files.createDirectories(basePath);
            for (int i = 0; i < images.length; i++) {
                MultipartFile image = images[i];
                if (image == null || image.isEmpty()) {
                    continue;
                }
                String originalName = image.getOriginalFilename() == null ? "image" : image.getOriginalFilename();
                
                // Validate image content type / extension
                String contentType = image.getContentType();
                if (contentType != null) {
                    if (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && 
                        !contentType.equals("image/webp") && !contentType.equals("image/gif")) {
                        throw new BadRequestException("Chỉ chấp nhận ảnh định dạng .jpg, .jpeg, .png, .webp");
                    }
                } else {
                    String ext = "";
                    int dotIndex = originalName.lastIndexOf(".");
                    if (dotIndex >= 0) {
                        ext = originalName.substring(dotIndex).toLowerCase();
                    }
                    if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png") && !ext.equals(".webp")) {
                        throw new BadRequestException("Chỉ chấp nhận ảnh định dạng .jpg, .jpeg, .png, .webp");
                    }
                }

                String fileName = UUID.randomUUID() + "-" + originalName.replace(" ", "_");
                Path target = basePath.resolve(fileName);
                Files.copy(image.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

                String imageUrl = target.toString().replace("\\", "/");
                if (imageUrl.startsWith("./")) {
                    imageUrl = imageUrl.substring(1);
                }
                if (!imageUrl.startsWith("/")) {
                    imageUrl = "/" + imageUrl;
                }

                ProductImage productImage = ProductImage.builder()
                        .product(product)
                        .imageUrl(imageUrl)
                        .sortOrder(i)
                        .build();
                savedImages.add(productImage);
            }
        } catch (IOException e) {
            throw new BadRequestException("Không thể lưu ảnh sản phẩm: " + e.getMessage());
        }

        if (!savedImages.isEmpty()) {
            productImageRepository.saveAll(savedImages);
            if (product.getThumbnailUrl() == null || product.getThumbnailUrl().isBlank()) {
                product.setThumbnailUrl(savedImages.get(0).getImageUrl());
                productRepository.save(product);
            }
        }
    }

    private void saveSpecs(Product product, List<ProductSpecRequest> specs) {
        if (specs == null || specs.isEmpty()) {
            return;
        }

        List<ProductSpec> entities = new ArrayList<>();
        for (ProductSpecRequest spec : specs) {
            if (spec == null || spec.getKey() == null || spec.getKey().isBlank() || spec.getValue() == null) {
                continue;
            }
            entities.add(ProductSpec.builder()
                    .product(product)
                    .specKey(spec.getKey().trim())
                    .specValue(spec.getValue().trim())
                    .build());
        }
        if (!entities.isEmpty()) {
            productSpecRepository.saveAll(entities);
        }
    }

    private ProductResponse toProductResponse(Product product) {
        Double avgRating = reviewRepository.findAverageRatingByProductIdAndStatus(product.getId(), ReviewStatus.APPROVED);
        if (avgRating == null) {
            avgRating = 0.0;
        }
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .originalPrice(product.getOriginalPrice())
                .salePrice(product.getSalePrice())
                .thumbnailUrl(product.getThumbnailUrl())
                .avgRating(avgRating)
                .stockQty(product.getStockQty())
                .soldQty(product.getSoldQty())
                .build();
    }
}
