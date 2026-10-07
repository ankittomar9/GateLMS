package com.gatelms.controller

import com.gatelms.dto.AuthLoginRequest
import com.gatelms.dto.AuthMeResponse
import com.gatelms.dto.AuthStatusResponse
import com.gatelms.security.CurrentUserProvider
import com.gatelms.security.GateLmsUserDetails
import com.gatelms.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val currentUserProvider: CurrentUserProvider
) {

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody loginRequest: AuthLoginRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<AuthMeResponse> {
        val user = authService.login(loginRequest)
        val userDetails = GateLmsUserDetails.fromUser(user)

        val authentication = UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.authorities
        )

        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = authentication
        SecurityContextHolder.setContext(context)

        val session = httpRequest.getSession(true)
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            context
        )

        val response = AuthMeResponse(
            userId = user.id.toString(),
            username = user.username,
            displayName = user.displayName,
            role = user.role.name
        )

        return ResponseEntity.ok(response)
    }

    @PostMapping("/logout")
    fun logout(httpRequest: HttpServletRequest): ResponseEntity<AuthStatusResponse> {
        val session = httpRequest.getSession(false)
        session?.invalidate()
        SecurityContextHolder.clearContext()
        return ResponseEntity.ok(AuthStatusResponse(status = "LOGGED_OUT"))
    }

    @GetMapping("/me")
    fun me(): ResponseEntity<AuthMeResponse> {
        val userId = currentUserProvider.getCurrentUserId()
        val response = authService.getMe(userId)
        return ResponseEntity.ok(response)
    }
}
