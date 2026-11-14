package com.gatelms.service;

import com.gatelms.dto.AnalyticsDtos.DayRollupDto;
import com.gatelms.dto.AnalyticsDtos.Rolling7Stats;
import com.gatelms.dto.AnalyticsDtos.TrendsResponse;
import com.gatelms.entity.DailyRollup;
import com.gatelms.error.Exceptions.ValidationException;
import com.gatelms.repository.DailyRollupRepository;
import com.gatelms.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class RollupService {

    private final DailyRollupRepository dailyRollupRepository;
    private final CurrentUserProvider currentUserProvider;

    public RollupService(DailyRollupRepository dailyRollupRepository, CurrentUserProvider currentUserProvider) {
        this.dailyRollupRepository = dailyRollupRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public void recordAnswer(LocalDate activityDate, boolean correct, int seconds, String mode, UUID userId) {
        UUID currentUserId = userId != null ? userId : currentUserProvider.getCurrentUserId();
        boolean isPractice = "practice".equalsIgnoreCase(mode);
        int minutesPracticed = isPractice ? Math.max(0, (seconds + 59) / 60) : 0;
        int minutesMocked = !isPractice ? Math.max(0, (seconds + 59) / 60) : 0;

        dailyRollupRepository.upsertRollup(
            currentUserId,
            activityDate,
            1,
            correct ? 1 : 0,
            minutesPracticed,
            minutesMocked,
            0
        );
    }

    @Transactional
    public void recordMockSubmit(
        LocalDate activityDate,
        int questionsAnswered,
        int correctAnswers,
        int minutesMocked,
        UUID userId
    ) {
        UUID currentUserId = userId != null ? userId : currentUserProvider.getCurrentUserId();
        dailyRollupRepository.upsertRollup(
            currentUserId,
            activityDate,
            questionsAnswered,
            correctAnswers,
            0,
            minutesMocked,
            1
        );
    }

    @Transactional(readOnly = true)
    public TrendsResponse getTrends(int days) {
        if (days != 7 && days != 30 && days != 90) {
            throw new ValidationException("Invalid days parameter", Map.of("days", "Must be one of 7, 30, or 90"));
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        UUID currentUserId = currentUserProvider.getCurrentUserId();

        List<DailyRollup> rows = dailyRollupRepository.findByDateRange(currentUserId, startDate, endDate);
        Map<LocalDate, DailyRollup> rowMap = new HashMap<>();
        for (DailyRollup r : rows) {
            rowMap.put(r.getActivityDate(), r);
        }

        List<DayRollupDto> dayDtos = new ArrayList<>();
        LocalDate curr = startDate;
        while (!curr.isAfter(endDate)) {
            DailyRollup r = rowMap.get(curr);
            int answered = r != null ? r.getQuestionsAnswered() : 0;
            int correct = r != null ? r.getCorrectAnswers() : 0;
            double accuracy = answered > 0
                ? BigDecimal.valueOf((double) correct / (double) answered * 100.0).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
            int totalMins = r != null ? (r.getMinutesPracticed() + r.getMinutesMocked()) : 0;

            dayDtos.add(new DayRollupDto(
                curr.toString(),
                answered,
                correct,
                accuracy,
                totalMins,
                r != null ? r.getMockAttempts() : 0
            ));
            curr = curr.plusDays(1);
        }

        // Rolling 7 days stats
        LocalDate last7Start = endDate.minusDays(6);
        List<DayRollupDto> last7Days = dayDtos.stream()
            .filter(d -> !LocalDate.parse(d.date()).isBefore(last7Start))
            .toList();

        double avgQuestions = last7Days.stream().mapToInt(DayRollupDto::questionsAnswered).average().orElse(0.0);
        double avgMinutes = last7Days.stream().mapToInt(DayRollupDto::studyTimeMinutes).average().orElse(0.0);
        int totalAnswered7 = last7Days.stream().mapToInt(DayRollupDto::questionsAnswered).sum();
        int totalCorrect7 = last7Days.stream().mapToInt(DayRollupDto::correctAnswers).sum();
        double overallAccuracy7 = totalAnswered7 > 0
            ? BigDecimal.valueOf((double) totalCorrect7 / (double) totalAnswered7 * 100.0).setScale(2, RoundingMode.HALF_UP).doubleValue()
            : 0.0;

        Rolling7Stats rolling7 = new Rolling7Stats(
            BigDecimal.valueOf(avgQuestions).setScale(1, RoundingMode.HALF_UP).doubleValue(),
            BigDecimal.valueOf(avgMinutes).setScale(1, RoundingMode.HALF_UP).doubleValue(),
            overallAccuracy7
        );

        return new TrendsResponse(
            days,
            dayDtos,
            rolling7
        );
    }
}
