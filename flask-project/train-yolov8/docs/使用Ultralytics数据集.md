# 使用Ultralytics医疗药丸数据集

## 一、数据集介绍

Ultralytics提供了一个医疗药丸检测数据集作为示例，适合快速开始训练和测试。

**数据集信息**：
- **来源**：[Ultralytics文档](http://docs.ultralytics.com/zh/datasets/detect/medical-pills/)
- **下载链接**：https://github.com/ultralytics/assets/releases/download/v0.0.0/medical-pills.zip
- **训练集**：92张图片
- **验证集**：23张图片
- **类别**：pill（药丸）
- **许可证**：AGPL-3.0

## 二、快速开始

### 步骤1：下载数据集

```bash
cd train-yolov8
python scripts/download_dataset.py
```

这会自动：
1. 下载数据集zip文件
2. 解压到 `data/medical-pills/` 目录
3. 显示数据集统计信息

### 步骤2：检查数据集

```bash
python scripts/prepare_dataset.py
```

### 步骤3：开始训练

```bash
# 使用示例数据集训练
python scripts/train.py --data configs/medical-pills.yaml

# 或使用自定义参数
python scripts/train.py \
    --data configs/medical-pills.yaml \
    --model n \
    --epochs 100 \
    --batch 16 \
    --device auto
```

### 步骤4：导出模型

```bash
python scripts/export_model.py
```

## 三、数据集结构

下载后的目录结构：

```
data/medical-pills/
├── images/
│   ├── train/     # 92张训练图片
│   └── val/       # 23张验证图片
└── labels/
    ├── train/     # 92个训练标注文件
    └── val/       # 23个验证标注文件
```

## 四、注意事项

### 1. 类别限制

⚠️ **重要**：Ultralytics数据集只有一个类别（pill），而你的项目需要4个类别：
- PILL（药片/胶囊）
- BLISTER（泡罩板）
- BOTTLE（药瓶）
- BOX（药盒）

### 2. 使用建议

**适合场景**：
- ✅ 快速验证训练流程
- ✅ 测试代码是否正常工作
- ✅ 学习YOLOv8训练方法
- ✅ 作为起点，在此基础上扩展

**不适合场景**：
- ❌ 直接用于生产环境（类别不匹配）
- ❌ 需要多类别检测的场景

### 3. 扩展数据集

如果你想在Ultralytics数据集基础上扩展：

1. **保留现有数据**：保留 `data/medical-pills/` 作为参考
2. **添加新数据**：在 `data/images/` 和 `data/labels/` 中添加你的数据
3. **更新配置**：修改 `configs/dataset.yaml` 支持多类别
4. **合并训练**：将两个数据集合并或分别训练

## 五、训练示例

### 基础训练

```bash
python scripts/train.py --data configs/medical-pills.yaml
```

### 使用GPU训练

```bash
python scripts/train.py \
    --data configs/medical-pills.yaml \
    --device cuda \
    --epochs 200 \
    --batch 32
```

### 使用更大的模型

```bash
python scripts/train.py \
    --data configs/medical-pills.yaml \
    --model s \
    --epochs 150
```

## 六、训练结果

训练完成后，模型会保存在：
- `outputs/runs/detect/medication_detection/weights/best.pt` - 最佳模型
- `outputs/runs/detect/medication_detection/weights/last.pt` - 最后一轮模型

## 七、导出和使用

### 导出ONNX

```bash
python scripts/export_model.py \
    --model outputs/runs/detect/medication_detection/weights/best.pt
```

### 部署到项目

```bash
# 复制到项目models目录
cp outputs/runs/detect/medication_detection/weights/best.onnx \
   ../../models/medication-intake.onnx
```

### 注意事项

⚠️ **重要**：由于Ultralytics数据集只有一个类别（pill），导出的模型只能检测pill类别。如果你的项目需要检测多个类别，需要：

1. 使用自定义数据集训练
2. 或在Ultralytics数据集基础上添加更多类别数据

## 八、参考资源

- **官方文档**：http://docs.ultralytics.com/zh/datasets/detect/medical-pills/
- **数据集YAML**：https://github.com/ultralytics/ultralytics/blob/main/ultralytics/cfg/datasets/medical-pills.yaml
- **下载链接**：https://github.com/ultralytics/assets/releases/download/v0.0.0/medical-pills.zip

## 九、引用

如果使用此数据集，请引用：

```bibtex
@dataset{Jocher_Ultralytics_Datasets_2024,
    author = {Jocher, Glenn and Rizwan, Muhammad},
    license = {AGPL-3.0},
    month = {Dec},
    title = {Ultralytics Datasets: Medical-pills Detection Dataset},
    url = {https://docs.ultralytics.com/datasets/detect/medical-pills/},
    version = {1.0.0},
    year = {2024}
}
```



