package com.ecommerce.orderservice.feign.fallback;

import com.ecommerce.orderservice.dto.UserDTO;
import com.ecommerce.orderservice.feign.UserServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserServiceFallback implements UserServiceClient {

    @Override
    public UserDTO getUserById(Long id) {
        log.warn("Fallback triggered for getUserById({})", id);
        return UserDTO.builder()
                .id(id)
                .username("User Service Unavailable")
                .email("unavailable@example.com")
                .firstName("Service")
                .lastName("Unavailable")
                .active(false)
                .build();
    }

    @Override
    public String healthCheck() {
        return "User Service Unavailable (Fallback)";
    }

    // Fallback method for circuit breaker
    public UserDTO getUserByIdFallback(Long id, Exception e) {
        log.warn("Circuit breaker fallback for getUserById({}): {}", id, e.getMessage());
        return getUserById(id);
    }
}