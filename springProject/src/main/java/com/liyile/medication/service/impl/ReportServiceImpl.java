package com.liyile.medication.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyile.medication.entity.ReminderInstance;
import com.liyile.medication.mapper.ReminderInstanceMapper;
import com.liyile.medication.service.ReportService;
import com.liyile.medication.vo.ReportSummaryVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 统计报表服务实现类。
 *
 * <p>统一基于 reminder_instances 统计总提醒数、确认数、漏服数与异常数。</p>
 */
@Service
public class ReportServiceImpl implements ReportService {
  private final ReminderInstanceMapper reminderInstanceMapper;

  public ReportServiceImpl(ReminderInstanceMapper reminderInstanceMapper) {
    this.reminderInstanceMapper = reminderInstanceMapper;
  }

  @Override
  public ReportSummaryVO getSummary(Long patientId, String range) {
    LocalDate startDate = calculateStartDate(range);
    LambdaQueryWrapper<ReminderInstance> wrapper = new LambdaQueryWrapper<ReminderInstance>()
        .eq(ReminderInstance::getPatientId, patientId);
    if (startDate != null) {
      wrapper.ge(ReminderInstance::getScheduledDate, java.sql.Date.valueOf(startDate));
    }

    List<ReminderInstance> instances = reminderInstanceMapper.selectList(wrapper);
    int totalReminders = instances.size();
    int confirmedCount = (int) instances.stream()
        .filter(instance -> "confirmed".equalsIgnoreCase(instance.getStatus()))
        .count();
    int missedCount = (int) instances.stream()
        .filter(instance -> "missed".equalsIgnoreCase(instance.getStatus()))
        .count();
    int abnormalCount = (int) instances.stream()
        .filter(instance -> "abnormal".equalsIgnoreCase(instance.getStatus()))
        .count();
    long avgResponseTime = calculateAvgResponseTime(instances);

    ReportSummaryVO vo = new ReportSummaryVO();
    vo.setPatientId(patientId);
    vo.setRange(range != null ? range : "all");
    vo.setTotalReminders(totalReminders);
    vo.setConfirmedCount(confirmedCount);
    vo.setConfirmRate(totalReminders > 0 ? (double) confirmedCount / totalReminders : 0.0);
    vo.setAvgResponseTime(avgResponseTime);
    vo.setAbnormalCount(abnormalCount);
    vo.setMissedCount(missedCount);
    return vo;
  }

  private LocalDate calculateStartDate(String range) {
    if (range == null || range.isBlank() || "all".equalsIgnoreCase(range)) {
      return null;
    }
    LocalDate today = LocalDate.now();
    switch (range.toLowerCase()) {
      case "day":
        return today.minusDays(1);
      case "week":
        return today.minusWeeks(1);
      case "month":
        return today.minusMonths(1);
      default:
        return null;
    }
  }

  private long calculateAvgResponseTime(List<ReminderInstance> instances) {
    long totalResponseSeconds = 0;
    int count = 0;
    for (ReminderInstance instance : instances) {
      if (instance.getConfirmedAt() == null || instance.getWindowStartAt() == null) {
        continue;
      }
      LocalDateTime confirmAt = instance.getConfirmedAt().toInstant()
          .atZone(ZoneId.systemDefault())
          .toLocalDateTime();
      LocalDateTime windowStart = instance.getWindowStartAt().toInstant()
          .atZone(ZoneId.systemDefault())
          .toLocalDateTime();
      long seconds = java.time.Duration.between(windowStart, confirmAt).getSeconds();
      if (seconds >= 0) {
        totalResponseSeconds += seconds;
        count++;
      }
    }
    return count > 0 ? totalResponseSeconds / count : 0L;
  }
}
