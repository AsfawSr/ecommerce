package com.ecommerce.orderservice.feign.fallback;

import com.ecommerce.orderservice.dto.ProductDTO;
import com.ecommerce.orderservice.feign.ProductServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductServiceFallback implements ProductServiceClient {

    @Override
    public ProductDTO getProductById(Long id) {
        log.warn("Fallback triggered for getProductById({})", id);
        return ProductDTO.builder()
                .id(id)
                .name("Product Service Unavailable")
                .sku("N/A")
                .price(null)
                .stockQuantity(0)
                .active(false)
                .build();
    }

    @Override
    public ProductDTO reserveInventory(Long id, Integer quantity) {
        log.error("Cannot reserve inventory - Product Service unavailable");
        throw new RuntimeException("Product Service unavailable");
    }

    @Override
    public ProductDTO releaseInventory(Long id, Integer quantity) {
        log.error("Cannot release inventory - Product Service unavailable");
        throw new RuntimeException("Product Service unavailable");
    }

    @Override
    public Integer checkStock(Long id) {
        log.warn("Fallback triggered for checkStock({})", id);
        return 0;
    }

    @Override
    public String healthCheck() {
        return "Product Service Unavailable (Fallback)";
    }

    // Fallback methods for circuit breaker
    public ProductDTO getProductByIdFallback(Long id, Exception e) {
        log.warn("Circuit breaker fallback for getProductById({}): {}", id, e.getMessage());
        return getProductById(id);
    }

    public Integer checkStockFallback(Long id, Exception e) {
        log.warn("Circuit breaker fallback for checkStock({}): {}", id, e.getMessage());
        return 0;
    }
}