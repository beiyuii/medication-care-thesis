package com.liyile.medication.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyile.medication.entity.Alert;
import com.liyile.medication.entity.Patient;
import com.liyile.medication.entity.Schedule;
import com.liyile.medication.mapper.AlertMapper;
import com.liyile.medication.mapper.PatientMapper;
import com.liyile.medication.mapper.ScheduleMapper;
import com.liyile.medication.mapper.UserPatientRelationMapper;
import com.liyile.medication.service.PatientService;
import com.liyile.medication.service.ReminderInstanceService;
import com.liyile.medication.vo.ReminderInstanceVO;
import com.liyile.medication.vo.PatientSummaryVO;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 患者服务实现类。
 *
 * @author Liyile
 */
@Service
public class PatientServiceImpl implements PatientService {
  /** 患者Mapper */
  private final PatientMapper patientMapper;
  /** 用户患者关联Mapper */
  private final UserPatientRelationMapper userPatientRelationMapper;
  /** 计划Mapper */
  private final ScheduleMapper scheduleMapper;
  /** 告警Mapper */
  private final AlertMapper alertMapper;
  /** 提醒实例服务 */
  private final ReminderInstanceService reminderInstanceService;

  /** 构造方法注入依赖。 */
  public PatientServiceImpl(
      PatientMapper patientMapper,
      UserPatientRelationMapper userPatientRelationMapper,
      ScheduleMapper scheduleMapper,
      AlertMapper alertMapper,
      ReminderInstanceService reminderInstanceService) {
    this.patientMapper = patientMapper;
    this.userPatientRelationMapper = userPatientRelationMapper;
    this.scheduleMapper = scheduleMapper;
    this.alertMapper = alertMapper;
    this.reminderInstanceService = reminderInstanceService;
  }

  @Override
  public List<PatientSummaryVO> findPatientsByUserId(Long userId, String role) {
    List<Patient> patients;
    
    if ("elder".equals(role)) {
      // elder角色：查询自己的患者记录
      patients = patientMapper.selectList(
          new LambdaQueryWrapper<Patient>().eq(Patient::getElderUserId, userId));
    } else if ("caregiver".equals(role)) {
      // caregiver角色：查询关联的所有患者（一对多关系）
      List<Long> patientIds = userPatientRelationMapper.selectList(
          new LambdaQueryWrapper<com.liyile.medication.entity.UserPatientRelation>()
              .eq(com.liyile.medication.entity.UserPatientRelation::getUserId, userId)
              .eq(com.liyile.medication.entity.UserPatientRelation::getRelationType, "caregiver"))
          .stream()
          .map(com.liyile.medication.entity.UserPatientRelation::getPatientId)
          .collect(Collectors.toList());
      
      if (patientIds.isEmpty()) {
        return new ArrayList<>();
      }
      
      patients = patientMapper.selectBatchIds(patientIds);
    } else if ("child".equals(role)) {
      // child角色：查询关联的患者（一对一关系，但为了兼容性仍返回列表）
      List<Long> patientIds = userPatientRelationMapper.selectList(
          new LambdaQueryWrapper<com.liyile.medication.entity.UserPatientRelation>()
              .eq(com.liyile.medication.entity.UserPatientRelation::getUserId, userId)
              .eq(com.liyile.medication.entity.UserPatientRelation::getRelationType, "child"))
          .stream()
          .map(com.liyile.medication.entity.UserPatientRelation::getPatientId)
          .collect(Collectors.toList());
      
      if (patientIds.isEmpty()) {
        return new ArrayList<>();
      }
      
      patients = patientMapper.selectBatchIds(patientIds);
    } else {
      return new ArrayList<>();
    }
    
    // 转换为VO
    return patients.stream().map(patient -> {
      PatientSummaryVO vo = new PatientSummaryVO();
      vo.setId(patient.getId());
      vo.setName(patient.getName());
      
      // 查询该患者所有启用的计划
      List<Schedule> enabledSchedules = scheduleMapper.selectList(
          new LambdaQueryWrapper<Schedule>()
              .eq(Schedule::getPatientId, patient.getId())
              .eq(Schedule::getStatus, "enabled"));
      
      ReminderInstanceVO nextReminder = reminderInstanceService.getNextReminder(patient.getId());
      vo.setNextIntakeTime(nextReminder != null ? nextReminder.getWindowStartAt() : null);
      
      // 获取计划状态（是否有启用的计划）
      vo.setPlanStatus(enabledSchedules.isEmpty() ? "paused" : "active");
      
      // 获取告警数量（未处理的告警）
      long alertCount = alertMapper.selectCount(
          new LambdaQueryWrapper<Alert>()
              .eq(Alert::getPatientId, patient.getId())
              .ne(Alert::getStatus, "resolved"));
      vo.setAlertCount((int) alertCount);
      
      return vo;
    }).collect(Collectors.toList());
  }

  @Override
  public PatientSummaryVO findCurrentPatientByUserId(Long userId, String role) {
    // 只有child角色才使用此方法
    if (!"child".equals(role)) {
      return null;
    }
    
    // 查询child角色关联的患者（应该只有一个）
    List<com.liyile.medication.entity.UserPatientRelation> relations = 
        userPatientRelationMapper.selectList(
            new LambdaQueryWrapper<com.liyile.medication.entity.UserPatientRelation>()
                .eq(com.liyile.medication.entity.UserPatientRelation::getUserId, userId)
                .eq(com.liyile.medication.entity.UserPatientRelation::getRelationType, "child")
                .last("LIMIT 1"));
    
    if (relations.isEmpty()) {
      return null;
    }
    
    Long patientId = relations.get(0).getPatientId();
    Patient patient = patientMapper.selectById(patientId);
    
    if (patient == null) {
      return null;
    }
    
    // 转换为VO
    PatientSummaryVO vo = new PatientSummaryVO();
    vo.setId(patient.getId());
    vo.setName(patient.getName());
    
    // 查询该患者所有启用的计划
    List<Schedule> enabledSchedules = scheduleMapper.selectList(
        new LambdaQueryWrapper<Schedule>()
            .eq(Schedule::getPatientId, patient.getId())
            .eq(Schedule::getStatus, "enabled"));
    
    ReminderInstanceVO nextReminder = reminderInstanceService.getNextReminder(patient.getId());
    vo.setNextIntakeTime(nextReminder != null ? nextReminder.getWindowStartAt() : null);
    
    // 获取计划状态（是否有启用的计划）
    vo.setPlanStatus(enabledSchedules.isEmpty() ? "paused" : "active");
    
    // 获取告警数量（未处理的告警）
    long alertCount = alertMapper.selectCount(
        new LambdaQueryWrapper<Alert>()
            .eq(Alert::getPatientId, patient.getId())
            .ne(Alert::getStatus, "resolved"));
    vo.setAlertCount((int) alertCount);
    
    return vo;
  }

  @Override
  public Patient findById(Long id) {
    return patientMapper.selectById(id);
  }
}
