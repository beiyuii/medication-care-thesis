package com.liyile.medication.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyile.medication.entity.Settings;
import com.liyile.medication.mapper.SettingsMapper;
import com.liyile.medication.service.SettingsService;
import com.liyile.medication.vo.SettingsVO;
import org.springframework.stereotype.Service;

/**
 * 用户设置服务实现类。
 *
 * @author Liyile
 */
@Service
public class SettingsServiceImpl implements SettingsService {
  /** 设置Mapper */
  private final SettingsMapper settingsMapper;

  /** 构造方法注入依赖。 */
  public SettingsServiceImpl(SettingsMapper settingsMapper) {
    this.settingsMapper = settingsMapper;
  }

  @Override
  public SettingsVO getSettings(Long userId) {
    Settings settings = settingsMapper.selectOne(
        new LambdaQueryWrapper<Settings>().eq(Settings::getUserId, userId));
    
    if (settings == null) {
      // 返回默认设置
      return SettingsVO.defaultSettings();
    }
    
    return SettingsVO.from(settings);
  }

  @Override
  public SettingsVO updateSettings(Long userId, Settings updateSettings) {
    Settings existing = settingsMapper.selectOne(
        new LambdaQueryWrapper<Settings>().eq(Settings::getUserId, userId));
    
    if (existing == null) {
      // 如果不存在，创建新记录
      updateSettings.setUserId(userId);
      settingsMapper.insert(updateSettings);
      return SettingsVO.from(updateSettings);
    }
    
    // 部分更新：只更新非空字段
    if (updateSettings.getReminderEnableVoice() != null) {
      existing.setReminderEnableVoice(updateSettings.getReminderEnableVoice());
    }
    if (updateSettings.getReminderAdvanceMinutes() != null) {
      existing.setReminderAdvanceMinutes(updateSettings.getReminderAdvanceMinutes());
    }
    if (updateSettings.getReminderVolume() != null) {
      existing.setReminderVolume(updateSettings.getReminderVolume());
    }
    if (updateSettings.getDetectionAutoStart() != null) {
      existing.setDetectionAutoStart(updateSettings.getDetectionAutoStart());
    }
    if (updateSettings.getDetectionLowLightEnhance() != null) {
      existing.setDetectionLowLightEnhance(updateSettings.getDetectionLowLightEnhance());
    }
    if (updateSettings.getDetectionFallbackMode() != null) {
      existing.setDetectionFallbackMode(updateSettings.getDetectionFallbackMode());
    }
    if (updateSettings.getPrivacyCameraPermission() != null) {
      existing.setPrivacyCameraPermission(updateSettings.getPrivacyCameraPermission());
    }
    if (updateSettings.getPrivacyUploadConsent() != null) {
      existing.setPrivacyUploadConsent(updateSettings.getPrivacyUploadConsent());
    }
    if (updateSettings.getPrivacyShareToCaregiver() != null) {
      existing.setPrivacyShareToCaregiver(updateSettings.getPrivacyShareToCaregiver());
    }
    
    settingsMapper.updateById(existing);
    return SettingsVO.from(existing);
  }
}

