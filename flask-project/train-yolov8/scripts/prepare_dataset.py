#!/usr/bin/env python3
"""数据集准备脚本：检查数据集结构并生成统计信息。"""

import sys
from pathlib import Path
from collections import Counter


def check_dataset_structure(data_dir: Path):
    """
    检查数据集结构。

    Args:
        data_dir: 数据集根目录
    """
    print(f"📁 检查数据集结构: {data_dir}\n")

    # 检查目录结构
    required_dirs = [
        "images/train",
        "images/val",
        "images/test",
        "labels/train",
        "labels/val",
        "labels/test",
    ]

    missing_dirs = []
    for dir_path in required_dirs:
        full_path = data_dir / dir_path
        if not full_path.exists():
            missing_dirs.append(dir_path)
        else:
            print(f"✅ {dir_path}")

    if missing_dirs:
        print(f"\n⚠️  缺少以下目录:")
        for dir_path in missing_dirs:
            print(f"   - {dir_path}")
        print("\n请创建缺失的目录:")
        for dir_path in missing_dirs:
            (data_dir / dir_path).mkdir(parents=True, exist_ok=True)
            print(f"   mkdir -p data/{dir_path}")

    return len(missing_dirs) == 0


def count_files(data_dir: Path):
    """
    统计数据集文件数量。

    Args:
        data_dir: 数据集根目录
    """
    print("\n📊 数据集统计:\n")

    splits = ["train", "val", "test"]
    total_images = 0
    total_labels = 0

    for split in splits:
        images_dir = data_dir / "images" / split
        labels_dir = data_dir / "labels" / split

        image_count = len(list(images_dir.glob("*.jpg"))) + len(
            list(images_dir.glob("*.png"))
        )
        label_count = len(list(labels_dir.glob("*.txt")))

        total_images += image_count
        total_labels += label_count

        print(f"{split.upper():6s}:")
        print(f"  图片: {image_count:4d} 张")
        print(f"  标注: {label_count:4d} 个")

        if image_count != label_count:
            print(f"  ⚠️  警告: 图片和标注数量不匹配！")

    print(f"\n总计:")
    print(f"  图片: {total_images} 张")
    print(f"  标注: {total_labels} 个")

    return total_images, total_labels


def analyze_labels(data_dir: Path):
    """
    分析标注文件，统计类别分布。

    Args:
        data_dir: 数据集根目录
    """
    print("\n📈 类别分布分析:\n")

    class_names = {0: "PILL", 1: "BLISTER", 2: "BOTTLE", 3: "BOX"}
    class_counts = Counter()

    splits = ["train", "val", "test"]

    for split in splits:
        labels_dir = data_dir / "labels" / split
        split_counts = Counter()

        for label_file in labels_dir.glob("*.txt"):
            try:
                with open(label_file, "r") as f:
                    for line in f:
                        parts = line.strip().split()
                        if len(parts) >= 5:
                            class_id = int(parts[0])
                            split_counts[class_id] += 1
                            class_counts[class_id] += 1
            except Exception as e:
                print(f"⚠️  读取标注文件失败 {label_file}: {e}")

        if split_counts:
            print(f"{split.upper()}:")
            for class_id in sorted(split_counts.keys()):
                class_name = class_names.get(class_id, f"Unknown({class_id})")
                count = split_counts[class_id]
                print(f"  {class_name:10s}: {count:4d} 个")

    print(f"\n总计:")
    for class_id in sorted(class_counts.keys()):
        class_name = class_names.get(class_id, f"Unknown({class_id})")
        count = class_counts[class_id]
        print(f"  {class_name:10s}: {count:4d} 个")

    # 检查类别平衡
    if class_counts:
        max_count = max(class_counts.values())
        min_count = min(class_counts.values())
        if max_count > 0:
            imbalance_ratio = max_count / min_count
            if imbalance_ratio > 2:
                print(f"\n⚠️  警告: 类别不平衡，最大/最小比例 = {imbalance_ratio:.2f}")
                print("   建议: 增加少数类别的样本或使用类别权重")


def main():
    """主函数。"""
    script_dir = Path(__file__).parent.parent
    
    # 检查多个可能的数据目录位置
    possible_data_dirs = [
        script_dir / "data",  # train-yolov8/data
        script_dir.parent / "data",  # flask-project/data (Ultralytics数据集位置)
        script_dir.parent / "data" / "medical-pills",  # flask-project/data/medical-pills
    ]
    
    data_dir = None
    # 优先选择有实际数据的目录
    for dir_path in possible_data_dirs:
        if dir_path.exists() and (dir_path / "images").exists():
            # 检查是否有实际的图片文件
            train_images_dir = dir_path / "images" / "train"
            if train_images_dir.exists():
                # 检查是否有图片文件
                image_files = list(train_images_dir.glob("*.jpg")) + list(train_images_dir.glob("*.png"))
                if len(image_files) > 0:
                    data_dir = dir_path
                    break
    
    # 如果没找到有数据的目录，再检查空目录
    if data_dir is None:
        for dir_path in possible_data_dirs:
            if dir_path.exists() and (dir_path / "images").exists():
                data_dir = dir_path
                break
    
    if data_dir is None:
        # 如果都找不到，使用默认位置
        data_dir = script_dir / "data"
        if not data_dir.exists():
            print(f"❌ 错误: 数据目录不存在")
            print("\n已检查以下位置:")
            for dir_path in possible_data_dirs:
                print(f"  - {dir_path} ({'存在' if dir_path.exists() else '不存在'})")
            print("\n请创建数据目录结构或下载数据集:")
            print("  mkdir -p data/{images,labels}/{train,val,test}")
            print("  或运行: python scripts/download_dataset.py")
            sys.exit(1)

    # 检查目录结构
    if not check_dataset_structure(data_dir):
        print("\n💡 已自动创建缺失的目录，请添加数据后重新运行此脚本")
        return

    # 统计文件
    total_images, total_labels = count_files(data_dir)

    if total_images == 0:
        print("\n⚠️  数据集为空，请添加图片和标注文件")
        return

    # 分析标注
    analyze_labels(data_dir)

    print("\n✅ 数据集检查完成！")
    print("\n💡 下一步:")
    print("   1. 确保数据集结构正确")
    print("   2. 检查 configs/dataset.yaml 配置")
    print("   3. 运行训练: python scripts/train.py")


if __name__ == "__main__":
    main()

