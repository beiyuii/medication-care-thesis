package com.liyile.medication.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyile.medication.entity.Alert;
import com.liyile.medication.entity.IntakeEvent;
import com.liyile.medication.entity.ReminderInstance;
import com.liyile.medication.entity.Schedule;
import com.liyile.medication.mapper.AlertMapper;
import com.liyile.medication.mapper.IntakeEventMapper;
import com.liyile.medication.mapper.ReminderInstanceMapper;
import com.liyile.medication.mapper.ScheduleMapper;
import com.liyile.medication.vo.ReminderInstanceVO;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 提醒实例服务。
 */
@Service
public class ReminderInstanceService {
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final ReminderInstanceMapper reminderInstanceMapper;
  private final ScheduleMapper scheduleMapper;
  private final AlertMapper alertMapper;
  private final IntakeEventMapper intakeEventMapper;

  public ReminderInstanceService(
      ReminderInstanceMapper reminderInstanceMapper,
      ScheduleMapper scheduleMapper,
      AlertMapper alertMapper,
      IntakeEventMapper intakeEventMapper) {
    this.reminderInstanceMapper = reminderInstanceMapper;
    this.scheduleMapper = scheduleMapper;
    this.alertMapper = alertMapper;
    this.intakeEventMapper = intakeEventMapper;
  }

