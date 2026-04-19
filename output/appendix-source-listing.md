# 附录：源程序清单（核心代码节选）

本文附录不罗列全部源码，而是选取最能体现系统设计思路的核心实现片段，覆盖算法推理、动作识别、三态判定、后端业务接口以及前端检测流程 6 个方面。

## 1. YOLOv8 检测推理核心代码

来源文件：
- `flask-project/app/routers/v1/detection.py`
- `flask-project/app/services/video_detector.py`

说明：
这一部分体现算法服务的主入口，以及 ONNX Runtime 调用 YOLOv8 模型完成药品目标检测的核心流程。

```python
# 文件：flask-project/app/routers/v1/detection.py

async def _resolve_and_predict(payload: DetectionRequest):
    """根据 payload 解析 frame，并调用 detector 输出结果。"""

    frame_bytes = await load_frame_bytes(payload.frame_b64, payload.frame_url, settings)
    return await detector_service.predict(payload, frame_bytes)


@bp.post("/predict")
def predict():
    """主要入口：对请求体做参数校验，并输出统一 JSON。"""

    trace_id = get_trace_id()
    try:
        body = request.get_json(force=True, silent=False)
    except WerkzeugBadRequest as exc:
        raise BadRequestError("Request body must be valid JSON") from exc

    try:
        payload = DetectionRequest.model_validate(body or {})
    except ValidationError as exc:
        raise UnprocessableEntityError(exc.errors()) from exc

    logger.info("predict_in", patient_id=payload.patient_id, schedule_id=payload.schedule_id)
    response_model = asyncio.run(_resolve_and_predict(payload))
    logger.info(
        "predict_out",
        patient_id=payload.patient_id,
        schedule_id=payload.schedule_id,
        status=response_model.status,
    )
    return jsonify(response_model.model_dump(by_alias=True))
```

```python
# 文件：flask-project/app/services/video_detector.py

def _detect_medication_yolo(
    self, frame: np.ndarray, model: LoadedYOLOModel
) -> list[dict[str, Any]]:
    """
    使用YOLOv8检测药品。
    """
    input_height, input_width = model.input_shape[1], model.input_shape[0]
    frame_resized = cv2.resize(frame, (input_width, input_height))
    frame_rgb = cv2.cvtColor(frame_resized, cv2.COLOR_BGR2RGB)
    frame_normalized = frame_rgb.astype(np.float32) / 255.0

    # 转换为 NCHW 输入张量
    input_tensor = np.transpose(frame_normalized, (2, 0, 1))
    input_tensor = np.expand_dims(input_tensor, axis=0)

    try:
        outputs = model.session.run(model.output_names, {model.input_name: input_tensor})
        detections = []
        if len(outputs) > 0:
            output = outputs[0]
            logger.debug(f"YOLOv8 output shape: {output.shape}")

            if len(output.shape) == 3 and output.shape[0] > 0:
                batch_output = output[0]
                output_dim = output.shape[2]
                if output_dim < 5:
                    logger.warning(f"Unexpected output dimension: {output_dim}, expected at least 5")
                    return detections

                for detection in batch_output:
                    if len(detection) < 4:
                        continue

                    x_center = float(detection[0])
                    y_center = float(detection[1])
                    width = float(detection[2])
                    height = float(detection[3])
                    class_scores = detection[4:]

                    class_scores_array = np.array(class_scores)
                    class_scores_clipped = np.clip(class_scores_array, -500, 500)
                    probabilities = 1.0 / (1.0 + np.exp(-class_scores_clipped))

                    class_id = int(np.argmax(probabilities))
                    confidence = float(probabilities[class_id])
                    confidence = max(0.0, min(1.0, confidence))

                    if confidence < self.settings.confidence_threshold:
                        continue

                    x_min_px = (x_center - width / 2) * input_width
                    y_min_px = (y_center - height / 2) * input_height
                    x_max_px = (x_center + width / 2) * input_width
                    y_max_px = (y_center + height / 2) * input_height

                    x_min_px = max(0, min(x_min_px, input_width))
                    y_min_px = max(0, min(y_min_px, input_height))
                    x_max_px = max(0, min(x_max_px, input_width))
                    y_max_px = max(0, min(y_max_px, input_height))

                    label_map = ["PILL", "BLISTER", "BOTTLE", "BOX"]
                    label = label_map[class_id] if class_id < len(label_map) else "PILL"

                    detections.append(
                        {
                            "label": label,
                            "score": float(confidence),
                            "bbox": (
                                float(x_min_px / input_width),
                                float(y_min_px / input_height),
                                float(x_max_px / input_width),
                                float(y_max_px / input_height),
                            ),
                        }
                    )

        return detections
    except Exception as exc:
        logger.exception("yolo_inference_failed", error=str(exc))
        return self._generate_mock_detections(frame)
```

