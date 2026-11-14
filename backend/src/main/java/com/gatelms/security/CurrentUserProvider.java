package com.gatelms.security;

import com.gatelms.entity.User;
import com.gatelms.entity.UserRole;
import com.gatelms.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import java.util.UUID;

@Component
public class CurrentUserProvider {
    public static final UUID DEFAULT_OPERATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UUID getDefaultOperatorId() {
        return DEFAULT_OPERATOR_ID;
    }

    public UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Object principal = auth.getPrincipal();
            if (principal instanceof GateLmsUserDetails details) {
                return details.getUserId();
            }
            if (principal instanceof UserDetails ud) {
                return userRepository.findByUsername(ud.getUsername())
                    .map(User::getId)
                    .orElse(DEFAULT_OPERATOR_ID);
            }
            if (principal instanceof String str) {
                return userRepository.findByUsername(str)
                    .map(User::getId)
                    .orElse(DEFAULT_OPERATOR_ID);
            }
        }
        boolean isWebRequest = RequestContextHolder.getRequestAttributes() != null;
        if (isWebRequest) {
            throw new AuthenticationCredentialsNotFoundException("User is unauthenticated");
        }
        return DEFAULT_OPERATOR_ID;
    }

    public User getCurrentUser() {
        UUID id = getCurrentUserId();
        return userRepository.findById(id).orElseGet(() ->
            userRepository.findByUsername("ankur").orElseGet(() ->
                new User(
                    DEFAULT_OPERATOR_ID,
                    "ankur",
                    "!boot",
                    "Ankur",
                    UserRole.ADMIN,
                    java.time.Instant.now()
                )
            )
        );
    }
}
