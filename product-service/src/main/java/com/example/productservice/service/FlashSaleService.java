package com.example.productservice.service;

import com.example.productservice.dto.PurchaseResponse;
import com.example.productservice.entity.Order;
import com.example.productservice.entity.Product;
import com.example.productservice.repository.OrderRepository;
import com.example.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final RedissonClient redissonClient;

    /**
     * Mua sản phẩm Flash Sale với khóa phân tán Redisson Lock
     * Khóa phân tán: lock:product:{id}
     */
    public PurchaseResponse purchaseProduct(Long productId, String userId, int quantity) {
        String lockKey = "lock:product:" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        log.info("==> [User: {}] Đang cố gắng giành khóa phân tán '{}'...", userId, lockKey);

        try {
            // Chờ tối đa 5 giây để lấy khóa, tự động nhả sau 10 giây nếu service gặp sự cố
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);

            if (!acquired) {
                log.warn("==> [User: {}] Không thể lấy được khóa '{}' (Hệ thống quá tải)", userId, lockKey);
                return PurchaseResponse.builder()
                        .success(false)
                        .message("Hệ thống đang bận xử lý giao dịch khác, vui lòng thử lại sau!")
                        .remainingStock(null)
                        .orderId(null)
                        .build();
            }

            log.info("🔒 [User: {}] ĐÃ GIÀNH ĐƯỢC KHÓA '{}'. Bắt đầu kiểm tra tồn kho...", userId, lockKey);

            // Xử lý kiểm tra tồn kho và trừ số lượng trong khu vực được bảo vệ
            return processPurchaseUnderLock(productId, userId, quantity);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("==> [User: {}] Bị ngắt khi đang chờ khóa: {}", userId, e.getMessage());
            return PurchaseResponse.builder()
                    .success(false)
                    .message("Giao dịch bị gián đoạn")
                    .remainingStock(null)
                    .orderId(null)
                    .build();
        } finally {
            // Đảm bảo giải phóng khóa (Unlock) trong khối finally để tránh treo hệ thống
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("🔓 [User: {}] ĐÃ GIẢI PHÓNG KHÓA (Unlock) '{}' thành công.", userId, lockKey);
            }
        }
    }

    @Transactional
    protected PurchaseResponse processPurchaseUnderLock(Long productId, String userId, int quantity) {
        // 1. Kiểm tra sản phẩm trong database
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + productId));

        // 2. Kiểm tra tồn kho
        int currentStock = product.getStockQuantity();
        if (currentStock < quantity) {
            log.warn("❌ [User: {}] Hết hàng! Tồn kho hiện tại: {}, Số lượng yêu cầu: {}", userId, currentStock, quantity);
            return PurchaseResponse.builder()
                    .success(false)
                    .message("Sản phẩm đã hết hàng")
                    .remainingStock(currentStock)
                    .orderId(null)
                    .build();
        }

        // 3. Trừ tồn kho và lưu vào database
        int remainingStock = currentStock - quantity;
        product.setStockQuantity(remainingStock);
        productRepository.save(product);

        // 4. Tạo đơn hàng thành công
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = Order.builder()
                .id(orderId)
                .productId(productId)
                .userId(userId)
                .quantity(quantity)
                .createdAt(LocalDateTime.now())
                .build();
        orderRepository.save(order);

        log.info("✅ [User: {}] ĐẶT HÀNG THÀNH CÔNG! Mã đơn: {}, Tồn kho còn lại: {}", userId, orderId, remainingStock);

        return PurchaseResponse.builder()
                .success(true)
                .message("Đặt hàng thành công!")
                .remainingStock(remainingStock)
                .orderId(orderId)
                .build();
    }
}