## 2. 动作识别核心代码

来源文件：
- `flask-project/app/services/video_detector.py`
- `flask-project/app/services/result_aggregator.py`

说明：
该部分体现“手口距离 + 连续帧规则”的动作识别思路。前者负责从单帧估算动作特征，后者负责将连续帧合并为动作时间段。

```python
# 文件：flask-project/app/services/video_detector.py

def _detect_hand_landmarks(
    self, frame: np.ndarray
) -> tuple[list[tuple[float, float]] | None, tuple[float, float] | None, float | None]:
    """
    使用MediaPipe Hands检测手部关键点，并估算口部位置。
    """
    if not MEDIAPIPE_AVAILABLE or self._mp_hands is None:
        return self._detect_hand_landmarks_fallback(frame)

    frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    results = self._mp_hands.process(frame_rgb)

    if not results.multi_hand_landmarks:
        return None, None, None

    hand_landmarks = results.multi_hand_landmarks[0]
    height, width = frame.shape[:2]

    landmarks = []
    for landmark in hand_landmarks.landmark:
        x = landmark.x * width
        y = landmark.y * height
        landmarks.append((float(x), float(y)))

    # 简化估算口部位置
    mouth_x = width * 0.5
    mouth_y = height * 0.4
    mouth_position = (mouth_x, mouth_y)

    # 关键点 8 为食指指尖
    index_finger_tip = landmarks[8]
    hand_mouth_distance = np.sqrt(
        (index_finger_tip[0] - mouth_position[0]) ** 2
        + (index_finger_tip[1] - mouth_position[1]) ** 2
    )

    pixel_to_cm_ratio = 12.0 / width
    hand_mouth_distance_cm = hand_mouth_distance * pixel_to_cm_ratio

    return landmarks, mouth_position, hand_mouth_distance_cm
```

```python
# 文件：flask-project/app/services/result_aggregator.py

def detect_action_timeline(
    self, frame_results: list[FrameDetectionResult]
) -> list[ActionTimeline]:
    """
    检测动作时间线，基于手-口距离和药品检测。
    """
    action_segments = []
    current_segment_start: int | None = None
    consecutive_action_frames = 0

    for frame_result in frame_results:
        has_medication = len(frame_result.targets) > 0
        hand_mouth_close = (
            frame_result.hand_mouth_distance is not None
            and frame_result.hand_mouth_distance <= self.action_distance_threshold
        )

        is_action_frame = has_medication and hand_mouth_close

        if is_action_frame:
            if current_segment_start is None:
                current_segment_start = frame_result.frame_index
            consecutive_action_frames += 1
        else:
            if current_segment_start is not None:
                if consecutive_action_frames >= self.action_frames_threshold:
                    confidence = min(
                        0.99,
                        0.5 + (consecutive_action_frames / self.action_frames_threshold) * 0.4,
                    )
                    action_segments.append(
                        ActionTimeline(
                            start_frame=current_segment_start,
                            end_frame=frame_result.frame_index - 1,
                            confidence=round(confidence, 3),
                        )
                    )
                current_segment_start = None
                consecutive_action_frames = 0

    if current_segment_start is not None and consecutive_action_frames >= self.action_frames_threshold:
        confidence = min(
            0.99,
            0.5 + (consecutive_action_frames / self.action_frames_threshold) * 0.4,
        )
        action_segments.append(
            ActionTimeline(
                start_frame=current_segment_start,
                end_frame=frame_results[-1].frame_index,
                confidence=round(confidence, 3),
            )
        )

    logger.debug("action_timeline_detected", segment_count=len(action_segments))
    return action_segments
```

## 3. 三态结果协议判定代码

来源文件：
- `flask-project/app/services/result_aggregator.py`

