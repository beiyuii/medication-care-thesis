package com.liyile.medication.service;

import com.liyile.medication.entity.Settings;
import com.liyile.medication.vo.SettingsVO;

/**
 * 用户设置服务接口。
 * <p>提供用户设置的查询与更新功能。</p>
 *
 * @author Liyile
 */
public interface SettingsService {
  /**
   * 获取用户设置。
   * <p>如果用户设置不存在，返回默认值。</p>
   *
   * @param userId 用户ID
   * @return 设置VO对象
   */
  SettingsVO getSettings(Long userId);

  /**
   * 更新用户设置。
   * <p>支持部分更新，未提供的字段保持不变。</p>
   *
   * @param userId 用户ID
   * @param settings 设置实体对象（包含需要更新的字段）
   * @return 更新后的设置VO对象
   */
  SettingsVO updateSettings(Long userId, Settings settings);
}

