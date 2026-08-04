package com.skillenroll.service.interfaces;

import com.skillenroll.dto.UserRequest;
import com.skillenroll.dto.UserResponse;

import java.util.List;

/**
 * Business operations for {@link com.skillenroll.entity.User}.
 */
public interface UserService {

    UserResponse createUser(UserRequest request);

    /**
     * Returns the profile of the currently authenticated user, resolved from
     * the {@code SecurityContext} - never from a client-supplied id.
     */
    UserResponse getCurrentUser();

    UserResponse getUserById(Long id);

    List<UserResponse> getAllUsers();

    UserResponse updateUser(Long id, UserRequest request);

    void deleteUser(Long id);
}