说明：
该函数体现系统的三态协议：`confirmed / suspected / abnormal`。判定综合考虑“药品目标是否存在”“动作是否成立”“整体置信度是否达到阈值”。

```python
# 文件：flask-project/app/services/result_aggregator.py

def calculate_overall_status(
    self,
    targets: list[VideoDetectionTarget],
    action_timeline: list[ActionTimeline],
) -> tuple[str, float, bool]:
    """
    计算整体检测状态、置信度和是否检测到动作。
    """
    action_detected = len(action_timeline) > 0

    if not targets:
        return ("abnormal", 0.0, action_detected)

    avg_confidence = sum(t.score for t in targets) / len(targets)

    if action_detected and avg_confidence >= 0.85:
        status = "confirmed"
    elif action_detected or avg_confidence >= 0.5:
        status = "suspected"
    else:
        status = "abnormal"

    return (status, round(avg_confidence, 3), action_detected)
```

## 4. 用药计划管理接口

来源文件：
- `springProject/src/main/java/com/liyile/medication/controller/ScheduleController.java`

说明：
该部分体现计划管理模块的典型 CRUD 能力，以及“启用/暂停”状态切换逻辑。

```java
// 文件：springProject/src/main/java/com/liyile/medication/controller/ScheduleController.java

@GetMapping
public ApiResponse<List<Schedule>> list(
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

@PreAuthorize("hasRole('ELDER')")
@PostMapping
public ApiResponse<Schedule> create(@RequestBody Schedule schedule) {
  scheduleMapper.insert(schedule);
  return ApiResponse.success(schedule);
}

@PreAuthorize("hasRole('ELDER')")
@PatchMapping("/{id}")
public ApiResponse<Integer> update(
    @PathVariable("id") Long id,
    @RequestBody Schedule schedule) {
  schedule.setId(id);
  int rows = scheduleMapper.updateById(schedule);
  return ApiResponse.success(rows);
}

@PreAuthorize("hasRole('ELDER')")
@PostMapping("/{id}/toggle")
public ApiResponse<Schedule> toggle(
    @PathVariable("id") Long id,
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
    String status = body.get("status");
    targetStatus = "active".equalsIgnoreCase(status) ? "enabled" : "disabled";
  } else {
    targetStatus = "enabled".equalsIgnoreCase(s.getStatus()) ? "disabled" : "enabled";
  }

  s.setStatus(targetStatus);
  int rows = scheduleMapper.updateById(s);

  if (rows > 0) {
    Schedule updated = scheduleMapper.selectById(id);
    logger.info("启停计划切换成功: scheduleId={}, 旧状态={}, 新状态={}", id, oldStatus, targetStatus);
    return ApiResponse.success(updated);
  } else {
    logger.warn("启停计划切换失败: 更新影响行数为0, scheduleId={}", id);
    return ApiResponse.failure(ErrorCode.SERVER_ERROR, "更新失败");
  }
}
```

## 5. 服药事件处理接口

来源文件：
- `springProject/src/main/java/com/liyile/medication/controller/IntakeEventController.java`

说明：
该部分体现“服药事件入库 -> 异常告警生成 -> 人工确认 -> 自动关闭告警”的业务主链路。

```java
// 文件：springProject/src/main/java/com/liyile/medication/controller/IntakeEventController.java

@PostMapping
public ApiResponse<IntakeEvent> create(@RequestBody IntakeEvent event) {
  logger.info("收到创建服药事件请求: patientId={}, scheduleId={}, status={}",
      event.getPatientId(), event.getScheduleId(), event.getStatus());
  intakeEventMapper.insert(event);
  generateAlertForEvent(event);
  logger.info("创建服药事件成功: eventId={}, patientId={}, status={}",
      event.getId(), event.getPatientId(), event.getStatus());
  return ApiResponse.success(event);
}

@PreAuthorize("hasAnyRole('ELDER', 'CAREGIVER', 'CHILD')")
@PostMapping("/{id}/confirm")
public ApiResponse<IntakeEvent> confirm(
    @PathVariable("id") Long id,
    @RequestBody ConfirmEventDTO dto) {
  logger.info("收到确认服药事件请求: eventId={}, confirmedBy={}", id, dto.getConfirmedBy());

  IntakeEvent event = intakeEventMapper.selectById(id);
  if (event == null) {
    logger.warn("确认服药事件失败: 事件不存在, eventId={}", id);
    return ApiResponse.failure(ErrorCode.NOT_FOUND, "事件不存在");
  }

  String oldStatus = event.getStatus();
  event.setStatus("confirmed");
  event.setConfirmedBy(dto.getConfirmedBy());
  if (dto.getConfirmTime() != null) {
    try {
      Instant instant = Instant.parse(dto.getConfirmTime());
      event.setConfirmedAt(java.sql.Timestamp.from(instant));
    } catch (Exception e) {
      logger.warn("解析确认时间失败，使用当前时间: eventId={}, confirmTime={}", id, dto.getConfirmTime());
      event.setConfirmedAt(java.sql.Timestamp.from(Instant.now()));
    }
  } else {
    event.setConfirmedAt(java.sql.Timestamp.from(Instant.now()));
  }

  intakeEventMapper.updateById(event);
  syncReminderInstance(event, dto);
  resolvePendingAlertsForSchedule(event);
  logger.info("确认服药事件成功: eventId={}, 旧状态={}, 新状态=confirmed, confirmedBy={}",
      id, oldStatus, dto.getConfirmedBy());
  return ApiResponse.success(event);
}
```

