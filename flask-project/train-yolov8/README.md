# YOLOv8 药品检测模型训练指南

## 目录结构

```
train-yolov8/
├── README.md                 # 本文件
├── requirements.txt          # 训练依赖
├── configs/
│   └── dataset.yaml         # 数据集配置文件
├── data/
│   ├── images/              # 图片目录
│   │   ├── train/          # 训练集图片
│   │   ├── val/            # 验证集图片
│   │   └── test/           # 测试集图片
│   └── labels/             # 标注文件目录
│       ├── train/          # 训练集标注
│       ├── val/           # 验证集标注
│       └── test/          # 测试集标注
├── scripts/
│   ├── prepare_dataset.py  # 数据集准备脚本
│   ├── train.py           # 训练脚本
│   └── export_model.py    # 模型导出脚本
└── outputs/               # 训练输出目录
    └── runs/              # 训练结果（自动生成）
```

## 快速开始

### 方式一：使用Ultralytics示例数据集（推荐新手）

#### 1. 安装依赖

```bash
cd train-yolov8
pip install -r requirements.txt
```

#### 2. 下载示例数据集

Ultralytics提供了医疗药丸数据集作为示例：

```bash
python scripts/download_dataset.py
```

这会自动下载并解压数据集到 `data/medical-pills/` 目录。

**数据集信息**：
- 训练集：92张图片
- 验证集：23张图片
- 类别：pill（药丸）
- 来源：[Ultralytics医疗药丸数据集](http://docs.ultralytics.com/zh/datasets/detect/medical-pills/)

#### 3. 开始训练

```bash
python scripts/train.py --data configs/medical-pills.yaml
```

#### 4. 导出模型

```bash
python scripts/export_model.py
```

### 方式二：使用自定义数据集

#### 1. 安装依赖

```bash
cd train-yolov8
pip install -r requirements.txt
```

#### 2. 准备数据集

将你的图片和标注文件放入 `data/` 目录，然后运行：

```bash
python scripts/prepare_dataset.py
```

#### 3. 配置数据集

编辑 `configs/dataset.yaml`，确保路径正确。

#### 4. 开始训练

```bash
python scripts/train.py
```

#### 5. 导出模型

训练完成后，导出为ONNX格式：

```bash
python scripts/export_model.py
```

## 详细说明

### 数据集选项

#### 选项1：Ultralytics医疗药丸数据集（示例）

**优点**：
- ✅ 开箱即用，无需自己标注
- ✅ 适合快速测试和验证
- ✅ 官方提供，质量有保障

**限制**：
- ⚠️ 只有一个类别（pill）
- ⚠️ 数据量较小（115张图片）
- ⚠️ 需要扩展才能支持多类别检测

**使用方法**：
```bash
# 下载数据集
python scripts/download_dataset.py

# 使用示例数据集训练
python scripts/train.py --data configs/medical-pills.yaml
```

**数据集详情**：
- 来源：[Ultralytics文档](http://docs.ultralytics.com/zh/datasets/detect/medical-pills/)
- 下载：[medical-pills.zip](https://github.com/ultralytics/assets/releases/download/v0.0.0/medical-pills.zip)
- 训练集：92张图片
- 验证集：23张图片
- 类别：pill

#### 选项2：自定义数据集（推荐生产环境）

**优点**：
- ✅ 完全符合项目需求
- ✅ 支持多类别（PILL/BLISTER/BOTTLE/BOX）
- ✅ 可以针对特定场景优化

**要求**：
- **格式**：YOLO格式（每张图片对应一个.txt标注文件）
- **类别**：PILL, BLISTER, BOTTLE, BOX
- **最小数据量**：每个类别至少50-100张图片
- **推荐数据量**：每个类别500-1000张图片

### 标注格式

每个 `.txt` 文件格式：
```
class_id center_x center_y width height
```

例如：
```
0 0.5 0.5 0.2 0.3  # PILL类别
1 0.3 0.4 0.15 0.2  # BLISTER类别
```

### 类别映射

- 0: PILL（药片/胶囊）
- 1: BLISTER（泡罩板）
- 2: BOTTLE（药瓶）
- 3: BOX（药盒）

## 训练参数说明

训练脚本支持以下参数：

- `--data`: 数据集配置文件路径（默认：configs/dataset.yaml）
- `--model`: 模型大小 n/s/m/l/x（默认：n）
- `--epochs`: 训练轮数（默认：100）
- `--batch`: 批次大小（默认：16）
- `--imgsz`: 图像尺寸（默认：640）
- `--device`: 设备类型 cpu/cuda/mps（默认：auto）

## 输出说明

训练完成后，模型会保存在：
- `outputs/runs/detect/train/weights/best.pt` - 最佳模型
- `outputs/runs/detect/train/weights/last.pt` - 最后一轮模型

导出ONNX后：
- `outputs/runs/detect/train/weights/best.onnx` - ONNX模型

将ONNX模型复制到项目根目录：
```bash
cp outputs/runs/detect/train/weights/best.onnx ../models/medication-intake.onnx
```

## 常见问题

### Q: 训练需要多长时间？
A: 取决于数据量和硬件。1000张图片在GPU上约需1-2小时。

### Q: 没有GPU可以训练吗？
A: 可以，但速度较慢。建议使用Google Colab的免费GPU。

### Q: 如何提高检测精度？
A: 
1. 增加数据量
2. 提高数据质量
3. 使用更大的模型（s/m/l）
4. 增加训练轮数
5. 调整数据增强参数

## 下一步

1. 收集和标注数据
2. 准备数据集
3. 开始训练
4. 评估模型
5. 导出并部署

