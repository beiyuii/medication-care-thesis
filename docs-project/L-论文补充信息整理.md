# 论文补充信息整理

## 说明

- 本文档根据当前项目实际代码与仓库资料整理，用于补充论文中的算法参数、实现逻辑、页面介绍与资源清单。
- 仓库中未检索到现成的页面截图文件，因此下文统一使用“截图占位符”标记，后续可替换为真实截图。
- 如论文表述与早期方案文档冲突，应以当前实现代码为准。

---

## 一、算法公式与实现逻辑

### 1. YOLOv8 检测部分

- 当前训练模型为 `YOLOv8n`，训练入口为 `flask-project/train-yolov8/scripts/train.py`。
- 实际训练配置：
  - 模型：`yolov8n.pt`
  - 训练轮数：`100`
  - 批大小：`16`
  - 输入尺寸：`640`
  - 优化器：`AdamW`
  - 初始学习率：`0.01`
- 当前项目未对 YOLOv8 默认损失权重做二次自定义覆盖，训练产物中保留的是 Ultralytics 默认参数：
  - `box = 7.5`
  - `cls = 0.5`
  - `dfl = 1.5`

可在论文中写为：

> 本系统药品检测模块采用 YOLOv8n 轻量级目标检测模型。训练阶段使用 640×640 输入尺寸、100 轮迭代、16 的批大小和 AdamW 优化器，初始学习率设置为 0.01。损失函数权重未额外修改，沿用 YOLOv8 默认配置。

### 2. 服务端推理参数

- Flask 算法服务实际推理阈值：
  - 目标检测置信度阈值：`0.35`
  - 动作距离阈值：`8.0 cm`
  - 连续动作帧阈值：`45 帧`
- 当前推理代码中**没有单独执行 NMS 后处理逻辑**，因此不建议在论文中写“推理阶段 NMS IoU 阈值为某固定值”。
- 训练产物 `args.yaml` 中的 `iou = 0.7` 是训练/验证参数，不应直接写成线上推理 NMS 阈值。

### 3. YOLOv8 输出解析逻辑

当前 Flask 服务的实现流程为：

1. 将输入视频帧缩放到模型输入尺寸。
2. 转换为 RGB，并归一化到 `[0, 1]`。
3. 按 `NCHW` 格式送入 ONNX Runtime。
4. 对类别 logits 执行 sigmoid，得到类别置信度。
5. 选择最大类别概率作为该检测框的置信度。
6. 仅保留 `confidence >= 0.35` 的目标。

可写成公式：

\[
\hat{p}_c = \sigma(z_c) = \frac{1}{1 + e^{-z_c}}
\]

其中，\(z_c\) 为第 \(c\) 类的原始输出 logits，\(\hat{p}_c\) 为 sigmoid 后得到的类别置信度。

最终保留条件为：

\[
\max(\hat{p}_c) \ge 0.35
\]

### 4. MediaPipe 动作识别部分

- 当前使用 `MediaPipe Hands`，参数为：
  - `static_image_mode = False`
  - `max_num_hands = 2`
  - `min_detection_confidence = 0.5`
  - `min_tracking_confidence = 0.5`
- 关键点使用方式：
  - 识别手部 `21` 个关键点。
  - 取食指指尖 `landmarks[8]` 作为手部动作参考点。
- 口部区域定义方式：
  - 当前实现不是 Face Mesh 精确定位。
  - 口部采用固定估计点：

\[
x_m = 0.5W,\quad y_m = 0.4H
\]

其中，\(W\) 和 \(H\) 分别表示当前视频帧宽度与高度。

- 手口距离公式：

\[
d_{pixel} = \sqrt{(x_h - x_m)^2 + (y_h - y_m)^2}
\]

- 代码中再按“平均人脸宽约 12 cm”做粗略换算：

\[
r = \frac{12}{W}
\]

\[
d_{cm} = d_{pixel} \cdot r
\]

- 当前动作判定阈值为：

\[
d_{cm} \le 8
\]

### 5. 帧间平滑与连续帧判定逻辑

当前实现存在连续帧判定逻辑，不是单帧即判定。

动作帧满足条件：

\[
\text{is\_action\_frame} = (\text{has\_medication}) \land (d_{cm} \le 8)
\]

