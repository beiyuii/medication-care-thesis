package com.liyile.medication.controller;

import com.liyile.medication.common.ApiResponse;
import com.liyile.medication.common.ErrorCode;
import com.liyile.medication.dto.ConfirmReminderInstanceDTO;
import com.liyile.medication.dto.RequestEvidenceDTO;
import com.liyile.medication.dto.ReviewReminderInstanceDTO;
import com.liyile.medication.dto.SubmitReminderInstanceDTO;
import com.liyile.medication.entity.ReminderInstance;
import com.liyile.medication.service.ReminderInstanceService;
import com.liyile.medication.service.UserAccessService;
import com.liyile.medication.vo.ReminderInstanceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "提醒实例管理", description = "每日提醒实例查询、提交与护工审核接口")
@RestController
@RequestMapping("/api/reminder-instances")
public class ReminderInstanceController {
  private final ReminderInstanceService reminderInstanceService;
  private final UserAccessService userAccessService;

  public ReminderInstanceController(
      ReminderInstanceService reminderInstanceService, UserAccessService userAccessService) {
    this.reminderInstanceService = reminderInstanceService;
    this.userAccessService = userAccessService;
  }

  @Operation(summary = "查询提醒实例", description = "按患者、日期、状态查询当日提醒实例")
  @GetMapping
  public ApiResponse<List<ReminderInstanceVO>> list(
      @RequestHeader("Authorization") String authorization,
      @Parameter(name = "patientId", required = true) @RequestParam Long patientId,
      @Parameter(name = "date") @RequestParam(required = false) String date,
      @Parameter(name = "status") @RequestParam(required = false) String status) {
    userAccessService.assertCanAccessPatient(authorization, patientId);
    LocalDate targetDate = date != null && !date.isBlank() ? LocalDate.parse(date) : LocalDate.now();
    return ApiResponse.success(reminderInstanceService.listInstanceVOs(patientId, targetDate, status));
  }

  @Operation(summary = "老人提交服药记录", description = "老人完成服药后提交本次记录，进入待护工审核")
  @PreAuthorize("hasRole('ELDER')")
  @PostMapping("/{id}/submit")
  public ApiResponse<ReminderInstanceVO> submit(
      @RequestHeader("Authorization") String authorization,
      @PathVariable("id") Long id,
      @RequestBody(required = false) SubmitReminderInstanceDTO dto) {
    ReminderInstance instance = reminderInstanceService.getById(id);
    if (instance == null) {
      return ApiResponse.failure(ErrorCode.NOT_FOUND, "提醒实例不存在");
    }
    userAccessService.assertCanAccessPatient(authorization, instance.getPatientId());
    String submittedBy = dto != null && dto.getSubmittedBy() != null
        ? dto.getSubmittedBy()
        : userAccessService.resolve(authorization).user().getUsername();
    Timestamp submitTime = parseTimestamp(dto != null ? dto.getSubmitTime() : null);
    ReminderInstance submitted = reminderInstanceService.submitInstance(instance, submittedBy, submitTime);
    return ApiResponse.success(reminderInstanceService.toVOs(List.of(submitted)).get(0));
  }

  @Operation(summary = "护工审核提醒实例", description = "护工确认、驳回或要求补充证据")
  @PreAuthorize("hasRole('CAREGIVER')")
  @PostMapping("/{id}/review")
  public ApiResponse<ReminderInstanceVO> review(
      @RequestHeader("Authorization") String authorization,
      @PathVariable("id") Long id,
      @RequestBody ReviewReminderInstanceDTO dto) {
    ReminderInstance instance = reminderInstanceService.getById(id);
    if (instance == null) {
      return ApiResponse.failure(ErrorCode.NOT_FOUND, "提醒实例不存在");
    }
    userAccessService.assertCanAccessPatient(authorization, instance.getPatientId());
    String reviewedBy = dto != null && dto.getReviewedBy() != null
        ? dto.getReviewedBy()
        : userAccessService.resolve(authorization).user().getUsername();
    Timestamp reviewTime = parseTimestamp(dto != null ? dto.getReviewTime() : null);
    ReminderInstance reviewed = reminderInstanceService.reviewInstance(
        instance,
        dto != null ? dto.getDecision() : null,
        reviewedBy,
        reviewTime,
        dto != null ? dto.getReason() : null);
    return ApiResponse.success(reminderInstanceService.toVOs(List.of(reviewed)).get(0));
  }

  @Operation(summary = "护工要求补充证据", description = "检测异常时要求补充证据或线下核实")
  @PreAuthorize("hasRole('CAREGIVER')")
  @PostMapping("/{id}/request-evidence")
  public ApiResponse<ReminderInstanceVO> requestEvidence(
      @RequestHeader("Authorization") String authorization,
      @PathVariable("id") Long id,
      @RequestBody(required = false) RequestEvidenceDTO dto) {
    ReminderInstance instance = reminderInstanceService.getById(id);
    if (instance == null) {
      return ApiResponse.failure(ErrorCode.NOT_FOUND, "提醒实例不存在");
    }
    userAccessService.assertCanAccessPatient(authorization, instance.getPatientId());
    String requestedBy = dto != null && dto.getRequestedBy() != null
        ? dto.getRequestedBy()
        : userAccessService.resolve(authorization).user().getUsername();
    Timestamp requestTime = parseTimestamp(dto != null ? dto.getRequestTime() : null);
    ReminderInstance updated =
        reminderInstanceService.requestEvidence(instance, requestedBy, requestTime, dto != null ? dto.getNote() : null);
    return ApiResponse.success(reminderInstanceService.toVOs(List.of(updated)).get(0));
  }

  @Operation(summary = "兼容旧确认接口", description = "兼容旧版前端，将确认操作映射到护工审核通过")
  @PreAuthorize("hasRole('CAREGIVER')")
  @PostMapping("/{id}/confirm")
  public ApiResponse<ReminderInstanceVO> confirm(
      @RequestHeader("Authorization") String authorization,
      @PathVariable("id") Long id,
      @RequestBody(required = false) ConfirmReminderInstanceDTO dto) {
    ReviewReminderInstanceDTO reviewDto = new ReviewReminderInstanceDTO();
    reviewDto.setDecision(ReminderInstanceService.DECISION_CONFIRMED);
    reviewDto.setReviewedBy(dto != null ? dto.getConfirmedBy() : null);
    reviewDto.setReviewTime(dto != null ? dto.getConfirmTime() : null);
    return review(authorization, id, reviewDto);
  }

  private Timestamp parseTimestamp(String isoValue) {
    if (isoValue == null || isoValue.isBlank()) {
      return Timestamp.from(Instant.now());
    }
    try {
      return Timestamp.from(Instant.parse(isoValue));
    } catch (Exception ignored) {
      return Timestamp.from(Instant.now());
    }
  }
}
