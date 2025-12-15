package com.ecommerce.product_service.controller;

import com.ecommerce.product_service.dto.ProductDTO;
import com.ecommerce.product_service.dto.UserDTO;
import com.ecommerce.product_service.feign.UserServiceClient;
import com.ecommerce.product_service.service.ProductService;
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

    // ========== PRODUCT ENDPOINTS ==========

    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductDTO productDTO) {
        log.info("Creating product: {}", productDTO.getName());
        ProductDTO created = productService.createProduct(productDTO);
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
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @RequestBody ProductDTO productDTO) {
        log.info("Updating product ID: {}", id);
        ProductDTO updated = productService.updateProduct(id, productDTO);
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

    // ========== OPENFEIGN ENDPOINTS ==========

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

    @GetMapping("/feign/user-health")
    public ResponseEntity<String> checkUserServiceHealth() {
        log.info("📞 Checking User Service health via OpenFeign");
        String health = userServiceClient.healthCheck();
        log.info("✅ User Service health: {}", health);
        return ResponseEntity.ok(health);
    }

    @GetMapping("/feign/user-count")
    public ResponseEntity<Map<String, Long>> getUserCountViaFeign() {
        log.info("📞 Getting user count via OpenFeign");
        Map<String, Long> count = userServiceClient.getUserCount();
        log.info("✅ User count: {}", count);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{productId}/with-user/{userId}")
    public ResponseEntity<Map<String, Object>> getProductWithUserInfo(
            @PathVariable Long productId,
            @PathVariable Long userId) {

        log.info("🔄 Getting product {} with user {}", productId, userId);

        ProductDTO product = productService.getProductById(productId);
        UserDTO user = userServiceClient.getUserById(userId);

        Map<String, Object> response = Map.of(
                "product", product,
                "user", user,
                "message", "Successfully retrieved product with user info",
                "source", "Product Service using OpenFeign"
        );

        return ResponseEntity.ok(response);
    }

    // ========== HEALTH & TEST ==========

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

    @GetMapping("/test-feign")
    public ResponseEntity<Map<String, Object>> testFeign() {
        log.info("Testing OpenFeign integration");

        try {
            List<UserDTO> users = userServiceClient.getAllUsers();
            String health = userServiceClient.healthCheck();
            Map<String, Long> count = userServiceClient.getUserCount();

            return ResponseEntity.ok(Map.of(
                    "feignStatus", "WORKING",
                    "userCount", users.size(),
                    "userServiceHealth", health,
                    "userCountResponse", count,
                    "message", "OpenFeign is successfully communicating with User Service"
            ));
        } catch (Exception e) {
            log.error("Feign test failed: ", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "feignStatus", "FAILED",
                    "error", e.getMessage(),
                    "message", "OpenFeign communication failed. Is User Service running?"
            ));
        }
    }
}