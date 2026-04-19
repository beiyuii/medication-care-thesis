package com.liyile.medication.util;

import com.liyile.medication.entity.Schedule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 用药计划时间计算工具。
 *
 * <p>当前计划数据仅包含 HH:mm 时间窗，没有结构化频次规则，因此默认按“启用计划每天触发一次时间窗”进行计算。</p>
 */
public final class ScheduleTimeUtil {
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private ScheduleTimeUtil() {}

  /**
   * 计算单个计划的下一次提醒时间。
   *
   * @param schedule 用药计划
   * @param now 当前时间
   * @return ISO-8601 格式的下次提醒时间；若计划不可用则返回 null
   */
  public static String calculateNextIntake(Schedule schedule, LocalDateTime now) {
    if (schedule == null || !"enabled".equalsIgnoreCase(schedule.getStatus())) {
      return null;
    }
    if (schedule.getWinStart() == null || schedule.getWinEnd() == null) {
      return null;
    }

    try {
      LocalTime winStart = LocalTime.parse(schedule.getWinStart(), TIME_FORMATTER);
      LocalTime winEnd = LocalTime.parse(schedule.getWinEnd(), TIME_FORMATTER);
      LocalDate today = now.toLocalDate();
      LocalTime currentTime = now.toLocalTime();

      LocalDateTime nextTime;
      if (currentTime.isBefore(winStart)) {
        nextTime = LocalDateTime.of(today, winStart);
      } else if (currentTime.isAfter(winEnd) || currentTime.equals(winEnd)) {
        nextTime = LocalDateTime.of(today.plusDays(1), winStart);
      } else {
        nextTime = LocalDateTime.of(today, winStart);
      }
      return nextTime.format(DATE_TIME_FORMATTER);
    } catch (Exception exception) {
      return null;
    }
  }

  /**
   * 计算多个计划中最近的一次提醒时间。
   *
   * @param schedules 计划列表
   * @param now 当前时间
   * @return 最近的下次提醒时间；若不存在则返回 null
   */
  public static String calculateNextIntake(List<Schedule> schedules, LocalDateTime now) {
    if (schedules == null || schedules.isEmpty()) {
      return null;
    }

    Optional<String> nextIntake = schedules.stream()
        .map(schedule -> calculateNextIntake(schedule, now))
        .filter(Objects::nonNull)
        .min(Comparator.naturalOrder());

    return nextIntake.orElse(null);
  }

  /**
   * 解析计划窗口开始时间。
   *
   * @param schedule 计划
   * @return 开始时间；若无效返回 null
   */
  public static LocalTime parseWindowStart(Schedule schedule) {
    if (schedule == null || schedule.getWinStart() == null) {
      return null;
    }
    try {
      return LocalTime.parse(schedule.getWinStart(), TIME_FORMATTER);
    } catch (Exception exception) {
      return null;
    }
  }

  /**
   * 解析计划窗口结束时间。
   *
   * @param schedule 计划
   * @return 结束时间；若无效返回 null
   */
  public static LocalTime parseWindowEnd(Schedule schedule) {
    if (schedule == null || schedule.getWinEnd() == null) {
      return null;
    }
    try {
      return LocalTime.parse(schedule.getWinEnd(), TIME_FORMATTER);
    } catch (Exception exception) {
      return null;
    }
  }

  /**
   * 生成某一天的计划窗口结束时间。
   *
   * @param schedule 计划
   * @param date 日期
   * @return 时间窗结束时间；若无效返回 null
   */
  public static LocalDateTime buildWindowEnd(Schedule schedule, LocalDate date) {
    LocalTime winEnd = parseWindowEnd(schedule);
    if (winEnd == null || date == null) {
      return null;
    }
    return LocalDateTime.of(date, winEnd);
  }
}
