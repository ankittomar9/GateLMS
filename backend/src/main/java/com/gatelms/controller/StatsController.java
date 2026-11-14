package com.gatelms.controller;

import com.gatelms.dto.*;
import com.gatelms.service.MistakeService;
import com.gatelms.service.MockTestService;
import com.gatelms.service.ReadinessService;
import com.gatelms.service.RollupService;
import com.gatelms.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final StatsService statsService;
    private final MistakeService mistakeService;
    private final MockTestService mockTestService;
    private final ReadinessService readinessService;
    private final RollupService rollupService;

    public StatsController(
            StatsService statsService,
            MistakeService mistakeService,
            MockTestService mockTestService,
            ReadinessService readinessService,
            RollupService rollupService
    ) {
        this.statsService = statsService;
        this.mistakeService = mistakeService;
        this.mockTestService = mockTestService;
        this.readinessService = readinessService;
        this.rollupService = rollupService;
    }

    @GetMapping("/summary")
    public StatsSummaryDto getSummary() {
        return statsService.getStatsSummary();
    }

    @GetMapping("/curation")
    public CurationStatsDto getCurationStats() {
        return statsService.getCurationStats();
    }

    @GetMapping("/distribution")
    public DistributionStatsDto getDistributionStats() {
        return statsService.getDistributionStats();
    }

    @GetMapping("/mastery")
    public List<TopicMasteryStatsDto> getMasteryStats() {
        return mistakeService.getMasteryStats();
    }

    @GetMapping("/mock-trend")
    public List<MockTrendDto> getMockTrend() {
        return mockTestService.getMockTrend();
    }

    @GetMapping("/readiness")
    public ReadinessResponse getReadiness() {
        return readinessService.getReadiness();
    }

    @GetMapping("/heatmap")
    public HeatmapResponse getHeatmap() {
        return readinessService.getHeatmap();
    }

    @GetMapping("/trends")
    public TrendsResponse getTrends(@RequestParam(defaultValue = "90") int days) {
        return rollupService.getTrends(days);
    }

    @GetMapping("/pyp")
    public PypStatsResponse getPypStats() {
        return statsService.getPypStats();
    }
}
