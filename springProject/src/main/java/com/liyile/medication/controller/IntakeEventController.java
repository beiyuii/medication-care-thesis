package com.liyile.medication.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyile.medication.common.ApiResponse;
import com.liyile.medication.common.ErrorCode;
import com.liyile.medication.dto.ConfirmEventDTO;
import com.liyile.medication.entity.Alert;
import com.liyile.medication.entity.IntakeEvent;
import com.liyile.medication.entity.ReminderInstance;
import com.liyile.medication.entity.Schedule;
import com.liyile.medication.mapper.AlertMapper;
import com.liyile.medication.mapper.IntakeEventMapper;
import com.liyile.medication.mapper.ScheduleMapper;
import com.liyile.medication.service.ReminderInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 服药事件控制器。
 * <p>记录和查询服药事件，包括检测结果、提交记录、审核决策与超时轨迹。</p>
 *
 * @author Liyile
 */
@Tag(name = "服药事件管理", description = "服药事件的记录与查询接口，包括摄像头检测结果、手动确认状态等")
@RestController
@RequestMapping("/api/intake-events")
public class IntakeEventController {
  /** 日志记录器 */
  private static final Logger logger = LoggerFactory.getLogger(IntakeEventController.class);
  
  /** 事件表 Mapper */
  private final IntakeEventMapper intakeEventMapper;
  /** 告警 Mapper */
  private final AlertMapper alertMapper;
  /** 计划 Mapper */
  private final ScheduleMapper scheduleMapper;
  /** 提醒实例服务 */
  private final ReminderInstanceService reminderInstanceService;

  /** 构造方法注入依赖。 */
  public IntakeEventController(
      IntakeEventMapper intakeEventMapper,
      AlertMapper alertMapper,
      ScheduleMapper scheduleMapper,
      ReminderInstanceService reminderInstanceService) {
    this.intakeEventMapper = intakeEventMapper;
    this.alertMapper = alertMapper;
    this.scheduleMapper = scheduleMapper;
    this.reminderInstanceService = reminderInstanceService;
  }

  /**
   * 新增服药事件。
   * <p>记录一次服药事件，包括检测到的药品目标、吃药动作、事件状态等信息。
   * 事件状态包括：suspected（疑似已服药）、confirmed（已确认）、abnormal（异常）。</p>
   *
   * @param event 服药事件实体对象，包含patientId、scheduleId、status、action、targetsJson、imgUrl等字段
   * @return 创建成功的服药事件，包含自动生成的ID
   */
  @Operation(
      summary = "记录服药事件",
      description = "记录一次服药事件，包括检测状态（suspected/confirmed/abnormal）、药品目标（targetsJson）、动作检测（action）、关键帧图片URL等信息")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功，返回新增的事件对象")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "参数校验失败，必填字段缺失")
  @PostMapping
  public ApiResponse<IntakeEvent> create(
      @Parameter(description = "服药事件对象，包含patientId、scheduleId、ts（时间戳）、status（suspected/confirmed/abnormal）、action（是否检测到动作）、targetsJson（药品检测结果JSON）、imgUrl（关键帧图片URL）等字段")
      @RequestBody IntakeEvent event) {
    logger.info("收到创建服药事件请求: patientId={}, scheduleId={}, status={}", 
        event.getPatientId(), event.getScheduleId(), event.getStatus());
    intakeEventMapper.insert(event);
    generateAlertForEvent(event);
    logger.info("创建服药事件成功: eventId={}, patientId={}, status={}", 
        event.getId(), event.getPatientId(), event.getStatus());
    return ApiResponse.success(event);
  }

