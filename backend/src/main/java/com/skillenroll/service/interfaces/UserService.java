package com.skillenroll.service.interfaces;

import com.skillenroll.dto.UserRequest;
import com.skillenroll.dto.UserResponse;

import java.util.List;

/**
 * Business operations for {@link com.skillenroll.entity.User}.
 */
public interface UserService {

    UserResponse createUser(UserRequest request);

    UserResponse getUserById(Long id);

    List<UserResponse> getAllUsers();

    UserResponse updateUser(Long id, UserRequest request);

    void deleteUser(Long id);
}
