package com.liyile.medication.controller;

import com.liyile.medication.common.ApiResponse;
import com.liyile.medication.common.ErrorCode;
import com.liyile.medication.dto.ConfirmReminderInstanceDTO;
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

@Tag(name = "提醒实例管理", description = "每日提醒实例查询与确认接口")
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

  @Operation(summary = "人工确认提醒实例", description = "将提醒实例直接确认并同步事件与告警状态")
  @PreAuthorize("hasAnyRole('ELDER', 'CAREGIVER', 'CHILD')")
  @PostMapping("/{id}/confirm")
  public ApiResponse<ReminderInstanceVO> confirm(
      @RequestHeader("Authorization") String authorization,
      @PathVariable("id") Long id,
      @RequestBody(required = false) ConfirmReminderInstanceDTO dto) {
    ReminderInstance instance = reminderInstanceService.getById(id);
    if (instance == null) {
      return ApiResponse.failure(ErrorCode.NOT_FOUND, "提醒实例不存在");
    }
    userAccessService.assertCanAccessPatient(authorization, instance.getPatientId());
    String confirmedBy = dto != null && dto.getConfirmedBy() != null
        ? dto.getConfirmedBy()
        : userAccessService.resolve(authorization).user().getUsername();
    Timestamp confirmTime = Timestamp.from(Instant.now());
    if (dto != null && dto.getConfirmTime() != null && !dto.getConfirmTime().isBlank()) {
      try {
        confirmTime = Timestamp.from(Instant.parse(dto.getConfirmTime()));
      } catch (Exception ignored) {
        // keep now
      }
    }
    ReminderInstance confirmed =
        reminderInstanceService.confirmInstance(instance, confirmedBy, confirmTime, instance.getLastDetectionJobId());
    return ApiResponse.success(reminderInstanceService.toVOs(List.of(confirmed)).get(0));
  }
}
