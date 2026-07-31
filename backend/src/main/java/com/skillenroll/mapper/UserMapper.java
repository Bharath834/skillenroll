package com.skillenroll.mapper;

import com.skillenroll.dto.UserRequest;
import com.skillenroll.dto.UserResponse;
import com.skillenroll.dto.request.RegisterRequest;
import com.skillenroll.entity.User;
import com.skillenroll.enums.Role;

/**
 * Manual mapping between {@link User}, {@link UserRequest} and {@link UserResponse}.
 * The password is never copied into a response DTO.
 */
public final class UserMapper {

    private UserMapper() {
    }

    /**
     * Maps a {@link RegisterRequest} into a {@link User}. The password is
     * already BCrypt-encoded by the caller; the role is resolved by the service
     * (defaults to {@link Role#STUDENT} for public registrations).
     */
    public static User toEntity(RegisterRequest request, String encodedPassword, Role role) {
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(encodedPassword);
        user.setRole(role);
        return user;
    }

    public static User toEntity(UserRequest request) {
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(request.getPassword());
        user.setRole(request.getRole());
        return user;
    }

    public static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public static void updateEntity(User user, UserRequest request) {
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(request.getPassword());
        }
        user.setRole(request.getRole());
    }
}
