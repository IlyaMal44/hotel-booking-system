package com.example.booking.config;

import com.example.booking.domain.Booking;
import com.example.booking.domain.RoomGuard;
import com.example.booking.domain.User;
import com.example.booking.enums.BookingStatus;
import com.example.booking.enums.Role;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.RoomGuardRepository;
import com.example.booking.repository.UserRepository;
import com.example.common.DemoStay;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder {

    @Bean
    CommandLineRunner seedUsers(UserRepository users,
                                BookingRepository bookings,
                                RoomGuardRepository guards,
                                PasswordEncoder passwordEncoder) {
        return args -> {
            if (users.count() > 0) {
                return;
            }
            user(users, passwordEncoder, "admin", "admin123", Role.ADMIN);
            User guest = user(users, passwordEncoder, "user", "user123", Role.USER);
            user(users, passwordEncoder, "guest", "guest123", Role.USER);

            Booking booking = new Booking();
            booking.setUser(guest);
            booking.setRoomId(1L);
            booking.setStartDate(DemoStay.start());
            booking.setEndDate(DemoStay.end());
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setRequestId(DemoStay.REQUEST_ID);
            Instant now = Instant.now();
            booking.setCreatedAt(now);
            booking.setUpdatedAt(now);
            bookings.save(booking);
            guards.save(new RoomGuard(1L));
            log.info("Seeded users admin/admin123, user/user123, guest/guest123. Demo booking id will occupy room 1 on {}..{}",
                    DemoStay.start(), DemoStay.end());
        };
    }

    private User user(UserRepository users, PasswordEncoder encoder, String username, String password, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        user.setRole(role);
        user.setCreatedAt(Instant.now());
        return users.save(user);
    }
}
