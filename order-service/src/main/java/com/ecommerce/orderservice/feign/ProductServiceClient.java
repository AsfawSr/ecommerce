package com.ecommerce.orderservice.feign;

import com.ecommerce.orderservice.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "product-service-client",
        url = "${feign.client.config.product-service.url:http://localhost:8082}"
)
public interface ProductServiceClient {

    @GetMapping("/api/products/{id}")
    ProductDTO getProductById(@PathVariable Long id);

    @PostMapping("/api/products/{id}/reserve")
    ProductDTO reserveInventory(
            @PathVariable Long id,
            @RequestParam Integer quantity
    );

    @PostMapping("/api/products/{id}/release")
    ProductDTO releaseInventory(
            @PathVariable Long id,
            @RequestParam Integer quantity
    );

    @GetMapping("/api/products/{id}/stock")
    Integer checkStock(@PathVariable Long id);

    @GetMapping("/api/products/health")
    String healthCheck();
}