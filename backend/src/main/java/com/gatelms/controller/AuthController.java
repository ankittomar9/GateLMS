package com.gatelms.controller;

import com.gatelms.dto.AuthLoginRequest;
import com.gatelms.dto.AuthMeResponse;
import com.gatelms.dto.AuthStatusResponse;
import com.gatelms.entity.User;
import com.gatelms.security.CurrentUserProvider;
import com.gatelms.security.GateLmsUserDetails;
import com.gatelms.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserProvider currentUserProvider;

    public AuthController(AuthService authService, CurrentUserProvider currentUserProvider) {
        this.authService = authService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthMeResponse> login(
            @Valid @RequestBody AuthLoginRequest loginRequest,
            HttpServletRequest httpRequest
    ) {
        User user = authService.login(loginRequest);
        GateLmsUserDetails userDetails = GateLmsUserDetails.fromUser(user);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        AuthMeResponse response = new AuthMeResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole().name()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthStatusResponse> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new AuthStatusResponse("LOGGED_OUT"));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> me() {
        UUID userId = currentUserProvider.getCurrentUserId();
        AuthMeResponse response = authService.getMe(userId);
        return ResponseEntity.ok(response);
    }
}
