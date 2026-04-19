package com.liyile.medication.service;

import com.liyile.medication.vo.ReportSummaryVO;

/**
 * 统计报表服务接口。
 * <p>提供服药统计与报表功能。</p>
 *
 * @author Liyile
 */
public interface ReportService {
  /**
   * 获取统计摘要。
   * <p>根据患者ID和时间范围统计服药情况。</p>
   *
   * @param patientId 患者ID
   * @param range 时间范围（day/week/month）
   * @return 统计摘要VO
   */
  ReportSummaryVO getSummary(Long patientId, String range);
}

