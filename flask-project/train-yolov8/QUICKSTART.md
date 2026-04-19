# 快速开始指南

## 一、环境准备

### 1. 安装依赖

```bash
cd train-yolov8
pip install -r requirements.txt
```

### 2. 验证安装

```bash
python -c "from ultralytics import YOLO; print('✅ Ultralytics安装成功')"
```

## 二、准备数据集

### 方式A：使用Ultralytics示例数据集（快速开始）

如果你想快速测试训练流程，可以使用Ultralytics提供的医疗药丸数据集：

```bash
# 下载示例数据集
python scripts/download_dataset.py

# 使用示例数据集训练
python scripts/train.py --data configs/medical-pills.yaml
```

**注意**：示例数据集只有一个类别（pill），适合测试。如需多类别检测，请使用自定义数据集。

详细说明请查看：`docs/使用Ultralytics数据集.md`

### 方式B：使用自定义数据集

### 1. 数据集结构

将你的图片和标注文件按以下结构组织：

```
data/
├── images/
│   ├── train/     # 训练集图片（.jpg或.png）
│   ├── val/       # 验证集图片
│   └── test/      # 测试集图片（可选）
└── labels/
    ├── train/     # 训练集标注（.txt，YOLO格式）
    ├── val/       # 验证集标注
    └── test/      # 测试集标注（可选）
```

### 2. 标注格式

每个图片对应一个 `.txt` 文件，格式：
```
class_id center_x center_y width height
```

**类别映射**：
- 0: PILL（药片/胶囊）
- 1: BLISTER（泡罩板）
- 2: BOTTLE（药瓶）
- 3: BOX（药盒）

**示例标注**（`image001.txt`）：
```
0 0.5 0.5 0.2 0.3    # 图片中心有一个PILL
1 0.3 0.4 0.15 0.2  # 左上角有一个BLISTER
```

### 3. 检查数据集

```bash
python scripts/prepare_dataset.py
```

这会检查：
- ✅ 目录结构是否正确
- ✅ 图片和标注数量是否匹配
- ✅ 类别分布是否平衡

## 三、开始训练

### 1. 基础训练

```bash
python scripts/train.py
```

### 2. 自定义参数训练

```bash
# 使用更大的模型
python scripts/train.py --model s --epochs 200 --batch 32

# 使用GPU（如果有）
python scripts/train.py --device cuda

# 使用Apple Silicon GPU
python scripts/train.py --device mps
```

### 3. 训练参数说明

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `--data` | 数据集配置文件 | `configs/dataset.yaml` |
| `--model` | 模型大小 (n/s/m/l/x) | `n` |
| `--epochs` | 训练轮数 | `100` |
| `--batch` | 批次大小 | `16` |
| `--imgsz` | 图像尺寸 | `640` |
| `--device` | 设备 (cpu/cuda/mps/auto) | `auto` |

## 四、导出模型

训练完成后，导出为ONNX格式：

```bash
python scripts/export_model.py
```

这会：
1. 加载最佳模型（`best.pt`）
2. 导出为ONNX格式
3. 复制到 `../../models/medication-intake.onnx`

## 五、部署模型

导出后，重启Flask服务即可使用新模型：

```bash
cd ../..
python -m app.main
```

## 六、训练建议

### 最小数据集
- 每个类别：50-100张图片
- 总计：200-400张图片
- 适合快速验证

### 推荐数据集
- 每个类别：500-1000张图片
- 总计：2000-4000张图片
- 适合生产使用

### 数据质量要求
- ✅ 不同角度、光照、背景
- ✅ 类别平衡（每个类别数量相近）
- ✅ 标注准确（边界框准确，类别正确）

## 七、常见问题

### Q: 训练很慢怎么办？
A: 
- 使用GPU：`--device cuda` 或 `--device mps`
- 使用Google Colab免费GPU
- 减少批次大小：`--batch 8`
- 使用更小的模型：`--model n`

### Q: 内存不足怎么办？
A:
- 减少批次大小：`--batch 8` 或 `--batch 4`
- 使用更小的模型：`--model n`
- 减少图像尺寸：`--imgsz 416`

### Q: 如何提高检测精度？
A:
1. 增加数据量
2. 提高数据质量
3. 使用更大的模型（s/m/l）
4. 增加训练轮数
5. 调整数据增强参数

### Q: 训练中断了怎么办？
A:
- YOLOv8支持断点续训
- 使用 `--resume` 参数继续训练
- 或从 `last.pt` 继续训练

## 八、训练流程示例

```bash
# 1. 准备数据
# （将图片和标注文件放入data目录）

# 2. 检查数据集
python scripts/prepare_dataset.py

# 3. 开始训练
python scripts/train.py --model n --epochs 100

# 4. 等待训练完成（查看outputs/runs/detect/）

# 5. 导出模型
python scripts/export_model.py

# 6. 部署
cd ../..
python -m app.main
```

## 九、下一步

1. ✅ 收集和标注数据
2. ✅ 准备数据集
3. ✅ 开始训练
4. ✅ 评估模型性能
5. ✅ 导出并部署

详细说明请查看 `README.md`。