只有当连续满足动作条件的帧数达到阈值时，才认为视频中存在服药动作片段：

\[
N \ge 45
\]

在前端默认 `samplingRate = 30` 的情况下，`45 帧` 大约对应 `1.5 秒`。

### 6. 检测结果综合判定逻辑

算法服务最终输出 `confirmed / suspected / abnormal` 三态，规则如下：

#### 6.1 目标聚合

- 系统先对视频中的同类目标进行帧级聚合，计算平均置信度。
- 若最终没有检测到任何有效目标，则直接判为：

\[
status = abnormal
\]

#### 6.2 状态判定

设：

- `action_detected` 表示是否检测到满足连续帧条件的动作片段；
- `avg_confidence` 表示聚合后目标的平均置信度。

则综合判定规则为：

\[
status =
\begin{cases}
confirmed, & action\_detected = true \land avg\_confidence \ge 0.85 \\
suspected, & action\_detected = true \lor avg\_confidence \ge 0.5 \\
abnormal, & \text{otherwise}
\end{cases}
\]

对应论文口径：

- `confirmed`：药品目标与服药动作证据同时较强成立；
- `suspected`：已检测到部分证据，但仍需要人工确认；
- `abnormal`：未检测到有效服药行为，或检测结果不足以支持确认。

### 7. 时间窗超时逻辑

- 当前项目**没有固定“超时 N 分钟”**这一全局参数。
- 超时依据来自每条用药计划自身的时间窗 `win_end`。
- 后端逻辑是：当系统当前时间超过该计划当天的 `windowEnd`，且当天没有 `confirmed` 事件时，自动生成 `timeout` 告警。

论文建议写法：

> 系统不采用固定分钟数作为统一超时阈值，而是以每条用药计划配置的结束时间作为时间窗边界。当当前时间超过该计划当日时间窗结束时间且系统仍未检索到已确认服药事件时，即判定为超时未确认，并生成对应异常告警。

### 8. 业务层事件映射逻辑

- `confirmed`：可自动形成服药事件，并在前端/后端联动下完成确认落库。
- `suspected`：先生成待确认事件，后续由用户点击“确认已服药”完成确认。
- `abnormal`：生成异常事件，并可触发“检测失败”或“超时未确认”类告警。

---

## 二、系统页面介绍

## 1. 路由与角色结构

当前前端路由如下：

- 公共页：
  - `/login`
- 老年人端：
  - `/elder/home`
  - `/plans`
  - `/detection`
  - `/history`
  - `/alerts`
  - `/settings`
- 护工端：
  - `/caregiver/home`
  - `/history`
  - `/alerts`
- 子女端：
  - `/child/home`
  - `/history`
  - `/alerts`

说明：

- 项目中**没有单独的“统计分析页”路由**。
- “统计分析”与“历史记录”合并在 `/history` 页面中，页面标题为“历史记录与统计”。

---

## 2. 登录页

### 截图占位符

【截图占位符：登录页】

### 页面说明

- 页面为双栏布局：
  - 左侧是系统简介与隐私提示；
  - 右侧是登录/注册卡片。
- 登录模式下显示：
  - 用户名
  - 密码
  - 记住登录状态
  - 浏览器权限指南入口
- 注册模式下显示：
  - 用户名
  - 密码
  - 角色选择（老年人 / 护工 / 子女）
- 系统并不是“登录时选择角色”，而是：
  - 注册时选择角色；
  - 登录后根据后端返回的角色自动跳转到对应首页。

---

## 3. 老年人首页 / 仪表盘

### 截图占位符

【截图占位符：老年人首页】

### 页面说明

- 页面顶部在有异常时显示状态条，提示最新未处理告警，并提供跳转告警页入口。
- 首屏主体分为两部分：
  - 左侧主卡：展示“下一次提醒时间 + 药品名 + 完成度 + 开启摄像头检测按钮 + 查看异常提醒按钮”。
  - 右侧卡片：展示检测准备提示，例如保持桌面整洁、手部靠近口部约 1.5 秒、识别后点击确认等。
- 下方为“今日用药计划”列表：
  - 每个卡片显示药品名、剂量/频次、时间窗、状态标签。
  - 状态包括：待服药、已确认、异常。

