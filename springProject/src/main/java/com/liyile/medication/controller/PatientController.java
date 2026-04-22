package com.liyile.medication.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyile.medication.common.ApiResponse;
import com.liyile.medication.common.CommonConstants;
import com.liyile.medication.common.ErrorCode;
import com.liyile.medication.dto.BindPatientRequest;
import com.liyile.medication.entity.Alert;
import com.liyile.medication.entity.Patient;
import com.liyile.medication.entity.Schedule;
import com.liyile.medication.mapper.AlertMapper;
import com.liyile.medication.mapper.ScheduleMapper;
import com.liyile.medication.security.JwtTokenProvider;
import com.liyile.medication.service.PatientService;
import com.liyile.medication.util.ScheduleTimeUtil;
import com.liyile.medication.service.UserService;
import com.liyile.medication.vo.PatientDetailVO;
import com.liyile.medication.vo.PatientSummaryVO;
import com.liyile.medication.vo.ScheduleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 患者管理控制器。
 * <p>提供患者信息查询接口，支持elder查看自己的患者信息，caregiver/child查看关联的患者信息。</p>
 *
 * @author Liyile
 */
@Tag(name = "患者管理", description = "患者信息查询接口")
@RestController
@RequestMapping("/api/patients")
public class PatientController {
  /** 患者服务 */
  private final PatientService patientService;
  /** 用户服务 */
  private final UserService userService;
  /** JWT工具 */
  private final JwtTokenProvider jwtTokenProvider;
  /** 计划Mapper */
  private final ScheduleMapper scheduleMapper;
  /** 告警Mapper */
  private final AlertMapper alertMapper;

  /** 构造方法注入依赖。 */
  public PatientController(
      PatientService patientService,
      UserService userService,
      JwtTokenProvider jwtTokenProvider,
      ScheduleMapper scheduleMapper,
      AlertMapper alertMapper) {
    this.patientService = patientService;
    this.userService = userService;
    this.jwtTokenProvider = jwtTokenProvider;
    this.scheduleMapper = scheduleMapper;
    this.alertMapper = alertMapper;
  }

