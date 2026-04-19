package com.liyile.medication.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.liyile.medication.dto.ConfirmEventDTO;
import com.liyile.medication.entity.Alert;
import com.liyile.medication.entity.IntakeEvent;
import com.liyile.medication.entity.Schedule;
import com.liyile.medication.mapper.AlertMapper;
import com.liyile.medication.mapper.IntakeEventMapper;
import com.liyile.medication.mapper.ScheduleMapper;
import com.liyile.medication.service.ReminderInstanceService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class IntakeEventControllerTest {

  @Test
  @DisplayName("创建 abnormal 事件时应生成 pending 告警")
  void shouldCreateAlertWhenAbnormalEventCreated() {
    IntakeEventMapper intakeEventMapper = Mockito.mock(IntakeEventMapper.class);
    AlertMapper alertMapper = Mockito.mock(AlertMapper.class);
    ScheduleMapper scheduleMapper = Mockito.mock(ScheduleMapper.class);
    ReminderInstanceService reminderInstanceService = Mockito.mock(ReminderInstanceService.class);
    IntakeEventController controller =
        new IntakeEventController(
            intakeEventMapper, alertMapper, scheduleMapper, reminderInstanceService);

    IntakeEvent event = new IntakeEvent();
    event.setPatientId(1L);
    event.setScheduleId(2L);
    event.setStatus("abnormal");
    event.setTs("2026-03-07T10:00:00Z");

    Schedule schedule = new Schedule();
    schedule.setId(2L);
    schedule.setMedicineName("降压药");

    when(scheduleMapper.selectById(2L)).thenReturn(schedule);
    when(alertMapper.selectCount(any())).thenReturn(0L);

    controller.create(event);

    verify(intakeEventMapper).insert(event);
    verify(alertMapper).insert(any(Alert.class));
  }

  @Test
  @DisplayName("确认事件时应自动关闭同计划的 pending 告警")
  void shouldResolveScheduleAlertsWhenEventConfirmed() {
    IntakeEventMapper intakeEventMapper = Mockito.mock(IntakeEventMapper.class);
    AlertMapper alertMapper = Mockito.mock(AlertMapper.class);
    ScheduleMapper scheduleMapper = Mockito.mock(ScheduleMapper.class);
    ReminderInstanceService reminderInstanceService = Mockito.mock(ReminderInstanceService.class);
    IntakeEventController controller =
        new IntakeEventController(
            intakeEventMapper, alertMapper, scheduleMapper, reminderInstanceService);

    IntakeEvent event = new IntakeEvent();
    event.setId(9L);
    event.setPatientId(1L);
    event.setScheduleId(2L);
    event.setStatus("suspected");

    Alert alert = new Alert();
    alert.setId(10L);
    alert.setPatientId(1L);
    alert.setTitle("计划 #2 未确认服药");
    alert.setStatus("pending");

    ConfirmEventDTO dto = new ConfirmEventDTO();
    dto.setConfirmedBy("elder");
    dto.setConfirmTime("2026-03-07T10:10:00Z");

    when(intakeEventMapper.selectById(9L)).thenReturn(event);
    when(alertMapper.selectList(any())).thenReturn(List.of(alert));

    var response = controller.confirm(9L, dto);

    assertEquals("confirmed", response.getData().getStatus());
    verify(intakeEventMapper).updateById(event);
    verify(alertMapper).updateById(alert);
  }
}
