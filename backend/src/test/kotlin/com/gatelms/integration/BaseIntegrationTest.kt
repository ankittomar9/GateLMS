package com.gatelms.integration

import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class BaseIntegrationTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    protected val authInterceptor = TestAuthSessionInterceptor()

    @org.junit.jupiter.api.BeforeEach
    fun setupBaseAuth() {
        restTemplate.restTemplate.interceptors.removeIf { it is TestAuthSessionInterceptor }
        restTemplate.restTemplate.interceptors.add(0, authInterceptor)
        if (authInterceptor.autoLoginEnabled && authInterceptor.currentSessionId == null) {
            loginAs("ankur", "admin123")
        }
    }

    fun loginAs(username: String = "ankur", password: String = "admin123"): com.gatelms.dto.AuthMeResponse {
        authInterceptor.clear()
        val resp = restTemplate.postForEntity(
            "/api/v1/auth/login",
            com.gatelms.dto.AuthLoginRequest(username = username, password = password),
            com.gatelms.dto.AuthMeResponse::class.java
        )
        if (!resp.statusCode.is2xxSuccessful) {
            throw IllegalStateException("Login failed with status ${resp.statusCode}: ${resp.body}")
        }
        return resp.body!!
    }

    fun logout() {
        restTemplate.postForEntity("/api/v1/auth/logout", null, Void::class.java)
        authInterceptor.clear()
    }

    companion object {
        @JvmStatic
        @ServiceConnection
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine").apply {
            withDatabaseName("gatelms_test_db")
            withUsername("test")
            withPassword("test")
            start()
        }
    }
}
