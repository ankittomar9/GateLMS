package com.gatelms.controller

import com.gatelms.dto.StatsSummaryDto
import com.gatelms.service.StatsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/stats")
class StatsController(
    private val statsService: StatsService
) {

    @GetMapping("/summary")
    fun getSummary(): StatsSummaryDto {
        return statsService.getStatsSummary()
    }

    @GetMapping("/curation")
    fun getCurationStats(): com.gatelms.dto.CurationStatsDto {
        return statsService.getCurationStats()
    }

    @GetMapping("/distribution")
    fun getDistributionStats(): com.gatelms.dto.DistributionStatsDto {
        return statsService.getDistributionStats()
    }
}
