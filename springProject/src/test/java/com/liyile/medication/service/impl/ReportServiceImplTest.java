package com.liyile.medication.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.liyile.medication.entity.ReminderInstance;
import com.liyile.medication.mapper.ReminderInstanceMapper;
import com.liyile.medication.vo.ReportSummaryVO;
import java.sql.Date;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ReportServiceImplTest {

  @Test
  @DisplayName("getSummary 应基于提醒实例计算真实漏服次数")
  void shouldCalculateMissedCountFromReminderInstances() {
    ReminderInstanceMapper reminderInstanceMapper = Mockito.mock(ReminderInstanceMapper.class);
    ReportServiceImpl service = new ReportServiceImpl(reminderInstanceMapper);

    ReminderInstance confirmed = new ReminderInstance();
    confirmed.setPatientId(7L);
    confirmed.setScheduledDate(Date.valueOf(java.time.LocalDate.now()));
    confirmed.setStatus("confirmed");

    ReminderInstance missed = new ReminderInstance();
    missed.setPatientId(7L);
    missed.setScheduledDate(Date.valueOf(java.time.LocalDate.now().minusDays(1)));
    missed.setStatus("missed");

    when(reminderInstanceMapper.selectList(any())).thenReturn(List.of(confirmed, missed));

    ReportSummaryVO summary = service.getSummary(7L, "week");

    assertEquals(2, summary.getTotalReminders());
    assertEquals(1, summary.getConfirmedCount());
    assertEquals(1, summary.getMissedCount());
  }
}
