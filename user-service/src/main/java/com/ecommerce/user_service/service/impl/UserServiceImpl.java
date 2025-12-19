package com.ecommerce.user_service.service.impl;

import com.ecommerce.user_service.dto.UserDTO;
import com.ecommerce.user_service.dto.UserRequest;
import com.ecommerce.user_service.dto.UserResponseDTO;
import com.ecommerce.user_service.exception.DuplicateResourceException;
import com.ecommerce.user_service.exception.ResourceNotFoundException;
import com.ecommerce.user_service.mapper.UserMapper;
import com.ecommerce.user_service.model.User;
import com.ecommerce.user_service.repository.UserRepository;
import com.ecommerce.user_service.service.UserService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    @CircuitBreaker(name = "userService", fallbackMethod = "createUserFallback")
    public UserDTO createUser(UserRequest userRequest) {
        log.info("Creating new user: {}", userRequest.getUsername());

        // Check if username exists
        if (userRepository.existsByUsername(userRequest.getUsername())) {
            throw new DuplicateResourceException("Username", "username", userRequest.getUsername());
        }

        // Check if email exists
        if (userRepository.existsByEmail(userRequest.getEmail())) {
            throw new DuplicateResourceException("Email", "email", userRequest.getEmail());
        }

        // Create user entity
        User user = User.builder()
                .username(userRequest.getUsername())
                .email(userRequest.getEmail())
                .password(userRequest.getPassword())
                .firstName(userRequest.getFirstName())
                .lastName(userRequest.getLastName())
                .phoneNumber(userRequest.getPhoneNumber())
                .address(userRequest.getAddress())
                .enabled(true)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());

        return userMapper.toDTO(savedUser);
    }

    // Fallback method
    public UserDTO createUserFallback(UserRequest userRequest, Exception e) {
        log.error("Circuit breaker fallback for createUser: {}", e.getMessage());
        throw new RuntimeException("User service is temporarily unavailable. Please try again later.");
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    public UserDTO getUserById(Long id) {
        log.info("Getting user by ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id.toString()));
        return userMapper.toDTO(user);
    }

    public UserDTO getUserByIdFallback(Long id, Exception e) {
        log.error("Circuit breaker fallback for getUserById({}): {}", id, e.getMessage());
        throw new ResourceNotFoundException("User service unavailable for user ID: " + id);
    }

    @Override
    @Transactional
    public UserResponseDTO getUserResponseById(Long id) {
        log.info("Getting user response by ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id.toString()));
        return userMapper.toResponseDTO(user);
    }

    @Override
    @Transactional
    public UserDTO getUserByUsername(String username) {
        log.info("Getting user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return userMapper.toDTO(user);
    }

    @Override
    @Transactional
    public UserDTO getUserByEmail(String email) {
        log.info("Getting user by email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return userMapper.toDTO(user);
    }

    @Override
    @Transactional
    public List<UserDTO> getAllUsers() {
        log.info("Getting all users");
        return userRepository.findByEnabledTrue().stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<UserResponseDTO> getAllUserResponses() {
        log.info("Getting all user responses for external services");
        return userRepository.findByEnabledTrue().stream()
                .map(userMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<UserDTO> searchUsers(String keyword) {
        log.info("Searching users with keyword: {}", keyword);
        return userRepository.searchUsers(keyword).stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDTO updateUser(Long id, UserRequest userRequest) {
        log.info("Updating user ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id.toString()));

        // Check if new username already exists (if changed)
        if (!user.getUsername().equals(userRequest.getUsername()) &&
                userRepository.existsByUsername(userRequest.getUsername())) {
            throw new DuplicateResourceException("Username", "username", userRequest.getUsername());
        }

        // Check if new email already exists (if changed)
        if (!user.getEmail().equals(userRequest.getEmail()) &&
                userRepository.existsByEmail(userRequest.getEmail())) {
            throw new DuplicateResourceException("Email", "email", userRequest.getEmail());
        }

        user.setUsername(userRequest.getUsername());
        user.setEmail(userRequest.getEmail());

        // Only update password if provided
        if (userRequest.getPassword() != null && !userRequest.getPassword().isEmpty()) {
            user.setPassword(userRequest.getPassword());
        }

        user.setFirstName(userRequest.getFirstName());
        user.setLastName(userRequest.getLastName());
        user.setPhoneNumber(userRequest.getPhoneNumber());
        user.setAddress(userRequest.getAddress());

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", id);

        return userMapper.toDTO(updatedUser);
    }

    @Override
    @Transactional
    public UserDTO updateUserProfile(Long id, UserDTO userDTO) {
        log.info("Updating user profile ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id.toString()));

        // Update profile fields (non-critical)
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setAddress(userDTO.getAddress());
        user.setCity(userDTO.getCity());
        user.setState(userDTO.getState());
        user.setCountry(userDTO.getCountry());
        user.setZipCode(userDTO.getZipCode());
        user.setProfilePictureUrl(userDTO.getProfilePictureUrl());

        User updatedUser = userRepository.save(user);
        return userMapper.toDTO(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id.toString()));
        userRepository.delete(user);
        log.info("User deleted: {}", id);
    }

    @Override
    @Transactional
    public void disableUser(Long id) {
        log.info("Disabling user ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id.toString()));
        user.setEnabled(false);
        userRepository.save(user);
        log.info("User disabled: {}", id);
    }

    @Override
    @Transactional
    public void enableUser(Long id) {
        log.info("Enabling user ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id.toString()));
        user.setEnabled(true);
        userRepository.save(user);
        log.info("User enabled: {}", id);
    }

    @Override
    @Transactional
    public boolean userExists(Long id) {
        return userRepository.existsById(id);
    }

    @Override
    @Transactional
    public long countUsers() {
        return userRepository.countActiveUsers();
    }

    @Override
    @Transactional
    public UserDTO authenticate(String username, String password) {
        log.info("Authenticating user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Simple password check
        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.getEnabled()) {
            throw new RuntimeException("User account is disabled");
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return userMapper.toDTO(user);
    }
    @Override
    @Transactional
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countActiveUsers();
        long disabledUsers = totalUsers - activeUsers;

        // Get users by role (simplified)
        List<User> allUsers = userRepository.findAll();
        Map<String, Long> usersByRole = allUsers.stream()
                .flatMap(user -> user.getRoles().stream())
                .collect(Collectors.groupingBy(role -> role, Collectors.counting()));

        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("disabledUsers", disabledUsers);
        stats.put("usersByRole", usersByRole);

        return stats;
    }
}