package com.example.productservice;

import com.example.productservice.dto.PurchaseResponse;
import com.example.productservice.entity.Product;
import com.example.productservice.repository.OrderRepository;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.service.FlashSaleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlashSaleConcurrencyTest {

    @Autowired
    private FlashSaleService flashSaleService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Long productId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();

        // Tạo 1 sản phẩm chỉ có duy nhất 1 món trong kho
        Product product = Product.builder()
                .name("Tai nghe Sony WH-1000XM5 Flash Sale")
                .stockQuantity(1) // Tồn kho = 1
                .price(new BigDecimal("100000"))
                .build();
        Product saved = productRepository.save(product);
        productId = saved.getId();
    }

    @Test
    @DisplayName("Kiểm thử 100 người cùng bấm mua sản phẩm có tồn kho = 1: Không được bán lố (Overselling)")
    void testFlashSaleConcurrencyWith100Users() throws InterruptedException {
        int numberOfUsers = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfUsers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfUsers);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger outOfStockCount = new AtomicInteger(0);
        AtomicInteger otherFailures = new AtomicInteger(0);

        for (int i = 1; i <= numberOfUsers; i++) {
            final String userId = "user_" + i;
            executorService.submit(() -> {
                try {
                    // Chờ tất cả 100 luồng sẵn sàng để kích hoạt đồng thời
                    startLatch.await();

                    PurchaseResponse response = flashSaleService.purchaseProduct(productId, userId, 1);
                    if (response.isSuccess()) {
                        successCount.incrementAndGet();
                    } else if ("Sản phẩm đã hết hàng".equals(response.getMessage())) {
                        outOfStockCount.incrementAndGet();
                    } else {
                        otherFailures.incrementAndGet();
                    }
                } catch (Exception e) {
                    otherFailures.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Kích hoạt đồng loạt 100 luồng cùng lúc
        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        endLatch.await();
        long duration = System.currentTimeMillis() - startTime;
        executorService.shutdown();

        System.out.println("==================================================================");
        System.out.println(" KẾT QUẢ KIỂM THỬ ĐA LUỒNG FLASH SALE VỚI REDISSON LOCK:");
        System.out.println(" - Tổng số luồng yêu cầu mua đồng thời: " + numberOfUsers);
        System.out.println(" - Số đơn mua thành công             : " + successCount.get());
        System.out.println(" - Số yêu cầu nhận 'Hết hàng'        : " + outOfStockCount.get());
        System.out.println(" - Lỗi khác                          : " + otherFailures.get());
        System.out.println(" - Thời gian xử lý 100 yêu cầu       : " + duration + " ms");
        System.out.println("==================================================================");

        // 1. Chỉ duy nhất 1 người mua thành công
        assertThat(successCount.get()).isEqualTo(1);

        // 2. 99 người còn lại nhận thông báo "Sản phẩm đã hết hàng"
        assertThat(outOfStockCount.get()).isEqualTo(99);

        // 3. Tồn kho trong database phải còn đúng 0, tuyệt đối KHÔNG BỊ ÂM
        Product productInDb = productRepository.findById(productId).orElseThrow();
        assertThat(productInDb.getStockQuantity()).isEqualTo(0);

        // 4. Bảng orders chỉ được phép ghi nhận đúng 1 đơn hàng duy nhất
        long totalOrders = orderRepository.count();
        assertThat(totalOrders).isEqualTo(1L);
    }
}
