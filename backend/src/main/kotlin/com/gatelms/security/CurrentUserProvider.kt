package com.gatelms.security

import com.gatelms.entity.User
import com.gatelms.entity.UserRole
import com.gatelms.repository.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CurrentUserProvider(
    private val userRepository: UserRepository
) {
    val defaultOperatorId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")

    fun getCurrentUserId(): UUID {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth != null && auth.isAuthenticated && auth.principal != "anonymousUser") {
            val principal = auth.principal
            if (principal is GateLmsUserDetails) {
                return principal.userId
            }
            if (principal is org.springframework.security.core.userdetails.UserDetails) {
                val user = userRepository.findByUsername(principal.username)
                if (user.isPresent) {
                    return user.get().id ?: defaultOperatorId
                }
            }
            if (principal is String) {
                val user = userRepository.findByUsername(principal)
                if (user.isPresent) {
                    return user.get().id ?: defaultOperatorId
                }
            }
        }
        return defaultOperatorId
    }

    fun getCurrentUser(): User {
        val id = getCurrentUserId()
        return userRepository.findById(id).orElseGet {
            userRepository.findByUsername("ankur").orElseGet {
                User(
                    id = defaultOperatorId,
                    username = "ankur",
                    passwordHash = "!boot",
                    displayName = "Ankur",
                    role = UserRole.ADMIN
                )
            }
        }
    }
}
