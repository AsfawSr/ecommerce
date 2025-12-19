package com.ecommerce.product_service.controller;

import com.ecommerce.product_service.dto.ProductDTO;
import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.dto.UserDTO;
import com.ecommerce.product_service.feign.UserServiceClient;
import com.ecommerce.product_service.service.ProductService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final UserServiceClient userServiceClient;


    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductRequest productRequest) {
        log.info("Creating product: {}", productRequest.getName());
        ProductDTO created = productService.createProduct(productRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        log.info("Getting all products");
        List<ProductDTO> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        log.info("Getting product by ID: {}", id);
        ProductDTO product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductDTO> getProductBySku(@PathVariable String sku) {
        log.info("Getting product by SKU: {}", sku);
        ProductDTO product = productService.getProductBySku(sku);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductDTO>> getProductsByCategory(@PathVariable String category) {
        log.info("Getting products by category: {}", category);
        List<ProductDTO> products = productService.getProductsByCategory(category);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductDTO>> searchProducts(@RequestParam String keyword) {
        log.info("Searching products: {}", keyword);
        List<ProductDTO> products = productService.searchProducts(keyword);
        return ResponseEntity.ok(products);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest productRequest) {
        log.info("Updating product ID: {}", id);
        ProductDTO updated = productService.updateProduct(id, productRequest);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductDTO> updateStock(@PathVariable Long id, @RequestParam Integer quantity) {
        log.info("Updating stock for product ID: {} by {}", id, quantity);
        ProductDTO updated = productService.updateStock(id, quantity);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("Deleting product ID: {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{id}/reserve")
    @CircuitBreaker(name = "productService", fallbackMethod = "reserveInventoryFallback")
    public ResponseEntity<ProductDTO> reserveInventory(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        log.info("Reserving inventory for product ID: {}, quantity: {}", id, quantity);
        ProductDTO product = productService.reserveInventory(id, quantity);
        return ResponseEntity.ok(product);
    }

    public ResponseEntity<ProductDTO> reserveInventoryFallback(Long id, Integer quantity, Exception e) {
        log.error("Fallback for reserveInventory: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ProductDTO.builder()
                        .id(id)
                        .name("Service Unavailable")
                        .stockQuantity(0)
                        .build());
    }

    @PostMapping("/{id}/release")
    @CircuitBreaker(name = "productService", fallbackMethod = "releaseInventoryFallback")
    public ResponseEntity<ProductDTO> releaseInventory(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        log.info("Releasing inventory for product ID: {}, quantity: {}", id, quantity);
        ProductDTO product = productService.releaseInventory(id, quantity);
        return ResponseEntity.ok(product);
    }

    public ResponseEntity<ProductDTO> releaseInventoryFallback(Long id, Integer quantity, Exception e) {
        log.error("Fallback for releaseInventory: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ProductDTO.builder()
                        .id(id)
                        .name("Service Unavailable")
                        .stockQuantity(0)
                        .build());
    }

    @GetMapping("/{id}/stock")
    @CircuitBreaker(name = "productService", fallbackMethod = "checkStockFallback")
    public ResponseEntity<Map<String, Integer>> checkStock(@PathVariable Long id) {
        log.info("Checking stock for product ID: {}", id);
        Integer stock = productService.checkStock(id);
        return ResponseEntity.ok(Map.of("stock", stock));
    }

    public ResponseEntity<Map<String, Integer>> checkStockFallback(Long id, Exception e) {
        log.error("Fallback for checkStock: {}", e.getMessage());
        return ResponseEntity.ok(Map.of("stock", 0));
    }

    // ========== ADDITIONAL ENDPOINTS ==========

    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductDTO>> getLowStockProducts(
            @RequestParam(defaultValue = "10") Integer threshold) {
        log.info("Getting low stock products (threshold: {})", threshold);
        List<ProductDTO> products = productService.getLowStockProducts(threshold);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getProductCount() {
        log.info("Getting product count");
        long count = productService.countProducts();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/exists/{id}")
    public ResponseEntity<Map<String, Boolean>> productExists(@PathVariable Long id) {
        log.info("Checking if product exists: {}", id);
        boolean exists = productService.productExists(id);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getProductStatistics() {
        log.info("Getting product statistics");
        Map<String, Object> stats = productService.getProductStatistics();
        return ResponseEntity.ok(stats);
    }


    @GetMapping("/feign/users")
    public ResponseEntity<List<UserDTO>> getAllUsersViaFeign() {
        log.info("📞 Calling User Service via OpenFeign for all users");
        List<UserDTO> users = userServiceClient.getAllUsers();
        log.info("✅ Received {} users via OpenFeign", users.size());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/feign/users/{userId}")
    public ResponseEntity<UserDTO> getUserViaFeign(@PathVariable Long userId) {
        log.info("📞 Calling User Service via OpenFeign for user ID: {}", userId);
        UserDTO user = userServiceClient.getUserById(userId);
        log.info("✅ Received user via OpenFeign: {}", user.getUsername());
        return ResponseEntity.ok(user);
    }


    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "product-service",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Product Service is running!");
    }
}