## 目标与范围
- 目标：为后端 API 建立可重复、可验证、可度量的接口测试体系，覆盖输入校验、格式规范、响应一致性、异常场景与性能指标。
- 范围：`/auth/*`、`/health`、`/schedules/*`、`/intake-events/*`、`/logs/images*`、`/reports/*`、`/alerts/*`（基于当前控制器代码结构）。

## 环境与前置准备
- 测试环境：本地或 CI 环境启动后端（Dev Profile），准备 MySQL/Redis 可用实例与已初始化数据（`docs/sql/schema.sql`、`seed.sql`）。
- 变量管理：在 Postman/APIfox 配置 `baseUrl`、`jwtToken`、`patientId`、`eventId` 等环境变量。
- 认证前置：通过登录接口动态获取 JWT（Pre-request Script 写入到 `jwtToken`），所有受保护接口自动携带 `Authorization: Bearer {{jwtToken}}`。

## 工具与结构
- 首选：Postman 或 APIfox 集合（Collections）。
  - 层级结构：
    - `Auth`（登录、个人信息）
    - `Health`
    - `Schedules`（列表、创建、更新、启停）
    - `IntakeEvents`（创建、列表）
    - `LogImages`（上传、列表）
    - `Reports`（汇总）
    - `Alerts`（列表/更新，若有）
  - 每请求附带 Tests 脚本断言（状态码、JSON 架构、字段值）。
- 自动化执行：Newman CLI 集成到 CI（GitHub Actions/Jenkins）按环境变量执行；导出 JUnit 报告。
- 备选：Python `pytest + requests` 脚本，封装通用请求与断言，支持参数化与并行。

## 测试数据与账号
- 账号：使用 `seed.sql` 预置用户（如 `elder1` 等），并准备一组测试患者与计划/事件数据。
- 辅助：构造边界与非法数据集（超长字符串、空值、异常枚举、非法 JSON 等）。

## 用例设计（数据传入）
- 合法/非法参数组合：
  - 登录：正确用户名+正确密码；正确用户名+错误密码；不存在用户名；空用户名/密码；特殊字符用户名（中文、emoji、空格）。
  - 计划创建/更新：必填字段缺失（`patientId/type/dose/freq/winStart/winEnd/period/status`）；类型枚举非法（大小写不一致/未知值）；数值边界（`dose=freq=0/负数/极大`）。
  - 事件创建：`status/action` 非法值；`targetsJson` 非合法 JSON；`imgUrl` 非 URL 格式。
  - 图片上传：MIME 与扩展名不匹配（`image/png`+`.exe`）；空文件；超大小；非法扩展（`.jsp/.html`）。
