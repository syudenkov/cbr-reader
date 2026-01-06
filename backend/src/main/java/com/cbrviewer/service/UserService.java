package com.cbrviewer.service;

import com.cbrviewer.dto.AuditLogDto;
import com.cbrviewer.dto.UserCreateRequest;
import com.cbrviewer.dto.UserResponse;
import com.cbrviewer.dto.UserUpdateRequest;
import com.cbrviewer.exception.InvalidRoleException;
import com.cbrviewer.exception.UserAlreadyExistsException;
import com.cbrviewer.exception.UserNotFoundException;
import com.cbrviewer.model.AuditLog;
import com.cbrviewer.model.User;
import com.cbrviewer.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository,
                      BCryptPasswordEncoder passwordEncoder,
                      AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAllActive(pageable);
        return users.map(UserResponse::fromUser);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findByIdActive(id)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        return UserResponse.fromUser(user);
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request, Long currentAdminId) {
        // Check uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        // Validate role
        if (!request.getRole().equals("USER") && !request.getRole().equals("ADMIN")) {
            throw new InvalidRoleException("Role must be USER or ADMIN");
        }

        // Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        User savedUser = userRepository.save(user);

        // Log audit event
        auditLogService.log(currentAdminId, "CREATE_USER", "User", savedUser.getId(),
            String.format("{\"username\": \"%s\", \"role\": \"%s\"}",
                savedUser.getUsername(), savedUser.getRole()));

        return UserResponse.fromUser(savedUser);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request, Long currentAdminId) {
        User user = userRepository.findByIdActive(id)
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        boolean updated = false;

        // Update username if provided
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new UserAlreadyExistsException("Username already exists");
            }
            user.setUsername(request.getUsername());
            updated = true;
        }

        // Update email if provided
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email already exists");
            }
            user.setEmail(request.getEmail());
            updated = true;
        }

        // Update password if provided
        if (request.getPassword() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            updated = true;
        }

        // Update role if provided
        if (request.getRole() != null && !request.getRole().equals(user.getRole())) {
            if (!request.getRole().equals("USER") && !request.getRole().equals("ADMIN")) {
                throw new InvalidRoleException("Role must be USER or ADMIN");
            }
            user.setRole(request.getRole());
            updated = true;
        }

        if (updated) {
            User savedUser = userRepository.save(user);

            // Log audit event
            auditLogService.log(currentAdminId, "UPDATE_USER", "User", savedUser.getId(),
                String.format("{\"userId\": %d}", savedUser.getId()));

            return UserResponse.fromUser(savedUser);
        }

        return UserResponse.fromUser(user);
    }

    @Transactional
    public void deleteUser(Long id, Long currentAdminId) {
        User user = userRepository.findByIdActive(id)
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Soft delete
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        // Log audit event
        auditLogService.log(currentAdminId, "DELETE_USER", "User", id, "Soft delete");
    }

    @Transactional
    public void assignRole(Long userId, String role, Long currentAdminId) {
        // Validate role
        if (!role.equals("USER") && !role.equals("ADMIN")) {
            throw new InvalidRoleException("Role must be USER or ADMIN");
        }

        User user = userRepository.findByIdActive(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Prevent self-demotion
        if (userId.equals(currentAdminId) && role.equals("USER") && user.getRole().equals("ADMIN")) {
            throw new InvalidRoleException("Cannot demote yourself");
        }

        // Prevent removing last admin
        if (user.getRole().equals("ADMIN") && role.equals("USER")) {
            long adminCount = userRepository.countByRole("ADMIN");
            if (adminCount <= 1) {
                throw new InvalidRoleException("Cannot remove last admin");
            }
        }

        String oldRole = user.getRole();
        user.setRole(role);
        userRepository.save(user);

        // Log audit event
        auditLogService.log(currentAdminId, "ASSIGN_ROLE", "User", userId,
            String.format("{\"oldRole\": \"%s\", \"newRole\": \"%s\"}", oldRole, role));
    }

    public List<AuditLogDto> getUserActivity(Long userId) {
        List<AuditLog> logs = auditLogService.getUserActivity(userId, 20);
        return logs.stream()
            .map(AuditLogDto::fromAuditLog)
            .collect(Collectors.toList());
    }
}
