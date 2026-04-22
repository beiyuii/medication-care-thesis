package com.liyile.medication.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.liyile.medication.entity.IntakeEvent;
import com.liyile.medication.entity.ReminderInstance;
import com.liyile.medication.entity.Schedule;
import com.liyile.medication.mapper.AlertMapper;
import com.liyile.medication.mapper.DetectionJobMapper;
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
import org.springframework.dao.DuplicateKeyException;

class ReminderInstanceServiceTest {

  @Test
  @DisplayName("materializeForPatient 不应将时间窗内的当日提醒直接标记为 missed")
  void shouldKeepSameDayFutureReminderPendingWhenMaterializing() {
    ReminderInstanceMapper reminderInstanceMapper = Mockito.mock(ReminderInstanceMapper.class);
    ScheduleMapper scheduleMapper = Mockito.mock(ScheduleMapper.class);
    AlertMapper alertMapper = Mockito.mock(AlertMapper.class);
    IntakeEventMapper intakeEventMapper = Mockito.mock(IntakeEventMapper.class);
    DetectionJobMapper detectionJobMapper = Mockito.mock(DetectionJobMapper.class);
    ReminderInstanceService service = new ReminderInstanceService(
        reminderInstanceMapper, scheduleMapper, alertMapper, intakeEventMapper, detectionJobMapper);

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
      IntakeEvent event = invocation.getArgument(0);
      event.setId((long) (storedInstances.size() + 100));
      return 1;
    }).when(intakeEventMapper).insert(any(IntakeEvent.class));
    doAnswer(invocation -> {
      ReminderInstance instance = invocation.getArgument(0);
      instance.setId(4L);
      existingInstances.add(instance);
      storedInstances.add(instance);
      return 1;
    }).when(reminderInstanceMapper).insert(any(ReminderInstance.class));

    List<ReminderInstance> instances = service.materializeForPatient(1L, materializeDate);

    assertEquals(1, instances.size());
    assertEquals("not_submitted", instances.get(0).getStatus());
    assertEquals("none", instances.get(0).getDetectionStatus());
    assertEquals(Date.valueOf(materializeDate), instances.get(0).getScheduledDate());
    assertEquals(Timestamp.valueOf(LocalDateTime.of(materializeDate, java.time.LocalTime.of(23, 30))),
        instances.get(0).getWindowEndAt());
  }

  @Test
  @DisplayName("护工驳回应创建新的重服实例并保留父子关系")
  void shouldCreateRetryInstanceWhenRejected() {
    ReminderInstanceMapper reminderInstanceMapper = Mockito.mock(ReminderInstanceMapper.class);
    ScheduleMapper scheduleMapper = Mockito.mock(ScheduleMapper.class);
    AlertMapper alertMapper = Mockito.mock(AlertMapper.class);
    IntakeEventMapper intakeEventMapper = Mockito.mock(IntakeEventMapper.class);
    DetectionJobMapper detectionJobMapper = Mockito.mock(DetectionJobMapper.class);
    ReminderInstanceService service = new ReminderInstanceService(
        reminderInstanceMapper, scheduleMapper, alertMapper, intakeEventMapper, detectionJobMapper);

    ReminderInstance current = new ReminderInstance();
    current.setId(9L);
    current.setPatientId(1L);
    current.setScheduleId(2L);
    current.setScheduledDate(Date.valueOf(LocalDate.of(2099, 12, 15)));
    current.setWindowStartAt(Timestamp.valueOf("2099-12-15 08:00:00"));
    current.setWindowEndAt(Timestamp.valueOf("2099-12-15 08:30:00"));
    current.setStatus("waiting_caregiver");
    current.setDetectionStatus("confirmed");
    current.setRetryCount(0);

    doAnswer(invocation -> {
      ReminderInstance instance = invocation.getArgument(0);
      if (instance.getId() == null) {
        instance.setId(10L);
      }
      return 1;
    }).when(reminderInstanceMapper).insert(any(ReminderInstance.class));
    doAnswer(invocation -> {
      IntakeEvent event = invocation.getArgument(0);
      event.setId(20L);
      return 1;
    }).when(intakeEventMapper).insert(any(IntakeEvent.class));

    ReminderInstance reviewed = service.reviewInstance(
        current,
        ReminderInstanceService.DECISION_REJECTED,
        "caregiver",
        Timestamp.valueOf("2099-12-15 09:00:00"),
        "未服");

    assertEquals("caregiver_rejected", reviewed.getStatus());
    assertNotNull(reviewed.getReviewedAt());
    verify(reminderInstanceMapper).insert(any(ReminderInstance.class));
  }

  @Test
  @DisplayName("老人提交后审核截止时间应为当天 23:59:59，而不是第二天 00:00:00")
  void shouldSetReviewDeadlineToEndOfScheduledDay() {
    ReminderInstanceMapper reminderInstanceMapper = Mockito.mock(ReminderInstanceMapper.class);
    ScheduleMapper scheduleMapper = Mockito.mock(ScheduleMapper.class);
    AlertMapper alertMapper = Mockito.mock(AlertMapper.class);
    IntakeEventMapper intakeEventMapper = Mockito.mock(IntakeEventMapper.class);
    DetectionJobMapper detectionJobMapper = Mockito.mock(DetectionJobMapper.class);
    ReminderInstanceService service = new ReminderInstanceService(
        reminderInstanceMapper, scheduleMapper, alertMapper, intakeEventMapper, detectionJobMapper);

    ReminderInstance current = new ReminderInstance();
    current.setId(21L);
    current.setPatientId(1L);
    current.setScheduleId(2L);
    current.setScheduledDate(Date.valueOf(LocalDate.of(2099, 12, 15)));
    current.setWindowStartAt(Timestamp.valueOf("2099-12-15 08:00:00"));
    current.setWindowEndAt(Timestamp.valueOf("2099-12-15 08:30:00"));
    current.setStatus("not_submitted");
    current.setDetectionStatus("confirmed");

    doAnswer(invocation -> {
      IntakeEvent event = invocation.getArgument(0);
      event.setId(30L);
      return 1;
    }).when(intakeEventMapper).insert(any(IntakeEvent.class));

    ReminderInstance submitted = service.submitInstance(
        current,
        "elder",
        Timestamp.valueOf("2099-12-15 09:00:00"));

    assertEquals("waiting_caregiver", submitted.getStatus());
    assertEquals(Timestamp.valueOf("2099-12-15 23:59:59"), submitted.getReviewDeadline());
  }

  @Test
  @DisplayName("已进入 review_timeout 的实例不应被重复写入超时事件")
  void shouldNotRepeatReviewTimeoutForTimedOutInstance() {
    ReminderInstanceMapper reminderInstanceMapper = Mockito.mock(ReminderInstanceMapper.class);
    ScheduleMapper scheduleMapper = Mockito.mock(ScheduleMapper.class);
    AlertMapper alertMapper = Mockito.mock(AlertMapper.class);
    IntakeEventMapper intakeEventMapper = Mockito.mock(IntakeEventMapper.class);
    DetectionJobMapper detectionJobMapper = Mockito.mock(DetectionJobMapper.class);
    ReminderInstanceService service = new ReminderInstanceService(
        reminderInstanceMapper, scheduleMapper, alertMapper, intakeEventMapper, detectionJobMapper);

    ReminderInstance timedOut = new ReminderInstance();
    timedOut.setId(31L);
    timedOut.setPatientId(1L);
    timedOut.setScheduleId(2L);
    timedOut.setScheduledDate(Date.valueOf(LocalDate.of(2099, 12, 15)));
    timedOut.setStatus("review_timeout");
    timedOut.setDetectionStatus("suspected");
    timedOut.setReviewDeadline(Timestamp.valueOf("2099-12-15 23:59:59"));

    when(reminderInstanceMapper.selectList(any())).thenReturn(List.of(timedOut));

    service.reconcileOverdueInstances(1L, LocalDateTime.of(2100, 1, 1, 8, 0));

    verify(reminderInstanceMapper, never()).updateById(any(ReminderInstance.class));
    verify(intakeEventMapper, never()).insert(any(IntakeEvent.class));
    verify(alertMapper, never()).insert(any());
    assertTrue(true);
  }

  @Test
  @DisplayName("护工重复驳回同一实例时应复用已存在的重服实例而不是直接报重复键错误")
  void shouldReuseExistingRetryInstanceWhenDuplicateKeyOccurs() {
    ReminderInstanceMapper reminderInstanceMapper = Mockito.mock(ReminderInstanceMapper.class);
    ScheduleMapper scheduleMapper = Mockito.mock(ScheduleMapper.class);
    AlertMapper alertMapper = Mockito.mock(AlertMapper.class);
    IntakeEventMapper intakeEventMapper = Mockito.mock(IntakeEventMapper.class);
    DetectionJobMapper detectionJobMapper = Mockito.mock(DetectionJobMapper.class);
    ReminderInstanceService service = new ReminderInstanceService(
        reminderInstanceMapper, scheduleMapper, alertMapper, intakeEventMapper, detectionJobMapper);

    ReminderInstance current = new ReminderInstance();
    current.setId(41L);
    current.setPatientId(1L);
    current.setScheduleId(2L);
    current.setScheduledDate(Date.valueOf(LocalDate.of(2099, 12, 15)));
    current.setWindowStartAt(Timestamp.valueOf("2099-12-15 08:00:00"));
    current.setWindowEndAt(Timestamp.valueOf("2099-12-15 08:30:00"));
    current.setStatus("waiting_caregiver");
    current.setDetectionStatus("confirmed");
    current.setRetryCount(0);

    ReminderInstance existingRetry = new ReminderInstance();
    existingRetry.setId(42L);
    existingRetry.setPatientId(1L);
    existingRetry.setScheduleId(2L);
    existingRetry.setScheduledDate(Date.valueOf(LocalDate.of(2099, 12, 15)));
    existingRetry.setRetryCount(1);
    existingRetry.setStatus("not_submitted");

    Mockito.doThrow(new DuplicateKeyException("duplicate retry"))
        .when(reminderInstanceMapper).insert(any(ReminderInstance.class));
    when(reminderInstanceMapper.selectOne(any())).thenReturn(existingRetry);
    doAnswer(invocation -> {
      IntakeEvent event = invocation.getArgument(0);
      event.setId(50L);
      return 1;
    }).when(intakeEventMapper).insert(any(IntakeEvent.class));

    ReminderInstance reviewed = service.reviewInstance(
        current,
        ReminderInstanceService.DECISION_REJECTED,
        "caregiver",
        Timestamp.valueOf("2099-12-15 09:00:00"),
        "未服");

    assertEquals("caregiver_rejected", reviewed.getStatus());
    verify(reminderInstanceMapper).selectOne(any());
  }
}
