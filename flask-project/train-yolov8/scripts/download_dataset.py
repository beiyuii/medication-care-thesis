#!/usr/bin/env python3
"""下载Ultralytics医疗药丸数据集。"""

import argparse
import sys
import zipfile
from pathlib import Path

import httpx


def download_medical_pills_dataset(
    output_dir: str = "../data",
    dataset_url: str = "https://github.com/ultralytics/assets/releases/download/v0.0.0/medical-pills.zip",
):
    """
    下载Ultralytics医疗药丸数据集。

    Args:
        output_dir: 输出目录
        dataset_url: 数据集下载URL
    """
    script_dir = Path(__file__).parent.parent
    output_path = script_dir / output_dir
    zip_path = output_path / "medical-pills.zip"

    # 创建输出目录
    output_path.mkdir(parents=True, exist_ok=True)

    # 检查是否已存在数据集
    dataset_dir = output_path / "medical-pills"
    if dataset_dir.exists():
        print(f"✅ 数据集已存在: {dataset_dir}")
        print("💡 如需重新下载，请先删除该目录")
        return str(dataset_dir)

    print(f"📥 开始下载医疗药丸数据集...")
    print(f"   URL: {dataset_url}")
    print(f"   保存到: {zip_path}")

    try:
        # 下载文件
        with httpx.stream("GET", dataset_url, follow_redirects=True) as response:
            response.raise_for_status()
            total_size = int(response.headers.get("content-length", 0))

            with open(zip_path, "wb") as f:
                downloaded = 0
                for chunk in response.iter_bytes(chunk_size=8192):
                    f.write(chunk)
                    downloaded += len(chunk)
                    if total_size > 0:
                        percent = (downloaded / total_size) * 100
                        print(f"\r   进度: {percent:.1f}% ({downloaded}/{total_size} bytes)", end="")

        print(f"\n✅ 下载完成: {zip_path}")

        # 解压文件
        print(f"📦 解压数据集...")
        with zipfile.ZipFile(zip_path, "r") as zip_ref:
            # 检查zip文件内的结构
            file_list = zip_ref.namelist()
            # 如果zip内直接包含images和labels，需要创建medical-pills目录
            if any(name.startswith("images/") or name.startswith("labels/") for name in file_list):
                # zip文件内容直接是images和labels，需要解压到medical-pills子目录
                medical_pills_dir = output_path / "medical-pills"
                medical_pills_dir.mkdir(exist_ok=True)
                zip_ref.extractall(medical_pills_dir)
                dataset_dir = medical_pills_dir
            else:
                # zip文件内已经有medical-pills目录
                zip_ref.extractall(output_path)

        # 删除zip文件
        zip_path.unlink()
        print(f"✅ 解压完成")
        print(f"📁 数据集位置: {dataset_dir}")

        # 检查数据集实际位置（可能解压到了不同位置）
        possible_locations = [
            dataset_dir,
            output_path / "medical-pills",
            output_path,  # 可能直接解压到data目录
        ]

        actual_dataset_dir = None
        # 优先选择有实际数据的目录
        for loc in possible_locations:
            train_dir = loc / "images" / "train"
            if train_dir.exists():
                # 检查是否有实际的图片文件
                image_files = list(train_dir.glob("*.jpg")) + list(train_dir.glob("*.png"))
                if len(image_files) > 0:
                    actual_dataset_dir = loc
                    break

        if actual_dataset_dir is None:
            # 尝试在output_path下查找所有子目录
            for item in output_path.iterdir():
                if item.is_dir():
                    train_dir = item / "images" / "train"
                    if train_dir.exists():
                        image_files = list(train_dir.glob("*.jpg")) + list(train_dir.glob("*.png"))
                        if len(image_files) > 0:
                            actual_dataset_dir = item
                            break

        if actual_dataset_dir is None:
            print("⚠️  警告: 无法确定数据集位置，请手动检查")
            actual_dataset_dir = dataset_dir
        else:
            dataset_dir = actual_dataset_dir

        # 显示数据集信息
        print(f"\n📊 数据集信息:")
        train_dir = dataset_dir / "images" / "train"
        val_dir = dataset_dir / "images" / "val"
        
        train_images = len(list(train_dir.glob("*.jpg"))) + len(list(train_dir.glob("*.png")))
        val_images = len(list(val_dir.glob("*.jpg"))) + len(list(val_dir.glob("*.png")))
        
        print(f"   - 训练集: {train_images} 张图片")
        print(f"   - 验证集: {val_images} 张图片")
        print(f"   - 类别: pill (药丸)")
        print(f"   - 数据集路径: {dataset_dir}")

        # 检查yaml文件位置
        yaml_file = dataset_dir / "medical-pills.yaml"
        if not yaml_file.exists():
            # 可能在上级目录
            yaml_file = dataset_dir.parent / "medical-pills.yaml"
        
        if yaml_file.exists():
            print(f"   - YAML配置: {yaml_file}")

        print(f"\n💡 下一步:")
        print(f"   1. 检查数据集: python scripts/prepare_dataset.py")
        if yaml_file.exists():
            # 计算相对路径
            rel_yaml = yaml_file.relative_to(script_dir)
            print(f"   2. 开始训练: python scripts/train.py --data {rel_yaml}")
        else:
            print(f"   2. 开始训练: python scripts/train.py --data configs/medical-pills.yaml")
            print(f"      (需要先更新configs/medical-pills.yaml中的path路径)")

        return str(dataset_dir)

    except httpx.HTTPError as e:
        print(f"\n❌ 下载失败: {e}")
        if zip_path.exists():
            zip_path.unlink()
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ 处理失败: {e}")
        import traceback

        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="下载Ultralytics医疗药丸数据集")
    parser.add_argument(
        "--output",
        type=str,
        default="../data",
        help="输出目录",
    )
    parser.add_argument(
        "--url",
        type=str,
        default="https://github.com/ultralytics/assets/releases/download/v0.0.0/medical-pills.zip",
        help="数据集下载URL",
    )

    args = parser.parse_args()

    download_medical_pills_dataset(
        output_dir=args.output,
        dataset_url=args.url,
    )

