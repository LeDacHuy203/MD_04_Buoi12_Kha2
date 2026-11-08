package com.example.orderservice.controller;

import com.example.orderservice.dto.event.OrderCreatedEvent;
import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.producer.OrderProducerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderProducerService orderProducerService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        long startTime = System.currentTimeMillis();
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("==> [Order-Service] Nhận yêu cầu đặt hàng mới cho email: {}, productId: {}",
                request.getCustomerEmail(), request.getProductId());

        // 1. Lưu đơn hàng vào cơ sở dữ liệu
        Order order = Order.builder()
                .id(orderId)
                .customerEmail(request.getCustomerEmail())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalPrice(request.getTotalPrice())
                .status("CREATED")
                .createdAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);
        log.info("==> [Order-Service] Đã lưu đơn hàng vào DB với id: {}", orderId);

        // 2. Đóng gói OrderCreatedEvent và gửi vào Kafka topic "order-events"
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .customerEmail(order.getCustomerEmail())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .totalPrice(order.getTotalPrice())
                .createdAt(order.getCreatedAt())
                .build();

        orderProducerService.sendOrderCreatedEvent(event);

        // 3. Phản hồi ngay lập tức cho client (Bất đồng bộ - Không đợi trừ kho và gửi email)
        long duration = System.currentTimeMillis() - startTime;
        log.info("==> [Order-Service] Đã xử lý xong và phản hồi client trong {} ms", duration);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Đơn hàng đã được tiếp nhận thành công và đang được xử lý bất đồng bộ.");
        response.put("orderId", orderId);
        response.put("status", order.getStatus());
        response.put("processingTimeMs", duration);
        response.put("data", order);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable String id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<?> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAll());
    }
}