---

## 4. 用药计划管理页

### 截图占位符

【截图占位符：用药计划页】

### 页面说明

- 页面顶部有标题区、状态筛选按钮和“新增计划”按钮。
- 中间有 3 个统计卡片：
  - 进行中
  - 暂停中
  - 今日提醒
- 下方为计划列表，列表项展示：
  - 药品名称
  - 药品类型（药片 / 泡罩板 / 药瓶 / 药盒）
  - 当前状态
  - 下一次提醒时间
  - 剂量与频次
  - 时间窗
  - 周期
  - 编辑 / 暂停 / 启用操作

### 创建计划表单字段

- 药品名称
- 药品类型
- 剂量数值
- 剂量单位
- 频次
- 开始时间
- 结束时间
- 周期数值
- 周期单位
- 初始状态

---

## 5. 检测页面

### 截图占位符

【截图占位符：检测页】

### 页面说明

- 页面顶部为状态条：
  - 未开启权限时显示权限提示；
  - 检测异常时显示异常提示；
  - 检测中、疑似、已确认时显示不同状态反馈。
- 主体为左右双栏布局：
  - 左侧：
    - 摄像头实时画面区域
    - 录制中状态提示
    - 视频处理中的遮罩层
    - 三步步骤条：准备 / 记录 / 完成
    - 对应步骤操作按钮：开启摄像头、开始录像检测、结束并检测、重新检测、确认并保存记录
  - 右侧：
    - 用药计划选择卡
    - 检测状态卡
    - 检测指引卡
    - 倒计时确认按钮

### 检测结果展示区

- 展示检测结果状态：
  - 确认已服药
  - 疑似已服药
  - 检测异常
- 若识别到目标，还会显示目标标签及置信度百分比。
- 页面中明确提示：
  - 模型输入 `640×640`
  - 阈值 `0.35`
  - 动作检测条件为“手口距离 < 8cm 且 ≥45 帧”

---

## 6. 历史记录页 / 统计分析页

### 截图占位符

【截图占位符：历史记录与统计页】

### 页面说明

- 该页面同时承担“历史记录页”和“统计分析页”的功能。
- 顶部支持时间维度切换：
  - 今日
  - 本周
  - 本月
- 中间为 3 个统计卡片，通常包括：
  - 提醒次数
  - 确认率
  - 异常 / 漏服次数
- 下方为事件时间线：
  - 发生时间
  - 药品名称
  - 所属计划
  - 状态标签（已确认 / 待确认 / 异常）
  - 动作描述
  - 手动确认按钮
  - 查看详情按钮
  - 若有截图，则显示缩略图

### 详情弹窗内容

- 基本信息
- 事件截图
- 目标检测结果
- 原始 `targets_json`
- 动作检测描述
- 确认人
- 确认时间

---

## 7. 护工管理端首页

### 截图占位符

【截图占位符：护工端首页】

### 页面说明

- 护工端首页首先需要选择被照护人。
- 页面顶部展示 3 张摘要卡：
  - 下一次提醒
  - 计划状态
  - 未处理异常
- 下方分左右两栏：
  - 左侧：“当日服药记录”时间线
  - 右侧：“异常任务”列表
- 页面提供“查看详情”按钮，可打开患者详情弹窗。

### 患者详情弹窗

- 基础信息：姓名、年龄、联系电话
- 用药计划：最近 5 条
- 最近告警：最近 5 条

---

## 8. 子女管理端首页

### 截图占位符

【截图占位符：子女端首页】

### 页面说明

- 子女端首页不需要手动选择患者，而是基于一对一关联直接展示当前家人信息。
- 顶部展示：
  - 家人信息卡
  - 下一次提醒
  - 异常待处理
- 中部展示：
  - 服药达成率环形图
  - 即将提醒列表
- 下部展示：
  - 当日记录列表
- 同样支持查看患者详情弹窗。

---

## 9. 告警页面

### 截图占位符

【截图占位符：告警页】

### 页面说明

- 页面顶部有严重程度筛选：
  - 全部
  - 紧急
  - 重要
  - 一般
