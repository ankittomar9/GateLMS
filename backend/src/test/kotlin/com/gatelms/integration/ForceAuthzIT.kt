package com.gatelms.integration

import com.gatelms.entity.User
import com.gatelms.entity.UserRole
import com.gatelms.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID

class ForceAuthzIT : BaseIntegrationTest() {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private val studentUsername = "force_authz_student"

    @BeforeEach
    fun setup() {
        if (userRepository.findByUsername(studentUsername).isEmpty) {
            userRepository.save(
                User(
                    id = UUID.randomUUID(),
                    username = studentUsername,
                    displayName = "Student Tester",
                    passwordHash = passwordEncoder.encode("password123"),
                    role = UserRole.STUDENT
                )
            )
        }
    }

    @Test
    fun `G1 - student calling primer generate with force=true returns 403 ProblemDetail`() {
        loginAs(studentUsername, "password123")
        val resp = restTemplate.exchange(
            "/api/v1/journey/topics/topic_os_process_sync/primer/generate?force=true",
            HttpMethod.POST,
            null,
            Map::class.java
        )
        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
        assertEquals(403, resp.body?.get("status"))
        assertEquals("about:blank", resp.body?.get("type"))
        assertEquals("Forbidden", resp.body?.get("title"))
        assertEquals("force=true requires ADMIN", resp.body?.get("detail"))
    }

    @Test
    fun `G1 - student calling lesson generate with force=true returns 403 ProblemDetail`() {
        loginAs(studentUsername, "password123")
        val resp = restTemplate.exchange(
            "/api/v1/journey/concepts/critical-section-requirements/lesson/generate?force=true",
            HttpMethod.POST,
            null,
            Map::class.java
        )
        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
        assertEquals(403, resp.body?.get("status"))
        assertEquals("about:blank", resp.body?.get("type"))
        assertEquals("Forbidden", resp.body?.get("title"))
        assertEquals("force=true requires ADMIN", resp.body?.get("detail"))
    }

    @Test
    fun `G2 - student calling generation with force absent or false does not trigger force 403`() {
        loginAs(studentUsername, "password123")
        val primerResp = restTemplate.exchange(
            "/api/v1/journey/topics/topic_os_process_sync/primer/generate?force=false",
            HttpMethod.POST,
            null,
            Map::class.java
        )
        assert(primerResp.statusCode != HttpStatus.FORBIDDEN) {
            "Non-forced primer generation for student must not return 403, got ${primerResp.statusCode}"
        }

        val lessonResp = restTemplate.exchange(
            "/api/v1/journey/concepts/critical-section-requirements/lesson/generate?force=false",
            HttpMethod.POST,
            null,
            Map::class.java
        )
        assert(lessonResp.statusCode != HttpStatus.FORBIDDEN) {
            "Non-forced lesson generation for student must not return 403, got ${lessonResp.statusCode}"
        }
    }
}
