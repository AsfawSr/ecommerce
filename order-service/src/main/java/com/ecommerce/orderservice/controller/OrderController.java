package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.OrderDTO;
import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody OrderRequest orderRequest) {
        log.info("Creating order for user: {}", orderRequest.getUserId());
        OrderDTO order = orderService.createOrder(orderRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        log.info("Getting all orders");
        List<OrderDTO> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        log.info("Getting order by ID: {}", id);
        OrderDTO order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderDTO> getOrderByNumber(@PathVariable String orderNumber) {
        log.info("Getting order by number: {}", orderNumber);
        OrderDTO order = orderService.getOrderByOrderNumber(orderNumber);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDTO>> getOrdersByUser(@PathVariable Long userId) {
        log.info("Getting orders for user: {}", userId);
        List<OrderDTO> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderDTO>> getOrdersByStatus(@PathVariable String status) {
        log.info("Getting orders with status: {}", status);
        List<OrderDTO> orders = orderService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/search")
    public ResponseEntity<List<OrderDTO>> searchOrders(@RequestParam String keyword) {
        log.info("Searching orders: {}", keyword);
        List<OrderDTO> orders = orderService.searchOrders(keyword);
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        log.info("Updating order {} status to: {}", id, status);
        OrderDTO order = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDTO> updateOrder(
            @PathVariable Long id,
            @RequestBody OrderRequest orderRequest) {
        log.info("Updating order: {}", id);
        OrderDTO order = orderService.updateOrder(id, orderRequest);
        return ResponseEntity.ok(order);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        log.info("Cancelling order: {}", id);
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getOrderCount() {
        log.info("Getting order count");
        long count = orderService.countOrders();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/revenue")
    public ResponseEntity<Map<String, BigDecimal>> getTotalRevenue() {
        log.info("Getting total revenue");
        BigDecimal revenue = orderService.getTotalRevenue();
        return ResponseEntity.ok(Map.of("revenue", revenue));
    }

    @GetMapping("/exists/{id}")
    public ResponseEntity<Map<String, Boolean>> orderExists(@PathVariable Long id) {
        log.info("Checking if order exists: {}", id);
        boolean exists = orderService.orderExists(id);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    // OpenFeign Test Endpoints
    @GetMapping("/test-feign")
    public ResponseEntity<Map<String, Object>> testFeign() {
        log.info("Testing OpenFeign connections");

        try {
            // Test User Service connection
            String userHealth = "http://localhost:8081/api/users/health";
            // Test Product Service connection
            String productHealth = "http://localhost:8082/api/products/health";

            return ResponseEntity.ok(Map.of(
                    "status", "Testing OpenFeign",
                    "userService", userHealth,
                    "productService", productHealth,
                    "message", "Update URLs in Feign clients"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "error", e.getMessage(),
                    "message", "OpenFeign test failed"
            ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "order-service",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Order Service is running!");
    }

    @PatchMapping("/{orderId}/items/{itemId}/quantity")
    public ResponseEntity<OrderDTO> updateOrderItemQuantity(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestParam Integer quantity) {
        log.info("Updating quantity for item {} in order {} to {}", itemId, orderId, quantity);
        OrderDTO order = orderService.updateOrderItemQuantity(orderId, itemId, quantity);
        return ResponseEntity.ok(order);
    }

    // NEW: Check product stock
    @GetMapping("/products/{productId}/stock")
    public ResponseEntity<Map<String, Integer>> checkProductStock(@PathVariable Long productId) {
        log.info("Checking stock for product ID: {}", productId);
        Integer stock = orderService.checkProductStock(productId);
        return ResponseEntity.ok(Map.of("stock", stock));
    }

    // NEW: Bulk order creation with stock validation
    @PostMapping("/bulk")
    public ResponseEntity<List<OrderDTO>> createBulkOrders(@RequestBody List<OrderRequest> orderRequests) {
        log.info("Creating {} orders in bulk", orderRequests.size());
        List<OrderDTO> orders = orderRequests.stream()
                .map(orderService::createOrder)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(orders);
    }
}