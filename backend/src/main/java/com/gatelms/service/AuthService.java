package com.gatelms.service;

import com.gatelms.dto.AuthLoginRequest;
import com.gatelms.dto.AuthMeResponse;
import com.gatelms.entity.User;
import com.gatelms.error.Exceptions.EntityNotFoundException;
import com.gatelms.error.Exceptions.InvalidCredentialsException;
import com.gatelms.error.Exceptions.RateLimitException;
import com.gatelms.repository.UserRepository;
import com.gatelms.security.LoginRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter loginRateLimiter;
    private final String initialAdminPassword;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        LoginRateLimiter loginRateLimiter,
        @Value("${app.admin.initial-password:}") String initialAdminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginRateLimiter = loginRateLimiter;
        this.initialAdminPassword = initialAdminPassword;
    }

    @Transactional
    public User login(AuthLoginRequest request) {
        String username = request.username().trim();
        if (loginRateLimiter.isBlocked(username)) {
            long retryAfter = loginRateLimiter.getRetryAfterSeconds(username);
            logger.warn("Login blocked by rate limiter for username: {}", username);
            throw new RateLimitException("Too many failed login attempts. Please try again later.", retryAfter);
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            loginRateLimiter.recordFailure(username);
            logger.warn("Login failed: username '{}' not found", username);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        // Bootstrap flow: if operator password is placeholder '!boot'
        if ("!boot".equals(user.getPasswordHash())) {
            if (initialAdminPassword == null || initialAdminPassword.isBlank()) {
                logger.error("Operator initial bootstrap password is not configured! GATELMS_ADMIN_INITIAL_PASSWORD must be provided.");
                throw new IllegalStateException("Administrator initial password is not configured on this environment. Set GATELMS_ADMIN_INITIAL_PASSWORD.");
            }
            if (initialAdminPassword.equals(request.password())) {
                user.setPasswordHash(passwordEncoder.encode(request.password()));
                userRepository.save(user);
                logger.info("Operator '{}' initial bootstrap password verified and hashed successfully.", username);
            } else {
                loginRateLimiter.recordFailure(username);
                logger.warn("Bootstrap login failed for '{}': incorrect initial password", username);
                throw new InvalidCredentialsException("Invalid credentials");
            }
        } else {
            if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                loginRateLimiter.recordFailure(username);
                logger.warn("Login failed for '{}': invalid password", username);
                throw new InvalidCredentialsException("Invalid credentials");
            }
        }

        loginRateLimiter.reset(username);
        logger.info("User '{}' logged in successfully.", username);
        return user;
    }

    @Transactional(readOnly = true)
    public AuthMeResponse getMe(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() ->
            new EntityNotFoundException("User not found with id: " + userId)
        );
        return new AuthMeResponse(
            user.getId().toString(),
            user.getUsername(),
            user.getDisplayName(),
            user.getRole().name()
        );
    }
}
