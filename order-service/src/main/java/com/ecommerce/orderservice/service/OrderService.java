package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderDTO;
import com.ecommerce.orderservice.dto.OrderRequest;

import java.math.BigDecimal;
import java.util.List;

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
    void cancelOrder(Long id);
    boolean orderExists(Long id);
    long countOrders();
    BigDecimal getTotalRevenue();
}
