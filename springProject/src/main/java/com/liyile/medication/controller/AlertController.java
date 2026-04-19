package com.liyile.medication.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyile.medication.common.ApiResponse;
import com.liyile.medication.common.ErrorCode;
import com.liyile.medication.dto.ResolveAlertDTO;
import com.liyile.medication.entity.Alert;
import com.liyile.medication.mapper.AlertMapper;
import com.liyile.medication.service.ReminderInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * 异常与告警控制器。
 * <p>查询和管理用药异常告警，包括超时未确认、检测失败、权限异常等告警信息。</p>
 *
 * @author Liyile
 */
@Tag(name = "异常告警管理", description = "用药异常与告警的查询接口，包括超时未确认、检测失败等异常信息")
@RestController
@RequestMapping("/api/alerts")
public class AlertController {
  /** 告警 Mapper */
  private final AlertMapper alertMapper;
  /** 提醒实例服务 */
  private final ReminderInstanceService reminderInstanceService;

  /** 构造方法注入依赖。 */
  public AlertController(
      AlertMapper alertMapper, ReminderInstanceService reminderInstanceService) {
    this.alertMapper = alertMapper;
    this.reminderInstanceService = reminderInstanceService;
  }

  /**
   * 查询告警列表（按患者）。
   * <p>根据患者ID查询该患者的所有异常告警记录，用于护工/子女端查看和老年人端的异常提示。</p>
   *
   * @param patientId 患者ID，必填参数
   * @return 告警列表，包含告警类型、时间戳、状态等信息
   */
  @Operation(
      summary = "查询异常告警列表",
      description = "根据患者ID查询该患者的所有异常告警记录，包括超时未确认、检测失败、权限异常等告警类型")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功，返回告警列表")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "参数校验失败，patientId不能为空")
  @GetMapping
  public ApiResponse<List<Alert>> list(
      @Parameter(name = "patientId", description = "患者ID", required = true, example = "1")
      @RequestParam Long patientId) {
    reminderInstanceService.reconcileOverdueInstances(patientId, LocalDateTime.now());
    List<Alert> list = alertMapper.selectList(new LambdaQueryWrapper<Alert>().eq(Alert::getPatientId, patientId));
    return ApiResponse.success(list);
  }

  /**
   * 标记告警已处理。
   * <p>将告警状态更新为resolved，并记录处理时间和处理备注。</p>
   *
   * @param id 告警ID
   * @param dto 处理信息DTO，包含actionNote（处理备注）
   * @return 更新后的告警对象
   */
  @Operation(
      summary = "标记告警已处理",
      description = "将告警状态更新为resolved，并记录处理时间和处理备注")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "处理成功，返回更新后的告警对象")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "告警不存在")
  @PostMapping("/{id}/resolve")
  public ApiResponse<Alert> resolve(
      @Parameter(name = "id", description = "告警ID", required = true, example = "1")
      @PathVariable("id") Long id,
      @Parameter(description = "处理信息，包含actionNote（处理备注）")
      @RequestBody ResolveAlertDTO dto) {
    
    Alert alert = alertMapper.selectById(id);
    if (alert == null) {
      return ApiResponse.failure(ErrorCode.NOT_FOUND, "告警不存在");
    }
    
    alert.setStatus("resolved");
    alert.setResolvedAt(Timestamp.from(Instant.now()));
    if (dto != null && dto.getActionNote() != null) {
      alert.setActionNote(dto.getActionNote());
    }
    
    alertMapper.updateById(alert);
    return ApiResponse.success(alert);
  }
}
