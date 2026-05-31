package com.dienmay.entity.nhom5.controller.view;

import com.dienmay.entity.nhom5.dto.request.ProductFilterRequest;
import com.dienmay.entity.nhom5.dto.response.ProductResponse;
import com.dienmay.entity.nhom5.entity.Category;
import com.dienmay.entity.nhom5.repository.BrandRepository;
import com.dienmay.entity.nhom5.repository.CategoryRepository;
import com.dienmay.entity.nhom5.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    @GetMapping({"/", ""})
    public String index(Model model) {
        Pageable pageable = PageRequest.of(0, 8, Sort.by("soldQty").descending());
        Page<ProductResponse> featured = productService.getProducts(ProductFilterRequest.builder().build(), pageable);
        List<Category> rootCategories = categoryRepository.findByParentIsNullAndIsActiveTrue();

        model.addAttribute("featuredProducts", featured.getContent());
        model.addAttribute("categories", rootCategories);
        return "pages/index";
    }

    @GetMapping("/products")
    public String products(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {

        BigDecimal min = parsePrice(minPrice);
        BigDecimal max = parsePrice(maxPrice);

        Sort sortObj = switch (sort == null ? "newest" : sort) {
            case "price_asc" -> Sort.by("originalPrice").ascending();
            case "price_desc" -> Sort.by("originalPrice").descending();
            case "popular" -> Sort.by("soldQty").descending();
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Pageable pageable = PageRequest.of(page, size, sortObj);
        ProductFilterRequest filter = ProductFilterRequest.builder()
                .categoryId(categoryId)
                .brandId(brandId)
                .minPrice(min)
                .maxPrice(max)
                .keyword(keyword)
                .build();

        model.addAttribute("products", productService.getProducts(filter, pageable));
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("brands", brandRepository.findAll());
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentPage", page);
        model.addAttribute("keyword", keyword);
        return "pages/products";
    }

    @GetMapping("/products/{slug}")
    public String productDetail(@PathVariable String slug, Model model) {
        model.addAttribute("product", productService.getProductBySlug(slug));
        return "pages/product-detail";
    }

    @GetMapping("/cart")
    public String cart() {
        return "pages/cart";
    }

    @GetMapping("/checkout")
    public String checkout() {
        return "pages/checkout";
    }

    @GetMapping("/orders")
    public String orders() {
        return "pages/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("orderId", id);
        return "pages/order-detail";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("pageTitle", "Về chúng tôi");
        return "pages/about";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("pageTitle", "Liên hệ");
        return "pages/contact";
    }

    @GetMapping("/blog")
    public String blog(Model model) {
        model.addAttribute("pageTitle", "Tin tức");
        return "pages/blog";
    }

    @GetMapping("/wishlist")
    public String wishlist(Model model) {
        model.addAttribute("pageTitle", "Danh sách yêu thích");
        return "pages/wishlist";
    }

    private BigDecimal parsePrice(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}