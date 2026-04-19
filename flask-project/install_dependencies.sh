#!/bin/bash
# 安装视频检测所需的所有依赖

echo "🔧 正在安装依赖包..."

# 检查是否在虚拟环境中
if [ -z "$VIRTUAL_ENV" ] && [ -z "$CONDA_DEFAULT_ENV" ]; then
    echo "⚠️  警告: 未检测到虚拟环境，建议在虚拟环境中安装依赖"
    read -p "是否继续? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# 安装基础依赖
echo "📦 安装 opencv-python..."
pip install opencv-python>=4.8.0

echo "📦 安装 pillow..."
pip install pillow>=10.0.0

echo "📦 安装 onnxruntime..."
pip install onnxruntime>=1.16.0

# MediaPipe 作为可选依赖
echo "📦 尝试安装 mediapipe (可选)..."
pip install mediapipe>=0.10.0 2>/dev/null || echo "⚠️  MediaPipe 安装失败（可能不支持当前Python版本），将使用降级方案"

echo ""
echo "✅ 依赖安装完成！"
echo ""
echo "📋 已安装的包:"
pip list | grep -E "(opencv|numpy|onnxruntime|mediapipe|pillow)" || echo "未找到相关包"

echo ""
echo "🧪 测试导入..."
python -c "
try:
    import cv2
    print('✅ OpenCV:', cv2.__version__)
except ImportError:
    print('❌ OpenCV 未安装')

try:
    import onnxruntime as ort
    print('✅ ONNX Runtime:', ort.__version__)
except ImportError:
    print('❌ ONNX Runtime 未安装')

try:
    import mediapipe as mp
    print('✅ MediaPipe: 已安装')
except ImportError:
    print('⚠️  MediaPipe: 未安装（将使用降级方案）')

try:
    from PIL import Image
    print('✅ Pillow: 已安装')
except ImportError:
    print('❌ Pillow 未安装')
"

echo ""
echo "🚀 现在可以启动服务了:"
echo "   python -m app.main"

