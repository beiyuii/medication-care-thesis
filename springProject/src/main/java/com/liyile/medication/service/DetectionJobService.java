package com.liyile.medication.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liyile.medication.common.ErrorCode;
import com.liyile.medication.entity.DetectionJob;
import com.liyile.medication.entity.ReminderInstance;
import com.liyile.medication.mapper.DetectionJobMapper;
import com.liyile.medication.mapper.ReminderInstanceMapper;
import com.liyile.medication.vo.DetectionJobVO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * 检测任务服务。
 */
@Service
public class DetectionJobService {
  private static final Logger log = LoggerFactory.getLogger(DetectionJobService.class);
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final DetectionJobMapper detectionJobMapper;
  private final ReminderInstanceMapper reminderInstanceMapper;
  private final ReminderInstanceService reminderInstanceService;
  private final ObjectMapper objectMapper;
  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${app.detector.base-url:http://127.0.0.1:8000}")
  private String detectorBaseUrl;

  public DetectionJobService(
      DetectionJobMapper detectionJobMapper,
      ReminderInstanceMapper reminderInstanceMapper,
      ReminderInstanceService reminderInstanceService,
      ObjectMapper objectMapper) {
    this.detectionJobMapper = detectionJobMapper;
    this.reminderInstanceMapper = reminderInstanceMapper;
    this.reminderInstanceService = reminderInstanceService;
    this.objectMapper = objectMapper;
  }

  public DetectionJob createAndDispatch(
      Long patientId,
      Long reminderInstanceId,
      MultipartFile videoFile,
      String cameraId,
      String modelVersion,
      Integer samplingRate,
      Integer maxFrames) {
    ReminderInstance instance = reminderInstanceMapper.selectById(reminderInstanceId);
    if (instance == null || !instance.getPatientId().equals(patientId)) {
      throw new IllegalArgumentException("提醒实例不存在或与患者不匹配");
    }
    if (videoFile == null || videoFile.isEmpty()) {
      throw new IllegalArgumentException("视频文件不能为空");
    }

    DetectionJob job = new DetectionJob();
    job.setPatientId(patientId);
    job.setScheduleId(instance.getScheduleId());
    job.setReminderInstanceId(reminderInstanceId);
    job.setInputType("video");
    job.setSourceFilename(videoFile.getOriginalFilename());
    job.setStatus("queued");
    job.setTraceId(currentTraceId());
    Timestamp now = Timestamp.from(Instant.now());
    job.setCreatedAt(now);
    job.setUpdatedAt(now);
    detectionJobMapper.insert(job);
    reminderInstanceService.markDetecting(instance, job.getId());

    byte[] videoBytes = toBytes(videoFile);
    String filename = videoFile.getOriginalFilename() != null ? videoFile.getOriginalFilename() : "recording.webm";
    String contentType = videoFile.getContentType() != null ? videoFile.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

    CompletableFuture.runAsync(() ->
        processJob(job.getId(), instance.getId(), patientId, instance.getScheduleId(),
            filename, contentType, videoBytes, cameraId, modelVersion, samplingRate, maxFrames));

    return detectionJobMapper.selectById(job.getId());
  }

  public DetectionJob getById(Long id) {
    return id == null ? null : detectionJobMapper.selectById(id);
  }

  public DetectionJobVO toVO(DetectionJob job) {
    if (job == null) {
      return null;
    }
    DetectionJobVO vo = new DetectionJobVO();
    vo.setId(job.getId());
    vo.setPatientId(job.getPatientId());
    vo.setScheduleId(job.getScheduleId());
    vo.setReminderInstanceId(job.getReminderInstanceId());
    vo.setStatus(job.getStatus());
    vo.setResultStatus(job.getResultStatus());
    vo.setConfidence(job.getConfidence());
    vo.setActionDetected(job.getActionDetected());
    vo.setTargetsJson(job.getTargetsJson());
    vo.setLatencyMs(job.getLatencyMs());
    vo.setErrorCode(job.getErrorCode());
    vo.setErrorMessage(job.getErrorMessage());
    vo.setTraceId(job.getTraceId());
    vo.setStartedAt(formatTimestamp(job.getStartedAt()));
    vo.setCompletedAt(formatTimestamp(job.getCompletedAt()));
    return vo;
  }

  private void processJob(
      Long jobId,
      Long reminderInstanceId,
      Long patientId,
      Long scheduleId,
      String filename,
      String contentType,
      byte[] videoBytes,
      String cameraId,
      String modelVersion,
      Integer samplingRate,
      Integer maxFrames) {
    DetectionJob job = detectionJobMapper.selectById(jobId);
    ReminderInstance instance = reminderInstanceMapper.selectById(reminderInstanceId);
    if (job == null || instance == null) {
      return;
    }
    try {
      job.setStatus("processing");
      job.setStartedAt(Timestamp.from(Instant.now()));
      job.setUpdatedAt(Timestamp.from(Instant.now()));
      detectionJobMapper.updateById(job);

      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("patientId", String.valueOf(patientId));
      body.add("scheduleId", String.valueOf(scheduleId));
      body.add("timestamp", Instant.now().toString());
      if (cameraId != null && !cameraId.isBlank()) {
        body.add("cameraId", cameraId);
      }
      if (modelVersion != null && !modelVersion.isBlank()) {
        body.add("modelVersion", modelVersion);
      }
      if (samplingRate != null) {
        body.add("samplingRate", String.valueOf(samplingRate));
      }
      if (maxFrames != null) {
        body.add("maxFrames", String.valueOf(maxFrames));
      }

      body.add("videoFile", new NamedByteArrayResource(videoBytes, filename, contentType));

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);
      ResponseEntity<FlaskVideoDetectionResponse> response = restTemplate.postForEntity(
          detectorBaseUrl + "/v1/detections/video/predict",
          new HttpEntity<>(body, headers),
          FlaskVideoDetectionResponse.class);

      FlaskVideoDetectionResponse payload = response.getBody();
      if (payload == null) {
        throw new IllegalStateException("算法服务返回空结果");
      }

      String videoPublicPath = null;
      try {
        videoPublicPath = persistUploadedVideo(jobId, filename, videoBytes);
      } catch (IOException ioException) {
        log.warn("保存检测录像失败 jobId={}", jobId, ioException);
      }

      job.setStatus("succeeded");
      job.setResultStatus(payload.getStatus());
      job.setConfidence(payload.getConfidence());
      job.setActionDetected(payload.getActionDetected());
      job.setTargetsJson(payload.getTargetsJson(objectMapper));
      job.setLatencyMs(payload.getLatencyMs());
      job.setTraceId(payload.getTraceId() != null ? payload.getTraceId() : job.getTraceId());
      job.setCompletedAt(Timestamp.from(Instant.now()));
      job.setUpdatedAt(Timestamp.from(Instant.now()));
      detectionJobMapper.updateById(job);

      reminderInstanceService.applyDetectionOutcome(
          reminderInstanceMapper.selectById(instance.getId()),
          jobId,
          payload.getStatus(),
          payload.getActionDetected(),
          payload.getTargetsJson(objectMapper),
          Timestamp.from(Instant.now()),
          videoPublicPath);
    } catch (Exception exception) {
      job.setStatus("failed");
      job.setErrorCode(String.valueOf(ErrorCode.SERVER_ERROR));
      job.setErrorMessage(exception.getMessage());
      job.setCompletedAt(Timestamp.from(Instant.now()));
      job.setUpdatedAt(Timestamp.from(Instant.now()));
      detectionJobMapper.updateById(job);
      reminderInstanceService.markJobFailed(
          reminderInstanceMapper.selectById(instance.getId()), jobId, exception.getMessage());
    }
  }

