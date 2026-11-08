package com.example.productservice.config;

import com.example.productservice.entity.Product;
import com.example.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            Product flashSaleProduct = Product.builder()
                    .name("iPhone 16 Pro Max 256GB - Flash Sale 1k")
                    .stockQuantity(1) // Chỉ có đúng 1 món duy nhất trong kho
                    .price(new BigDecimal("1000"))
                    .build();

            productRepository.save(flashSaleProduct);
            log.info("==> [DataInitializer] Đã khởi tạo sản phẩm Flash Sale mẫu: ID={}, Name='{}', Tồn kho={}",
                    flashSaleProduct.getId(), flashSaleProduct.getName(), flashSaleProduct.getStockQuantity());
        }
    }
}
