package com.ecommerce.productservice.controller;


import com.ecommerce.productservice.dto.UserDTO;
import com.ecommerce.productservice.feign.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final UserServiceClient userServiceClient;

    // Test endpoint
    @GetMapping("/test")
    public String test() {
        return "Product Service is running!";
    }

    // OpenFeign Test 1: Get all users from User Service
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsersFromUserService() {
        log.info("📞 Calling User Service via OpenFeign to get all users");
        List<UserDTO> users = userServiceClient.getAllUsers();
        log.info("✅ Received {} users from User Service", users.size());
        return ResponseEntity.ok(users);
    }

    // OpenFeign Test 2: Get specific user
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserDTO> getUserFromUserService(@PathVariable Long userId) {
        log.info("📞 Calling User Service via OpenFeign for user ID: {}", userId);
        UserDTO user = userServiceClient.getUserById(userId);
        log.info("✅ Received user: {}", user.getUsername());
        return ResponseEntity.ok(user);
    }

    // OpenFeign Test 3: Check User Service health
    @GetMapping("/user-service-health")
    public ResponseEntity<String> checkUserServiceHealth() {
        log.info("📞 Checking User Service health via OpenFeign");
        String health = userServiceClient.healthCheck();
        log.info("✅ User Service health: {}", health);
        return ResponseEntity.ok(health);
    }

    // Product endpoints (simple for now)
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllProducts() {
        List<Map<String, Object>> products = List.of(
                Map.of("id", 1, "name", "Laptop", "price", 999.99),
                Map.of("id", 2, "name", "Smartphone", "price", 499.99),
                Map.of("id", 3, "name", "Tablet", "price", 299.99)
        );
        return ResponseEntity.ok(products);
    }

    // Combined endpoint: Product with User info
    @GetMapping("/{productId}/with-user/{userId}")
    public ResponseEntity<Map<String, Object>> getProductWithUser(
            @PathVariable Long productId,
            @PathVariable Long userId) {

        log.info("🔄 Getting product {} with user {}", productId, userId);

        // Get user from User Service via OpenFeign
        UserDTO user = userServiceClient.getUserById(userId);

        // Mock product data
        Map<String, Object> product = Map.of(
                "id", productId,
                "name", "Product " + productId,
                "price", 100 * productId,
                "description", "Description for product " + productId
        );

        Map<String, Object> response = Map.of(
                "product", product,
                "user", user,
                "message", "Successfully retrieved product with user info",
                "source", "Product Service using OpenFeign"
        );

        return ResponseEntity.ok(response);
    }
}