- 必填字段校验：对所有 `@NotNull` 或业务必填进行缺失测试，断言 `422` 与错误信息。
- 边界值：字符串长度（0、1、255、1024）、时间窗口边界（`00:00`、`23:59`）、分页参数（如未实现则断言拒绝或忽略）。
- 特殊字符处理：Unicode（中文、emoji）、转义字符（`"`, `\`）、SQL 注入样例（作为普通字符串），确保不触发异常或错误持久化。

## 用例设计（数据格式）
- JSON 格式：
  - 正确 JSON 体；错误 JSON（缺右括号、类型错置）；额外/未知字段（服务是否忽略）。
- Content-Type：
  - `application/json` 正常；`text/plain`/`application/x-www-form-urlencoded` 提交 JSON，预期 `415/422`。
- 日期/时间：
  - 遵循 `yyyy-MM-dd'T'HH:mm:ss`（`application.properties` 配置）；错误格式（如 `2025/11/13 08:00`）预期 `422`。
- 复杂结构：
  - `targetsJson` 提交数组/嵌套对象；提交非对象字符串，预期失败或按业务处理。

## 用例设计（返回类型与数据）
- 状态码：
  - 成功 `200`；未认证 `401`；无权限 `403`；资源不存在 `404`；参数错误 `422`；服务器错误 `500`。
- 统一响应：断言 `ApiResponse` 结构 `{code, message, data}`；成功 `code=0`；失败为语义错误码（`ErrorCode`）。
- 错误信息格式：校验 `message` 为人类可读；参数校验场景返回错误详情（如聚合字段错误，若实现）。
- 列表/排序/分页：
  - 列表返回结构与默认排序（按 `id` 或 `ts`）；若提供分页参数（`page/size/sort`），断言返回的分页元信息与排序顺序；未实现则断言忽略或报错一致性。

## 端点级用例清单
- `Auth`
  - `POST /auth/login`：成功签发 JWT；失败返回 `401` 与统一错误体；特殊字符用户名；大小写敏感性。
  - `GET /auth/profile`：携带有效/过期/伪造/缺失 Token；断言 `200/401` 与用户信息。
- `Health`
  - `GET /health`：公开访问返回 `200` 与固定消息。
- `Schedules`
  - `GET /schedules?patientId=`：必填参数校验；无/非法患者 ID；结果非空断言。
  - `POST /schedules`：合法创建；缺失必填；非法枚举；边界值；断言响应体实体字段。
  - `PATCH /schedules/{id}`：存在与不存在 ID；部分字段更新；响应 `rows`。
  - `POST /schedules/{id}/toggle`：存在与不存在 ID；状态切换 `enabled/disabled`；404 时统一错误码。
- `IntakeEvents`
  - `POST /intake-events`：合法/非法 `status/action/targetsJson`；边界；断言持久化字段。
  - `GET /intake-events?patientId=&scheduleId=`：组合过滤；空结果；排序断言。
- `LogImages`
  - `POST /logs/images`：合法 PNG/JPEG/WEBP；MIME 与扩展不一致；超大小；空文件；断言存储路径不泄露系统绝对路径。
  - `GET /logs/images?eventId=`：存在/不存在；权限校验；结果字段检查。
- `Reports`
  - `GET /reports/summary`：基本返回结构；断言聚合字段存在与类型。
- `Alerts`
  - `GET/POST/PATCH ...`（若实现）：列表/状态变更；权限与错误码。

## 自动化测试脚本
- Postman/APIfox：
  - 集合中为每个请求编写 Tests 脚本：
    - 断言状态码：`pm.response.code === 200`
    - 断言 JSON 结构：`pm.expect(body).to.have.all.keys('code','message','data')`
    - 断言业务字段：如 `data.token` 存在且非空、`data.role` 为允许枚举
    - 断言错误：`code` 为 `401/403/404/422` 等，`message` 包含关键提示
  - Pre-request：登录后将 `token` 写入环境变量，后续自动附加 Authorization 头。
- Newman 执行：`newman run collection.json -e env.json --reporters cli,junit --reporter-junit-export results.xml`
- Python 备选：`pytest` 参数化用例，封装通用断言（状态码、结构、字段），对复杂场景（如文件上传、多步操作）编写流水线测试。

## 性能测试方案（如需要）
- 工具：k6 或 JMeter。
- 场景：
  - 登录 1/min（鉴别签发延迟）；
  - 列表查询（Schedules/IntakeEvents）50–200 并发；
  - 图片上传（LogImages）低并发但较大负载，校验后端磁盘与内存占用。
- 指标：
  - p95 延迟：`<=200ms`（查询类）、`<=500ms`（上传类）；
  - 错误率：`<1%`；
  - 吞吐：按目标并发稳定；
  - 资源：CPU/内存不出现持续上升（无泄露）。
- 脚本：k6 脚本分阶段 ramp-up/ramp-down，携带真实 JWT 与数据；结果导出 JSON 与 HTML 报告。

## 测试报告模板
- 基本信息：用例编号、接口、方法、环境、时间、执行人。
- 前置条件：数据与状态准备。
- 请求详情：URL、路径参数、查询参数、头部、请求体（脱敏）。
- 期望结果：状态码、响应结构、关键字段断言、错误码与文案。
- 实际结果：状态码、响应体摘要、截图或日志链接。
- 结论：通过/失败；失败原因与复现步骤；修复建议。
- 附录：性能摘要（p95/p99/错误率）、依赖与版本、变更记录。

## 异常场景覆盖清单
- 认证与授权：缺失/过期/伪造 Token；访问未授权资源 → `401/403`。
- 参数与格式：JSON 语法错误；必填缺失；类型不匹配；非法枚举；日期格式错误 → `422`。
- 资源：不存在 ID → `404`；空列表 → 正常 `200` 与空数组。
- 存储与上传：非法 MIME/扩展；超大小；空文件；目录不可写/磁盘满 → `500` 与统一错误体。
- 系统异常：弱密钥触发（密钥未配/过短）；数据库/Redis 不可用 → `500` 或重试策略验证。
- 跨域：预检请求 `OPTIONS` 正常；暴露 `Authorization` 头可读；缓存 `maxAge` 生效。

## 执行与验收标准
- 所有用例在本地与 CI 可重复执行，断言明确且稳定。
- 覆盖范围达到：每接口至少 1 个正例 + 3 个反例；关键路径（登录、列表、上传）含边界与异常。
- 报告与日志：自动生成 JUnit/XML/HTML 报告；失败附带请求与响应关键摘要。

## 交付物清单
- Postman/APIfox 集合与环境文件（含 Pre-request 与 Tests 脚本）。
- Newman 执行脚本与 CI 配置示例（命令与工件导出）。
- k6/JMeter 性能脚本与报告模板。
- 测试用例设计文档与异常场景清单（按端点分组）。

## 英文翻译（你的需求）
Design a complete API testing plan focusing on: 1) input validation with legal/illegal combinations, required fields, boundary values, special characters; 2) data format validation including JSON spec, Content-Type variations, date/time formats, arrays/nested objects; 3) response verification for HTTP status codes, response body structure, error format, and pagination/sorting. Include: test case design document, automated scripts (Python/Postman), performance plan (if needed), report template, and exception coverage list, using professional tools like Postman/APIfox, ensuring repeatability and verifiable results.【已翻译】