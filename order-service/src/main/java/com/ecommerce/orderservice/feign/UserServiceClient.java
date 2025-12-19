package com.ecommerce.orderservice.feign;

import com.ecommerce.orderservice.dto.UserDTO;
import com.ecommerce.orderservice.feign.fallback.UserServiceFallback;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-service",
        url = "${feign.client.config.user-service-client.url}",
        fallback = UserServiceFallback.class
)
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    UserDTO getUserById(@PathVariable Long id);

    @GetMapping("/api/users/health")
    @CircuitBreaker(name = "userService")
    String healthCheck();
}