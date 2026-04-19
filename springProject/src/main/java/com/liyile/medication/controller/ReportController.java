package com.liyile.medication.controller;

import com.liyile.medication.common.ApiResponse;
import com.liyile.medication.service.ReportService;
import com.liyile.medication.vo.ReportSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计报表控制器。
 * <p>提供服药统计与报表功能，支持按日/周/月维度统计服药率、漏服次数等指标。</p>
 *
 * @author Liyile
 */
@Tag(name = "统计报表管理", description = "服药统计与报表接口，支持按日/周/月维度统计服药率、漏服次数等")
@RestController
@RequestMapping("/api/reports")
public class ReportController {
  /** 报表服务 */
  private final ReportService reportService;

  /** 构造方法注入依赖。 */
  public ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  /**
   * 查询统计摘要。
   * <p>根据患者ID和时间范围查询统计摘要，包括服药率、漏服次数、异常次数等指标。</p>
   *
   * @param patientId 患者ID，必填参数
   * @param range 时间范围（可选），支持"day"、"week"、"month"，默认为"all"
   * @return 统计摘要对象，包含服药率、漏服次数等指标
   */
  @Operation(
      summary = "查询统计摘要",
      description = "根据患者ID和时间范围（day/week/month）查询统计摘要，包括服药率、漏服次数、异常次数等指标")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功，返回统计摘要对象")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "参数校验失败，patientId不能为空")
  @GetMapping("/summary")
  public ApiResponse<ReportSummaryVO> summary(
      @Parameter(name = "patientId", description = "患者ID", required = true, example = "1")
      @RequestParam Long patientId,
      @Parameter(name = "range", description = "时间范围（可选），支持day/week/month，默认为all", required = false, example = "week")
      @RequestParam(required = false) String range) {
    ReportSummaryVO summary = reportService.getSummary(patientId, range);
    return ApiResponse.success(summary);
  }
}