  /**
   * 获取患者列表。
   * <p>返回当前账号可见的老年人列表。</p>
   * <p>elder角色：返回自己的患者记录</p>
   * <p>caregiver角色：返回关联的所有患者列表（一对多）</p>
   * <p>child角色：返回关联的患者列表（通常只有一个，一对一）</p>
   *
   * @param authorization Authorization头部（Bearer token）
   * @return 患者摘要列表
   */
  @Operation(
      summary = "获取患者列表",
      description = "返回当前账号可见的老年人列表。elder角色返回自己的患者；caregiver角色返回关联的所有患者（一对多）；child角色返回关联的患者（一对一，通常只有一个）")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功，返回患者列表")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或token无效")
  @GetMapping
  public ApiResponse<List<PatientSummaryVO>> list(
      @Parameter(name = "Authorization", description = "JWT token，格式为\"Bearer {token}\"", required = true)
      @RequestHeader("Authorization") String authorization) {
    String token = authorization.startsWith(CommonConstants.BEARER_PREFIX)
        ? authorization.substring(CommonConstants.BEARER_PREFIX.length())
        : authorization;
    String username = jwtTokenProvider.getUsername(token);
    String role = jwtTokenProvider.getRole(token);
    
    // 通过username查询用户ID
    com.liyile.medication.entity.User user = userService.findByUsername(username);
    if (user == null) {
      return ApiResponse.failure(ErrorCode.UNAUTHORIZED, "用户不存在");
    }
    
    List<PatientSummaryVO> patients = patientService.findPatientsByUserId(user.getId(), role);
    return ApiResponse.success(patients);
  }

  /**
   * 获取当前患者（用于child角色的一对一关系）。
   * <p>child角色专用接口，返回唯一关联的患者信息。</p>
   *
   * @param authorization Authorization头部（Bearer token）
   * @return 患者摘要VO，如果不是child角色或不存在则返回null
   */
  @Operation(
      summary = "获取当前患者（child角色专用）",
      description = "child角色专用接口，返回唯一关联的患者信息（一对一关系）。如果不是child角色或不存在关联患者，返回null")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功，返回患者信息或null")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或token无效")
  @GetMapping("/current")
  public ApiResponse<PatientSummaryVO> getCurrentPatient(
      @Parameter(name = "Authorization", description = "JWT token，格式为\"Bearer {token}\"", required = true)
      @RequestHeader("Authorization") String authorization) {
    String token = authorization.startsWith(CommonConstants.BEARER_PREFIX)
        ? authorization.substring(CommonConstants.BEARER_PREFIX.length())
        : authorization;
    String username = jwtTokenProvider.getUsername(token);
    String role = jwtTokenProvider.getRole(token);
    
    // 通过username查询用户ID
    com.liyile.medication.entity.User user = userService.findByUsername(username);
    if (user == null) {
      return ApiResponse.failure(ErrorCode.UNAUTHORIZED, "用户不存在");
    }
    
    PatientSummaryVO patient = patientService.findCurrentPatientByUserId(user.getId(), role);
    return ApiResponse.success(patient);
  }

  /**
   * 获取患者详情。
   * <p>返回单个老年人详情，包含基础信息、关联计划摘要、最近告警等。</p>
   *
   * @param id 患者ID
   * @return 患者详情VO
   */
  @Operation(
      summary = "获取患者详情",
      description = "返回单个老年人详情，包含基础信息、关联计划摘要（最近5条）、最近告警（最近5条）")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功，返回患者详情")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "患者不存在")
  @GetMapping("/{id}")
  public ApiResponse<PatientDetailVO> detail(
      @Parameter(name = "id", description = "患者ID", required = true, example = "1")
      @PathVariable("id") Long id) {
    Patient patient = patientService.findById(id);
    if (patient == null) {
      return ApiResponse.failure(ErrorCode.NOT_FOUND, "患者不存在");
    }
    
    PatientDetailVO vo = new PatientDetailVO();
    vo.setId(patient.getId());
    vo.setName(patient.getName());
    vo.setAge(patient.getAge());
    vo.setPhone(patient.getPhone());
    
    // 查询最近5条用药计划
    List<Schedule> schedules = scheduleMapper.selectList(
        new LambdaQueryWrapper<Schedule>()
            .eq(Schedule::getPatientId, id)
            .orderByDesc(Schedule::getId)
            .last("LIMIT 5"));
    vo.setSchedules(schedules.stream()
        .map(schedule -> {
          schedule.setNextIntake(ScheduleTimeUtil.calculateNextIntake(schedule, LocalDateTime.now()));
          ScheduleVO scheduleVO = ScheduleVO.from(schedule);
          return scheduleVO;
        })
        .collect(Collectors.toList()));
    
    // 查询最近5条告警
    List<Alert> alerts = alertMapper.selectList(
        new LambdaQueryWrapper<Alert>()
            .eq(Alert::getPatientId, id)
            .orderByDesc(Alert::getTs)
            .last("LIMIT 5"));
    vo.setRecentAlerts(alerts.stream()
        .map(alert -> {
          PatientDetailVO.AlertSummary alertSummary = new PatientDetailVO.AlertSummary();
          alertSummary.setId(alert.getId());
          alertSummary.setTitle(alert.getTitle());
          alertSummary.setOccurredAt(alert.getTs());
          return alertSummary;
        })
        .collect(Collectors.toList()));
    
    return ApiResponse.success(vo);
  }

  /**
   * 绑定老人账号到当前 caregiver / child。
   *
   * @param authorization 当前登录 token
   * @param request 绑定请求
   * @return 绑定后的患者摘要
   */
  @Operation(
      summary = "绑定老人账号",
      description = "护工/子女输入老人用户名后，与该老人对应的患者档案建立关联关系")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "绑定成功")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "角色不支持绑定或老人用户名不存在")
  @PostMapping("/bind")
  public ApiResponse<PatientSummaryVO> bind(
      @RequestHeader("Authorization") String authorization,
      @RequestBody BindPatientRequest request) {
    String token = authorization.startsWith(CommonConstants.BEARER_PREFIX)
        ? authorization.substring(CommonConstants.BEARER_PREFIX.length())
        : authorization;
    String username = jwtTokenProvider.getUsername(token);
    String role = jwtTokenProvider.getRole(token);

    com.liyile.medication.entity.User user = userService.findByUsername(username);
    if (user == null) {
      return ApiResponse.failure(ErrorCode.UNAUTHORIZED, "用户不存在");
    }

    try {
      return ApiResponse.success(
          patientService.bindPatientByElderUsername(user, role, request.getElderUsername()));
    } catch (IllegalArgumentException exception) {
      return ApiResponse.failure(ErrorCode.UNPROCESSABLE, exception.getMessage());
    }
  }
}
