// OrderRequest.java
package com.ecommerce.orderservice.dto;

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
    private Long userId;
    private String shippingAddress;
    private String billingAddress;
    private String paymentMethod;
    private String notes;
    private List<OrderItemRequest> items;
}
