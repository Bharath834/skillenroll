package com.skillenroll.service.impl;

import com.skillenroll.dto.request.LoginRequest;
import com.skillenroll.dto.request.RegisterRequest;
import com.skillenroll.dto.response.JwtResponse;
import com.skillenroll.entity.User;
import com.skillenroll.enums.Role;
import com.skillenroll.exception.DuplicateResourceException;
import com.skillenroll.mapper.UserMapper;
import com.skillenroll.repository.UserRepository;
import com.skillenroll.security.jwt.JwtService;
import com.skillenroll.security.service.CustomUserDetailsService;
import com.skillenroll.service.interfaces.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements registration and login. Passwords are stored exclusively as
 * BCrypt hashes; plain-text credentials never reach the database.
 */
@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           CustomUserDetailsService customUserDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    @Transactional
    public JwtResponse register(RegisterRequest request) {
        log.info("Registration started for email: {}", request.getEmail());
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration rejected - email already exists: {}", request.getEmail());
            throw new DuplicateResourceException("User with email '" + request.getEmail() + "' already exists");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            log.warn("Registration rejected - phone number already exists: {}", request.getPhoneNumber());
            throw new DuplicateResourceException("User with phone number '" + request.getPhoneNumber() + "' already exists");
        }
        // Public self-registration is always a STUDENT; higher roles are
        // granted only through the protected admin/user management endpoints.
        User user = UserMapper.toEntity(request, passwordEncoder.encode(request.getPassword()), Role.STUDENT);
        User saved = userRepository.save(user);
        log.info("User saved with id {} and role {}", saved.getId(), saved.getRole());
        JwtResponse response = buildJwtResponse(saved);
        log.info("Registration successful for email: {}", saved.getEmail());
        return response;
    }

    @Override
    public JwtResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (AuthenticationException ex) {
            log.warn("Authentication failed for email: {} ({})", request.getEmail(), ex.getClass().getSimpleName());
            throw ex;
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        JwtResponse response = buildJwtResponse(user);
        log.info("Login successful for email: {}", email);
        return response;
    }

    private JwtResponse buildJwtResponse(User user) {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);
        log.info("JWT generated for user: {}", user.getEmail());
        return JwtResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs() / 1000)
                .user(UserMapper.toResponse(user))
                .build();
    }
}