```java
// 文件：springProject/src/main/java/com/liyile/medication/controller/IntakeEventController.java

private void generateAlertForEvent(IntakeEvent event) {
  if (event == null || event.getPatientId() == null || event.getScheduleId() == null) {
    return;
  }
  if (!"abnormal".equalsIgnoreCase(event.getStatus())) {
    return;
  }

  Schedule schedule = scheduleMapper.selectById(event.getScheduleId());
  String medicineName = schedule != null ? schedule.getMedicineName() : "计划 #" + event.getScheduleId();
  String title = String.format("计划 #%d 检测异常", event.getScheduleId());

  long existing = alertMapper.selectCount(new LambdaQueryWrapper<Alert>()
      .eq(Alert::getPatientId, event.getPatientId())
      .eq(Alert::getTitle, title)
      .eq(Alert::getStatus, "pending"));
  if (existing > 0) {
    return;
  }

  Alert alert = new Alert();
  alert.setPatientId(event.getPatientId());
  alert.setTitle(title);
  alert.setDescription(String.format("%s 检测结果异常，请及时核查。", medicineName));
  alert.setSeverity("high");
  alert.setType("detection_failed");
  alert.setTs(event.getTs() != null ? event.getTs() : Instant.now().toString());
  alert.setStatus("pending");
  alertMapper.insert(alert);
}

private void resolvePendingAlertsForSchedule(IntakeEvent event) {
  if (event == null || event.getPatientId() == null || event.getScheduleId() == null) {
    return;
  }
  String titlePrefix = String.format("计划 #%d ", event.getScheduleId());
  java.sql.Timestamp resolvedAt = java.sql.Timestamp.from(Instant.now());
  List<Alert> alerts = alertMapper.selectList(new LambdaQueryWrapper<Alert>()
      .eq(Alert::getPatientId, event.getPatientId())
      .eq(Alert::getStatus, "pending"));
  for (Alert alert : alerts) {
    if (alert.getTitle() != null && alert.getTitle().startsWith(titlePrefix)) {
      alert.setStatus("resolved");
      alert.setResolvedAt(resolvedAt);
      if (alert.getActionNote() == null || alert.getActionNote().isBlank()) {
        alert.setActionNote("服药事件已确认，系统自动关闭告警");
      }
      alertMapper.updateById(alert);
    }
  }
}
```

## 6. 前端检测页面核心逻辑

来源文件：
- `vue-project/src/views/DetectionRoomView.vue`
- `vue-project/src/composables/useCameraDetection.ts`
- `vue-project/src/services/detectionJobService.ts`

说明：
这一部分体现前端检测页的核心设计：摄像头采集、视频录制、上传检测任务、轮询结果、人工确认回写。

