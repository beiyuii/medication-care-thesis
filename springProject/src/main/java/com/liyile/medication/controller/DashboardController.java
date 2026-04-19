package com.liyile.medication.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyile.medication.common.ApiResponse;
import com.liyile.medication.entity.Alert;
import com.liyile.medication.entity.IntakeEvent;
import com.liyile.medication.entity.Schedule;
import com.liyile.medication.service.PatientService;
import com.liyile.medication.service.ReminderInstanceService;
import com.liyile.medication.service.ReportService;
import com.liyile.medication.service.UserAccessService;
import com.liyile.medication.vo.CaregiverDashboardVO;
import com.liyile.medication.vo.ElderDashboardVO;
import com.liyile.medication.vo.PatientSummaryVO;
import com.liyile.medication.vo.ReminderInstanceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.liyile.medication.mapper.AlertMapper;
import com.liyile.medication.mapper.IntakeEventMapper;
import com.liyile.medication.mapper.ScheduleMapper;

@Tag(name = "仪表盘聚合", description = "多角色首页统一聚合接口")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
  private final UserAccessService userAccessService;
  private final PatientService patientService;
  private final ReminderInstanceService reminderInstanceService;
  private final ReportService reportService;
  private final AlertMapper alertMapper;
  private final IntakeEventMapper intakeEventMapper;
  private final ScheduleMapper scheduleMapper;

  public DashboardController(
      UserAccessService userAccessService,
      PatientService patientService,
      ReminderInstanceService reminderInstanceService,
      ReportService reportService,
      AlertMapper alertMapper,
      IntakeEventMapper intakeEventMapper,
      ScheduleMapper scheduleMapper) {
    this.userAccessService = userAccessService;
    this.patientService = patientService;
    this.reminderInstanceService = reminderInstanceService;
    this.reportService = reportService;
    this.alertMapper = alertMapper;
    this.intakeEventMapper = intakeEventMapper;
    this.scheduleMapper = scheduleMapper;
  }

  @Operation(summary = "elder 首页聚合", description = "返回老年人首页所需的下一次提醒、今日实例、活跃告警与完成率")
  @PreAuthorize("hasRole('ELDER')")
  @GetMapping("/elder")
  public ApiResponse<ElderDashboardVO> elder(@RequestHeader("Authorization") String authorization) {
    List<PatientSummaryVO> patients = userAccessService.getAccessiblePatients(authorization);
    ElderDashboardVO vo = new ElderDashboardVO();
    if (patients.isEmpty()) {
      vo.setTodayInstances(List.of());
      vo.setActiveAlerts(List.of());
      vo.setCompletionRate(0);
      return ApiResponse.success(vo);
    }

    Long patientId = patients.get(0).getId();
    List<ReminderInstanceVO> todayInstances =
        reminderInstanceService.listInstanceVOs(patientId, LocalDate.now(), null);
    vo.setTodayInstances(todayInstances);
    vo.setNextReminder(reminderInstanceService.getNextReminder(patientId));
    vo.setActiveAlerts(alertMapper.selectList(new LambdaQueryWrapper<Alert>()
        .eq(Alert::getPatientId, patientId)
        .ne(Alert::getStatus, "resolved")
        .orderByDesc(Alert::getTs)
        .last("LIMIT 10")));
    vo.setCompletionRate((int) Math.round(reportService.getSummary(patientId, "day").getConfirmRate() * 100));
    return ApiResponse.success(vo);
  }

  @Operation(summary = "caregiver/child 首页聚合", description = "返回关联患者列表、当前患者摘要、最近事件与活跃告警")
  @PreAuthorize("hasAnyRole('CAREGIVER', 'CHILD')")
  @GetMapping("/caregiver")
  public ApiResponse<CaregiverDashboardVO> caregiver(
      @RequestHeader("Authorization") String authorization,
      @RequestParam(required = false) Long patientId) {
    List<PatientSummaryVO> patients = userAccessService.getAccessiblePatients(authorization);
    CaregiverDashboardVO vo = new CaregiverDashboardVO();
    vo.setPatients(patients);
    if (patients.isEmpty()) {
      vo.setActivePatient(null);
      vo.setActiveAlerts(Collections.emptyList());
      vo.setRecentEvents(Collections.emptyList());
      return ApiResponse.success(vo);
    }

    PatientSummaryVO activePatient = patients.stream()
        .filter(patient -> patientId != null && patientId.equals(patient.getId()))
        .findFirst()
        .orElse(patients.get(0));
    Long activePatientId = activePatient.getId();
    vo.setActivePatient(activePatient);
    vo.setActiveAlerts(alertMapper.selectList(new LambdaQueryWrapper<Alert>()
        .eq(Alert::getPatientId, activePatientId)
        .ne(Alert::getStatus, "resolved")
        .orderByDesc(Alert::getTs)
        .last("LIMIT 10")));
    vo.setCompletionRate(
        (int) Math.round(reportService.getSummary(activePatientId, "day").getConfirmRate() * 100));
    List<IntakeEvent> recentEvents = intakeEventMapper.selectList(new LambdaQueryWrapper<IntakeEvent>()
        .eq(IntakeEvent::getPatientId, activePatientId)
        .orderByDesc(IntakeEvent::getTs)
        .last("LIMIT 10"));
    attachScheduleMetadata(recentEvents);
    vo.setRecentEvents(recentEvents);
    return ApiResponse.success(vo);
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
