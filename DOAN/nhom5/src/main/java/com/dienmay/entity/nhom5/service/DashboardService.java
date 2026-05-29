package com.dienmay.entity.nhom5.service;

import com.dienmay.entity.nhom5.dto.response.ProductResponse;
import com.dienmay.entity.nhom5.entity.OrderStatus;
import com.dienmay.entity.nhom5.entity.Product;
import com.dienmay.entity.nhom5.entity.ReviewStatus;
import com.dienmay.entity.nhom5.repository.OrderRepository;
import com.dienmay.entity.nhom5.repository.ProductRepository;
import com.dienmay.entity.nhom5.repository.ReviewRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    @Value("${app.inventory.low-stock-threshold:5}")
    private int defaultLowStockThreshold;

    public BigDecimal getTodayRevenue() {
        LocalDate today = LocalDate.now();
        return orderRepository.sumRevenueByStatusBetween(
                OrderStatus.COMPLETED,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
    }

    public BigDecimal getMonthlyRevenue(int year, int month) {
        LocalDateTime from = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime to = from.plusMonths(1);
        return orderRepository.sumRevenueByStatusBetween(OrderStatus.COMPLETED, from, to);
    }

    public Map<OrderStatus, Long> countOrdersByStatus() {
        Map<OrderStatus, Long> result = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            result.put(status, 0L);
        }

        List<Object[]> rows = orderRepository.countOrdersByStatusGroup();
        for (Object[] row : rows) {
            OrderStatus status = (OrderStatus) row[0];
            Long count = (Long) row[1];
            result.put(status, count);
        }
        return result;
    }

    public List<ProductResponse> getTopSellingProducts(int limit) {
        // Thêm .getContent() để lấy List<Product> từ Page<Product> trước khi stream
        return productRepository.findByIsActiveTrueOrderBySoldQtyDesc(PageRequest.of(0, limit))
                .getContent()
                .stream()
                .map(this::toProductResponse)
                .toList();
    }

    public List<ProductResponse> getLowStockProducts(int threshold) {
        int stockThreshold = threshold > 0 ? threshold : defaultLowStockThreshold;
        return productRepository.findByIsActiveTrueAndStockQtyLessThanEqual(stockThreshold)
                .stream()
                .map(this::toProductResponse)
                .toList();
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