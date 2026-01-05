package com.cbrviewer.service;

import com.cbrviewer.dto.LoginRequest;
import com.cbrviewer.dto.RegisterRequest;
import com.cbrviewer.dto.UserDto;
import com.cbrviewer.exception.InvalidCredentialsException;
import com.cbrviewer.exception.UserAlreadyExistsException;
import com.cbrviewer.model.User;
import com.cbrviewer.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User(
            request.getUsername(),
            request.getEmail(),
            hashedPassword,
            "USER"
        );

        User savedUser = userRepository.save(user);
        return UserDto.fromUser(savedUser);
    }

    public UserDto login(LoginRequest request, HttpSession session) {
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole());

        return UserDto.fromUser(user);
    }

    public UserDto getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new InvalidCredentialsException("Not authenticated");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        return UserDto.fromUser(user);
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }
}
