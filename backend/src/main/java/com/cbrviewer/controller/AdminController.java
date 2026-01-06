package com.cbrviewer.controller;

import com.cbrviewer.dto.*;
import com.cbrviewer.exception.InvalidCredentialsException;
import com.cbrviewer.service.LlmConfigService;
import com.cbrviewer.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final LlmConfigService llmConfigService;

    public AdminController(UserService userService, LlmConfigService llmConfigService) {
        this.userService = userService;
        this.llmConfigService = llmConfigService;
    }

    private Long getCurrentUserId(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new InvalidCredentialsException("Not authenticated");
        }
        return userId;
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort) {

        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equals("asc")
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        Page<UserResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(
        @Valid @RequestBody UserCreateRequest request,
        HttpSession session) {

        Long currentAdminId = getCurrentUserId(session);
        UserResponse user = userService.createUser(request, currentAdminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UserUpdateRequest request,
        HttpSession session) {

        Long currentAdminId = getCurrentUserId(session);
        UserResponse user = userService.updateUser(id, request, currentAdminId);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(
        @PathVariable Long id,
        HttpSession session) {

        Long currentAdminId = getCurrentUserId(session);
        userService.deleteUser(id, currentAdminId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<Void> assignRole(
        @PathVariable Long id,
        @RequestBody Map<String, String> request,
        HttpSession session) {

        String role = request.get("role");
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }

        Long currentAdminId = getCurrentUserId(session);
        userService.assignRole(id, role, currentAdminId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/{id}/activity")
    public ResponseEntity<List<AuditLogDto>> getUserActivity(@PathVariable Long id) {
        List<AuditLogDto> activity = userService.getUserActivity(id);
        return ResponseEntity.ok(activity);
    }

    // LLM Configuration Management Endpoints

    @GetMapping("/llm-config")
    public ResponseEntity<List<LlmConfigDto>> getAllLlmConfigs() {
        List<LlmConfigDto> configs = llmConfigService.getAllConfigs();
        return ResponseEntity.ok(configs);
    }

    @PutMapping("/llm-config/{id}")
    public ResponseEntity<LlmConfigDto> updateLlmConfig(
        @PathVariable Long id,
        @Valid @RequestBody UpdateLlmConfigRequest request,
        HttpSession session) {

        Long currentAdminId = getCurrentUserId(session);
        LlmConfigDto updated = llmConfigService.updateConfig(id, request, currentAdminId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/llm-config/test/{id}")
    public ResponseEntity<LlmConfigTestResult> testLlmConfig(
        @PathVariable Long id,
        HttpSession session) {

        Long currentAdminId = getCurrentUserId(session);
        LlmConfigTestResult result = llmConfigService.testConfig(id, currentAdminId);
        return ResponseEntity.ok(result);
    }
}
