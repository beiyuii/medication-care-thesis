系统架构与技术方案（修订版）

一、架构概览
- 前端：Vue3 + Vite + TypeScript + Pinia，负责登录、计划管理、检测页交互、历史统计、告警与设置。
- 业务后端：Spring Boot + MyBatis-Plus + MySQL + JWT，负责用户与角色、患者关联、用药计划、服药事件、告警、日志图片、统计摘要。
- 算法服务：Flask + ONNX Runtime + OpenCV，负责图片帧检测、视频检测、视频上传与模型就绪检查。
- 当前实现不是浏览器端 onnxruntime-web 推理，而是“浏览器采集帧/视频 -> Flask 推理 -> Spring 落库”的三服务协作模式。

二、实际数据流
- 登录后，前端通过 Spring API 获取患者、计划、设置与历史数据。
- 检测页录制视频或抓取图片帧后，前端调用 Flask 的 `/v1/detections/predict` 或 `/v1/detections/video/predict`。
- Flask 返回统一的检测结果：`status / confidence / actionDetected / targets / traceId / latencyMs`。
- 前端再调用 Spring 的服药事件、日志图片、告警接口，将检测结果转成业务记录。
- 护工/子女端从 Spring 查询同一份结构化业务数据，不直接访问模型结果。

三、分层职责
- Vue 前端：
  - 身份认证与路由守卫
  - 摄像头权限与录制交互
  - 检测结果展示、手动确认、页面回流
  - 适老化样式与无数据态处理
- Spring 后端：
  - 角色权限：elder 可写，caregiver/child 只读
  - 患者摘要与详情、计划 CRUD、事件确认、告警处理
  - `nextIntake` 计算、漏服统计、超时告警生成
- Flask 算法服务：
  - ONNX 模型加载与 readiness 检查
  - 单帧目标检测
  - 视频抽帧检测与动作时间线聚合
  - MediaPipe 不可用时的降级方案

四、存储与部署
- 主数据存储为 MySQL，不使用 SQLite。
- 图片日志由 Spring 写入本地上传目录；视频文件由 Flask 写入本地 `video/` 目录用于检测与样例留存。
- 当前部署定位为本机开发/答辩演示环境，不按生产高可用部署设计。
- 关键配置已改为环境变量优先：数据库、Redis、JWT secret 不再固定写死在正式配置方案中。

五、边界说明
- 该系统定位为“健康辅助系统原型/实验系统”，不是医疗器械系统。
- 当前算法重点是“真实模型接入、统一推理协议、可演示闭环”，不宣称已经完成严格医疗级精度验证。
- 频次和周期字段目前仍以业务字符串为主，统计逻辑按“启用计划每天一个时间窗”进行约束化计算，这一点需在论文中如实说明。
