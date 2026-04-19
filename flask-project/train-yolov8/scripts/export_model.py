#!/usr/bin/env python3
"""导出训练好的YOLOv8模型为ONNX格式。"""

import argparse
import shutil
import sys
from pathlib import Path

from ultralytics import YOLO


def export_model(
    model_path: str = "outputs/runs/detect/medication_detection/weights/best.pt",
    output_path: str = "../../models/medication-intake.onnx",
    image_size: int = 640,
):
    """
    导出训练好的模型为ONNX格式。

    Args:
        model_path: 训练好的模型路径（.pt文件）
        output_path: 输出ONNX文件路径
        image_size: 输入图像尺寸
    """
    script_dir = Path(__file__).parent.parent
    model_file = script_dir / model_path
    output_file = Path(output_path).resolve()

    # 检查模型文件是否存在
    if not model_file.exists():
        print(f"❌ 错误: 模型文件不存在: {model_file}")
        print("\n请先训练模型，或指定正确的模型路径:")
        print("  python scripts/export_model.py --model <path_to_model.pt>")
        sys.exit(1)

    print(f"📦 加载模型: {model_file}")

    try:
        # 加载模型
        model = YOLO(str(model_file))

        # 导出为ONNX
        print(f"📤 导出为ONNX格式...")
        print(f"   - 输入尺寸: {image_size}x{image_size}")
        print(f"   - 输出路径: {output_file}")

        exported_path = model.export(
            format="onnx",
            imgsz=image_size,
            simplify=True,
            opset=12,
            dynamic=False,
        )

        # 复制到目标位置
        exported_file = Path(exported_path)
        if exported_file.exists():
            # 确保输出目录存在
            output_file.parent.mkdir(parents=True, exist_ok=True)

            # 复制文件
            shutil.copy(exported_file, output_file)
            print(f"\n✅ ONNX模型已导出并复制到: {output_file}")

            # 显示文件信息
            file_size = output_file.stat().st_size / (1024 * 1024)  # MB
            print(f"📊 文件大小: {file_size:.2f} MB")

            print("\n💡 下一步:")
            print("   1. 重启Flask服务")
            print("   2. 测试检测功能")
            print(f"   3. 模型文件位置: {output_file}")
        else:
            print(f"⚠️  导出的文件未找到: {exported_path}")

    except Exception as e:
        print(f"❌ 导出失败: {e}")
        import traceback

        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="导出YOLOv8模型为ONNX格式")
    parser.add_argument(
        "--model",
        type=str,
        default="outputs/runs/detect/medication_detection/weights/best.pt",
        help="训练好的模型路径",
    )
    parser.add_argument(
        "--output",
        type=str,
        default="../../models/medication-intake.onnx",
        help="输出ONNX文件路径",
    )
    parser.add_argument("--imgsz", type=int, default=640, help="图像尺寸")

    args = parser.parse_args()

    export_model(
        model_path=args.model,
        output_path=args.output,
        image_size=args.imgsz,
    )



