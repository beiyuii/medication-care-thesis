# 依赖安装说明

## 问题说明

如果遇到 `ModuleNotFoundError: No module named 'cv2'` 或其他模块导入错误，说明虚拟环境中缺少必要的依赖包。

## 快速安装

### 方法1：使用安装脚本（推荐）

```bash
cd flask-project
bash install_dependencies.sh
```

### 方法2：手动安装

确保已激活虚拟环境（conda 或 venv），然后运行：

```bash
cd flask-project

# 激活 conda 环境（如果使用 conda）
conda activate algo-env

# 或激活 venv 环境（如果使用 venv）
source venv/bin/activate  # Linux/Mac
# 或
venv\Scripts\activate  # Windows

# 安装依赖
pip install opencv-python>=4.8.0
pip install pillow>=10.0.0
pip install onnxruntime>=1.16.0

# MediaPipe 作为可选依赖（如果安装失败不影响使用）
pip install mediapipe>=0.10.0  # 可能在某些Python版本上不可用
```

### 方法3：从 pyproject.toml 安装

```bash
cd flask-project
pip install -e .
```

## 验证安装

运行测试脚本验证所有依赖是否已正确安装：

```bash
python test_startup.py
```

如果看到 "✅ 所有检查通过！可以启动服务了。" 说明安装成功。

## 常见问题

### 1. MediaPipe 安装失败

**问题**：`ERROR: Could not find a version that satisfies the requirement mediapipe`

**原因**：MediaPipe 可能不支持当前的 Python 版本（特别是 Python 3.13）

**解决**：这是正常的，代码已经做了兼容处理，会在 MediaPipe 不可用时使用降级方案。应用仍然可以正常运行，只是手部检测精度会降低。

### 2. 在错误的 Python 环境中安装

**问题**：安装了依赖但运行时还是报错找不到模块

**原因**：可能在系统 Python 中安装了，但运行时使用的是虚拟环境

**解决**：
1. 确认已激活正确的虚拟环境
2. 使用 `which python` 检查当前使用的 Python 路径
3. 使用对应环境的 pip 安装，例如：
   ```bash
   /opt/miniconda3/envs/algo-env/bin/pip install opencv-python
   ```

### 3. Conda 环境问题

如果使用 conda 环境，确保：
```bash
conda activate algo-env
which python  # 应该显示 conda 环境的路径
pip install opencv-python pillow onnxruntime
```

## 依赖列表

必需的依赖：
- `opencv-python>=4.8.0` - 视频处理
- `numpy>=1.24.0` - 数组操作
- `onnxruntime>=1.16.0` - YOLOv8模型推理
- `pillow>=10.0.0` - 图像处理

可选依赖：
- `mediapipe>=0.10.0` - 手部关键点检测（如果不可用会使用降级方案）

## 启动服务

安装完成后，启动服务：

```bash
python -m app.main
```

然后在浏览器访问 `http://localhost:8000` 查看测试页面。

