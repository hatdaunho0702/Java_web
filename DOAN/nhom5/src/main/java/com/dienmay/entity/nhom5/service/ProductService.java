package com.dienmay.entity.nhom5.service;

import com.dienmay.entity.nhom5.dto.request.CreateProductRequest;
import com.dienmay.entity.nhom5.dto.request.ProductFilterRequest;
import com.dienmay.entity.nhom5.dto.request.ProductSpecRequest;
import com.dienmay.entity.nhom5.dto.response.ProductDetailResponse;
import com.dienmay.entity.nhom5.dto.response.ProductResponse;
import com.dienmay.entity.nhom5.dto.response.ProductSpecDto;
import com.dienmay.entity.nhom5.entity.Brand;
import com.dienmay.entity.nhom5.entity.Category;
import com.dienmay.entity.nhom5.entity.Product;
import com.dienmay.entity.nhom5.entity.ProductImage;
import com.dienmay.entity.nhom5.entity.ProductSpec;
import com.dienmay.entity.nhom5.entity.ReviewStatus;
import com.dienmay.entity.nhom5.exception.BadRequestException;
import com.dienmay.entity.nhom5.exception.ResourceNotFoundException;
import com.dienmay.entity.nhom5.repository.BrandRepository;
import com.dienmay.entity.nhom5.repository.CategoryRepository;
import com.dienmay.entity.nhom5.repository.ProductImageRepository;
import com.dienmay.entity.nhom5.repository.ProductRepository;
import com.dienmay.entity.nhom5.repository.ProductSpecRepository;
import com.dienmay.entity.nhom5.repository.ReviewRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Value("${app.storage.product-image-dir:./uploads/images}")
    private String productImageDir;

    public Page<ProductResponse> getProducts(ProductFilterRequest filter, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isActive")));

            if (filter != null) {
                if (filter.getCategoryId() != null) {
                    predicates.add(cb.equal(root.get("category").get("id"), filter.getCategoryId()));
                }
                if (filter.getBrandId() != null) {
                    predicates.add(cb.equal(root.get("brand").get("id"), filter.getBrandId()));
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
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return productRepository.findAll(spec, pageable).map(this::toProductResponse);
    }

    public ProductDetailResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với slug: " + slug));

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

        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .originalPrice(product.getOriginalPrice())
                .salePrice(product.getSalePrice())
                .stockQty(product.getStockQty())
                .thumbnailUrl(product.getThumbnailUrl())
                .avgRating(avgRating)
                .imageUrls(imageUrls)
                .specs(specs)
                .build();
    }

    @Transactional
    public ProductDetailResponse createProduct(CreateProductRequest req, MultipartFile[] images) {
        validateProductRequest(req);

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
        if (req.getOriginalPrice() == null || req.getOriginalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Giá gốc phải lớn hơn 0");
        }
        if (req.getSalePrice() != null && req.getSalePrice().compareTo(req.getOriginalPrice()) >= 0) {
            throw new BadRequestException("Giá khuyến mãi phải nhỏ hơn giá gốc");
        }
        if (req.getCategoryId() == null || req.getBrandId() == null) {
            throw new BadRequestException("Danh mục và thương hiệu là bắt buộc");
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
                String fileName = UUID.randomUUID() + "-" + originalName.replace(" ", "_");
                Path target = basePath.resolve(fileName);
                Files.copy(image.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

                ProductImage productImage = ProductImage.builder()
                        .product(product)
                        .imageUrl(target.toString().replace("\\", "/"))
                        .sortOrder(i)
                        .build();
                savedImages.add(productImage);
            }
        } catch (IOException e) {
            throw new BadRequestException("Không thể lưu ảnh sản phẩm");
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
                .build();
    }
}
