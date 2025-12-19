
package com.ecommerce.orderservice.service.impl;

import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.exception.ResourceNotFoundException;
import com.ecommerce.orderservice.feign.ProductServiceClient;
import com.ecommerce.orderservice.feign.UserServiceClient;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;

    @Override
    public OrderDTO createOrder(OrderRequest orderRequest) {
        log.info("Creating order for user ID: {}", orderRequest.getUserId());

        // 1. Fetch user details from User Service via Feign
        UserDTO user;
        try {
            user = userServiceClient.getUserById(orderRequest.getUserId());
            log.info("Fetched user: {}", user.getUsername());
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user details: " + e.getMessage());
        }

        // 2. Generate unique order number
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 3. Create order
        Order order = Order.builder()
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

        // 4. Add order items
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : orderRequest.getItems()) {
            // Fetch product details from Product Service via Feign
            ProductDTO product;
            try {
                product = productServiceClient.getProductById(itemRequest.getProductId());
                log.info("Fetched product: {}", product.getName());
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch product details for ID: " + itemRequest.getProductId());
            }

            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .build();

            orderItem.calculateSubtotal();
            order.addOrderItem(orderItem);
            totalAmount = totalAmount.add(orderItem.getSubtotal());
        }

        order.setTotalAmount(totalAmount);

        // 5. Save order
        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully: {}", orderNumber);

        return convertToDTO(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        log.info("Getting order by ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));
        return convertToDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderByOrderNumber(String orderNumber) {
        log.info("Getting order by number: {}", orderNumber);
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
        return convertToDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        log.info("Getting all orders");
        return orderRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByUserId(Long userId) {
        log.info("Getting orders for user ID: {}", userId);
        return orderRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByStatus(String status) {
        log.info("Getting orders with status: {}", status);
        return orderRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> searchOrders(String keyword) {
        log.info("Searching orders with keyword: {}", keyword);
        return orderRepository.searchOrders(keyword).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OrderDTO updateOrderStatus(Long id, String status) {
        log.info("Updating order {} status to: {}", id, status);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        order.setStatus(status);

        // If delivered, update payment status to PAID
        if ("DELIVERED".equals(status)) {
            order.setPaymentStatus("PAID");
        }

        Order updatedOrder = orderRepository.save(order);
        return convertToDTO(updatedOrder);
    }

    @Override
    public OrderDTO updateOrder(Long id, OrderRequest orderRequest) {
        log.info("Updating order ID: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        // Update order fields
        order.setShippingAddress(orderRequest.getShippingAddress());
        order.setBillingAddress(orderRequest.getBillingAddress());
        order.setPaymentMethod(orderRequest.getPaymentMethod());
        order.setNotes(orderRequest.getNotes());

        Order updatedOrder = orderRepository.save(order);
        return convertToDTO(updatedOrder);
    }

    @Override
    public void cancelOrder(Long id) {
        log.info("Cancelling order ID: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        if ("DELIVERED".equals(order.getStatus())) {
            throw new RuntimeException("Cannot cancel a delivered order");
        }

        order.setStatus("CANCELLED");
        order.setPaymentStatus("REFUNDED");
        orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean orderExists(Long id) {
        return orderRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countOrders() {
        return orderRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        BigDecimal revenue = orderRepository.getTotalRevenue();
        return revenue != null ? revenue : BigDecimal.ZERO;
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