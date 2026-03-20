package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.dispatcher.api.dto.AuthResponse;
import com.cloudbalancer.dispatcher.api.dto.LoginRequest;
import com.cloudbalancer.dispatcher.api.dto.RefreshRequest;
import com.cloudbalancer.dispatcher.security.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthController(UserService userService, JwtService jwtService,
                          RefreshTokenRepository refreshTokenRepository) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return userService.authenticate(request.username(), request.password())
            .map(user -> {
                String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole());
                String refreshToken = createRefreshToken(user);
                return ResponseEntity.ok(new AuthResponse(
                    accessToken, refreshToken, jwtService.getAccessTokenExpirationSeconds()
                ));
            })
            .orElse(ResponseEntity.status(401).build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        return refreshTokenRepository.findByToken(request.refreshToken())
            .filter(token -> !token.isRevoked())
            .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
            .map(token -> {
                // Revoke old token (rotation)
                token.setRevoked(true);
                refreshTokenRepository.save(token);

                User user = token.getUser();
                String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole());
                String newRefreshToken = createRefreshToken(user);
                return ResponseEntity.ok(new AuthResponse(
                    accessToken, newRefreshToken, jwtService.getAccessTokenExpirationSeconds()
                ));
            })
            .orElse(ResponseEntity.status(401).build());
    }

    private String createRefreshToken(User user) {
        var token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(jwtService.getRefreshTokenExpirationSeconds()));
        token.setRevoked(false);
        refreshTokenRepository.save(token);
        return token.getToken();
    }
}
