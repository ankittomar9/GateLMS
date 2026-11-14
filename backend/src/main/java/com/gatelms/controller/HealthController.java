package com.gatelms.controller;

import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    public record HealthResponse(
            String status,
            String service,
            String version,
            String timestamp
    ) {}

    private final Optional<BuildProperties> buildProperties;

    public HealthController(Optional<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping
    public HealthResponse healthCheck() {
        String version = buildProperties.isPresent() ? buildProperties.get().getVersion() : "1.0.0-SNAPSHOT";
        return new HealthResponse(
                "UP",
                "GateLMS Core Backend",
                version,
                Instant.now().toString()
        );
    }
}
