package com.example.productservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseResponse {
    private boolean success;
    private String message;
    private Integer remainingStock;
    private String orderId;
}
