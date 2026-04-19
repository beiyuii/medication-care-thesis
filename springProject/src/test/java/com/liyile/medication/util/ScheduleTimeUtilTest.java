package com.liyile.medication.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.liyile.medication.entity.Schedule;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScheduleTimeUtilTest {

  @Test
  @DisplayName("calculateNextIntake 应返回当天时间窗开始时间")
  void shouldReturnTodayWindowStartWhenNowIsBeforeWindow() {
    Schedule schedule = buildEnabledSchedule("09:00", "10:00");
    LocalDateTime now = LocalDateTime.now().with(LocalTime.of(8, 0));

    String nextIntake = ScheduleTimeUtil.calculateNextIntake(schedule, now);

    assertNotNull(nextIntake);
    assertEquals(now.toLocalDate() + "T09:00:00", nextIntake);
  }

  @Test
  @DisplayName("calculateNextIntake 应在计划停用时返回 null")
  void shouldReturnNullWhenScheduleDisabled() {
    Schedule schedule = buildEnabledSchedule("09:00", "10:00");
    schedule.setStatus("disabled");

    assertNull(ScheduleTimeUtil.calculateNextIntake(schedule, LocalDateTime.now()));
  }

  private Schedule buildEnabledSchedule(String winStart, String winEnd) {
    Schedule schedule = new Schedule();
    schedule.setStatus("enabled");
    schedule.setWinStart(winStart);
    schedule.setWinEnd(winEnd);
    return schedule;
  }
}
