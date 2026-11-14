package com.gatelms.service;

import com.gatelms.dto.ExamDtos.ExamCountdownDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class ExamCountdownService {

    private final String examDateConfig;

    public ExamCountdownService(@Value("${app.exam.date:2028-02-12}") String examDateConfig) {
        this.examDateConfig = examDateConfig;
    }

    public ExamCountdownDto getCountdown() {
        LocalDate targetDate;
        try {
            targetDate = LocalDate.parse(examDateConfig.trim());
        } catch (Exception e) {
            targetDate = LocalDate.of(2028, 2, 12);
        }

        LocalDate today = LocalDate.now();
        long daysRemaining = ChronoUnit.DAYS.between(today, targetDate);
        long clampedDays = Math.max(0L, daysRemaining);
        long weeksRemaining = clampedDays / 7L;

        return new ExamCountdownDto(
            targetDate.toString(),
            clampedDays,
            weeksRemaining,
            "config"
        );
    }
}
