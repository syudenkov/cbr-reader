package com.cbrviewer.service;

import com.cbrviewer.dto.LoginRequest;
import com.cbrviewer.dto.RegisterRequest;
import com.cbrviewer.dto.UserDto;
import com.cbrviewer.exception.InvalidCredentialsException;
import com.cbrviewer.exception.UserAlreadyExistsException;
import com.cbrviewer.model.User;
import com.cbrviewer.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "hashedpassword", "USER");
        testUser.setId(1L);
    }

    @Nested
    @DisplayName("Register")
    class RegisterTests {

        @Test
        @DisplayName("should register new user successfully")
        void shouldRegisterNewUser() {
            RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "password123");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedpassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });

            UserDto result = authService.register(request);

            assertNotNull(result);
            assertEquals("newuser", result.getUsername());
            assertEquals("new@example.com", result.getEmail());
            assertEquals("USER", result.getRole());

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw exception when username already exists")
        void shouldThrowExceptionWhenUsernameExists() {
            RegisterRequest request = new RegisterRequest("existinguser", "new@example.com", "password123");

            when(userRepository.existsByUsername("existinguser")).thenReturn(true);

            UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.register(request)
            );

            assertEquals("Username already exists", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            RegisterRequest request = new RegisterRequest("newuser", "existing@example.com", "password123");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.register(request)
            );

            assertEquals("Email already exists", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        @DisplayName("should login successfully with valid credentials")
        void shouldLoginWithValidCredentials() {
            LoginRequest request = new LoginRequest("testuser", "password123");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedpassword")).thenReturn(true);

            UserDto result = authService.login(request, session);

            assertNotNull(result);
            assertEquals("testuser", result.getUsername());
            assertEquals("test@example.com", result.getEmail());

            verify(session).setAttribute("userId", 1L);
            verify(session).setAttribute("username", "testuser");
            verify(session).setAttribute("role", "USER");
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            LoginRequest request = new LoginRequest("nonexistent", "password123");

            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request, session)
            );

            assertEquals("Invalid username or password", exception.getMessage());
            verify(session, never()).setAttribute(anyString(), any());
        }

        @Test
        @DisplayName("should throw exception when password is incorrect")
        void shouldThrowExceptionWhenPasswordIncorrect() {
            LoginRequest request = new LoginRequest("testuser", "wrongpassword");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongpassword", "hashedpassword")).thenReturn(false);

            InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request, session)
            );

            assertEquals("Invalid username or password", exception.getMessage());
            verify(session, never()).setAttribute(anyString(), any());
        }
    }

    @Nested
    @DisplayName("Get Current User")
    class GetCurrentUserTests {

        @Test
        @DisplayName("should return current user when authenticated")
        void shouldReturnCurrentUserWhenAuthenticated() {
            when(session.getAttribute("userId")).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            UserDto result = authService.getCurrentUser(session);

            assertNotNull(result);
            assertEquals("testuser", result.getUsername());
        }

        @Test
        @DisplayName("should throw exception when not authenticated")
        void shouldThrowExceptionWhenNotAuthenticated() {
            when(session.getAttribute("userId")).thenReturn(null);

            InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.getCurrentUser(session)
            );

            assertEquals("Not authenticated", exception.getMessage());
        }

        @Test
        @DisplayName("should throw exception when user not found in database")
        void shouldThrowExceptionWhenUserNotFoundInDb() {
            when(session.getAttribute("userId")).thenReturn(999L);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.getCurrentUser(session)
            );

            assertEquals("User not found", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Logout")
    class LogoutTests {

        @Test
        @DisplayName("should invalidate session on logout")
        void shouldInvalidateSessionOnLogout() {
            authService.logout(session);

            verify(session).invalidate();
        }
    }
}
