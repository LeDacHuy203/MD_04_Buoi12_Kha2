package com.example.inventoryservice.controller;

import com.example.inventoryservice.consumer.InventoryConsumerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryConsumerService inventoryConsumerService;

    @GetMapping("/stock")
    public ResponseEntity<Map<Long, Integer>> getStock() {
        return ResponseEntity.ok(inventoryConsumerService.getStockStatus());
    }
}
