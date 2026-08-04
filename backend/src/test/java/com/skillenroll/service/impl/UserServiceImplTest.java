package com.skillenroll.service.impl;

import com.skillenroll.dto.UserResponse;
import com.skillenroll.entity.User;
import com.skillenroll.enums.Role;
import com.skillenroll.repository.UserRepository;
import com.skillenroll.security.service.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link UserServiceImpl#getCurrentUser()}, which must resolve
 * the caller from the {@code SecurityContext} - never from a client-supplied id.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_withAuthenticatedPrincipal_shouldReturnProfileFromContext() {
        User user = User.builder()
                .id(1L)
                .firstName("Bharath")
                .lastName("Kumar")
                .email("bharath@gmail.com")
                .phoneNumber("9876543210")
                .password("$2a$10$enc0d3d.passw0rd.hash.abcdefghijklmnopqrstuvwxyz0123456789")
                .role(Role.STUDENT)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CustomUserDetails.from(user), null));

        UserResponse response = userService.getCurrentUser();

        assertEquals(1L, response.getId());
        assertEquals("Bharath", response.getFirstName());
        assertEquals("Kumar", response.getLastName());
        assertEquals("bharath@gmail.com", response.getEmail());
        assertEquals("9876543210", response.getPhoneNumber());
        assertEquals(Role.STUDENT, response.getRole());
    }

    @Test
    void getCurrentUser_withoutAuthentication_shouldThrow() {
        SecurityContextHolder.clearContext();

        assertThrows(IllegalStateException.class, () -> userService.getCurrentUser());
    }

    @Test
    void getCurrentUser_withNonUserDetailsPrincipal_shouldThrow() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymous", null));

        assertThrows(IllegalStateException.class, () -> userService.getCurrentUser());
    }
}
