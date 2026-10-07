package com.gatelms.integration

import com.gatelms.dto.AuthLoginRequest
import com.gatelms.dto.AuthMeResponse
import com.gatelms.dto.AuthStatusResponse
import com.gatelms.entity.User
import com.gatelms.entity.UserRole
import com.gatelms.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import org.springframework.security.crypto.password.PasswordEncoder

class AuthFlowIT : BaseIntegrationTest() {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun setupUsers() {
        authInterceptor.autoLoginEnabled = false
        authInterceptor.clear()

        // Ensure student user exists
        if (userRepository.findByUsername("test_student").isEmpty) {
            userRepository.save(
                User(
                    username = "test_student",
                    passwordHash = passwordEncoder.encode("studentPass123"),
                    displayName = "Test Student",
                    role = UserRole.STUDENT
                )
            )
        }
    }

    @Test
    fun `1 login ok with valid credentials returns 200 and sets session cookies`() {
        val req = AuthLoginRequest(username = "ankur", password = "admin123")
        val resp = restTemplate.postForEntity("/api/v1/auth/login", req, AuthMeResponse::class.java)

        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body!!
        assertEquals("ankur", body.username)
        assertEquals("ADMIN", body.role)
        assertEquals("Ankur", body.displayName)

        // Verify Set-Cookie header contains SESSION and XSRF-TOKEN
        val cookies = resp.headers[HttpHeaders.SET_COOKIE]
        assertNotNull(cookies, "Set-Cookie headers must be present")
        val joinedCookies = cookies!!.joinToString("; ")
        assertTrue(joinedCookies.contains("SESSION="), "Must contain SESSION cookie")
        assertTrue(joinedCookies.contains("XSRF-TOKEN="), "Must contain XSRF-TOKEN cookie")
    }

    @Test
    fun `2 401 on bad credentials returns generic unauthorized error`() {
        val req = AuthLoginRequest(username = "ankur", password = "wrongpassword999")
        val resp = restTemplate.postForEntity("/api/v1/auth/login", req, Map::class.java)

        assertEquals(HttpStatus.UNAUTHORIZED, resp.statusCode)
        val detail = resp.body?.get("detail")?.toString() ?: ""
        assertTrue(detail.contains("Invalid credentials"), "Error must be generic: $detail")
    }

    @Test
    fun `3 429 after 5 failures in 15 minutes`() {
        val testUser = "rate_limit_victim_${System.currentTimeMillis()}"
        userRepository.save(
            User(
                username = testUser,
                passwordHash = passwordEncoder.encode("validPass123"),
                displayName = "Victim",
                role = UserRole.STUDENT
            )
        )

        val badReq = AuthLoginRequest(username = testUser, password = "wrong_pass")
        for (i in 1..5) {
            val resp = restTemplate.postForEntity("/api/v1/auth/login", badReq, Map::class.java)
            assertEquals(HttpStatus.UNAUTHORIZED, resp.statusCode, "Attempt $i must fail with 401")
        }

        // 6th attempt should return 429
        val rateLimitedResp = restTemplate.postForEntity("/api/v1/auth/login", badReq, Map::class.java)
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, rateLimitedResp.statusCode)
        val detail = rateLimitedResp.body?.get("detail")?.toString() ?: ""
        assertTrue(detail.contains("Too many failed login attempts"), "Detail must mention rate limit: $detail")
    }

    @Test
    fun `4 me reflects session identity`() {
        // Log in as student
        val loginResp = restTemplate.postForEntity(
            "/api/v1/auth/login",
            AuthLoginRequest(username = "test_student", password = "studentPass123"),
            AuthMeResponse::class.java
        )
        assertEquals(HttpStatus.OK, loginResp.statusCode)

        // Make GET /api/v1/auth/me using captured session cookies
        val meResp = restTemplate.getForEntity("/api/v1/auth/me", AuthMeResponse::class.java)
        assertEquals(HttpStatus.OK, meResp.statusCode)
        val me = meResp.body!!
        assertEquals("test_student", me.username)
        assertEquals("STUDENT", me.role)
        assertEquals("Test Student", me.displayName)
    }

    @Test
    fun `5 logout invalidates session`() {
        // Log in as student
        val loginResp = restTemplate.postForEntity(
            "/api/v1/auth/login",
            AuthLoginRequest(username = "test_student", password = "studentPass123"),
            AuthMeResponse::class.java
        )
        assertEquals(HttpStatus.OK, loginResp.statusCode)

        // Call /api/v1/auth/logout
        val logoutResp = restTemplate.postForEntity("/api/v1/auth/logout", null, AuthStatusResponse::class.java)
        assertEquals(HttpStatus.OK, logoutResp.statusCode)
        assertEquals("LOGGED_OUT", logoutResp.body?.status)

        // Subsequent call to /api/v1/auth/me should return 401 Unauthorized
        val meAfterLogout = restTemplate.getForEntity("/api/v1/auth/me", Map::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, meAfterLogout.statusCode)
    }

    @Test
    fun `6 actuator admin-gated 403 as STUDENT but 200 for health`() {
        // Health endpoint is permitAll
        val healthResp = restTemplate.getForEntity("/actuator/health", Map::class.java)
        assertEquals(HttpStatus.OK, healthResp.statusCode)

        // Log in as STUDENT
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            AuthLoginRequest(username = "test_student", password = "studentPass123"),
            AuthMeResponse::class.java
        )

        // Actuator metrics should return 403 Forbidden for STUDENT
        val metricsResp = restTemplate.getForEntity("/actuator/metrics", Map::class.java)
        assertEquals(HttpStatus.FORBIDDEN, metricsResp.statusCode)

        // Log in as ADMIN (ankur)
        authInterceptor.clear()
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            AuthLoginRequest(username = "ankur", password = "admin123"),
            AuthMeResponse::class.java
        )

        // Actuator metrics should return 200 OK for ADMIN
        val adminMetricsResp = restTemplate.getForEntity("/actuator/metrics", Map::class.java)
        assertEquals(HttpStatus.OK, adminMetricsResp.statusCode)
    }
}