  public List<ReminderInstance> materializeForPatient(Long patientId, LocalDate date) {
    if (patientId == null || date == null) {
      return List.of();
    }
    List<Schedule> schedules = scheduleMapper.selectList(new LambdaQueryWrapper<Schedule>()
        .eq(Schedule::getPatientId, patientId));
    List<Schedule> enabledSchedules = schedules.stream()
        .filter(this::isScheduleEnabled)
        .collect(Collectors.toList());

    List<ReminderInstance> existing = reminderInstanceMapper.selectList(new LambdaQueryWrapper<ReminderInstance>()
        .eq(ReminderInstance::getPatientId, patientId)
        .eq(ReminderInstance::getScheduledDate, Date.valueOf(date)));
    Set<Long> existingScheduleIds = existing.stream()
        .map(ReminderInstance::getScheduleId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    for (Schedule schedule : enabledSchedules) {
      if (schedule.getId() == null || existingScheduleIds.contains(schedule.getId())) {
        continue;
      }
      ReminderInstance instance = buildInstance(schedule, date);
      try {
        reminderInstanceMapper.insert(instance);
        existing.add(instance);
      } catch (RuntimeException exception) {
        if (!isDuplicateKey(exception)) {
          throw exception;
        }
        ReminderInstance persisted = reminderInstanceMapper.selectOne(new LambdaQueryWrapper<ReminderInstance>()
            .eq(ReminderInstance::getPatientId, patientId)
            .eq(ReminderInstance::getScheduleId, schedule.getId())
            .eq(ReminderInstance::getScheduledDate, Date.valueOf(date))
            .last("LIMIT 1"));
        if (persisted != null) {
          existing.add(persisted);
        }
      }
    }

    reconcileOverdueInstances(patientId, LocalDateTime.now());
    return reminderInstanceMapper.selectList(new LambdaQueryWrapper<ReminderInstance>()
        .eq(ReminderInstance::getPatientId, patientId)
        .eq(ReminderInstance::getScheduledDate, Date.valueOf(date))
        .orderByAsc(ReminderInstance::getWindowStartAt));
  }

  public List<ReminderInstanceVO> listInstanceVOs(
      Long patientId, LocalDate date, String statusFilter) {
    List<ReminderInstance> instances = materializeForPatient(patientId, date);
    if (statusFilter != null && !statusFilter.isBlank()) {
      instances = instances.stream()
          .filter(instance -> statusFilter.equalsIgnoreCase(instance.getStatus()))
          .collect(Collectors.toList());
    }
    return toVOs(instances);
  }

  public ReminderInstance getById(Long id) {
    return id == null ? null : reminderInstanceMapper.selectById(id);
  }

  public ReminderInstanceVO getNextReminder(Long patientId) {
    LocalDate today = LocalDate.now();
    List<ReminderInstance> instances = materializeForPatient(patientId, today);
    LocalDateTime now = LocalDateTime.now();
    return instances.stream()
        .filter(instance -> !"resolved".equalsIgnoreCase(instance.getStatus()))
        .filter(instance -> instance.getWindowEndAt() == null
            || instance.getWindowEndAt().toLocalDateTime().isAfter(now))
        .min(Comparator.comparing(ReminderInstance::getWindowStartAt))
        .map(instance -> toVOs(List.of(instance)).stream().findFirst().orElse(null))
        .orElse(null);
  }

  public void markDetecting(ReminderInstance instance, Long detectionJobId) {
    if (instance == null) {
      return;
    }
    instance.setStatus("detecting");
    instance.setLastDetectionJobId(detectionJobId);
    instance.setUpdatedAt(Timestamp.from(Instant.now()));
    reminderInstanceMapper.updateById(instance);
  }

  public ReminderInstance confirmInstance(
      ReminderInstance instance,
      String confirmedBy,
      Timestamp confirmTime,
      Long detectionJobId) {
    if (instance == null) {
      return null;
    }
    Timestamp effectiveConfirmTime = confirmTime != null ? confirmTime : Timestamp.from(Instant.now());
    if (instance.getLastEventId() != null) {
      IntakeEvent event = intakeEventMapper.selectById(instance.getLastEventId());
      if (event != null) {
        event.setStatus("confirmed");
        event.setConfirmedBy(confirmedBy);
        event.setConfirmedAt(effectiveConfirmTime);
        intakeEventMapper.updateById(event);
      }
    } else {
      IntakeEvent event = createEvent(instance, detectionJobId, "confirmed", "manual_confirm", null, null);
      intakeEventMapper.insert(event);
      instance.setLastEventId(event.getId());
    }

    instance.setStatus("confirmed");
    instance.setConfirmedAt(effectiveConfirmTime);
    instance.setUpdatedAt(Timestamp.from(Instant.now()));
    reminderInstanceMapper.updateById(instance);
    resolveAlertsForInstance(instance, "服药已确认，系统自动关闭告警");
    return instance;
  }

  public void applyDetectionOutcome(
      ReminderInstance instance,
      Long detectionJobId,
      String resultStatus,
      Boolean actionDetected,
      String targetsJson,
      Timestamp occurredAt,
      String videoUrl) {
    if (instance == null) {
      return;
    }
    String normalizedStatus = normalizeInstanceStatus(resultStatus);
    IntakeEvent event = createEvent(instance, detectionJobId, normalizedStatus,
        Boolean.TRUE.equals(actionDetected) ? "hand_to_mouth" : "detected", targetsJson, videoUrl);
    event.setTs((occurredAt != null ? occurredAt : Timestamp.from(Instant.now())).toInstant().toString());
    intakeEventMapper.insert(event);

    instance.setStatus(normalizedStatus);
    instance.setLastEventId(event.getId());
    instance.setLastDetectionJobId(detectionJobId);
    instance.setUpdatedAt(Timestamp.from(Instant.now()));
    if ("confirmed".equals(normalizedStatus)) {
      instance.setConfirmedAt(Timestamp.from(Instant.now()));
    }
    reminderInstanceMapper.updateById(instance);

    if ("abnormal".equals(normalizedStatus)) {
      createAlertIfMissing(instance, "detection_failed",
          String.format("计划 #%d 检测异常", instance.getScheduleId()),
          "检测结果异常，请重新拍摄或人工确认。", "high");
    } else if ("confirmed".equals(normalizedStatus)) {
      resolveAlertsForInstance(instance, "检测已确认，系统自动关闭告警");
    }
  }

  public void markJobFailed(ReminderInstance instance, Long detectionJobId, String message) {
    if (instance == null) {
      return;
    }
    instance.setStatus("abnormal");
    instance.setLastDetectionJobId(detectionJobId);
    instance.setUpdatedAt(Timestamp.from(Instant.now()));
    reminderInstanceMapper.updateById(instance);
    createAlertIfMissing(instance, "detection_failed",
        String.format("计划 #%d 检测任务失败", instance.getScheduleId()),
        message != null ? message : "检测任务失败，请稍后重试。", "high");
  }

  public void reconcileOverdueInstances(Long patientId, LocalDateTime now) {
    if (patientId == null || now == null) {
      return;
    }
    List<ReminderInstance> candidates = reminderInstanceMapper.selectList(new LambdaQueryWrapper<ReminderInstance>()
        .eq(ReminderInstance::getPatientId, patientId));
    for (ReminderInstance instance : candidates) {
      if (!canBecomeMissed(instance)) {
        continue;
      }
      if (instance.getWindowEndAt() == null || !instance.getWindowEndAt().toLocalDateTime().isBefore(now)) {
        continue;
      }
      instance.setStatus("missed");
      instance.setUpdatedAt(Timestamp.from(Instant.now()));
      reminderInstanceMapper.updateById(instance);
      createAlertIfMissing(instance, "timeout",
          String.format("计划 #%d 未确认服药", instance.getScheduleId()),
          "用药时间窗结束后仍未确认服药。", "high");
    }
  }

  public List<ReminderInstanceVO> toVOs(List<ReminderInstance> instances) {
    if (instances == null || instances.isEmpty()) {
      return List.of();
    }
    Map<Long, Schedule> schedules = scheduleMapper.selectBatchIds(
            instances.stream().map(ReminderInstance::getScheduleId).collect(Collectors.toSet()))
        .stream()
        .collect(Collectors.toMap(Schedule::getId, Function.identity()));
    Map<Long, List<Alert>> alertsByInstance = alertMapper.selectList(new LambdaQueryWrapper<Alert>()
            .in(Alert::getReminderInstanceId,
                instances.stream().map(ReminderInstance::getId).collect(Collectors.toSet())))
        .stream()
        .collect(Collectors.groupingBy(Alert::getReminderInstanceId));

    List<ReminderInstanceVO> result = new ArrayList<>();
    for (ReminderInstance instance : instances) {
      Schedule schedule = schedules.get(instance.getScheduleId());
      ReminderInstanceVO vo = new ReminderInstanceVO();
      vo.setId(instance.getId());
      vo.setPatientId(instance.getPatientId());
      vo.setScheduleId(instance.getScheduleId());
      vo.setScheduledDate(formatDate(instance.getScheduledDate()));
      vo.setWindowStartAt(formatTimestamp(instance.getWindowStartAt()));
      vo.setWindowEndAt(formatTimestamp(instance.getWindowEndAt()));
      vo.setStatus(instance.getStatus());
      vo.setConfirmedAt(formatTimestamp(instance.getConfirmedAt()));
      vo.setDetectionJobId(instance.getLastDetectionJobId());
      vo.setLastEventId(instance.getLastEventId());
      if (schedule != null) {
        vo.setMedicineName(schedule.getMedicineName());
        vo.setDose(schedule.getDose());
        vo.setFrequency(schedule.getFreq());
      }
      List<Alert> alerts = alertsByInstance.getOrDefault(instance.getId(), List.of());
      vo.setActiveAlertTitles(alerts.stream()
          .filter(alert -> !"resolved".equalsIgnoreCase(alert.getStatus()))
          .map(Alert::getTitle)
          .collect(Collectors.toList()));
      result.add(vo);
    }
    return result;
  }

  private ReminderInstance buildInstance(Schedule schedule, LocalDate date) {
    ReminderInstance instance = new ReminderInstance();
    instance.setPatientId(schedule.getPatientId());
    instance.setScheduleId(schedule.getId());
    instance.setScheduledDate(Date.valueOf(date));
    instance.setWindowStartAt(Timestamp.valueOf(LocalDateTime.of(date, parseTime(schedule.getWinStart()))));
    instance.setWindowEndAt(Timestamp.valueOf(LocalDateTime.of(date, parseTime(schedule.getWinEnd()))));
    instance.setStatus("pending");
    Timestamp now = Timestamp.from(Instant.now());
    instance.setCreatedAt(now);
    instance.setUpdatedAt(now);
    return instance;
  }

  private boolean isScheduleEnabled(Schedule schedule) {
    if (schedule == null || schedule.getStatus() == null) {
      return false;
    }
    String status = schedule.getStatus().toLowerCase();
    return "enabled".equals(status) || "active".equals(status);
  }

  private boolean canBecomeMissed(ReminderInstance instance) {
    if (instance == null || instance.getStatus() == null) {
      return false;
    }
    String status = instance.getStatus().toLowerCase();
    return "pending".equals(status) || "detecting".equals(status) || "suspected".equals(status);
  }

  private String normalizeInstanceStatus(String resultStatus) {
    if (resultStatus == null || resultStatus.isBlank()) {
      return "abnormal";
    }
    String status = resultStatus.toLowerCase();
    if ("confirmed".equals(status) || "suspected".equals(status) || "abnormal".equals(status)) {
      return status;
    }
    return "abnormal";
  }

  private IntakeEvent createEvent(
      ReminderInstance instance,
      Long detectionJobId,
      String status,
      String action,
      String targetsJson,
      String videoUrl) {
    IntakeEvent event = new IntakeEvent();
    event.setPatientId(instance.getPatientId());
    event.setScheduleId(instance.getScheduleId());
    event.setReminderInstanceId(instance.getId());
    event.setDetectionJobId(detectionJobId);
    event.setTs(Instant.now().toString());
    event.setStatus(status);
    event.setAction(action);
    event.setTargetsJson(targetsJson);
    event.setVideoUrl(videoUrl);
    return event;
  }

  private void createAlertIfMissing(
      ReminderInstance instance, String type, String title, String description, String severity) {
    Alert existing = alertMapper.selectOne(new LambdaQueryWrapper<Alert>()
        .eq(Alert::getPatientId, instance.getPatientId())
        .eq(Alert::getTitle, title)
        .eq(Alert::getType, type)
        .eq(Alert::getStatus, "pending")
        .last("LIMIT 1"));
    if (existing != null) {
      return;
    }
    Alert alert = new Alert();
    alert.setPatientId(instance.getPatientId());
    alert.setReminderInstanceId(instance.getId());
    alert.setDetectionJobId(instance.getLastDetectionJobId());
    alert.setTitle(title);
    alert.setDescription(description);
    alert.setSeverity(severity);
    alert.setType(type);
    alert.setTs(Instant.now().toString());
    alert.setStatus("pending");
    alertMapper.insert(alert);
  }

  private void resolveAlertsForInstance(ReminderInstance instance, String actionNote) {
    List<Alert> alerts = alertMapper.selectList(new LambdaQueryWrapper<Alert>()
        .eq(Alert::getPatientId, instance.getPatientId())
        .eq(Alert::getStatus, "pending"));
    Timestamp now = Timestamp.from(Instant.now());
    String scheduleTitlePrefix = String.format("计划 #%d ", instance.getScheduleId());
    for (Alert alert : alerts) {
      boolean linkedToInstance = alert.getReminderInstanceId() != null
          && alert.getReminderInstanceId().equals(instance.getId());
      boolean sameScheduleLegacyAlert = alert.getTitle() != null
          && alert.getTitle().startsWith(scheduleTitlePrefix);
      if (!linkedToInstance && !sameScheduleLegacyAlert) {
        continue;
      }
      alert.setStatus("resolved");
      alert.setResolvedAt(now);
      alert.setActionNote(actionNote);
      alertMapper.updateById(alert);
    }
  }

  private LocalTime parseTime(String value) {
    if (value == null || value.isBlank()) {
      return LocalTime.MIDNIGHT;
    }
    return LocalTime.parse(value);
  }

  private String formatDate(Date value) {
    return value == null ? null : value.toLocalDate().format(DATE_FORMATTER);
  }

  private String formatTimestamp(Timestamp value) {
    return value == null
        ? null
        : value.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DATE_TIME_FORMATTER);
  }

  private boolean isDuplicateKey(RuntimeException exception) {
    if (exception instanceof DuplicateKeyException) {
      return true;
    }
    Throwable current = exception;
    while (current != null) {
      String message = current.getMessage();
      if (message != null && message.contains("Duplicate entry")) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}
