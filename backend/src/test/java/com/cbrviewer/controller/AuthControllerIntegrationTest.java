package com.cbrviewer.controller;

import com.cbrviewer.dto.LoginRequest;
import com.cbrviewer.dto.RegisterRequest;
import com.cbrviewer.model.User;
import com.cbrviewer.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = new User(
            "testuser",
            "test@example.com",
            passwordEncoder.encode("password123"),
            "USER"
        );
        testUser = userRepository.save(testUser);
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("should register new user successfully")
        void shouldRegisterNewUser() throws Exception {
            RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "password123");

            mockMvc.perform(post("/api/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("newuser")))
                .andExpect(jsonPath("$.email", is("new@example.com")))
                .andExpect(jsonPath("$.role", is("USER")));
        }

        @Test
        @DisplayName("should return 400 for duplicate username")
        void shouldRejectDuplicateUsername() throws Exception {
            RegisterRequest request = new RegisterRequest("testuser", "another@example.com", "password123");

            mockMvc.perform(post("/api/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 400 for invalid email")
        void shouldRejectInvalidEmail() throws Exception {
            RegisterRequest request = new RegisterRequest("validuser", "invalid-email", "password123");

            mockMvc.perform(post("/api/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for short password")
        void shouldRejectShortPassword() throws Exception {
            RegisterRequest request = new RegisterRequest("validuser", "valid@example.com", "12345");

            mockMvc.perform(post("/api/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for blank username")
        void shouldRejectBlankUsername() throws Exception {
            RegisterRequest request = new RegisterRequest("", "valid@example.com", "password123");

            mockMvc.perform(post("/api/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("should login successfully with valid credentials")
        void shouldLoginWithValidCredentials() throws Exception {
            LoginRequest request = new LoginRequest("testuser", "password123");

            mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.email", is("test@example.com")))
                .andExpect(jsonPath("$.role", is("USER")));
        }

        @Test
        @DisplayName("should return 401 for wrong password")
        void shouldRejectWrongPassword() throws Exception {
            LoginRequest request = new LoginRequest("testuser", "wrongpassword");

            mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 401 for non-existent user")
        void shouldRejectNonExistentUser() throws Exception {
            LoginRequest request = new LoginRequest("nonexistent", "password123");

            mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 400 for blank username")
        void shouldRejectBlankUsername() throws Exception {
            LoginRequest request = new LoginRequest("", "password123");

            mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("should return current user when authenticated")
        void shouldReturnCurrentUserWhenAuthenticated() throws Exception {
            // First login to get session
            LoginRequest loginRequest = new LoginRequest("testuser", "password123");

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

            MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

            // Then get current user
            mockMvc.perform(get("/api/auth/me")
                    .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.email", is("test@example.com")));
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void shouldReturnForbiddenWhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("should logout successfully")
        void shouldLogoutSuccessfully() throws Exception {
            // First login
            LoginRequest loginRequest = new LoginRequest("testuser", "password123");

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

            MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

            // Logout
            mockMvc.perform(post("/api/auth/logout")
                    .with(csrf())
                    .session(session))
                .andExpect(status().isOk());

            // Session should be invalidated - create new request should return 403
            mockMvc.perform(get("/api/auth/me")
                    .session(session))
                .andExpect(status().isForbidden());
        }
    }
}
