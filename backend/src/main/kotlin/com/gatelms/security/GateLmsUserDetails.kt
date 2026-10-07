package com.gatelms.security

import com.gatelms.entity.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

class GateLmsUserDetails(
    val userId: UUID,
    private val username: String,
    private val passwordHash: String,
    val displayName: String,
    val role: String,
    private val authorities: Collection<GrantedAuthority>
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> = authorities
    override fun getPassword(): String = passwordHash
    override fun getUsername(): String = username
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true

    companion object {
        fun fromUser(user: User): GateLmsUserDetails {
            val roleName = "ROLE_" + user.role.name
            return GateLmsUserDetails(
                userId = user.id ?: UUID.fromString("00000000-0000-0000-0000-000000000001"),
                username = user.username,
                passwordHash = user.passwordHash,
                displayName = user.displayName,
                role = user.role.name,
                authorities = listOf(SimpleGrantedAuthority(roleName))
            )
        }
    }
}