  /**
   * 查询服药事件（按患者与时间范围）。
   * <p>根据患者ID查询服药事件列表，可选时间范围过滤。</p>
   *
   * @param patientId 患者ID，必填参数
   * @param range 时间范围过滤（可选），如"day"、"week"、"month"
   * @return 服药事件列表，包含该患者的所有事件记录
   */
  @Operation(
      summary = "查询服药事件列表",
      description = "根据患者ID查询服药事件列表，可选按时间范围过滤（day/week/month）")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功，返回事件列表")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "参数校验失败，patientId不能为空")
  @GetMapping
  public ApiResponse<List<IntakeEvent>> list(
      @Parameter(name = "patientId", description = "患者ID", required = true, example = "1")
      @RequestParam @NotNull Long patientId,
      @Parameter(name = "range", description = "时间范围（可选），支持day/week/month", required = false, example = "week")
      @RequestParam(required = false) String range) {
    logger.info("收到查询服药事件列表请求: patientId={}, range={}", patientId, range);
    
    LambdaQueryWrapper<IntakeEvent> wrapper = new LambdaQueryWrapper<IntakeEvent>()
        .eq(IntakeEvent::getPatientId, patientId);
    
    // 实现range过滤逻辑
    if (range != null && !range.isEmpty()) {
      LocalDateTime startTime = calculateStartTime(range);
      if (startTime != null) {
        String startTimeStr = startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        wrapper.ge(IntakeEvent::getTs, startTimeStr);
        logger.debug("应用时间范围过滤: range={}, startTime={}", range, startTimeStr);
      }
    }
    
    wrapper.orderByDesc(IntakeEvent::getTs);
    List<IntakeEvent> list = intakeEventMapper.selectList(wrapper);
    attachScheduleMetadata(list);
    logger.info("查询服药事件列表成功: patientId={}, range={}, 事件数量={}", patientId, range, list.size());
    return ApiResponse.success(list);
  }

