package com.liyile.medication.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyile.medication.entity.Alert;
import com.liyile.medication.entity.DetectionJob;
import com.liyile.medication.entity.IntakeEvent;
import com.liyile.medication.entity.ReminderInstance;
import com.liyile.medication.entity.Schedule;
import com.liyile.medication.mapper.AlertMapper;
import com.liyile.medication.mapper.DetectionJobMapper;
import com.liyile.medication.mapper.IntakeEventMapper;
import com.liyile.medication.mapper.ReminderInstanceMapper;
import com.liyile.medication.mapper.ScheduleMapper;
import com.liyile.medication.vo.ReminderInstanceVO;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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

  public static final String REVIEW_NOT_SUBMITTED = "not_submitted";
  public static final String REVIEW_WAITING_CAREGIVER = "waiting_caregiver";
  public static final String REVIEW_ABNORMAL_PENDING = "abnormal_pending_review";
  public static final String REVIEW_EVIDENCE_REQUIRED = "evidence_required";
  public static final String REVIEW_CONFIRMED = "caregiver_confirmed";
  public static final String REVIEW_REJECTED = "caregiver_rejected";
  public static final String REVIEW_TIMEOUT = "review_timeout";
  public static final String REVIEW_MISSED = "missed";
  public static final String REVIEW_WAITING_LATE = "waiting_caregiver_late";
  public static final String REVIEW_MANUAL_INTERVENTION = "manual_intervention";

  public static final String DETECTION_NONE = "none";
  public static final String DETECTION_SUSPECTED = "suspected";
  public static final String DETECTION_CONFIRMED = "confirmed";
  public static final String DETECTION_ABNORMAL = "abnormal";

  public static final String EVENT_PLAN_SCHEDULED = "plan_scheduled";
  public static final String EVENT_INTAKE_SUBMITTED = "intake_submitted";
  public static final String EVENT_DETECTION_COMPLETED = "detection_completed";
  public static final String EVENT_REVIEW_DECIDED = "review_decided";
  public static final String EVENT_INSTANCE_TIMEOUT = "instance_timeout";
  public static final String EVENT_RETRY_CREATED = "retry_created";

  public static final String DECISION_CONFIRMED = "confirmed";
  public static final String DECISION_REJECTED = "rejected";
  public static final String DECISION_NEEDS_EVIDENCE = "needs_evidence";

  private static final int MAX_RETRY_COUNT = 2;

  private final ReminderInstanceMapper reminderInstanceMapper;
  private final ScheduleMapper scheduleMapper;
  private final AlertMapper alertMapper;
  private final IntakeEventMapper intakeEventMapper;
  private final DetectionJobMapper detectionJobMapper;

  public ReminderInstanceService(
      ReminderInstanceMapper reminderInstanceMapper,
      ScheduleMapper scheduleMapper,
      AlertMapper alertMapper,
      IntakeEventMapper intakeEventMapper,
      DetectionJobMapper detectionJobMapper) {
    this.reminderInstanceMapper = reminderInstanceMapper;
    this.scheduleMapper = scheduleMapper;
    this.alertMapper = alertMapper;
    this.intakeEventMapper = intakeEventMapper;
    this.detectionJobMapper = detectionJobMapper;
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
    Set<Long> existingRootScheduleIds = existing.stream()
        .filter(instance -> instance.getParentInstanceId() == null
            || Integer.valueOf(0).equals(defaultRetryCount(instance)))
        .map(ReminderInstance::getScheduleId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    for (Schedule schedule : enabledSchedules) {
      if (schedule.getId() == null || existingRootScheduleIds.contains(schedule.getId())) {
        continue;
      }
      ReminderInstance instance = buildInstance(schedule, date, null, 0, REVIEW_NOT_SUBMITTED);
      try {
        reminderInstanceMapper.insert(instance);
        createLifecycleEvent(instance, EVENT_PLAN_SCHEDULED, DETECTION_NONE, null, null, null, null, null);
        existing.add(instance);
      } catch (RuntimeException exception) {
        if (!isDuplicateKey(exception)) {
          throw exception;
        }
        ReminderInstance persisted = reminderInstanceMapper.selectOne(new LambdaQueryWrapper<ReminderInstance>()
            .eq(ReminderInstance::getPatientId, patientId)
            .eq(ReminderInstance::getScheduleId, schedule.getId())
            .eq(ReminderInstance::getScheduledDate, Date.valueOf(date))
            .eq(ReminderInstance::getParentInstanceId, null)
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
        .orderByAsc(ReminderInstance::getWindowStartAt)
        .orderByAsc(ReminderInstance::getRetryCount)
        .orderByAsc(ReminderInstance::getId));
  }

  public List<ReminderInstanceVO> listInstanceVOs(Long patientId, LocalDate date, String statusFilter) {
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
    return instances.stream()
        .filter(instance -> !isTerminalReviewStatus(instance.getStatus()))
        .min(Comparator
            .comparingInt(this::resolveElderPriority)
            .thenComparing(ReminderInstance::getWindowStartAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(ReminderInstance::getRetryCount, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(ReminderInstance::getId, Comparator.nullsLast(Comparator.naturalOrder())))
        .map(instance -> toVOs(List.of(instance)).stream().findFirst().orElse(null))
        .orElse(null);
  }

  public List<ReminderInstance> listPendingReviewInstances(Long patientId, LocalDate date) {
    return materializeForPatient(patientId, date).stream()
        .filter(instance -> isReviewQueueStatus(instance.getStatus()))
        .sorted(Comparator
            .comparing(ReminderInstance::getReviewDeadline, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(ReminderInstance::getWindowStartAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(ReminderInstance::getRetryCount, Comparator.nullsLast(Comparator.naturalOrder())))
        .collect(Collectors.toList());
  }

  public void markDetecting(ReminderInstance instance, Long detectionJobId) {
    if (instance == null) {
      return;
    }
    instance.setLastDetectionJobId(detectionJobId);
    if (instance.getDetectionStatus() == null || instance.getDetectionStatus().isBlank()) {
      instance.setDetectionStatus(DETECTION_NONE);
    }
    instance.setUpdatedAt(Timestamp.from(Instant.now()));
    reminderInstanceMapper.updateById(instance);
  }

  public ReminderInstance submitInstance(ReminderInstance instance, String submittedBy, Timestamp submitTime) {
    if (instance == null) {
      return null;
    }
    Timestamp effectiveSubmitTime = submitTime != null ? submitTime : Timestamp.from(Instant.now());
    String nextReviewStatus = REVIEW_WAITING_CAREGIVER;
    if (REVIEW_MISSED.equals(instance.getStatus())) {
      nextReviewStatus = REVIEW_WAITING_LATE;
    } else if (DETECTION_ABNORMAL.equals(normalizeDetectionStatus(instance.getDetectionStatus()))) {
      nextReviewStatus = REVIEW_ABNORMAL_PENDING;
    }

    instance.setStatus(nextReviewStatus);
    instance.setReviewDeadline(resolveReviewDeadline(instance, effectiveSubmitTime));
    instance.setLateMinutes(resolveLateMinutes(instance, effectiveSubmitTime));
    instance.setUpdatedAt(Timestamp.from(Instant.now()));
    reminderInstanceMapper.updateById(instance);

    createLifecycleEvent(
        instance,
        EVENT_INTAKE_SUBMITTED,
        normalizeDetectionStatus(instance.getDetectionStatus()),
        null,
        null,
        "elder_submitted",
        submittedBy,
        effectiveSubmitTime);
    return instance;
  }

  public ReminderInstance reviewInstance(
      ReminderInstance instance,
      String decision,
      String reviewedBy,
      Timestamp reviewTime,
      String reason) {
    if (instance == null) {
      return null;
    }
    String normalizedDecision = normalizeDecision(decision);
    Timestamp effectiveReviewTime = reviewTime != null ? reviewTime : Timestamp.from(Instant.now());

    if (DECISION_CONFIRMED.equals(normalizedDecision) && REVIEW_CONFIRMED.equals(instance.getStatus())) {
      return instance;
    }
    if (DECISION_NEEDS_EVIDENCE.equals(normalizedDecision) && REVIEW_EVIDENCE_REQUIRED.equals(instance.getStatus())) {
      return instance;
    }
    if (DECISION_REJECTED.equals(normalizedDecision)
        && REVIEW_REJECTED.equals(instance.getStatus())
        && findExistingRetryInstance(instance, defaultRetryCount(instance) + 1) != null) {
      return instance;
    }

    instance.setReviewedBy(reviewedBy);
    instance.setReviewedAt(effectiveReviewTime);
    instance.setReviewReason(reason);
    instance.setUpdatedAt(Timestamp.from(Instant.now()));

    if (DECISION_NEEDS_EVIDENCE.equals(normalizedDecision)) {
      instance.setStatus(REVIEW_EVIDENCE_REQUIRED);
      reminderInstanceMapper.updateById(instance);
      createLifecycleEvent(
          instance,
          EVENT_REVIEW_DECIDED,
          normalizeDetectionStatus(instance.getDetectionStatus()),
          normalizedDecision,
          reason,
          "caregiver_request_evidence",
          reviewedBy,
          effectiveReviewTime);
      return instance;
    }

    if (DECISION_CONFIRMED.equals(normalizedDecision)) {
      instance.setStatus(REVIEW_CONFIRMED);
      instance.setConfirmedAt(effectiveReviewTime);
      reminderInstanceMapper.updateById(instance);
      createLifecycleEvent(
          instance,
          EVENT_REVIEW_DECIDED,
          normalizeDetectionStatus(instance.getDetectionStatus()),
          normalizedDecision,
          reason,
          "caregiver_confirmed",
          reviewedBy,
          effectiveReviewTime);
      resolveAlertsForInstance(instance, "护工已确认服药完成");
      return instance;
    }

    instance.setStatus(REVIEW_REJECTED);
    reminderInstanceMapper.updateById(instance);
    createLifecycleEvent(
        instance,
        EVENT_REVIEW_DECIDED,
        normalizeDetectionStatus(instance.getDetectionStatus()),
        normalizedDecision,
        reason,
        "caregiver_rejected",
        reviewedBy,
        effectiveReviewTime);
    resolveAlertsForInstance(instance, "护工已处理本次记录并要求重新服用");

    int nextRetryCount = defaultRetryCount(instance) + 1;
    if (nextRetryCount > MAX_RETRY_COUNT) {
      ReminderInstance manualInstance = createRetryInstance(instance, effectiveReviewTime, nextRetryCount,
          REVIEW_MANUAL_INTERVENTION);
      createLifecycleEvent(
          manualInstance,
          EVENT_RETRY_CREATED,
          DETECTION_NONE,
          null,
          reason,
          "manual_intervention",
          reviewedBy,
          effectiveReviewTime);
      createAlertIfMissing(
          manualInstance,
          "manual_intervention",
          String.format("计划 #%d 需人工介入", manualInstance.getScheduleId()),
          "同一时段重服次数已达上限，请尽快人工介入。",
          "high");
      return instance;
    }

    ReminderInstance retryInstance = createRetryInstance(instance, effectiveReviewTime, nextRetryCount,
        REVIEW_NOT_SUBMITTED);
    createLifecycleEvent(
        retryInstance,
        EVENT_RETRY_CREATED,
        DETECTION_NONE,
        null,
        reason,
        "retry_created",
        reviewedBy,
        effectiveReviewTime);
    return instance;
  }

  public ReminderInstance requestEvidence(
      ReminderInstance instance, String requestedBy, Timestamp requestTime, String note) {
    return reviewInstance(instance, DECISION_NEEDS_EVIDENCE, requestedBy, requestTime, note);
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
    String normalizedStatus = normalizeDetectionStatus(resultStatus);
    IntakeEvent event = createEvent(instance, detectionJobId, mapEventStatus(normalizedStatus),
        Boolean.TRUE.equals(actionDetected) ? "hand_to_mouth" : "detected", targetsJson, videoUrl);
    event.setEventType(EVENT_DETECTION_COMPLETED);
    event.setDetectionStatus(normalizedStatus);
    event.setTs((occurredAt != null ? occurredAt : Timestamp.from(Instant.now())).toInstant().toString());
    intakeEventMapper.insert(event);

    instance.setDetectionStatus(normalizedStatus);
    instance.setLastEventId(event.getId());
    instance.setLastDetectionJobId(detectionJobId);
    instance.setUpdatedAt(Timestamp.from(Instant.now()));
    reminderInstanceMapper.updateById(instance);

    if (DETECTION_ABNORMAL.equals(normalizedStatus)) {
      createAlertIfMissing(instance, "detection_failed",
          String.format("计划 #%d 检测异常", instance.getScheduleId()),
          "检测结果异常，请由护工核查后决定是否重服。", "high");
    } else {
      resolveAlertsForInstance(instance, "检测结果已更新");
    }
  }

  public void markJobFailed(ReminderInstance instance, Long detectionJobId, String message) {
    if (instance == null) {
      return;
    }
    instance.setDetectionStatus(DETECTION_ABNORMAL);
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
    Timestamp updatedAt = Timestamp.from(Instant.now());

    for (ReminderInstance instance : candidates) {
      if (shouldBecomeMissed(instance, now)) {
        instance.setStatus(REVIEW_MISSED);
        instance.setUpdatedAt(updatedAt);
        reminderInstanceMapper.updateById(instance);
        createLifecycleEvent(instance, EVENT_INSTANCE_TIMEOUT,
            normalizeDetectionStatus(instance.getDetectionStatus()), null, null, "missed_timeout", null, updatedAt);
        createAlertIfMissing(instance, "timeout",
            String.format("计划 #%d 未提交服药记录", instance.getScheduleId()),
            "用药时间窗结束后仍未提交服药记录。", "high");
        continue;
      }

      if (shouldBecomeReviewTimeout(instance, now)) {
        instance.setStatus(REVIEW_TIMEOUT);
        instance.setUpdatedAt(updatedAt);
        reminderInstanceMapper.updateById(instance);
        createLifecycleEvent(instance, EVENT_INSTANCE_TIMEOUT,
            normalizeDetectionStatus(instance.getDetectionStatus()), null, null, "review_timeout", null, updatedAt);
        createAlertIfMissing(instance, "review_timeout",
            String.format("计划 #%d 护工未及时审核", instance.getScheduleId()),
            "当日结束前护工仍未完成审核，请尽快跟进。", "high");
      }
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
    Set<Long> detectionJobIds = instances.stream()
        .map(ReminderInstance::getLastDetectionJobId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    Map<Long, DetectionJob> detectionJobMap = detectionJobIds.isEmpty()
        ? Collections.emptyMap()
        : detectionJobMapper.selectBatchIds(detectionJobIds).stream()
            .collect(Collectors.toMap(DetectionJob::getId, Function.identity()));

    List<ReminderInstanceVO> result = new ArrayList<>();
    for (ReminderInstance instance : instances) {
      Schedule schedule = schedules.get(instance.getScheduleId());
      DetectionJob detectionJob = instance.getLastDetectionJobId() != null
          ? detectionJobMap.get(instance.getLastDetectionJobId())
          : null;
      ReminderInstanceVO vo = new ReminderInstanceVO();
      vo.setId(instance.getId());
      vo.setPatientId(instance.getPatientId());
      vo.setScheduleId(instance.getScheduleId());
      vo.setScheduledDate(formatDate(instance.getScheduledDate()));
      vo.setWindowStartAt(formatTimestamp(instance.getWindowStartAt()));
      vo.setWindowEndAt(formatTimestamp(instance.getWindowEndAt()));
      vo.setStatus(instance.getStatus());
      vo.setReviewStatus(instance.getStatus());
      vo.setDetectionStatus(normalizeDetectionStatus(instance.getDetectionStatus()));
      vo.setParentInstanceId(instance.getParentInstanceId());
      vo.setRetryCount(defaultRetryCount(instance));
      vo.setReviewDeadline(formatTimestamp(instance.getReviewDeadline()));
      vo.setLateMinutes(instance.getLateMinutes());
      vo.setReviewedBy(instance.getReviewedBy());
      vo.setReviewedAt(formatTimestamp(instance.getReviewedAt()));
      vo.setReviewReason(instance.getReviewReason());
      vo.setConfirmedAt(formatTimestamp(instance.getConfirmedAt()));
      vo.setDetectionJobId(instance.getLastDetectionJobId());
      if (detectionJob != null) {
        vo.setTargetConfidence(detectionJob.getTargetConfidence());
        vo.setActionConfidence(detectionJob.getActionConfidence());
        vo.setFinalConfidence(
            detectionJob.getFinalConfidence() != null
                ? detectionJob.getFinalConfidence()
                : detectionJob.getConfidence());
        vo.setDetectionReasonCode(detectionJob.getReasonCode());
        vo.setDetectionReasonText(detectionJob.getReasonText());
        vo.setDetectionRiskTag(detectionJob.getRiskTag());
      }
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

  private ReminderInstance buildInstance(
      Schedule schedule, LocalDate date, Long parentInstanceId, int retryCount, String reviewStatus) {
    ReminderInstance instance = new ReminderInstance();
    instance.setPatientId(schedule.getPatientId());
    instance.setScheduleId(schedule.getId());
    instance.setScheduledDate(Date.valueOf(date));
    instance.setWindowStartAt(Timestamp.valueOf(LocalDateTime.of(date, parseTime(schedule.getWinStart()))));
    instance.setWindowEndAt(Timestamp.valueOf(LocalDateTime.of(date, parseTime(schedule.getWinEnd()))));
    instance.setStatus(reviewStatus);
    instance.setDetectionStatus(DETECTION_NONE);
    instance.setParentInstanceId(parentInstanceId);
    instance.setRetryCount(retryCount);
    instance.setReviewDeadline(resolveEndOfDay(date));
    Timestamp now = Timestamp.from(Instant.now());
    instance.setCreatedAt(now);
    instance.setUpdatedAt(now);
    return instance;
  }

  private ReminderInstance createRetryInstance(
      ReminderInstance source,
      Timestamp createdAt,
      int retryCount,
      String reviewStatus) {
    ReminderInstance retry = new ReminderInstance();
    retry.setPatientId(source.getPatientId());
    retry.setScheduleId(source.getScheduleId());
    retry.setScheduledDate(source.getScheduledDate());
    retry.setWindowStartAt(source.getWindowStartAt());
    retry.setWindowEndAt(source.getWindowEndAt());
    retry.setStatus(reviewStatus);
    retry.setDetectionStatus(DETECTION_NONE);
    retry.setParentInstanceId(source.getId());
    retry.setRetryCount(retryCount);
    retry.setReviewDeadline(resolveReviewDeadline(source, createdAt));
    retry.setCreatedAt(createdAt);
    retry.setUpdatedAt(createdAt);
    try {
      reminderInstanceMapper.insert(retry);
      return retry;
    } catch (RuntimeException exception) {
      if (!isDuplicateKey(exception)) {
        throw exception;
      }
      ReminderInstance existingRetry = reminderInstanceMapper.selectOne(new LambdaQueryWrapper<ReminderInstance>()
          .eq(ReminderInstance::getPatientId, source.getPatientId())
          .eq(ReminderInstance::getScheduleId, source.getScheduleId())
          .eq(ReminderInstance::getScheduledDate, source.getScheduledDate())
          .eq(ReminderInstance::getRetryCount, retryCount)
          .last("LIMIT 1"));
      if (existingRetry != null) {
        return existingRetry;
      }
      throw exception;
    }
  }

  private ReminderInstance findExistingRetryInstance(ReminderInstance source, int retryCount) {
    if (source == null || source.getId() == null) {
      return null;
    }
    return reminderInstanceMapper.selectOne(new LambdaQueryWrapper<ReminderInstance>()
        .eq(ReminderInstance::getParentInstanceId, source.getId())
        .eq(ReminderInstance::getRetryCount, retryCount)
        .last("LIMIT 1"));
  }

  private void createLifecycleEvent(
      ReminderInstance instance,
      String eventType,
      String detectionStatus,
      String reviewDecision,
      String reviewReason,
      String action,
      String operator,
      Timestamp occurredAt) {
    IntakeEvent event = createEvent(instance, instance != null ? instance.getLastDetectionJobId() : null,
        mapEventStatusByContext(eventType, detectionStatus, reviewDecision), action, null, null);
    event.setEventType(eventType);
    event.setDetectionStatus(detectionStatus);
    event.setReviewDecision(reviewDecision);
    event.setReviewReason(reviewReason);
    event.setConfirmedBy(operator);
    event.setConfirmedAt(occurredAt);
    event.setTs((occurredAt != null ? occurredAt : Timestamp.from(Instant.now())).toInstant().toString());
    intakeEventMapper.insert(event);
    if (instance != null) {
      instance.setLastEventId(event.getId());
      reminderInstanceMapper.updateById(instance);
    }
  }

  private boolean isScheduleEnabled(Schedule schedule) {
    if (schedule == null || schedule.getStatus() == null) {
      return false;
    }
    String status = schedule.getStatus().toLowerCase();
    return "enabled".equals(status) || "active".equals(status);
  }

  private boolean shouldBecomeMissed(ReminderInstance instance, LocalDateTime now) {
    if (instance == null || !REVIEW_NOT_SUBMITTED.equals(instance.getStatus()) || instance.getWindowEndAt() == null) {
      return false;
    }
    return instance.getWindowEndAt().toLocalDateTime().isBefore(now);
  }

  private boolean shouldBecomeReviewTimeout(ReminderInstance instance, LocalDateTime now) {
    if (instance == null || !isReviewTimeoutCandidate(instance.getStatus())) {
      return false;
    }
    Timestamp deadline = instance.getReviewDeadline();
    if (deadline == null) {
      return false;
    }
    return deadline.toLocalDateTime().isBefore(now);
  }

  private boolean isReviewQueueStatus(String status) {
    return REVIEW_WAITING_CAREGIVER.equals(status)
        || REVIEW_ABNORMAL_PENDING.equals(status)
        || REVIEW_EVIDENCE_REQUIRED.equals(status)
        || REVIEW_WAITING_LATE.equals(status)
        || REVIEW_TIMEOUT.equals(status);
  }

  private boolean isReviewTimeoutCandidate(String status) {
    return REVIEW_WAITING_CAREGIVER.equals(status)
        || REVIEW_ABNORMAL_PENDING.equals(status)
        || REVIEW_EVIDENCE_REQUIRED.equals(status)
        || REVIEW_WAITING_LATE.equals(status);
  }

  private boolean isTerminalReviewStatus(String status) {
    return REVIEW_CONFIRMED.equals(status) || REVIEW_MANUAL_INTERVENTION.equals(status);
  }

  private int resolveElderPriority(ReminderInstance instance) {
    String status = instance.getStatus();
    if (REVIEW_REJECTED.equals(status)) return 0;
    if (REVIEW_NOT_SUBMITTED.equals(status)) return 1;
    if (REVIEW_MANUAL_INTERVENTION.equals(status)) return 2;
    if (REVIEW_WAITING_CAREGIVER.equals(status)
        || REVIEW_TIMEOUT.equals(status)
        || REVIEW_WAITING_LATE.equals(status)
        || REVIEW_ABNORMAL_PENDING.equals(status)
        || REVIEW_EVIDENCE_REQUIRED.equals(status)) {
      return 3;
    }
    return 9;
  }

  private Timestamp resolveReviewDeadline(ReminderInstance instance, Timestamp referenceTime) {
    LocalDate date = instance != null && instance.getScheduledDate() != null
        ? instance.getScheduledDate().toLocalDate()
        : referenceTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    return resolveEndOfDay(date);
  }

  private Timestamp resolveEndOfDay(LocalDate date) {
    return Timestamp.valueOf(LocalDateTime.of(date, LocalTime.of(23, 59, 59)));
  }

  private Integer resolveLateMinutes(ReminderInstance instance, Timestamp submitTime) {
    if (instance == null || submitTime == null || instance.getWindowEndAt() == null) {
      return null;
    }
    long minutes = Duration.between(instance.getWindowEndAt().toInstant(), submitTime.toInstant()).toMinutes();
    return minutes > 0 ? (int) minutes : null;
  }

  private int defaultRetryCount(ReminderInstance instance) {
    return instance.getRetryCount() != null ? instance.getRetryCount() : 0;
  }

  private String normalizeDetectionStatus(String resultStatus) {
    if (resultStatus == null || resultStatus.isBlank()) {
      return DETECTION_NONE;
    }
    String status = resultStatus.toLowerCase();
    if (DETECTION_CONFIRMED.equals(status)
        || DETECTION_SUSPECTED.equals(status)
        || DETECTION_ABNORMAL.equals(status)
        || DETECTION_NONE.equals(status)) {
      return status;
    }
    return DETECTION_ABNORMAL;
  }

  private String normalizeDecision(String decision) {
    if (decision == null || decision.isBlank()) {
      return DECISION_CONFIRMED;
    }
    String normalized = decision.toLowerCase();
    if (DECISION_CONFIRMED.equals(normalized)
        || DECISION_REJECTED.equals(normalized)
        || DECISION_NEEDS_EVIDENCE.equals(normalized)) {
      return normalized;
    }
    return DECISION_CONFIRMED;
  }

  private String mapEventStatus(String detectionStatus) {
    if (DETECTION_CONFIRMED.equals(detectionStatus)) {
      return "confirmed";
    }
    if (DETECTION_SUSPECTED.equals(detectionStatus)) {
      return "suspected";
    }
    return "abnormal";
  }

  private String mapEventStatusByContext(String eventType, String detectionStatus, String reviewDecision) {
    if (EVENT_REVIEW_DECIDED.equals(eventType)) {
      if (DECISION_CONFIRMED.equals(reviewDecision)) {
        return "confirmed";
      }
      if (DECISION_NEEDS_EVIDENCE.equals(reviewDecision)) {
        return "suspected";
      }
      return "abnormal";
    }
    if (EVENT_RETRY_CREATED.equals(eventType) || EVENT_PLAN_SCHEDULED.equals(eventType)) {
      return "suspected";
    }
    return mapEventStatus(normalizeDetectionStatus(detectionStatus));
  }

  private IntakeEvent createEvent(
      ReminderInstance instance,
      Long detectionJobId,
      String status,
      String action,
      String targetsJson,
      String videoUrl) {
    IntakeEvent event = new IntakeEvent();
    if (instance != null) {
      event.setPatientId(instance.getPatientId());
      event.setScheduleId(instance.getScheduleId());
      event.setReminderInstanceId(instance.getId());
      event.setDetectionStatus(normalizeDetectionStatus(instance.getDetectionStatus()));
    }
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
