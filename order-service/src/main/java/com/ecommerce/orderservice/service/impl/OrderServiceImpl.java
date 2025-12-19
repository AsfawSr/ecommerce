package com.ecommerce.orderservice.service.impl;

import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.exception.InsufficientStockException;
import com.ecommerce.orderservice.exception.OrderProcessingException;
import com.ecommerce.orderservice.exception.ResourceNotFoundException;
import com.ecommerce.orderservice.feign.ProductServiceClient;
import com.ecommerce.orderservice.feign.UserServiceClient;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;

    @Override
    @Transactional
    @CircuitBreaker(name = "orderService", fallbackMethod = "createOrderFallback")
    public OrderDTO createOrder(OrderRequest orderRequest) {
        log.info("Creating order for user ID: {}", orderRequest.getUserId());

        // 1. Fetch user details from User Service via Feign
        UserDTO user = fetchUserDetails(orderRequest.getUserId());

        // 2. Generate unique order number
        String orderNumber = generateOrderNumber();

        // 3. Create order
        Order order = buildOrder(orderRequest, user, orderNumber);

        // 4. Process order items with inventory validation
        List<OrderItem> orderItems = processOrderItems(orderRequest.getItems(), order);
        order.setOrderItems(orderItems);

        // 5. Calculate total
        order.setTotalAmount(calculateTotal(orderItems));

        try {
            // 6. Reserve inventory for all items
            reserveInventory(orderItems);

            // 7. Save order
            Order savedOrder = orderRepository.save(order);
            log.info("Order created successfully: {}", orderNumber);

            // 8. Return DTO
            return convertToDTO(savedOrder);

        } catch (Exception e) {
            // If order creation fails, release any reserved inventory
            log.error("Order creation failed, releasing reserved inventory", e);
            releaseInventoryOnFailure(orderItems);
            throw new OrderProcessingException("Failed to create order: " + e.getMessage(), e);
        }
    }

    // Fallback method for circuit breaker
    public OrderDTO createOrderFallback(OrderRequest orderRequest, Exception e) {
        log.error("Circuit breaker fallback triggered for createOrder: {}", e.getMessage());
        throw new OrderProcessingException("Order service is temporarily unavailable. Please try again later.");
    }

    private UserDTO fetchUserDetails(Long userId) {
        try {
            UserDTO user = userServiceClient.getUserById(userId);
            log.info("Fetched user: {}", user.getUsername());
            return user;
        } catch (Exception e) {
            log.error("Failed to fetch user details for ID: {}", userId, e);
            throw new OrderProcessingException("Failed to fetch user details: " + e.getMessage(), e);
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                + "-" + LocalDateTime.now().getYear();
    }

    private Order buildOrder(OrderRequest orderRequest, UserDTO user, String orderNumber) {
        return Order.builder()
                .orderNumber(orderNumber)
                .userId(orderRequest.getUserId())
                .userName(user.getFirstName() + " " + user.getLastName())
                .userEmail(user.getEmail())
                .status("PENDING")
                .shippingAddress(orderRequest.getShippingAddress())
                .billingAddress(orderRequest.getBillingAddress())
                .paymentMethod(orderRequest.getPaymentMethod())
                .paymentStatus("PENDING")
                .notes(orderRequest.getNotes())
                .estimatedDelivery(LocalDateTime.now().plusDays(3))
                .build();
    }

    private List<OrderItem> processOrderItems(List<OrderItemRequest> itemRequests, Order order) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemRequest : itemRequests) {
            // Fetch product details
            ProductDTO product = fetchProductDetails(itemRequest.getProductId());

            // Validate stock availability
            validateStockAvailability(product, itemRequest.getQuantity());

            // Use product price if unit price not provided (but validation ensures it's provided)
            BigDecimal unitPrice = itemRequest.getUnitPrice();

            // Create order item
            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .order(order)
                    .build();

            orderItem.calculateSubtotal();
            orderItems.add(orderItem);
        }

        return orderItems;
    }

    private ProductDTO fetchProductDetails(Long productId) {
        try {
            ProductDTO product = productServiceClient.getProductById(productId);
            log.info("Fetched product: {}", product.getName());
            return product;
        } catch (Exception e) {
            log.error("Failed to fetch product details for ID: {}", productId, e);
            throw new OrderProcessingException("Failed to fetch product details for ID: " + productId, e);
        }
    }

    private void validateStockAvailability(ProductDTO product, Integer requestedQuantity) {
        if (product.getStockQuantity() == null || product.getStockQuantity() < requestedQuantity) {
            throw new InsufficientStockException(
                    product.getName(),
                    requestedQuantity,
                    product.getStockQuantity() != null ? product.getStockQuantity() : 0
            );
        }

        if (!product.isActive()) {
            throw new OrderProcessingException("Product " + product.getName() + " is not available");
        }
    }

    private BigDecimal calculateTotal(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void reserveInventory(List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            try {
                ProductDTO updatedProduct = productServiceClient.reserveInventory(
                        item.getProductId(),
                        item.getQuantity()
                );
                log.info("Reserved {} units of product ID: {}", item.getQuantity(), item.getProductId());
            } catch (Exception e) {
                log.error("Failed to reserve inventory for product ID: {}", item.getProductId(), e);
                throw new OrderProcessingException("Failed to reserve inventory", e);
            }
        }
    }

    private void releaseInventoryOnFailure(List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            try {
                productServiceClient.releaseInventory(item.getProductId(), item.getQuantity());
                log.info("Released {} units of product ID: {}", item.getQuantity(), item.getProductId());
            } catch (Exception e) {
                log.error("Failed to release inventory for product ID: {}", item.getProductId(), e);
            }
        }
    }

    @Override
    @Transactional
    public OrderDTO getOrderById(Long id) {
        log.info("Getting order by ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));
        return convertToDTO(order);
    }

    @Override
    @Transactional
    public OrderDTO getOrderByOrderNumber(String orderNumber) {
        log.info("Getting order by number: {}", orderNumber);
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
        return convertToDTO(order);
    }

    @Override
    @Transactional
    public List<OrderDTO> getAllOrders() {
        log.info("Getting all orders");
        return orderRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<OrderDTO> getOrdersByUserId(Long userId) {
        log.info("Getting orders for user ID: {}", userId);
        return orderRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<OrderDTO> getOrdersByStatus(String status) {
        log.info("Getting orders with status: {}", status);
        return orderRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<OrderDTO> searchOrders(String keyword) {
        log.info("Searching orders with keyword: {}", keyword);
        return orderRepository.searchOrders(keyword).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(Long id, String status) {
        log.info("Updating order {} status to: {}", id, status);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        String previousStatus = order.getStatus();
        order.setStatus(status.toUpperCase());

        // Handle inventory based on status changes
        handleStatusChangeInventory(previousStatus, status.toUpperCase(), order.getOrderItems());

        // Update payment status based on order status
        updatePaymentStatusBasedOnOrderStatus(order, status.toUpperCase());

        Order updatedOrder = orderRepository.save(order);
        log.info("Order {} status updated from {} to {}", id, previousStatus, status);
        return convertToDTO(updatedOrder);
    }

    private void handleStatusChangeInventory(String previousStatus, String newStatus, List<OrderItem> orderItems) {
        // If order is cancelled, release inventory
        if ("CANCELLED".equals(newStatus) && !"CANCELLED".equals(previousStatus)) {
            releaseInventory(orderItems);
        }

        // If moving from cancelled back to another status, reserve inventory again
        if ("CANCELLED".equals(previousStatus) && !"CANCELLED".equals(newStatus)) {
            reserveInventory(orderItems);
        }
    }

    private void updatePaymentStatusBasedOnOrderStatus(Order order, String status) {
        switch (status) {
            case "DELIVERED":
                order.setPaymentStatus("PAID");
                break;
            case "CANCELLED":
                order.setPaymentStatus("REFUNDED");
                break;
            case "PROCESSING":
            case "SHIPPED":
                order.setPaymentStatus("PAID"); // Assuming payment is captured before shipping
                break;
        }
    }

    private void releaseInventory(List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            try {
                productServiceClient.releaseInventory(item.getProductId(), item.getQuantity());
                log.info("Released {} units of product ID: {}", item.getQuantity(), item.getProductId());
            } catch (Exception e) {
                log.error("Failed to release inventory for product ID: {}", item.getProductId(), e);
            }
        }
    }

    @Override
    @Transactional
    public OrderDTO updateOrder(Long id, OrderRequest orderRequest) {
        log.info("Updating order ID: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        // Only allow updating if order is still PENDING
        if (!"PENDING".equals(order.getStatus())) {
            throw new OrderProcessingException("Cannot update order with status: " + order.getStatus());
        }

        // Update allowed fields
        order.setShippingAddress(orderRequest.getShippingAddress());
        order.setBillingAddress(orderRequest.getBillingAddress());
        order.setPaymentMethod(orderRequest.getPaymentMethod());
        order.setNotes(orderRequest.getNotes());

        Order updatedOrder = orderRepository.save(order);
        return convertToDTO(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDTO updateOrderItemQuantity(Long orderId, Long itemId, Integer newQuantity) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!"PENDING".equals(order.getStatus())) {
            throw new OrderProcessingException("Cannot modify items for order with status: " + order.getStatus());
        }

        OrderItem item = order.getOrderItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));

        // Check if new quantity is different
        if (!item.getQuantity().equals(newQuantity)) {
            // Check stock availability
            ProductDTO product = fetchProductDetails(item.getProductId());
            validateStockAvailability(product, newQuantity);

            // Calculate quantity difference
            int quantityDiff = newQuantity - item.getQuantity();

            try {
                if (quantityDiff > 0) {
                    // Reserve additional inventory
                    productServiceClient.reserveInventory(item.getProductId(), quantityDiff);
                } else {
                    // Release excess inventory
                    productServiceClient.releaseInventory(item.getProductId(), -quantityDiff);
                }

                item.setQuantity(newQuantity);
                item.calculateSubtotal();

                // Recalculate order total
                order.setTotalAmount(calculateTotal(order.getOrderItems()));

                log.info("Updated quantity for item {} in order {}: {} -> {}",
                        itemId, orderId, item.getQuantity(), newQuantity);

            } catch (Exception e) {
                log.error("Failed to update inventory for item {}: {}", itemId, e.getMessage());
                throw new OrderProcessingException("Failed to update item quantity: " + e.getMessage(), e);
            }
        }

        Order updatedOrder = orderRepository.save(order);
        return convertToDTO(updatedOrder);
    }

    @Override
    @Transactional
    public void cancelOrder(Long id) {
        log.info("Cancelling order ID: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        if ("DELIVERED".equals(order.getStatus())) {
            throw new OrderProcessingException("Cannot cancel a delivered order");
        }

        // Release inventory
        releaseInventory(order.getOrderItems());

        order.setStatus("CANCELLED");
        order.setPaymentStatus("REFUNDED");
        orderRepository.save(order);

        log.info("Order {} has been cancelled", id);
    }

    @Override
    @Transactional
    public boolean orderExists(Long id) {
        return orderRepository.existsById(id);
    }

    @Override
    @Transactional
    public long countOrders() {
        return orderRepository.count();
    }

    @Override
    @Transactional
    public BigDecimal getTotalRevenue() {
        BigDecimal revenue = orderRepository.getTotalRevenue();
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public Integer checkProductStock(Long productId) {
        try {
            return productServiceClient.checkStock(productId);
        } catch (Exception e) {
            log.error("Failed to check stock for product ID: {}", productId, e);
            throw new OrderProcessingException("Failed to check product stock", e);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> getOrderStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalOrders", orderRepository.count());
        stats.put("deliveredOrders", orderRepository.countDeliveredOrders());
        stats.put("totalRevenue", getTotalRevenue());

        // Count orders by status
        List<String> statuses = Arrays.asList("PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED");
        Map<String, Long> ordersByStatus = new HashMap<>();
        for (String status : statuses) {
            ordersByStatus.put(status, (long) orderRepository.findByStatus(status).size());
        }
        stats.put("ordersByStatus", ordersByStatus);

        return stats;
    }

    private OrderDTO convertToDTO(Order order) {
        List<OrderItemDTO> itemDTOs = order.getOrderItems().stream()
                .map(item -> OrderItemDTO.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .productSku(item.getProductSku())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .userName(order.getUserName())
                .userEmail(order.getUserEmail())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .shippingAddress(order.getShippingAddress())
                .billingAddress(order.getBillingAddress())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .trackingNumber(order.getTrackingNumber())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .estimatedDelivery(order.getEstimatedDelivery())
                .orderItems(itemDTOs)
                .build();
    }
}