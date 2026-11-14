package com.gatelms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class DailyRollupId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate = LocalDate.now();

    public DailyRollupId() {}

    public DailyRollupId(UUID userId, LocalDate activityDate) {
        this.userId = userId;
        this.activityDate = activityDate;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public LocalDate getActivityDate() { return activityDate; }
    public void setActivityDate(LocalDate activityDate) { this.activityDate = activityDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyRollupId that = (DailyRollupId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(activityDate, that.activityDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, activityDate);
    }
}
