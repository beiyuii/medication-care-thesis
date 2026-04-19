package com.liyile.medication.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyile.medication.common.ApiResponse;
import com.liyile.medication.common.ErrorCode;
import com.liyile.medication.entity.Schedule;
import com.liyile.medication.mapper.ScheduleMapper;
import com.liyile.medication.util.ScheduleTimeUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用药计划控制器。
 * <p>提供用药计划的增删改查与启停控制功能，包括药品类型、剂量、频次、时间窗等信息的管理。</p>
 *
 * @author Liyile
 */
@Tag(name = "用药计划管理", description = "用药计划的增删改查与启停控制接口")
@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {
  /** 日志记录器 */
  private static final Logger logger = LoggerFactory.getLogger(ScheduleController.class);
  
  /** 计划表 Mapper */
  private final ScheduleMapper scheduleMapper;

  /** 构造方法注入依赖。 */
  public ScheduleController(ScheduleMapper scheduleMapper) {
    this.scheduleMapper = scheduleMapper;
  }

  /**
   * 查询计划列表（按患者）。
   * <p>根据患者ID查询该患者的所有用药计划，包括药品类型、剂量、频次、时间窗等信息。</p>
   *
   * @param patientId 患者ID，必填参数
   * @return 用药计划列表，包含所有计划的详细信息
   */
  @Operation(
      summary = "查询用药计划列表",
      description = "根据患者ID查询该患者的所有用药计划，包括药品类型（PILL/BLISTER/BOTTLE/BOX）、剂量、频次、时间窗等信息")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功，返回计划列表")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "参数校验失败，patientId不能为空")
  @GetMapping
  public ApiResponse<List<Schedule>> list(
      @Parameter(name = "patientId", description = "患者ID", required = true, example = "1")
      @RequestParam @NotNull Long patientId) {
    logger.info("收到查询用药计划列表请求: patientId={}", patientId);
    List<Schedule> list =
        scheduleMapper.selectList(new LambdaQueryWrapper<Schedule>().eq(Schedule::getPatientId, patientId));
    LocalDateTime now = LocalDateTime.now();
    list.forEach(schedule -> schedule.setNextIntake(ScheduleTimeUtil.calculateNextIntake(schedule, now)));
    ApiResponse<List<Schedule>> response = ApiResponse.success(list);
    logger.info("查询用药计划列表成功: patientId={}, 计划数量={}, code={}", patientId, list.size(), response.getCode());
    return response;
  }

  /**
   * 新增计划。
   * <p>创建新的用药计划，需要指定药品类型、剂量、频次、时间窗、周期等信息。</p>
   *
   * @param schedule 用药计划实体对象，包含所有必填字段
   * @return 创建成功的用药计划，包含自动生成的ID
   */
  @Operation(
      summary = "创建用药计划",
      description = "创建新的用药计划，需要指定药品类型（PILL/BLISTER/BOTTLE/BOX）、剂量、频次、时间窗（winStart、winEnd）、周期（period）等信息")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功，返回新增的计划对象")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "参数校验失败，必填字段缺失或格式错误")
  @PreAuthorize("hasRole('ELDER')")
  @PostMapping
  public ApiResponse<Schedule> create(
      @Parameter(description = "用药计划对象，包含patientId、type、dose、freq、winStart、winEnd、period、status等字段")
      @RequestBody Schedule schedule) {
    scheduleMapper.insert(schedule);
    return ApiResponse.success(schedule);
  }

  /**
   * 更新计划。
   * <p>根据计划ID更新用药计划的部分或全部字段。</p>
   *
   * @param id 计划ID，路径参数
   * @param schedule 用药计划实体对象，包含需要更新的字段
   * @return 更新影响的行数，通常为1表示更新成功
   */
  @Operation(
      summary = "更新用药计划",
      description = "根据计划ID更新用药计划的部分或全部字段，支持修改药品类型、剂量、频次、时间窗等信息")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功，返回影响的行数")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "计划不存在")
  @PreAuthorize("hasRole('ELDER')")
  @PatchMapping("/{id}")
  public ApiResponse<Integer> update(
      @Parameter(name = "id", description = "计划ID", required = true, example = "1")
      @PathVariable("id") Long id,
      @Parameter(description = "用药计划对象，包含需要更新的字段")
      @RequestBody Schedule schedule) {
    schedule.setId(id);
    int rows = scheduleMapper.updateById(schedule);
    return ApiResponse.success(rows);
  }

  /**
   * 启停计划切换。
   * <p>切换用药计划的启用/禁用状态，支持通过Body参数指定目标状态（active/paused）。</p>
   *
   * @param id 计划ID，路径参数
   * @param body 请求体，包含status字段（active/paused），可选
   * @return 更新影响的行数，通常为1表示切换成功
   */
  @Operation(
      summary = "启停计划切换",
      description = "切换用药计划的启用/禁用状态，支持通过Body参数指定目标状态（active/paused），如果不提供则自动切换")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "切换成功，返回影响的行数")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "计划不存在")
  @PreAuthorize("hasRole('ELDER')")
  @PostMapping("/{id}/toggle")
  public ApiResponse<Schedule> toggle(
      @Parameter(name = "id", description = "计划ID", required = true, example = "1")
      @PathVariable("id") Long id,
      @Parameter(description = "请求体，包含status字段（active/paused），可选")
      @RequestBody(required = false) java.util.Map<String, String> body) {
    logger.info("收到启停计划切换请求: scheduleId={}, body={}", id, body);
    
    Schedule s = scheduleMapper.selectById(id);
    if (s == null) {
      logger.warn("启停计划切换失败: 计划不存在, scheduleId={}", id);
      return ApiResponse.failure(ErrorCode.NOT_FOUND, "计划不存在");
    }
    
    String oldStatus = s.getStatus();
    String targetStatus;
    if (body != null && body.containsKey("status")) {
      // 如果提供了status参数，使用提供的值
      String status = body.get("status");
      // 转换：active -> enabled, paused -> disabled
      targetStatus = "active".equalsIgnoreCase(status) ? "enabled" : "disabled";
    } else {
      // 如果没有提供，自动切换
      targetStatus = "enabled".equalsIgnoreCase(s.getStatus()) ? "disabled" : "enabled";
    }
    
    s.setStatus(targetStatus);
    int rows = scheduleMapper.updateById(s);
    
    if (rows > 0) {
      // 重新查询更新后的计划对象
      Schedule updated = scheduleMapper.selectById(id);
      logger.info("启停计划切换成功: scheduleId={}, 旧状态={}, 新状态={}", id, oldStatus, targetStatus);
      return ApiResponse.success(updated);
    } else {
      logger.warn("启停计划切换失败: 更新影响行数为0, scheduleId={}", id);
      return ApiResponse.failure(ErrorCode.SERVER_ERROR, "更新失败");
    }
  }
}
