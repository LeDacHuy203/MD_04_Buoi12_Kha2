package com.example.inventoryservice.consumer;

import com.example.inventoryservice.dto.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Slf4j
@Service
public class InventoryConsumerService {

    // Giả lập kho sản phẩm trong bộ nhớ (ProductId -> Stock Quantity)
    private final Map<Long, Integer> stockMap = new ConcurrentHashMap<>();

    public InventoryConsumerService() {
        // Khởi tạo sẵn một số mặt hàng mẫu trong kho
        stockMap.put(101L, 100);
        stockMap.put(102L, 50);
        stockMap.put(103L, 20);
    }

    @KafkaListener(
            topics = "${app.kafka.topic.order-events:order-events}",
            groupId = "${spring.kafka.consumer.group-id:inventory-group}"
    )
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("==================================================================");
        log.info("📦 [INVENTORY-SERVICE] [Group: inventory-group] Nhận sự kiện OrderCreatedEvent!");
        log.info("📦 [INVENTORY-SERVICE] Chi tiết đơn hàng: Mã đơn={}, Sản phẩm ID={}, Số lượng={}",
                event.getOrderId(), event.getProductId(), event.getQuantity());

        Long productId = event.getProductId();
        int requestedQty = event.getQuantity();

        int currentStock = stockMap.getOrDefault(productId, 50);
        int remainingStock = Math.max(0, currentStock - requestedQty);
        stockMap.put(productId, remainingStock);

        log.info("📦 [INVENTORY-SERVICE] ==> Đã TRỪ KHO thành công cho đơn hàng '{}'. Tồn kho hiện tại của sản phẩm {}: {} (trước đó: {})",
                event.getOrderId(), productId, remainingStock, currentStock);
        log.info("==================================================================");
    }

    public Map<Long, Integer> getStockStatus() {
        return stockMap;
    }
}