  /**
   * 手动确认服药事件。
   * <p>用户手动确认"疑似已服药"事件，将状态更新为confirmed。</p>
   *
   * @param id 事件ID
   * @param dto 确认信息DTO，包含confirmedBy和confirmTime
   * @return 更新后的事件对象
   */
  @Operation(
      summary = "兼容旧版确认事件",
      description = "旧版入口，实际会映射为护工对关联提醒实例执行确认通过审核")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "确认成功，返回更新后的事件对象")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "事件不存在")
  @PreAuthorize("hasRole('CAREGIVER')")
  @PostMapping("/{id}/confirm")
  public ApiResponse<IntakeEvent> confirm(
      @Parameter(name = "id", description = "事件ID", required = true, example = "1")
      @PathVariable("id") Long id,
      @Parameter(description = "确认信息，包含confirmedBy（确认人）和confirmTime（确认时间）")
      @RequestBody ConfirmEventDTO dto) {
    logger.info("收到确认服药事件请求: eventId={}, confirmedBy={}", id, dto.getConfirmedBy());
    
    IntakeEvent event = intakeEventMapper.selectById(id);
    if (event == null) {
      logger.warn("确认服药事件失败: 事件不存在, eventId={}", id);
      return ApiResponse.failure(ErrorCode.NOT_FOUND, "事件不存在");
    }
    
    java.sql.Timestamp reviewTime = java.sql.Timestamp.from(Instant.now());
    if (dto != null && dto.getConfirmTime() != null) {
      try {
        reviewTime = java.sql.Timestamp.from(Instant.parse(dto.getConfirmTime()));
      } catch (Exception e) {
        logger.warn("解析确认时间失败，使用当前时间: eventId={}, confirmTime={}", id, dto.getConfirmTime());
      }
    }

    event.setStatus("confirmed");
    event.setEventType(ReminderInstanceService.EVENT_REVIEW_DECIDED);
    event.setReviewDecision(ReminderInstanceService.DECISION_CONFIRMED);
    event.setConfirmedBy(dto != null ? dto.getConfirmedBy() : event.getConfirmedBy());
    event.setConfirmedAt(reviewTime);
    intakeEventMapper.updateById(event);

    if (event.getReminderInstanceId() != null) {
      ReminderInstance instance = reminderInstanceService.getById(event.getReminderInstanceId());
      if (instance != null) {
        reminderInstanceService.reviewInstance(
            instance,
            ReminderInstanceService.DECISION_CONFIRMED,
            dto != null ? dto.getConfirmedBy() : event.getConfirmedBy(),
            reviewTime,
            null);
      }
    } else {
      resolvePendingAlertsForSchedule(event);
    }

    logger.info("确认服药事件成功: eventId={}, confirmedBy={}", id, dto != null ? dto.getConfirmedBy() : null);
    return ApiResponse.success(event);
  }

  /**
   * 计算时间范围的起始时间。
   *
   * @param range 时间范围（day/week/month）
   * @return 起始时间，如果range无效则返回null
   */
  private LocalDateTime calculateStartTime(String range) {
    LocalDateTime now = LocalDateTime.now();
    switch (range.toLowerCase()) {
      case "day":
        return now.minusDays(1);
      case "week":
        return now.minusWeeks(1);
      case "month":
        return now.minusMonths(1);
      default:
        return null;
    }
  }

  private void generateAlertForEvent(IntakeEvent event) {
    if (event == null || event.getPatientId() == null || event.getScheduleId() == null) {
      return;
    }
    if (!"abnormal".equalsIgnoreCase(event.getStatus())) {
      return;
    }

    Schedule schedule = scheduleMapper.selectById(event.getScheduleId());
    String medicineName = schedule != null ? schedule.getMedicineName() : "计划 #" + event.getScheduleId();
    String title = String.format("计划 #%d 检测异常", event.getScheduleId());

    long existing = alertMapper.selectCount(new LambdaQueryWrapper<Alert>()
        .eq(Alert::getPatientId, event.getPatientId())
        .eq(Alert::getTitle, title)
        .eq(Alert::getStatus, "pending"));
    if (existing > 0) {
      return;
    }

    Alert alert = new Alert();
    alert.setPatientId(event.getPatientId());
    alert.setTitle(title);
    alert.setDescription(String.format("%s 检测结果异常，请及时核查。", medicineName));
    alert.setSeverity("high");
    alert.setType("detection_failed");
    alert.setTs(event.getTs() != null ? event.getTs() : Instant.now().toString());
    alert.setStatus("pending");
    alertMapper.insert(alert);
  }

  private void resolvePendingAlertsForSchedule(IntakeEvent event) {
    if (event == null || event.getPatientId() == null || event.getScheduleId() == null) {
      return;
    }
    String titlePrefix = String.format("计划 #%d ", event.getScheduleId());
    java.sql.Timestamp resolvedAt = java.sql.Timestamp.from(Instant.now());
    List<Alert> alerts = alertMapper.selectList(new LambdaQueryWrapper<Alert>()
        .eq(Alert::getPatientId, event.getPatientId())
        .eq(Alert::getStatus, "pending"));
    for (Alert alert : alerts) {
      if (alert.getTitle() != null && alert.getTitle().startsWith(titlePrefix)) {
        alert.setStatus("resolved");
        alert.setResolvedAt(resolvedAt);
        if (alert.getActionNote() == null || alert.getActionNote().isBlank()) {
          alert.setActionNote("服药事件已确认，系统自动关闭告警");
        }
        alertMapper.updateById(alert);
      }
    }
  }

  private void attachScheduleMetadata(List<IntakeEvent> events) {
    if (events == null || events.isEmpty()) {
      return;
    }
    Set<Long> scheduleIds = events.stream()
        .map(IntakeEvent::getScheduleId)
        .filter(id -> id != null && id > 0)
        .collect(Collectors.toSet());
    if (scheduleIds.isEmpty()) {
      return;
    }
    Map<Long, Schedule> scheduleMap = scheduleMapper.selectBatchIds(scheduleIds).stream()
        .collect(Collectors.toMap(Schedule::getId, schedule -> schedule));
    for (IntakeEvent event : events) {
      Schedule schedule = scheduleMap.get(event.getScheduleId());
      if (schedule == null) {
        continue;
      }
      String medicineName = schedule.getMedicineName();
      if (medicineName == null || medicineName.isBlank()) {
        medicineName = "计划 #" + event.getScheduleId();
      }
      event.setMedicineName(medicineName);
      event.setScheduleName(medicineName);
    }
  }
}
