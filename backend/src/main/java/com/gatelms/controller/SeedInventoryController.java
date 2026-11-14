package com.gatelms.controller;

import com.gatelms.dto.SeedInventoryResponse;
import com.gatelms.service.SeedInventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/seed")
public class SeedInventoryController {

    private final SeedInventoryService seedInventoryService;

    public SeedInventoryController(SeedInventoryService seedInventoryService) {
        this.seedInventoryService = seedInventoryService;
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeedInventoryResponse> getInventory() {
        return ResponseEntity.ok(seedInventoryService.getInventory());
    }
}