```ts
// 文件：vue-project/src/composables/useCameraDetection.ts

const startCamera = async () => {
  if (isStreaming.value) return
  errorMessage.value = null

  const privacySetting = userPrivacySetting?.value
  if (privacySetting && privacySetting.cameraPermission === false) {
    permissionGranted.value = false
    errorMessage.value = '摄像头权限未开启，请在设置中心开启摄像头权限'
    return
  }

  const permissionStatus = await checkCameraPermission()
  if (permissionStatus.supported && permissionStatus.state === 'denied') {
    permissionGranted.value = false
    errorMessage.value = '摄像头权限已被拒绝，请在浏览器设置中重新授权'
    return
  }

  try {
    try {
      mediaStream.value = await navigator.mediaDevices.getUserMedia({
        video: { width: 1280, height: 720 },
        audio: false,
      })
    } catch (highResError) {
      const errorType = getCameraErrorType(highResError)
      if (errorType === CAMERA_ERROR_TYPE.OVERCONSTRAINED) {
        mediaStream.value = await navigator.mediaDevices.getUserMedia({
          video: true,
          audio: false,
        })
      } else {
        throw highResError
      }
    }

    permissionGranted.value = true
    if (videoElement.value) {
      videoElement.value.srcObject = mediaStream.value
      await videoElement.value.play()
    }
    isStreaming.value = true
  } catch (error) {
    console.error('摄像头启动失败:', error)
    const errorType = getCameraErrorType(error)
    permissionGranted.value = false
    isStreaming.value = false
    errorMessage.value = getErrorMessage(errorType)
  }
}

const startRecording = async (): Promise<boolean> => {
  if (!mediaStream.value || isRecording.value) {
    return false
  }

  try {
    recordingChunks.value = []
    videoBlob.value = null
    recordingDuration.value = 0
    recordingMimeType.value = ''

    const mimeTypes = [
      'video/mp4;codecs=avc1.42E01E',
      'video/mp4;codecs=avc1.4D001E',
      'video/mp4;codecs=avc1.640028',
      'video/mp4',
      'video/webm;codecs=vp8',
      'video/webm;codecs=vp9',
      'video/webm',
    ]

    let selectedMimeType = ''
    for (const mimeType of mimeTypes) {
      if (MediaRecorder.isTypeSupported(mimeType)) {
        selectedMimeType = mimeType
        break
      }
    }

    if (!selectedMimeType) {
      errorMessage.value = '当前浏览器不支持视频录制'
      return false
    }

    recordingMimeType.value = selectedMimeType
    const recorder = new MediaRecorder(mediaStream.value, {
      mimeType: selectedMimeType,
      videoBitsPerSecond: 2500000,
    })

    recorder.ondataavailable = (event) => {
      if (event.data && event.data.size > 0) {
        recordingChunks.value.push(event.data)
      }
    }

    recorder.onstop = () => {
      if (recordingChunks.value.length > 0) {
        videoBlob.value = new Blob(recordingChunks.value, {
          type: selectedMimeType,
        })
      }
      if (recordingDurationInterval.value) {
        clearInterval(recordingDurationInterval.value)
        recordingDurationInterval.value = null
      }
    }

    mediaRecorder.value = recorder
    recorder.start(1000)
    isRecording.value = true
    return true
  } catch (error) {
    console.error('开始录制失败:', error)
    errorMessage.value = '开始录制失败，请重试'
    return false
  }
}

const stopRecording = async (): Promise<Blob | null> => {
  if (!isRecording.value || !mediaRecorder.value) {
    return null
  }

  try {
    if (mediaRecorder.value.state === 'recording') {
      mediaRecorder.value.stop()
    }

    isRecording.value = false
    mediaRecorder.value = null

    return new Promise((resolve) => {
      const timeout = setTimeout(() => resolve(videoBlob.value), 3000)
      if (videoBlob.value) {
        clearTimeout(timeout)
        resolve(videoBlob.value)
        return
      }
      const checkInterval = setInterval(() => {
        if (videoBlob.value) {
          clearTimeout(timeout)
          clearInterval(checkInterval)
          resolve(videoBlob.value)
        }
      }, 100)
    })
  } catch (error) {
    console.error('停止录制失败:', error)
    errorMessage.value = '停止录制失败'
    isRecording.value = false
    return null
  }
}
```

