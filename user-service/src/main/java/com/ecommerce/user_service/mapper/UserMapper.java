// UserMapper.java
package com.ecommerce.user_service.mapper;

import com.ecommerce.user_service.dto.UserDTO;
import com.ecommerce.user_service.dto.UserResponseDTO;
import com.ecommerce.user_service.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    UserDTO toDTO(User user);

    User toEntity(UserDTO userDTO);

    @Mapping(target = "active", source = "enabled")
    UserResponseDTO toResponseDTO(User user);

    @Mapping(target = "enabled", source = "active")
    User fromResponseDTO(UserResponseDTO responseDTO);
}