package com.dienmay.entity.nhom5.controller.view;

import com.dienmay.entity.nhom5.dto.request.CreateProductRequest;
import com.dienmay.entity.nhom5.dto.response.ProductDetailResponse;
import com.dienmay.entity.nhom5.entity.OrderStatus;
import com.dienmay.entity.nhom5.repository.BrandRepository;
import com.dienmay.entity.nhom5.repository.CategoryRepository;
import com.dienmay.entity.nhom5.repository.OrderRepository;
import com.dienmay.entity.nhom5.service.DashboardService;
import com.dienmay.entity.nhom5.service.ProductService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import com.dienmay.entity.nhom5.dto.response.ContactMessageResponse;
import com.dienmay.entity.nhom5.repository.ContactMessageRepository;
import com.dienmay.entity.nhom5.service.ContactMessageService;
import org.springframework.data.domain.Page;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    private final DashboardService dashboardService;
    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final OrderRepository orderRepository;
    private final ContactMessageService contactMessageService;
    private final ContactMessageRepository contactMessageRepository;

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
    public String products(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("pageTitle", "Quản lý sản phẩm");
        model.addAttribute("activePage", "products");
        model.addAttribute("products", productService.getAdminProducts(keyword, categoryId, isActive, pageable));
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("brands", brandRepository.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("isActive", isActive);
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
        try {
            ProductDetailResponse product = productService.getProductById(id);

            model.addAttribute("activePage", "products");
            model.addAttribute("pageTitle", "Sửa sản phẩm — " + product.getName());
            model.addAttribute("product", product);
            model.addAttribute("isEdit", true);
            model.addAttribute("productId", id);

            // Load danh mục + thương hiệu
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("brands", brandRepository.findAll());

            return "admin/product-form";

        } catch (com.dienmay.entity.nhom5.exception.ResourceNotFoundException e) {
            return "redirect:/admin/products";
        } catch (Exception e) {
            return "redirect:/admin/products";
        }
    }

    @PostMapping("/products")
    public String saveProduct(
            @ModelAttribute CreateProductRequest req,
            @RequestParam(value = "specKeys", required = false) List<String> specKeys,
            @RequestParam(value = "specValues", required = false) List<String> specValues,
            @RequestParam("images") MultipartFile[] images,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            List<com.dienmay.entity.nhom5.dto.request.ProductSpecRequest> specs = new java.util.ArrayList<>();
            if (specKeys != null && specValues != null) {
                for (int i = 0; i < Math.min(specKeys.size(), specValues.size()); i++) {
                    String k = specKeys.get(i);
                    String v = specValues.get(i);
                    if (k != null && !k.isBlank() && v != null && !v.isBlank()) {
                        specs.add(com.dienmay.entity.nhom5.dto.request.ProductSpecRequest.builder()
                                .key(k.trim())
                                .value(v.trim())
                                .build());
                    }
                }
            }
            req.setSpecs(specs);

            productService.createProduct(req, images);
            redirectAttributes.addFlashAttribute("toastMessage", "Thêm sản phẩm thành công!");
            redirectAttributes.addFlashAttribute("toastType", "success");
            return "redirect:/admin/products";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("toastMessage", "Lỗi: " + e.getMessage());
            redirectAttributes.addFlashAttribute("toastType", "danger");
            return "redirect:/admin/products/new";
        }
    }

    @PostMapping("/products/{id}/edit")
    public String updateProduct(
            @PathVariable Long id,
            @ModelAttribute CreateProductRequest req,
            @RequestParam(value = "specKeys", required = false) List<String> specKeys,
            @RequestParam(value = "specValues", required = false) List<String> specValues,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "deleteImageIds", required = false) List<Long> deleteImageIds,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            List<com.dienmay.entity.nhom5.dto.request.ProductSpecRequest> specs = new java.util.ArrayList<>();
            if (specKeys != null && specValues != null) {
                for (int i = 0; i < Math.min(specKeys.size(), specValues.size()); i++) {
                    String k = specKeys.get(i);
                    String v = specValues.get(i);
                    if (k != null && !k.isBlank() && v != null && !v.isBlank()) {
                        specs.add(com.dienmay.entity.nhom5.dto.request.ProductSpecRequest.builder()
                                .key(k.trim())
                                .value(v.trim())
                                .build());
                    }
                }
            }
            req.setSpecs(specs);

            productService.updateProductFromForm(id, req, images, deleteImageIds);
            redirectAttributes.addFlashAttribute("toastMessage", "Cập nhật sản phẩm thành công!");
            redirectAttributes.addFlashAttribute("toastType", "success");
            return "redirect:/admin/products";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("toastMessage", "Lỗi: " + e.getMessage());
            redirectAttributes.addFlashAttribute("toastType", "danger");
            return "redirect:/admin/products/" + id + "/edit";
        }
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
        model.addAttribute("order", orderRepository.findById(id).orElse(null));
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

    @GetMapping("/contact")
    public String contact(Model model) {
        Page<ContactMessageResponse> messages = contactMessageService.getAll(null, PageRequest.of(0, 20));
        model.addAttribute("messages", messages);
        model.addAttribute("activePage", "contact");
        model.addAttribute("pageTitle", "Tin nhắn liên hệ");
        model.addAttribute("unreadCount", contactMessageRepository.countUnread());
        return "admin/contact";
    }

    @GetMapping("/reviews")
    public String reviews(Model model) {
        model.addAttribute("pageTitle", "Quản lý đánh giá");
        model.addAttribute("activePage", "reviews");
        return "admin/reviews";
    }
}