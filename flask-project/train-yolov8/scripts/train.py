#!/usr/bin/env python3
"""YOLOv8 药品检测模型训练脚本。"""

import argparse
import sys
from pathlib import Path

from ultralytics import YOLO


def train_medication_model(
    dataset_yaml: str = "configs/dataset.yaml",
    model_size: str = "n",
    epochs: int = 100,
    batch_size: int = 16,
    image_size: int = 640,
    device: str = "auto",
    project: str = "outputs/runs/detect",
    name: str = "medication_detection",
):
    """
    训练药品检测模型。

    Args:
        dataset_yaml: 数据集配置文件路径
        model_size: 模型大小 (n/s/m/l/x)
        epochs: 训练轮数
        batch_size: 批次大小
        image_size: 输入图像尺寸
        device: 设备类型
        project: 项目目录
        name: 实验名称
    """
    # 获取脚本所在目录的父目录（train-yolov8目录）
    script_dir = Path(__file__).parent.parent
    dataset_path = script_dir / dataset_yaml

    # 检查数据集配置文件
    if not dataset_path.exists():
        print(f"❌ 错误: {dataset_path} 不存在")
        print("\n请确保数据集配置文件存在。")
        print("\n💡 快速开始选项：")
        print("   1. 使用Ultralytics示例数据集:")
        print("      python scripts/download_dataset.py")
        print("      python scripts/train.py --data configs/medical-pills.yaml")
        print("\n   2. 使用自定义数据集:")
        print("      创建 configs/dataset.yaml，格式如下：")
        print("""
path: ../data
train: images/train
val: images/val
test: images/test

nc: 4
names:
  0: PILL
  1: BLISTER
  2: BOTTLE
  3: BOX
        """)
        sys.exit(1)

    # 自动检测设备
    if device == "auto":
        try:
            import torch

            if torch.cuda.is_available():
                device = "cuda"
                print("✅ 检测到CUDA GPU")
            elif hasattr(torch.backends, "mps") and torch.backends.mps.is_available():
                device = "mps"  # Apple Silicon
                print("✅ 检测到Apple Silicon GPU")
            else:
                device = "cpu"
                print("⚠️  使用CPU训练（速度较慢，建议使用GPU）")
        except ImportError:
            device = "cpu"
            print("⚠️  PyTorch未安装，使用CPU训练")

    print(f"\n🔧 训练配置:")
    print(f"   - 数据集: {dataset_path}")
    print(f"   - 模型: yolov8{model_size}")
    print(f"   - 设备: {device}")
    print(f"   - 轮数: {epochs}")
    print(f"   - 批次大小: {batch_size}")
    print(f"   - 图像尺寸: {image_size}")
    print(f"   - 输出目录: {project}/{name}\n")

    try:
        # 加载预训练模型
        print(f"📦 加载预训练模型 yolov8{model_size}.pt...")
        model = YOLO(f"yolov8{model_size}.pt")
    except Exception as e:
        print(f"❌ 无法加载模型: {e}")
        print("💡 提示: 首次运行时会自动下载预训练模型")
        sys.exit(1)

    try:
        # 开始训练
        print("🚀 开始训练...\n")
        results = model.train(
            data=str(dataset_path),
            epochs=epochs,
            imgsz=image_size,
            batch=batch_size,
            device=device,
            project=str(script_dir / project),
            name=name,
            save=True,
            plots=True,
            verbose=True,
            # 数据增强
            hsv_h=0.015,
            hsv_s=0.7,
            hsv_v=0.4,
            degrees=10,
            translate=0.1,
            scale=0.5,
            fliplr=0.5,
            mosaic=1.0,
            mixup=0.1,
            # 优化器设置
            optimizer="AdamW",
            lr0=0.01,
            lrf=0.01,
            # 早停
            patience=50,
        )

        print("\n✅ 训练完成！")
        print(f"📁 模型保存在: {results.save_dir}")
        print(f"📊 最佳模型: {results.save_dir}/weights/best.pt")
        print(f"📊 最后一轮: {results.save_dir}/weights/last.pt")

        # 显示训练结果摘要
        if hasattr(results, "results_dict"):
            print("\n📈 训练结果:")
            for key, value in results.results_dict.items():
                if isinstance(value, (int, float)):
                    print(f"   - {key}: {value:.4f}")

        print("\n💡 下一步:")
        print(f"   1. 查看训练曲线: {results.save_dir}")
        print(f"   2. 导出ONNX模型: python scripts/export_model.py")
        print(f"   3. 复制模型到项目: cp {results.save_dir}/weights/best.onnx ../../models/medication-intake.onnx")

    except KeyboardInterrupt:
        print("\n⚠️  训练被用户中断")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ 训练失败: {e}")
        import traceback

        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="训练YOLOv8药品检测模型")
    parser.add_argument(
        "--data",
        type=str,
        default="configs/dataset.yaml",
        help="数据集配置文件路径",
    )
    parser.add_argument(
        "--model",
        type=str,
        default="n",
        choices=["n", "s", "m", "l", "x"],
        help="模型大小 (n/s/m/l/x)",
    )
    parser.add_argument("--epochs", type=int, default=100, help="训练轮数")
    parser.add_argument("--batch", type=int, default=16, help="批次大小")
    parser.add_argument("--imgsz", type=int, default=640, help="图像尺寸")
    parser.add_argument(
        "--device",
        type=str,
        default="auto",
        choices=["auto", "cpu", "cuda", "mps"],
        help="设备类型",
    )
    parser.add_argument(
        "--project",
        type=str,
        default="outputs/runs/detect",
        help="项目目录",
    )
    parser.add_argument(
        "--name",
        type=str,
        default="medication_detection",
        help="实验名称",
    )

    args = parser.parse_args()

    train_medication_model(
        dataset_yaml=args.data,
        model_size=args.model,
        epochs=args.epochs,
        batch_size=args.batch,
        image_size=args.imgsz,
        device=args.device,
        project=args.project,
        name=args.name,
    )

