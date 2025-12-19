// ProductServiceClient.java
package com.ecommerce.orderservice.feign;

import com.ecommerce.orderservice.dto.ProductDTO;
import com.ecommerce.orderservice.feign.fallback.ProductServiceFallback;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "product-service",
        url = "${feign.client.config.product-service-client.url}",
        fallback = ProductServiceFallback.class
)
public interface ProductServiceClient {

    @GetMapping("/api/products/{id}")
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductByIdFallback")
    ProductDTO getProductById(@PathVariable Long id);

    @PostMapping("/api/products/{id}/reserve")
    @CircuitBreaker(name = "productService")
    ProductDTO reserveInventory(
            @PathVariable Long id,
            @RequestParam Integer quantity
    );

    @PostMapping("/api/products/{id}/release")
    @CircuitBreaker(name = "productService")
    ProductDTO releaseInventory(
            @PathVariable Long id,
            @RequestParam Integer quantity
    );

    @GetMapping("/api/products/{id}/stock")
    @CircuitBreaker(name = "productService", fallbackMethod = "checkStockFallback")
    Integer checkStock(@PathVariable Long id);

    @GetMapping("/api/products/health")
    @CircuitBreaker(name = "productService")
    String healthCheck();
}