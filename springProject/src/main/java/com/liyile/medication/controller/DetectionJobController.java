package com.liyile.medication.controller;

import com.liyile.medication.common.ApiResponse;
import com.liyile.medication.common.ErrorCode;
import com.liyile.medication.dto.CreateDetectionJobRequest;
import com.liyile.medication.entity.DetectionJob;
import com.liyile.medication.service.DetectionJobService;
import com.liyile.medication.service.UserAccessService;
import com.liyile.medication.vo.DetectionJobVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "检测任务管理", description = "Spring 统一编排的视频检测任务接口")
@RestController
@RequestMapping("/api/detection-jobs")
public class DetectionJobController {
  private final DetectionJobService detectionJobService;
  private final UserAccessService userAccessService;

  public DetectionJobController(
      DetectionJobService detectionJobService, UserAccessService userAccessService) {
    this.detectionJobService = detectionJobService;
    this.userAccessService = userAccessService;
  }

  @Operation(summary = "创建检测任务", description = "上传视频并创建异步检测任务")
  @PreAuthorize("hasRole('ELDER')")
  @PostMapping(consumes = {"multipart/form-data"})
  public ApiResponse<DetectionJobVO> create(
      @RequestHeader("Authorization") String authorization,
      @ModelAttribute CreateDetectionJobRequest request,
      @RequestPart("videoFile") MultipartFile videoFile) {
    userAccessService.assertCanAccessPatient(authorization, request.getPatientId());
    DetectionJob job = detectionJobService.createAndDispatch(
        request.getPatientId(),
        request.getReminderInstanceId(),
        videoFile,
        request.getCameraId(),
        request.getModelVersion(),
        request.getSamplingRate(),
        request.getMaxFrames());
    return ApiResponse.success(detectionJobService.toVO(job));
  }

  @Operation(summary = "查询检测任务", description = "按任务 ID 查询当前检测状态与结果")
  @GetMapping("/{id}")
  public ApiResponse<DetectionJobVO> detail(
      @RequestHeader("Authorization") String authorization, @PathVariable("id") Long id) {
    DetectionJob job = detectionJobService.getById(id);
    if (job == null) {
      return ApiResponse.failure(ErrorCode.NOT_FOUND, "检测任务不存在");
    }
    userAccessService.assertCanAccessPatient(authorization, job.getPatientId());
    return ApiResponse.success(detectionJobService.toVO(job));
  }
}
