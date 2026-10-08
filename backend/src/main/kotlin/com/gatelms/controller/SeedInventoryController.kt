package com.gatelms.controller

import com.gatelms.dto.SeedInventoryResponse
import com.gatelms.service.SeedInventoryService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/seed")
class SeedInventoryController(
    private val seedInventoryService: SeedInventoryService
) {
    @GetMapping("/inventory")
    @PreAuthorize("hasRole('ADMIN')")
    fun getInventory(): ResponseEntity<SeedInventoryResponse> {
        return ResponseEntity.ok(seedInventoryService.getInventory())
    }
}
