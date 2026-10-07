package com.gatelms.controller

import com.gatelms.dto.*
import com.gatelms.service.MistakeService
import com.gatelms.service.MockTestService
import com.gatelms.service.ReadinessService
import com.gatelms.service.RollupService
import com.gatelms.service.StatsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/stats")
class StatsController(
    private val statsService: StatsService,
    private val mistakeService: MistakeService,
    private val mockTestService: MockTestService,
    private val readinessService: ReadinessService,
    private val rollupService: RollupService
) {

    @GetMapping("/summary")
    fun getSummary(): StatsSummaryDto {
        return statsService.getStatsSummary()
    }

    @GetMapping("/curation")
    fun getCurationStats(): CurationStatsDto {
        return statsService.getCurationStats()
    }

    @GetMapping("/distribution")
    fun getDistributionStats(): DistributionStatsDto {
        return statsService.getDistributionStats()
    }

    @GetMapping("/mastery")
    fun getMasteryStats(): List<TopicMasteryStatsDto> {
        return mistakeService.getMasteryStats()
    }

    @GetMapping("/mock-trend")
    fun getMockTrend(): List<MockTrendDto> {
        return mockTestService.getMockTrend()
    }

    @GetMapping("/readiness")
    fun getReadiness(): ReadinessResponse {
        return readinessService.getReadiness()
    }

    @GetMapping("/heatmap")
    fun getHeatmap(): HeatmapResponse {
        return readinessService.getHeatmap()
    }

    @GetMapping("/trends")
    fun getTrends(@RequestParam(defaultValue = "90") days: Int): TrendsResponse {
        return rollupService.getTrends(days)
    }
}
