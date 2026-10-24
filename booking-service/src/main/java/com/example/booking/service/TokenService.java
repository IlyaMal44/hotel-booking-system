package com.example.booking.service;

import com.example.booking.domain.User;
import com.example.booking.dto.AuthResponse;
import com.example.booking.mapper.BookingMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final BookingMapper bookingMapper;
    private final Duration ttl;

    public TokenService(JwtEncoder jwtEncoder,
                        BookingMapper bookingMapper,
                        @Value("${app.jwt.ttl:PT1H}") Duration ttl) {
        this.jwtEncoder = jwtEncoder;
        this.bookingMapper = bookingMapper;
        this.ttl = ttl;
    }

    public AuthResponse issue(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("booking-service")
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(user.getUsername())
                .claim("roles", List.of(user.getRole().name()))
                .claim("userId", user.getId())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", ttl.toSeconds(), bookingMapper.toResponse(user));
    }
}
