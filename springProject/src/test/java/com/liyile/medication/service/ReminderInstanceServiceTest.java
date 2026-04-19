package com.liyile.medication.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.liyile.medication.entity.ReminderInstance;
import com.liyile.medication.entity.Schedule;
import com.liyile.medication.mapper.AlertMapper;
import com.liyile.medication.mapper.IntakeEventMapper;
import com.liyile.medication.mapper.ReminderInstanceMapper;
import com.liyile.medication.mapper.ScheduleMapper;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ReminderInstanceServiceTest {

  @Test
  @DisplayName("materializeForPatient 不应将时间窗内的当日提醒直接标记为 missed")
  void shouldKeepSameDayFutureReminderPendingWhenMaterializing() {
    ReminderInstanceMapper reminderInstanceMapper = Mockito.mock(ReminderInstanceMapper.class);
    ScheduleMapper scheduleMapper = Mockito.mock(ScheduleMapper.class);
    AlertMapper alertMapper = Mockito.mock(AlertMapper.class);
    IntakeEventMapper intakeEventMapper = Mockito.mock(IntakeEventMapper.class);
    ReminderInstanceService service = new ReminderInstanceService(
        reminderInstanceMapper, scheduleMapper, alertMapper, intakeEventMapper);

    // 使用未来日期，避免当日晚间时间窗已被 reconcile 标为 missed 导致用例随运行时刻失败
    LocalDate materializeDate = LocalDate.of(2099, 12, 15);
    Schedule eveningSchedule = new Schedule();
    eveningSchedule.setId(2L);
    eveningSchedule.setPatientId(1L);
    eveningSchedule.setMedicineName("维生素D");
    eveningSchedule.setWinStart("20:00");
    eveningSchedule.setWinEnd("23:30");
    eveningSchedule.setStatus("enabled");

    List<ReminderInstance> existingInstances = new ArrayList<>();
    List<ReminderInstance> storedInstances = new ArrayList<>();
    when(scheduleMapper.selectList(any())).thenReturn(List.of(eveningSchedule));
    when(reminderInstanceMapper.selectList(any()))
        .thenReturn(existingInstances, storedInstances, storedInstances);
    doAnswer(invocation -> {
      ReminderInstance instance = invocation.getArgument(0);
      instance.setId(4L);
      existingInstances.add(instance);
      storedInstances.add(instance);
      return 1;
    }).when(reminderInstanceMapper).insert(any(ReminderInstance.class));

    List<ReminderInstance> instances = service.materializeForPatient(1L, materializeDate);

    assertEquals(1, instances.size());
    assertEquals("pending", instances.get(0).getStatus());
    assertEquals(Date.valueOf(materializeDate), instances.get(0).getScheduledDate());
    assertEquals(Timestamp.valueOf(LocalDateTime.of(materializeDate, java.time.LocalTime.of(23, 30))),
        instances.get(0).getWindowEndAt());
  }
}
