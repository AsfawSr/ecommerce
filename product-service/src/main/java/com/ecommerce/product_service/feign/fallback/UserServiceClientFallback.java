package com.ecommerce.product_service.feign.fallback;

import com.ecommerce.product_service.dto.UserDTO;
import com.ecommerce.product_service.feign.UserServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public List<UserDTO> getAllUsers() {
        log.warn("Fallback: User Service unavailable, returning empty list");
        return Collections.emptyList();
    }

    @Override
    public UserDTO getUserById(Long id) {
        log.warn("Fallback: User Service unavailable for user ID: {}", id);
        return UserDTO.builder()
                .id(id)
                .username("Service Unavailable")
                .email("service@unavailable.com")
                .fullName("Fallback User")
                .active(false)
                .build();
    }

    @Override
    public String healthCheck() {
        return "User Service is DOWN (Fallback)";
    }

    @Override
    public Map<String, Long> getUserCount() {
        return Map.of("count", 0L);
    }
}