```ts
// 文件：vue-project/src/services/detectionJobService.ts

export async function createDetectionJob(payload: CreateDetectionJobPayload): Promise<DetectionJob> {
  const formData = new FormData()
  formData.append('patientId', String(payload.patientId))
  formData.append('reminderInstanceId', String(payload.reminderInstanceId))
  formData.append('videoFile', payload.videoFile, `detection-${Date.now()}.webm`)
  if (payload.cameraId) {
    formData.append('cameraId', payload.cameraId)
  }
  if (payload.modelVersion) {
    formData.append('modelVersion', payload.modelVersion)
  }
  if (typeof payload.samplingRate === 'number') {
    formData.append('samplingRate', String(payload.samplingRate))
  }
  if (typeof payload.maxFrames === 'number') {
    formData.append('maxFrames', String(payload.maxFrames))
  }
  return http.post('/detection-jobs', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

export async function getDetectionJob(id: number): Promise<DetectionJob> {
  return http.get(`/detection-jobs/${id}`)
}
```

```ts
// 文件：vue-project/src/views/DetectionRoomView.vue

const handleStopRecordingAndProcess = async () => {
  if (!isRecording.value) return
  if (!selectedScheduleId.value || !selectedReminderInstance.value) {
    message.warning('请先选择用药计划')
    return
  }

  isVideoProcessing.value = true
  videoProcessingMessage.value = '正在停止录制...'

  try {
    const finalBlob = await stopRecording()
    if (!finalBlob || finalBlob.size === 0) {
      message.error('视频数据获取失败，请重试')
      return
    }

    const patientId = currentPatientId.value ?? (await resolvePatientId())
    videoProcessingMessage.value = '正在创建检测任务...'
    const job = await createDetectionJob({
      patientId,
      reminderInstanceId: selectedReminderInstance.value.id,
      videoFile: finalBlob,
      cameraId: 'web-cam',
      modelVersion: 'spring-flask-v1',
      samplingRate: 30,
      maxFrames: 300,
    })
    activeDetectionJob.value = job
    videoProcessingMessage.value = '正在等待检测结果...'

    let latestJob = job
    for (let attempt = 0; attempt < 30; attempt += 1) {
      await sleep(1500)
      latestJob = await getDetectionJob(job.id)
      activeDetectionJob.value = latestJob
      if (latestJob.status === 'succeeded' || latestJob.status === 'failed') {
        break
      }
      videoProcessingMessage.value = `检测进行中（第 ${attempt + 1} 次轮询）...`
    }

    if (latestJob.status !== 'succeeded') {
      throw new Error(latestJob.errorMessage ?? '检测任务未成功完成')
    }

    videoDetectionResult.value = {
      status: latestJob.resultStatus ?? 'abnormal',
      actionDetected: Boolean(latestJob.actionDetected),
      targets: parseTargets(latestJob.targetsJson),
      message: buildResultMessage(latestJob),
      confidence: latestJob.confidence,
      latencyMs: latestJob.latencyMs,
      traceId: latestJob.traceId,
    }

    await loadSchedules()
  } catch (error: unknown) {
    const errorMsg = error instanceof Error ? error.message : '视频处理失败，请重试'
    console.error('视频处理失败:', error)
    message.error(`处理失败: ${errorMsg}`)
  } finally {
    isVideoProcessing.value = false
    videoProcessingMessage.value = null
  }
}

const handleConfirm = async () => {
  if (!selectedScheduleId.value || !selectedReminderInstance.value) {
    message.warning('请先选择用药计划')
    return
  }
  if (detectionState.value !== 'confirmed' && detectionState.value !== 'suspected') {
    message.warning('请等待系统检测到药品和服药动作')
    return
  }

  isCreatingEvent.value = true
  try {
    await confirmReminderInstance(selectedReminderInstance.value.id, {
      confirmedBy: authStore.user?.name ?? authStore.user?.id ?? 'elder',
      confirmTime: new Date().toISOString(),
    })
    message.success('提醒实例已确认，服药记录已同步保存')
    videoDetectionResult.value = null
    activeDetectionJob.value = null
    videoBlob.value = null
    await loadSchedules()
  } catch (error: unknown) {
    const errorMsg = error instanceof Error ? error.message : '记录失败，请重试'
    console.error('确认服药失败:', error)
    message.error(`保存失败: ${errorMsg}`)
  } finally {
    isCreatingEvent.value = false
  }
}
```

## 建议使用方式

如果论文附录篇幅有限，建议优先保留以下顺序：

1. YOLOv8 检测推理核心代码
2. 动作识别核心代码
3. 三态结果协议判定代码
4. 服药事件处理接口
5. 前端检测页面核心逻辑
6. 用药计划管理接口

附录文件生成位置：
- `output/appendix-source-listing.md`