  /**
   * 将本次检测上传的视频写入本地 uploads/videos，并返回可供前端访问的相对 URL。
   *
   * @param jobId 检测任务 ID
   * @param originalFilename 原始文件名（用于选择扩展名）
   * @param videoBytes 视频二进制内容
   * @return 如 /uploads/videos/job-1.webm
   */
  private String persistUploadedVideo(long jobId, String originalFilename, byte[] videoBytes)
      throws IOException {
    String projectRoot = System.getProperty("user.dir");
    Path videosDir = Paths.get(projectRoot, "uploads", "videos");
    Files.createDirectories(videosDir);
    String extension = ".webm";
    if (originalFilename != null) {
      int dot = originalFilename.lastIndexOf('.');
      if (dot >= 0 && dot < originalFilename.length() - 1) {
        String candidate = originalFilename.substring(dot).toLowerCase();
        if (candidate.matches("\\.(webm|mp4|mov|mkv)")) {
          extension = candidate;
        }
      }
    }
    String fileName = "job-" + jobId + extension;
    Path target = videosDir.resolve(fileName);
    Files.write(target, videoBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    return "/uploads/videos/" + fileName;
  }

  private byte[] toBytes(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (Exception exception) {
      throw new IllegalArgumentException("读取视频文件失败", exception);
    }
  }

  private String currentTraceId() {
    String traceId = MDC.get("traceId");
    return traceId != null ? traceId : UUID.randomUUID().toString().replace("-", "");
  }

  private String formatTimestamp(Timestamp timestamp) {
    return timestamp == null
        ? null
        : timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DATE_TIME_FORMATTER);
  }

  /**
   * 兼容 Flask 当前返回结构的内部 DTO。
   */
  public static class FlaskVideoDetectionResponse {
    private String status;
    private Double confidence;
    private Boolean actionDetected;
    private Object targets;
    private Integer latencyMs;
    private String traceId;

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public Double getConfidence() {
      return confidence;
    }

    public void setConfidence(Double confidence) {
      this.confidence = confidence;
    }

    public Boolean getActionDetected() {
      return actionDetected;
    }

    public void setActionDetected(Boolean actionDetected) {
      this.actionDetected = actionDetected;
    }

    public Object getTargets() {
      return targets;
    }

    public void setTargets(Object targets) {
      this.targets = targets;
    }

    public Integer getLatencyMs() {
      return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
      this.latencyMs = latencyMs;
    }

    public String getTraceId() {
      return traceId;
    }

    public void setTraceId(String traceId) {
      this.traceId = traceId;
    }

    public String getTargetsJson(ObjectMapper objectMapper) {
      if (targets == null) {
        return null;
      }
      try {
        return objectMapper.writeValueAsString(targets);
      } catch (JsonProcessingException exception) {
        return String.valueOf(targets);
      }
    }
  }

  private static class NamedByteArrayResource extends ByteArrayResource {
    private final String filename;
    private final String contentType;

    private NamedByteArrayResource(byte[] byteArray, String filename, String contentType) {
      super(byteArray);
      this.filename = filename;
      this.contentType = contentType;
    }

    @Override
    public String getFilename() {
      return filename;
    }

    @Override
    public long contentLength() {
      return getByteArray().length;
    }

    @Override
    public String getDescription() {
      return contentType;
    }
  }
}
