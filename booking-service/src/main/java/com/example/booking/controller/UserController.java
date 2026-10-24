package com.example.booking.controller;

import com.example.booking.dto.AdminCreateUserRequest;
import com.example.booking.dto.AuthRequest;
import com.example.booking.dto.AuthResponse;
import com.example.booking.dto.RegisterRequest;
import com.example.booking.dto.UpdateUserRequest;
import com.example.booking.dto.UserResponse;
import com.example.booking.service.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/user", "/user"})
@Tag(name = "Users")
public class UserController {

    private final UserAccountService userAccountService;

    public UserController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @PostMapping("/register")
    @Operation(summary = "Зарегистрировать пользователя и получить JWT на один час")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(userAccountService.register(request));
    }

    @PostMapping("/auth")
    @Operation(summary = "Войти и получить JWT на один час")
    public AuthResponse auth(@Valid @RequestBody AuthRequest request) {
        return userAccountService.authenticate(request);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    /** Админ создает пользователя с выбранной ролью */
    public ResponseEntity<UserResponse> create(@Valid @RequestBody AdminCreateUserRequest request) {
        return ResponseEntity.status(201).body(userAccountService.createByAdmin(request));
    }

    @PatchMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    /** Админ меняет пароль или роль, id лежит в теле */
    public UserResponse update(@Valid @RequestBody UpdateUserRequest request) {
        return userAccountService.update(request);
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    /** Админ удаляет пользователя по id из query */
    public ResponseEntity<Void> delete(@RequestParam Long id, @AuthenticationPrincipal Jwt jwt) {
        userAccountService.delete(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    /** Список пользователей для админа */
    public Page<UserResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return userAccountService.list(pageable);
    }
}
