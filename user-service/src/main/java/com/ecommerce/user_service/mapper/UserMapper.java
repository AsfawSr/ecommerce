package com.ecommerce.user_service.mapper;

import com.ecommerce.user_service.dto.UserDTO;
import com.ecommerce.user_service.dto.UserRequest;
import com.ecommerce.user_service.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "zipCode", ignore = true)
    @Mapping(target = "profilePictureUrl", ignore = true)
    User toEntity(UserRequest userRequest);

    UserDTO toDTO(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "zipCode", ignore = true)
    @Mapping(target = "profilePictureUrl", ignore = true)
    void updateEntityFromDTO(UserDTO userDTO, @org.mapstruct.MappingTarget User user);
}