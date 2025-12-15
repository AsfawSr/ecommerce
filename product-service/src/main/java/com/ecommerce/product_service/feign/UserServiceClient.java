package com.ecommerce.product_service.feign;

import com.ecommerce.product_service.dto.UserDTO;
import com.ecommerce.product_service.feign.fallback.UserServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "user-service-client",
        url = "http://localhost:8081",
        fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {

    @GetMapping("/api/users")
    List<UserDTO> getAllUsers();

    @GetMapping("/api/users/{id}")
    UserDTO getUserById(@PathVariable Long id);

    @GetMapping("/api/users/health")
    String healthCheck();

    @GetMapping("/api/users/count")
    Map<String, Long> getUserCount();
}