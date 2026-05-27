package com.dienmay.entity.nhom5.service;

import com.dienmay.entity.nhom5.dto.request.OrderItemRequest;
import com.dienmay.entity.nhom5.dto.request.PlaceOrderRequest;
import com.dienmay.entity.nhom5.dto.response.OrderResponse;
import com.dienmay.entity.nhom5.entity.Coupon;
import com.dienmay.entity.nhom5.entity.CouponDiscountType;
import com.dienmay.entity.nhom5.entity.InventoryLog;
import com.dienmay.entity.nhom5.entity.InventoryReason;
import com.dienmay.entity.nhom5.entity.Notification;
import com.dienmay.entity.nhom5.entity.NotificationType;
import com.dienmay.entity.nhom5.entity.Order;
import com.dienmay.entity.nhom5.entity.OrderItem;
import com.dienmay.entity.nhom5.entity.OrderStatus;
import com.dienmay.entity.nhom5.entity.OrderStatusHistory;
import com.dienmay.entity.nhom5.entity.PaymentMethod;
import com.dienmay.entity.nhom5.entity.PaymentStatus;
import com.dienmay.entity.nhom5.entity.Product;
import com.dienmay.entity.nhom5.entity.TargetRole;
import com.dienmay.entity.nhom5.entity.User;
import com.dienmay.entity.nhom5.exception.BadRequestException;
import com.dienmay.entity.nhom5.exception.ResourceNotFoundException;
import com.dienmay.entity.nhom5.repository.CartItemRepository;
import com.dienmay.entity.nhom5.repository.CouponRepository;
import com.dienmay.entity.nhom5.repository.InventoryLogRepository;
import com.dienmay.entity.nhom5.repository.NotificationRepository;
import com.dienmay.entity.nhom5.repository.OrderItemRepository;
import com.dienmay.entity.nhom5.repository.OrderRepository;
import com.dienmay.entity.nhom5.repository.OrderStatusHistoryRepository;
import com.dienmay.entity.nhom5.repository.ProductRepository;
import com.dienmay.entity.nhom5.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final NotificationRepository notificationRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;

    @Value("${app.inventory.low-stock-threshold:5}")
    private int lowStockThreshold;

    @Transactional
    public OrderResponse placeOrder(String userId, PlaceOrderRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Đơn hàng phải có ít nhất một sản phẩm");
        }

        User user = userRepository.findByUid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        List<OrderLine> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            if (itemRequest == null || itemRequest.getProductId() == null || itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
                throw new BadRequestException("Thông tin sản phẩm đặt hàng không hợp lệ");
            }

            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

            if (!Boolean.TRUE.equals(product.getIsActive())) {
                throw new BadRequestException("Sản phẩm " + product.getName() + " đang tạm ngưng bán");
            }

            if (product.getStockQty() < itemRequest.getQuantity()) {
                throw new BadRequestException("Sản phẩm " + product.getName() + " không đủ hàng");
            }

            BigDecimal unitPrice = product.getSalePrice() != null ? product.getSalePrice() : product.getOriginalPrice();
            BigDecimal lineSubtotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            lines.add(new OrderLine(product, itemRequest.getQuantity(), unitPrice, lineSubtotal));
            subtotal = subtotal.add(lineSubtotal);
        }

        Coupon coupon = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            coupon = couponRepository.findByCode(request.getCouponCode())
                    .orElseThrow(() -> new BadRequestException("Mã giảm giá không tồn tại"));
            LocalDateTime now = LocalDateTime.now();
            if (!Boolean.TRUE.equals(coupon.getIsActive())) {
                throw new BadRequestException("Mã giảm giá đã bị vô hiệu hóa");
            }
            if (coupon.getStartDate().isAfter(now) || coupon.getEndDate().isBefore(now)) {
                throw new BadRequestException("Mã giảm giá không nằm trong thời gian áp dụng");
            }
            if (coupon.getMinOrderValue() != null && subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
                throw new BadRequestException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã giảm giá");
            }
            if (coupon.getUsageLimit() != null && coupon.getUsedCount() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
                throw new BadRequestException("Mã giảm giá đã hết lượt sử dụng");
            }
            discountAmount = calculateDiscount(coupon, subtotal);
        }

        BigDecimal totalAmount = subtotal.subtract(discountAmount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .user(user)
                .coupon(coupon)
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .shippingAddress(request.getShippingAddress())
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .shippingFee(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod() == null ? PaymentMethod.COD : request.getPaymentMethod())
                .paymentStatus(PaymentStatus.UNPAID)
                .note(request.getNote())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        order = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderLine line : lines) {
            int affectedRows = productRepository.decreaseStock(line.product().getId(), line.quantity());
            if (affectedRows == 0) {
                throw new BadRequestException("Sản phẩm " + line.product().getName() + " không đủ hàng");
            }
            productRepository.increaseSoldQty(line.product().getId(), line.quantity());

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(line.product())
                    .productName(line.product().getName())
                    .productImage(line.product().getThumbnailUrl())
                    .unitPrice(line.unitPrice())
                    .quantity(line.quantity())
                    .subtotal(line.subtotal())
                    .build();
            orderItems.add(orderItem);

            inventoryLogRepository.save(InventoryLog.builder()
                    .product(line.product())
                    .changeQty(-line.quantity())
                    .reason(InventoryReason.PURCHASE)
                    .refId(order.getId())
                    .note("Đặt hàng: " + order.getOrderCode())
                    .createdBy(user)
                    .createdAt(LocalDateTime.now())
                    .build());

            Product latest = productRepository.findById(line.product().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
            if (latest.getStockQty() < lowStockThreshold) {
                notificationRepository.save(Notification.builder()
                        .type(NotificationType.LOW_STOCK)
                        .title("Cảnh báo tồn kho thấp")
                        .message("Sản phẩm " + latest.getName() + " sắp hết hàng")
                        .targetRole(TargetRole.ADMIN)
                        .isRead(false)
                        .refId(latest.getId())
                        .createdAt(LocalDateTime.now())
                        .build());
            }
        }
        orderItemRepository.saveAll(orderItems);

        if (coupon != null) {
            coupon.setUsedCount((coupon.getUsedCount() == null ? 0 : coupon.getUsedCount()) + 1);
            couponRepository.save(coupon);
        }

        cartItemRepository.deleteByUser_Uid(userId);

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .oldStatus(null)
                .newStatus(OrderStatus.PENDING.name())
                .changedBy(null)
                .note("Tạo đơn hàng")
                .changedAt(LocalDateTime.now())
                .build());

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .build();
    }

    @Transactional
    public void cancelOrder(Long orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        if (!order.getUser().getUid().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền hủy đơn hàng này");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể hủy đơn hàng đang chờ xác nhận");
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            productRepository.increaseStock(item.getProduct().getId(), item.getQuantity());
            productRepository.decreaseSoldQty(item.getProduct().getId(), item.getQuantity());

            inventoryLogRepository.save(InventoryLog.builder()
                    .product(item.getProduct())
                    .changeQty(item.getQuantity())
                    .reason(InventoryReason.RETURN)
                    .refId(order.getId())
                    .note("Hủy đơn hàng: " + order.getOrderCode())
                    .createdBy(order.getUser())
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .oldStatus(oldStatus.name())
                .newStatus(OrderStatus.CANCELLED.name())
                .changedBy(order.getUser())
                .note("Khách hàng hủy đơn")
                .changedAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus, String adminId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        OrderStatus oldStatus = order.getStatus();
        if (!isValidTransition(oldStatus, newStatus)) {
            throw new BadRequestException("Không thể chuyển trạng thái từ " + oldStatus + " sang " + newStatus);
        }

        User admin = userRepository.findByUid(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản quản trị"));

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .oldStatus(oldStatus.name())
                .newStatus(newStatus.name())
                .changedBy(admin)
                .note("Admin cập nhật trạng thái")
                .changedAt(LocalDateTime.now())
                .build());
    }

    private boolean isValidTransition(OrderStatus oldStatus, OrderStatus newStatus) {
        return (oldStatus == OrderStatus.PENDING && newStatus == OrderStatus.CONFIRMED)
                || (oldStatus == OrderStatus.CONFIRMED && newStatus == OrderStatus.SHIPPING)
                || (oldStatus == OrderStatus.SHIPPING && newStatus == OrderStatus.COMPLETED);
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        if (coupon.getDiscountType() == CouponDiscountType.FIXED) {
            return coupon.getDiscountValue().min(subtotal);
        }

        BigDecimal percentDiscount = subtotal
                .multiply(coupon.getDiscountValue())
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

        if (coupon.getMaxDiscount() != null) {
            percentDiscount = percentDiscount.min(coupon.getMaxDiscount());
        }
        return percentDiscount.min(subtotal);
    }

    private String generateOrderCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase(Locale.ROOT);
        return "DM-" + datePart + "-" + randomPart;
    }

    private record OrderLine(Product product, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }
}
