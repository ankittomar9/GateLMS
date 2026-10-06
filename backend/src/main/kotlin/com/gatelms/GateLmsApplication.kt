package com.gatelms

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@SpringBootApplication
class GateLmsApplication

fun main(args: Array<String>) {
    runApplication<GateLmsApplication>(*args)
}

@RestController
@RequestMapping("/api/v1/health")
class HealthController {

    @GetMapping
    fun healthCheck(): HealthResponse {
        return HealthResponse(
            status = "UP",
            service = "GateLMS Core Backend",
            version = "1.0.0-SNAPSHOT",
            timestamp = Instant.now().toString()
        )
    }
}

data class HealthResponse(
    val status: String,
    val service: String,
    val version: String,
    val timestamp: String
)
