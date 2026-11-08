package com.example.productservice.controller;

import com.example.productservice.dto.PurchaseRequest;
import com.example.productservice.dto.PurchaseResponse;
import com.example.productservice.entity.Product;
import com.example.productservice.repository.OrderRepository;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.service.FlashSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/flashsale")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    /**
     * Mua sản phẩm Flash Sale có bảo vệ bằng Redisson Distributed Lock
     */
    @PostMapping("/purchase/{productId}")
    public ResponseEntity<PurchaseResponse> purchase(
            @PathVariable Long productId,
            @Valid @RequestBody PurchaseRequest request) {

        PurchaseResponse response = flashSaleService.purchaseProduct(productId, request.getUserId(), request.getQuantity());

        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else if ("Sản phẩm đã hết hàng".equals(response.getMessage())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        }
    }

    /**
     * Xem thông tin chi tiết và tồn kho hiện tại của sản phẩm
     */
    @GetMapping("/products/{productId}")
    public ResponseEntity<?> getProduct(@PathVariable Long productId) {
        return productRepository.findById(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Khởi tạo lại tồn kho cho sản phẩm (phục vụ test)
     */
    @PostMapping("/products/{productId}/reset-stock")
    public ResponseEntity<?> resetStock(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") int stock) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
        product.setStockQuantity(stock);
        productRepository.save(product);

        return ResponseEntity.ok(Map.of(
                "message", "Reset tồn kho thành công",
                "productId", productId,
                "newStock", stock
        ));
    }

    /**
     * Xem danh sách các đơn hàng đã mua thành công
     */
    @GetMapping("/orders")
    public ResponseEntity<?> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAll());
    }
}
