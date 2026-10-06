package com.gatelms.controller

import org.springframework.boot.info.BuildProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.Optional

@RestController
@RequestMapping("/api/v1/health")
class HealthController(
    private val buildProperties: Optional<BuildProperties>
) {

    @GetMapping
    fun healthCheck(): HealthResponse {
        val version = if (buildProperties.isPresent) buildProperties.get().version else "1.0.0-SNAPSHOT"
        return HealthResponse(
            status = "UP",
            service = "GateLMS Core Backend",
            version = version,
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
