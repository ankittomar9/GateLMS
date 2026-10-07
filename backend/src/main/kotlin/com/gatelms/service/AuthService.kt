package com.gatelms.service

import com.gatelms.dto.AuthLoginRequest
import com.gatelms.dto.AuthMeResponse
import com.gatelms.entity.User
import com.gatelms.error.EntityNotFoundException
import com.gatelms.error.InvalidCredentialsException
import com.gatelms.error.RateLimitException
import com.gatelms.repository.UserRepository
import com.gatelms.security.LoginRateLimiter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val loginRateLimiter: LoginRateLimiter,
    @Value("\${app.admin.initial-password:admin123}")
    private val initialAdminPassword: String
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    @Transactional
    fun login(request: AuthLoginRequest): User {
        val username = request.username.trim()
        if (loginRateLimiter.isBlocked(username)) {
            val retryAfter = loginRateLimiter.getRetryAfterSeconds(username)
            logger.warn("Login blocked by rate limiter for username: {}", username)
            throw RateLimitException("Too many failed login attempts. Please try again later.", retryAfter)
        }

        val user = userRepository.findByUsername(username).orElse(null)
        if (user == null) {
            loginRateLimiter.recordFailure(username)
            logger.warn("Login failed: username '{}' not found", username)
            throw InvalidCredentialsException("Invalid credentials")
        }

        // Bootstrap flow: if operator password is placeholder '!boot'
        if (user.passwordHash == "!boot") {
            if (request.password == initialAdminPassword) {
                user.passwordHash = passwordEncoder.encode(request.password)
                userRepository.save(user)
                logger.info("Operator '{}' initial bootstrap password verified and hashed successfully.", username)
            } else {
                loginRateLimiter.recordFailure(username)
                logger.warn("Bootstrap login failed for '{}': incorrect initial password", username)
                throw InvalidCredentialsException("Invalid credentials")
            }
        } else {
            if (!passwordEncoder.matches(request.password, user.passwordHash)) {
                loginRateLimiter.recordFailure(username)
                logger.warn("Login failed for '{}': invalid password", username)
                throw InvalidCredentialsException("Invalid credentials")
            }
        }

        loginRateLimiter.reset(username)
        logger.info("User '{}' logged in successfully.", username)
        return user
    }

    @Transactional(readOnly = true)
    fun getMe(userId: UUID): AuthMeResponse {
        val user = userRepository.findById(userId).orElseThrow {
            EntityNotFoundException("User not found with id: $userId")
        }
        return AuthMeResponse(
            userId = user.id.toString(),
            username = user.username,
            displayName = user.displayName,
            role = user.role.name
        )
    }
}
