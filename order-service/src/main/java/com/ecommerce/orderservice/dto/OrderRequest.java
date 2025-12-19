package com.ecommerce.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private Long userId;

    @NotBlank(message = "Shipping address is required")
    @Size(max = 500, message = "Shipping address cannot exceed 500 characters")
    private String shippingAddress;

    @NotBlank(message = "Billing address is required")
    @Size(max = 500, message = "Billing address cannot exceed 500 characters")
    private String billingAddress;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;
}