package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderDTO;
import com.ecommerce.orderservice.dto.OrderRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface OrderService {

    OrderDTO createOrder(OrderRequest orderRequest);
    OrderDTO getOrderById(Long id);
    OrderDTO getOrderByOrderNumber(String orderNumber);
    List<OrderDTO> getAllOrders();
    List<OrderDTO> getOrdersByUserId(Long userId);
    List<OrderDTO> getOrdersByStatus(String status);
    List<OrderDTO> searchOrders(String keyword);
    OrderDTO updateOrderStatus(Long id, String status);
    OrderDTO updateOrder(Long id, OrderRequest orderRequest);

    // New method: Update order item quantity
    OrderDTO updateOrderItemQuantity(Long orderId, Long itemId, Integer newQuantity);

    void cancelOrder(Long id);
    boolean orderExists(Long id);
    long countOrders();
    BigDecimal getTotalRevenue();

    // New method: Check product stock
    Integer checkProductStock(Long productId);

    public Map<String, Object> getOrderStatistics();
}