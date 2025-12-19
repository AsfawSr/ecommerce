package com.ecommerce.user_service.service;

import com.ecommerce.user_service.dto.UserDTO;
import com.ecommerce.user_service.dto.UserRequest;
import com.ecommerce.user_service.dto.UserResponseDTO;

import java.util.List;
import java.util.Map;

public interface UserService {

    UserDTO createUser(UserRequest userRequest);
    UserDTO getUserById(Long id);
    UserDTO getUserByUsername(String username);
    UserDTO getUserByEmail(String email);
    List<UserDTO> getAllUsers();
    List<UserDTO> searchUsers(String keyword);
    UserDTO updateUser(Long id, UserRequest userRequest);
    UserDTO updateUserProfile(Long id, UserDTO userDTO);
    void deleteUser(Long id);
    void disableUser(Long id);
    void enableUser(Long id);
    boolean userExists(Long id);
    long countUsers();
    UserDTO authenticate(String username, String password);

    UserResponseDTO getUserResponseById(Long id);
    List<UserResponseDTO> getAllUserResponses();

    public Map<String, Object> getUserStatistics();
}