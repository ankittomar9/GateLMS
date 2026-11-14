package com.gatelms.service;

import com.gatelms.dto.DayRollupDto;
import com.gatelms.dto.Rolling7Stats;
import com.gatelms.dto.TrendsResponse;
import com.gatelms.entity.DailyRollup;
import com.gatelms.error.ValidationException;
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
    public void recordAnswer(LocalDate activityDate, boolean correct, int seconds, String mode) {
        recordAnswer(activityDate, correct, seconds, mode, null);
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

    @Transactional
    public void recordMockSubmit(
        LocalDate activityDate,
        int questionsAnswered,
        int correctAnswers,
        int minutesMocked
    ) {
        recordMockSubmit(activityDate, questionsAnswered, correctAnswers, minutesMocked, null);
    }

    @Transactional(readOnly = true)
    public TrendsResponse getTrends(int days) {
        if (days < 7 || days > 365) {
            throw new ValidationException(
                "Invalid days parameter",
                Map.of("days", "Days must be between 7 and 365")
            );
        }

        UUID currentUserId = currentUserProvider.getCurrentUserId();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);

        List<DailyRollup> recordsList = dailyRollupRepository.findByActivityDateBetween(currentUserId, startDate, today);
        Map<LocalDate, DailyRollup> records = new HashMap<>();
        for (DailyRollup r : recordsList) {
            records.put(r.getActivityDate(), r);
        }

        List<DayRollupDto> filledDays = new ArrayList<>();
        LocalDate curr = startDate;
        while (!curr.isAfter(today)) {
            DailyRollup record = records.get(curr);
            filledDays.add(new DayRollupDto(
                curr,
                record != null ? record.getQuestionsAnswered() : 0,
                record != null ? record.getCorrectAnswers() : 0,
                record != null ? record.getMinutesPracticed() : 0,
                record != null ? record.getMinutesMocked() : 0
            ));
            curr = curr.plusDays(1);
        }

        List<DayRollupDto> last7Days = filledDays.subList(Math.max(0, filledDays.size() - 7), filledDays.size());
        int rollingVolume = last7Days.stream().mapToInt(DayRollupDto::questionsAnswered).sum();
        int rollingCorrect = last7Days.stream().mapToInt(DayRollupDto::correct).sum();
        double rollingAccuracy = rollingVolume > 0
            ? BigDecimal.valueOf(100.0 * rollingCorrect / rollingVolume).setScale(1, RoundingMode.HALF_UP).doubleValue()
            : 0.0;

        return new TrendsResponse(
            filledDays,
            new Rolling7Stats(
                rollingAccuracy,
                rollingVolume
            )
        );
    }
}
