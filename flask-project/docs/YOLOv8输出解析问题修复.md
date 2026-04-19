# YOLOv8输出解析问题修复

## 问题描述

错误信息：
```
1 validation error for VideoDetectionTarget
score
  Input should be less than or equal to 1 [type=less_than_equal, input_value=24.568, input_type=float]
```

**问题原因**：
- YOLOv8模型输出的置信度分数是logits（未归一化的值），范围可能是任意实数
- 代码直接将logits值作为置信度使用，导致值超出[0, 1]范围
- Pydantic验证失败，因为`score`字段要求值在[0, 1]范围内

## 根本原因

YOLOv8的输出格式：
- 形状：`[batch, num_detections, 4+num_classes]`
- 前4个值：归一化的边界框坐标 `[x_center, y_center, width, height]`
- 后面值：每个类别的logits（未归一化的分数）

**原代码问题**：
```python
# 错误：直接将logits当作置信度
confidence = detection[4]  # 可能是24.568这样的值
score = float(confidence)  # 超出[0, 1]范围
```

## 解决方案

### 1. 正确解析YOLOv8输出格式

```python
# 提取边界框坐标（归一化的）
x_center = float(detection[0])
y_center = float(detection[1])
width = float(detection[2])
height = float(detection[3])

# 提取类别分数（logits）
class_scores = detection[4:]
```

### 2. 应用sigmoid将logits转换为置信度

YOLOv8使用独立的sigmoid函数，每个类别的置信度是独立的：

```python
# 应用sigmoid将logits转换为置信度
class_scores_array = np.array(class_scores)
# 使用clip避免溢出
class_scores_clipped = np.clip(class_scores_array, -500, 500)
probabilities = 1.0 / (1.0 + np.exp(-class_scores_clipped))

# 找到最高置信度的类别
class_id = int(np.argmax(probabilities))
confidence = float(probabilities[class_id])

# 确保置信度在[0, 1]范围内
confidence = max(0.0, min(1.0, confidence))
```

### 3. 正确转换边界框坐标

```python
# 将归一化的边界框坐标转换为像素坐标
x_min_px = (x_center - width / 2) * input_width
y_min_px = (y_center - height / 2) * input_height
x_max_px = (x_center + width / 2) * input_width
y_max_px = (y_center + height / 2) * input_height

# 确保边界框在图像范围内
x_min_px = max(0, min(x_min_px, input_width))
y_min_px = max(0, min(y_min_px, input_height))
x_max_px = max(0, min(x_max_px, input_width))
y_max_px = max(0, min(y_max_px, input_height))

# 归一化边界框坐标到[0, 1]范围
bbox = (
    float(x_min_px / input_width),
    float(y_min_px / input_height),
    float(x_max_px / input_width),
    float(y_max_px / input_height),
)
```

## 修复内容

已修复 `app/services/video_detector.py` 中的 `_detect_medication_yolo` 方法：

1. ✅ 正确解析YOLOv8输出格式
2. ✅ 应用sigmoid将logits转换为[0, 1]范围的置信度
3. ✅ 正确转换边界框坐标
4. ✅ 添加调试日志以便排查问题
5. ✅ 添加边界检查和错误处理

## 验证

修复后，检测结果应该：
- ✅ `score`值在[0, 1]范围内
- ✅ 通过Pydantic验证
- ✅ 正确显示检测结果

## 测试

重启服务后测试：

```bash
# 重启服务
cd flask-project
python -m app.main

# 测试视频检测
curl -X POST http://localhost:5000/api/v1/video-detection/detect \
  -F "video=@test-video.mp4" \
  -F "patientId=1" \
  -F "scheduleId=1" \
  -F "timestamp=2024-01-01T10:00:00Z"
```

## 相关文档

- [YOLOv8官方文档](https://docs.ultralytics.com/)
- [ONNX Runtime文档](https://onnxruntime.ai/docs/)

## 注意事项

1. **模型类别顺序**：确保`label_map`的顺序与训练时的类别顺序一致
2. **置信度阈值**：可以通过`ALGO_CONFIDENCE_THRESHOLD`环境变量调整
3. **输出格式**：如果使用不同版本的YOLOv8，输出格式可能略有不同，需要相应调整



