package com.ecommerce.user_service.controller;

import com.ecommerce.user_service.dto.UserDTO;
import com.ecommerce.user_service.dto.UserRequest;
import com.ecommerce.user_service.dto.UserResponseDTO;
import com.ecommerce.user_service.service.UserService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController {

    private final UserService userService;

    // ========== INTERNAL ENDPOINTS ==========

    @PostMapping
    @Operation(summary = "Create a new user")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserRequest userRequest) {
        log.info("Creating new user: {}", userRequest.getUsername());
        UserDTO createdUser = userService.createUser(userRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @GetMapping
    @Operation(summary = "Get all users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        log.info("Getting all users");
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        log.info("Getting user by ID: {}", id);
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
        log.info("Getting user by username: {}", username);
        UserDTO user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/search")
    @Operation(summary = "Search users")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam String keyword) {
        log.info("Searching users with keyword: {}", keyword);
        List<UserDTO> users = userService.searchUsers(keyword);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest userRequest) {
        log.info("Updating user ID: {}", id);
        UserDTO updatedUser = userService.updateUser(id, userRequest);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{id}/profile")
    @Operation(summary = "Update user profile")
    public ResponseEntity<UserDTO> updateUserProfile(
            @PathVariable Long id,
            @RequestBody UserDTO userDTO) {
        log.info("Updating user profile ID: {}", id);
        UserDTO updatedUser = userService.updateUserProfile(id, userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Deleting user ID: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/disable")
    @Operation(summary = "Disable user")
    public ResponseEntity<Void> disableUser(@PathVariable Long id) {
        log.info("Disabling user ID: {}", id);
        userService.disableUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/enable")
    @Operation(summary = "Enable user")
    public ResponseEntity<Void> enableUser(@PathVariable Long id) {
        log.info("Enabling user ID: {}", id);
        userService.enableUser(id);
        return ResponseEntity.noContent().build();
    }

    // ========== EXTERNAL ENDPOINTS (For Order/Product Services) ==========

    @GetMapping("/external")
    @CircuitBreaker(name = "userService", fallbackMethod = "getAllUserResponsesFallback")
    @Operation(summary = "Get all users for external services")
    public ResponseEntity<List<UserResponseDTO>> getAllUserResponses() {
        log.info("Getting all users for external services");
        List<UserResponseDTO> users = userService.getAllUserResponses();
        return ResponseEntity.ok(users);
    }

    public ResponseEntity<List<UserResponseDTO>> getAllUserResponsesFallback(Exception e) {
        log.error("Fallback for getAllUserResponses: {}", e.getMessage());
        return ResponseEntity.ok(List.of()); // Return empty list as fallback
    }

    @GetMapping("/external/{id}")
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserResponseByIdFallback")
    @Operation(summary = "Get user by ID for external services")
    public ResponseEntity<UserResponseDTO> getUserResponseById(@PathVariable Long id) {
        log.info("Getting user response by ID: {}", id);
        UserResponseDTO user = userService.getUserResponseById(id);
        return ResponseEntity.ok(user);
    }

    public ResponseEntity<UserResponseDTO> getUserResponseByIdFallback(Long id, Exception e) {
        log.error("Fallback for getUserResponseById({}): {}", id, e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(UserResponseDTO.builder()
                        .id(id)
                        .username("Service Unavailable")
                        .email("unavailable@example.com")
                        .firstName("Service")
                        .lastName("Unavailable")
                        .active(false)
                        .build());
    }

    // ========== UTILITY ENDPOINTS ==========

    @GetMapping("/count")
    @Operation(summary = "Get user count")
    public ResponseEntity<Map<String, Long>> getUserCount() {
        log.info("Getting user count");
        long count = userService.countUsers();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/exists/{id}")
    @Operation(summary = "Check if user exists")
    public ResponseEntity<Map<String, Boolean>> userExists(@PathVariable Long id) {
        log.info("Checking if user exists: {}", id);
        boolean exists = userService.userExists(id);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get user statistics")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        log.info("Getting user statistics");
        Map<String, Object> stats = userService.getUserStatistics();
        return ResponseEntity.ok(stats);
    }

    // ========== HEALTH & MONITORING ENDPOINTS ==========

    @GetMapping("/health")
    @CircuitBreaker(name = "userService")
    public ResponseEntity<String> healthCheck() {
        log.info("Health check requested");
        return ResponseEntity.ok("User Service is UP and running with MySQL!");
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("User Service with MySQL is working!");
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "user-service",
                "database", "MySQL",
                "timestamp", java.time.LocalDateTime.now().toString(),
                "userCount", userService.countUsers()
        ));
    }

    @PostMapping("/authenticate")
    @Operation(summary = "Authenticate user")
    public ResponseEntity<UserDTO> authenticate(
            @RequestParam String username,
            @RequestParam String password) {
        log.info("Authenticating user: {}", username);
        UserDTO user = userService.authenticate(username, password);
        return ResponseEntity.ok(user);
    }
}