- 支持导出 CSV。
- 告警列表卡片字段包括：
  - 告警标题
  - 严重程度
  - 发生时间
  - 关联老人
  - 关联药品
  - 告警描述
  - 处理建议
  - 处理状态

### 交互方式

- 查看详情
- 标记已处理
- 导出 CSV

### 告警详情弹窗内容

- 告警标题
- 告警类型
- 发生时间
- 严重程度
- 关联患者
- 关联药品
- 详细描述
- 处理建议
- 处理状态
- 处理时间
- 处理备注

---

## 三、其他可选补充信息

## 1. 系统架构图 / 流程图 / 设计文档

仓库中已有可直接整理进论文的文档：

- `docs-project/D-系统架构与技术方案.md`
- `docs-project/B-概念原型与交互规划.md`
- `docs-project/G-设计明细.md`
- `docs-project/I-论文撰写支撑材料.md`
- `docs-project/K-论文信息汇总.md`

这些文档可用于整理：

- 系统总体架构图
- 业务流程图
- 页面功能说明
- 论文统一写法口径

## 2. API 接口文档

仓库中存在 OpenAPI 文档：

- `springProject/docs/openapi.yaml`

可直接作为接口章节或附录的基础材料。

## 3. ER 图 / 数据库结构

- 仓库中未找到现成的 ER 图图片文件。
- 但数据库初始化脚本完整，可直接据此绘制 ER 图。

核心实体包括：

- `users`
- `patients`
- `user_patient_relation`
- `schedules`
- `intake_events`
- `alerts`
- `log_images`
- `settings`

## 4. 前端路由结构

前端路由文件：

- `vue-project/src/router/index.ts`

可直接整理成论文中的“页面结构图”或“功能导航结构图”。

## 5. 组件树

- 仓库中未发现现成的组件树文档。
- 目前只能根据：
  - `views/`
  - `layouts/`
  - `components/ui/`
  手工整理组件结构。

---

## 四、可直接引用的资源清单

## 1. 算法与训练资源

- `flask-project/app/core/config.py`
- `flask-project/app/services/video_detector.py`
- `flask-project/app/services/result_aggregator.py`
- `flask-project/train-yolov8/scripts/train.py`
- `flask-project/train-yolov8/outputs/runs/detect/medication_detection/args.yaml`

## 2. 页面与交互资源

- `vue-project/src/router/index.ts`
- `vue-project/src/layouts/CoreLayout.vue`
- `vue-project/src/views/LoginView.vue`
- `vue-project/src/views/ElderDashboardView.vue`
- `vue-project/src/views/PlanBoardView.vue`
- `vue-project/src/views/DetectionRoomView.vue`
- `vue-project/src/views/HistoryCenterView.vue`
- `vue-project/src/views/AlertCenterView.vue`
- `vue-project/src/views/CaregiverDashboardView.vue`
- `vue-project/src/views/ChildDashboardView.vue`

## 3. 后端业务与告警逻辑资源

- `springProject/src/main/java/com/liyile/medication/controller/AlertController.java`
- `springProject/src/main/java/com/liyile/medication/controller/IntakeEventController.java`
- `springProject/docs/openapi.yaml`
- `springProject/docs/sql/schema.sql`

## 4. 可用于论文插图的训练结果图片

- `flask-project/train-yolov8/outputs/runs/detect/medication_detection/results.png`
- `flask-project/train-yolov8/outputs/runs/detect/medication_detection/confusion_matrix.png`
- `flask-project/train-yolov8/outputs/runs/detect/medication_detection/BoxPR_curve.png`
- `flask-project/train-yolov8/outputs/runs/detect/medication_detection/BoxP_curve.png`
- `flask-project/train-yolov8/outputs/runs/detect/medication_detection/BoxR_curve.png`
- `flask-project/train-yolov8/outputs/runs/detect/medication_detection/BoxF1_curve.png`

---

## 五、当前缺失项说明

- 仓库中未检索到现成的“登录页 / 首页 / 计划页 / 检测页 / 历史页 / 告警页”截图文件。
- 仓库中未检索到现成 ER 图图片。
- 仓库中未检索到现成组件树文档。

因此，论文目前可以先使用本文件中的截图占位符与文字说明；如需最终定稿，建议后续补抓实际页面截图并替换占位符。
