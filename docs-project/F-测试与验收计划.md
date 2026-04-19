测试与验收计划（修订版）

一、测试目标
- 验证三服务链路可运行：Vue 前端可构建，Spring 后端可通过单元测试，Flask 默认 `pytest` 可执行。
- 验证业务闭环：登录 -> 计划 -> 检测 -> 事件 -> 历史 -> 告警。
- 验证关键统计语义：`nextIntake`、`missedCount`、超时告警生成与自动关闭。

二、当前自动化验证结果
- 前端：`cd vue-project && npm run build` 通过。
- Spring：`cd springProject && mvn test` 通过，共 7 个测试。
- Flask：`cd flask-project && pytest` 通过，共 7 个测试。
- Flask 性能片段：`pytest app/tests/perf/test_perf_snippet.py -q -s` 当前可运行，但结果只用于实验环境参考，不作为论文最终性能结论。

三、关键验收场景
- elder 登录后创建并启用用药计划。
- 检测页完成一次图片或视频检测，产生 `suspected / confirmed / abnormal` 结果之一。
- 检测完成后生成服药事件，并可上传关键帧日志。
- 时间窗结束后未确认服药时，Spring 在查询告警时补齐 timeout 告警。
- 服药事件确认后，同一计划的 pending 告警自动关闭。
- caregiver / child 端可查看相同患者的历史记录与告警状态。

四、测试环境说明
- 前端：Vue3 + Vite，本机构建验证。
- 业务后端：Spring Boot + MySQL，当前测试以单元测试为主，不依赖真实数据库连接。
- 算法服务：Flask + ONNX Runtime + OpenCV；测试中使用真实图片样本和模型加载路径。
- 浏览器与摄像头联调属于手工验收项，建议使用 Chrome/Edge 进行答辩演示。

五、缺陷关注点
- 模型识别效果受样本、光照、角度影响明显，需在论文中把算法验证与系统闭环验证分开表述。
- 当前周期/频次为字符串字段，统计按“每日时间窗”进行约束化计算，属于可解释近似方案。
- 生产级安全、容器部署、分布式调度不在本项目当前验收范围内。
