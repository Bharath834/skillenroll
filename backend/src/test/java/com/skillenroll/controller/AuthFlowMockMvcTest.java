package com.skillenroll.controller;

import com.jayway.jsonpath.JsonPath;
import com.skillenroll.entity.User;
import com.skillenroll.enums.Role;
import com.skillenroll.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end MockMvc tests against the real security chain (filter + services)
 * with an in-memory H2 database. Every test rolls back its own data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthFlowMockMvcTest {

    private static final String STUDENT_EMAIL = "student@test.com";
    private static final String STUDENT_PHONE = "9111111111";
    private static final String PASSWORD = "Passw0rd!";

    /** Access token + refresh token returned by register/login. */
    private record TokenPair(String token, String refreshToken) {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------------
    // Register
    // ------------------------------------------------------------------

    @Test
    void register_shouldReturnCreatedWithTokenAndRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(STUDENT_EMAIL, STUDENT_PHONE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.role").value("STUDENT"));
    }

    @Test
    void register_withDuplicateEmail_shouldReturnConflict() throws Exception {
        registerStudent();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(STUDENT_EMAIL, "9222222222")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ------------------------------------------------------------------
    // Login
    // ------------------------------------------------------------------

    @Test
    void login_shouldReturnJwtForValidCredentials() throws Exception {
        registerStudent();
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(STUDENT_EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void login_withWrongPassword_shouldReturnUnauthorized() throws Exception {
        registerStudent();
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(STUDENT_EMAIL, "Wrong123!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ------------------------------------------------------------------
    // Refresh
    // ------------------------------------------------------------------

    @Test
    void refresh_shouldRotateTokenPair() throws Exception {
        TokenPair pair = registerStudentPair();

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + pair.refreshToken() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        String rotatedRefreshToken = JsonPath.parse(result.getResponse().getContentAsString())
                .read("$.data.refreshToken");
        assertThat(rotatedRefreshToken).isNotEqualTo(pair.refreshToken());

        // Reusing the rotated token must trigger reuse detection (409).
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + pair.refreshToken() + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void refresh_withUnknownToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"unknown-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Logout
    // ------------------------------------------------------------------

    @Test
    void logout_shouldRevokeRefreshTokenAndBlacklistAccessToken() throws Exception {
        TokenPair pair = registerStudentPair();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + pair.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + pair.refreshToken() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // The logged-out access token must now be rejected (blacklisted).
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + pair.token()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("JWT token has been revoked"));
    }

    @Test
    void logout_withoutAccessToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"whatever\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Protected API
    // ------------------------------------------------------------------

    @Test
    void protectedApi_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void protectedApi_withValidToken_shouldReturnOk() throws Exception {
        String token = registerStudent();
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ------------------------------------------------------------------
    // Role authorization
    // ------------------------------------------------------------------

    @Test
    void createUser_withStudentToken_shouldReturnForbidden() throws Exception {
        String studentToken = registerStudent();
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userBody("someone@test.com", "9333333333")))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_withAdminToken_shouldReturnCreated() throws Exception {
        createAdminUser();
        String adminToken = login("admin@test.com", PASSWORD);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userBody("new.user@test.com", "9444444444")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String registerStudent() throws Exception {
        return registerStudentPair().token();
    }

    private TokenPair registerStudentPair() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(STUDENT_EMAIL, STUDENT_PHONE)))
                .andExpect(status().isCreated())
                .andReturn();
        return parsePair(result);
    }

    private TokenPair parsePair(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return new TokenPair(
                JsonPath.parse(body).read("$.data.token"),
                JsonPath.parse(body).read("$.data.refreshToken"));
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return parsePair(result).token();
    }

    private void createAdminUser() {
        userRepository.save(User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@test.com")
                .phoneNumber("9000000001")
                .password(passwordEncoder.encode(PASSWORD))
                .role(Role.ADMIN)
                .build());
    }

    private String registerBody(String email, String phone) {
        return """
                {"firstName":"Bharath","lastName":"Kumar","email":"%s","phoneNumber":"%s","password":"%s"}
                """.formatted(email, phone, PASSWORD).strip();
    }

    private String loginBody(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password).strip();
    }

    /** Body for the admin-only POST /api/users endpoint (UserRequest). */
    private String userBody(String email, String phone) {
        return """
                {"firstName":"Bharath","lastName":"Kumar","email":"%s","phoneNumber":"%s","password":"%s","role":"STUDENT"}
                """.formatted(email, phone, PASSWORD).strip();
    }
}
