package com.example.productservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseRequest {

    @NotBlank(message = "User ID không được để trống")
    private String userId;

    @Builder.Default
    @Min(value = 1, message = "Số lượng mua tối thiểu là 1")
    private int quantity = 1;
}
