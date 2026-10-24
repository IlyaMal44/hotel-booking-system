package com.example.booking.service;

import com.example.booking.domain.User;
import com.example.booking.dto.AdminCreateUserRequest;
import com.example.booking.dto.AuthRequest;
import com.example.booking.dto.AuthResponse;
import com.example.booking.dto.RegisterRequest;
import com.example.booking.dto.UpdateUserRequest;
import com.example.booking.dto.UserResponse;
import com.example.booking.enums.Role;
import com.example.booking.exception.ApiException;
import com.example.booking.exception.BadRequestException;
import com.example.booking.exception.ConflictException;
import com.example.booking.exception.NotFoundException;
import com.example.booking.mapper.BookingMapper;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final BookingMapper bookingMapper;

    public UserAccountService(UserRepository userRepository,
                              BookingRepository bookingRepository,
                              PasswordEncoder passwordEncoder,
                              TokenService tokenService,
                              BookingMapper bookingMapper) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.bookingMapper = bookingMapper;
    }

    @Transactional
    /** Регистрирует пользователя с ролью USER и сразу выдает JWT */
    public AuthResponse register(RegisterRequest request) {
        User user = create(request.username(), request.password(), Role.USER);
        return tokenService.issue(user);
    }

    @Transactional(readOnly = true)
    /** Проверяет логин и пароль и выдает JWT на час */
    public AuthResponse authenticate(AuthRequest request) {
        User user = userRepository.findByUsername(request.username())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        return tokenService.issue(user);
    }

    @Transactional
    /** Создает пользователя с ролью, которую передал админ */
    public UserResponse createByAdmin(AdminCreateUserRequest request) {
        return bookingMapper.toResponse(create(request.username(), request.password(), request.role()));
    }

    @Transactional
    /** Меняет пароль и/или роль существующего пользователя */
    public UserResponse update(UpdateUserRequest request) {
        if ((request.password() == null || request.password().isBlank()) && request.role() == null) {
            throw new BadRequestException("Password or role must be provided");
        }
        User user = userRepository.findById(request.id()).orElseThrow(() -> new NotFoundException("User not found"));
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        return bookingMapper.toResponse(user);
    }

    @Transactional
    /** Удаляет пользователя, если это не он сам и у него нет броней */
    public void delete(Long id, String actor) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getUsername().equals(actor)) {
            throw new ConflictException("You cannot delete your own account");
        }
        if (bookingRepository.existsByUserId(id)) {
            throw new ConflictException("User still has bookings");
        }
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    /** Возвращает страницу пользователей для админа */
    public Page<UserResponse> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(bookingMapper::toResponse);
    }

    /** Сохраняет пользователя с захешированным паролем */
    private User create(String username, String password, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username is already taken");
        }
        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }
}
