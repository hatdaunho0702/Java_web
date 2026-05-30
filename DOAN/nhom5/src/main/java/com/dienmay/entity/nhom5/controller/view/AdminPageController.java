package com.dienmay.entity.nhom5.controller.view;

import com.dienmay.entity.nhom5.dto.request.CreateProductRequest;
import com.dienmay.entity.nhom5.dto.response.ProductDetailResponse;
import com.dienmay.entity.nhom5.entity.OrderStatus;
import com.dienmay.entity.nhom5.repository.BrandRepository;
import com.dienmay.entity.nhom5.repository.CategoryRepository;
import com.dienmay.entity.nhom5.service.DashboardService;
import com.dienmay.entity.nhom5.service.ProductService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    private final DashboardService dashboardService;
    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    @GetMapping
    public String index() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        LocalDate now = LocalDate.now();
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("todayRevenue", dashboardService.getTodayRevenue());
        model.addAttribute("monthlyRevenue", dashboardService.getMonthlyRevenue(now.getYear(), now.getMonthValue()));
        model.addAttribute("ordersByStatus", dashboardService.countOrdersByStatus());
        model.addAttribute("topProducts", dashboardService.getTopSellingProducts(5));
        model.addAttribute("lowStockProducts", dashboardService.getLowStockProducts(5));
        model.addAttribute("recentOrders", dashboardService.getRecentOrders(5));
        return "admin/dashboard";
    }

    @GetMapping("/products")
    public String products(Model model) {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("pageTitle", "Quản lý sản phẩm");
        model.addAttribute("activePage", "products");
        model.addAttribute("products", productService.getAdminProducts(null, null, null, pageable));
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("brands", brandRepository.findAll());
        return "admin/products";
    }

    @GetMapping("/products/new")
    public String createProduct(Model model) {
        model.addAttribute("pageTitle", "Thêm sản phẩm");
        model.addAttribute("activePage", "products");
        model.addAttribute("product", new CreateProductRequest());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("brands", brandRepository.findAll());
        model.addAttribute("isEdit", false);
        return "admin/product-form";
    }

    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model) {
        ProductDetailResponse product = productService.getProductById(id);
        model.addAttribute("pageTitle", "Sửa sản phẩm");
        model.addAttribute("activePage", "products");
        model.addAttribute("product", CreateProductRequest.builder()
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .originalPrice(product.getOriginalPrice())
                .salePrice(product.getSalePrice())
                .stockQty(product.getStockQty())
                .build());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("brands", brandRepository.findAll());
        model.addAttribute("isEdit", true);
        return "admin/product-form";
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("pageTitle", "Quản lý đơn hàng");
        model.addAttribute("activePage", "orders");
        model.addAttribute("statusList", OrderStatus.values());
        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Chi tiết đơn hàng");
        model.addAttribute("activePage", "orders");
        model.addAttribute("orderId", id);
        return "admin/order-detail";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("pageTitle", "Quản lý người dùng");
        model.addAttribute("activePage", "users");
        return "admin/users";
    }

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("pageTitle", "Quản lý danh mục");
        model.addAttribute("activePage", "categories");
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/categories";
    }

    @GetMapping("/coupons")
    public String coupons(Model model) {
        model.addAttribute("pageTitle", "Quản lý khuyến mãi");
        model.addAttribute("activePage", "coupons");
        return "admin/coupons";
    }
}