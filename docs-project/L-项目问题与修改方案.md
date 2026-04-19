# 项目问题与修改方案

> 基于真实运行测试、UI 行为测试与数据链路核查

## 1. 文档目的与适用范围

本文档用于整理当前项目在真实运行、用户行为测试和数据链路核查中暴露出的主要问题，并给出面向开发修复的整改方案。  
文档目标不是复述测试过程，而是形成可执行的工程说明，便于开发人员按优先级推进整改，也便于项目负责人或答辩老师快速判断当前系统的主要缺陷、影响范围与后续修改方向。

本文档适用于以下场景：
- 作为项目整改实施单，指导后续开发修复。
- 作为阶段性问题汇总材料，用于项目复盘和风险评估。
- 作为论文中“系统不足与改进方向”章节的事实依据。

本文档基于本次已完成的真实测试结果整理，不重新引入新的测试结论，也不直接提供代码 patch。

## 2. 当前测试结论摘要

### 2.1 自动化测试结论
- 前端构建：`npm run build` 通过。
- Spring Boot 后端单元测试：`mvn test` 通过，共 7 个测试。
- Flask 算法服务测试：`pytest` 通过，共 7 个测试。

### 2.2 真实运行结论
- 在默认配置下，后端真实登录链路无法直接稳定运行。
- 在补齐临时数据库密码和合法 JWT 密钥后，老年用户和护工用户均可进入前端首页。
- 进入首页后，核心业务接口继续因数据库 schema 与代码不一致而失败。
- 护工端患者列表链路在真实库上直接断裂，无法完成多角色查看主流程。
- 前端存在“业务接口失败被误判为 token 无效”的因果链设计问题，会导致误登出。

### 2.3 总体判断
当前项目的静态工程质量和自动化测试结果较好，但真实运行闭环没有跑通。  
项目的主要问题集中在运行配置、数据库模式一致性、异常处理规范和前后端因果链设计上，而不是页面结构或单一算法接口本身。

## 3. 问题分级说明

- `P0`：阻断主流程，导致系统无法完成登录、首页加载、角色联动或基本业务闭环。
- `P1`：不一定阻断所有流程，但会造成严重误判、用户体验错误或难以定位问题。
- `P2`：不影响主流程可用性，但影响可维护性、可诊断性或后续扩展质量。

## 4. 核心问题清单

### 问题 P0-01：默认数据库连接配置不可直接运行

- 问题编号：`P0-01`
- 优先级：`P0`
- 现象：
  - 后端默认配置使用 `root` 空密码连接 `jdbc:mysql://127.0.0.1:3306/lyl`。
  - 在本地真实环境下，该配置无法直接获取数据库连接，导致登录接口首层即失败。
- 复现条件：
  - 使用仓库默认配置启动 Spring Boot 后端。
  - 调用 `/api/auth/login`。
- 影响范围：
  - 登录链路
  - 所有依赖数据库的业务接口
  - 前端全部真实业务页面
- 根因：
  - 默认 `DB_PASSWORD` 为空，不匹配当前本地 MySQL 实例。
  - 默认配置假设数据库直接开放在 `127.0.0.1:3306`，与当前本机实际运行方式不完全一致。
- 修改目标：
  - 使项目在标准开发环境下可按文档直接启动，不需要额外猜测数据库密码或连接方式。
- 建议修改位置：
  - `springProject/src/main/resources/application.properties`
  - `springProject/src/main/resources/application-dev.properties`
  - `springProject/README.md`
  - `springProject/docs-project/H-测试实施指南.md`
- 实施任务：
  1. 统一开发环境数据库接入说明，明确库名、账号、密码与启动方式。
  2. 为本地开发环境提供明确可用的 `.env` 或环境变量示例。
  3. 校正文档中数据库名称与实际配置不一致的问题。
  4. 若项目要求默认开箱可跑，提供本地默认数据库初始化脚本和约定密码说明。
- 验收标准：
  - 按仓库文档执行启动步骤后，无需额外人工猜测配置即可成功访问 `/api/auth/login`。
  - 后端启动后数据库连接成功，无连接拒绝或鉴权失败。

### 问题 P0-02：默认 JWT 密钥配置导致登录链路不稳定

- 问题编号：`P0-02`
- 优先级：`P0`
- 现象：
  - 登录请求在校验用户名密码后，可能因 JWT 密钥配置问题在生成 token 时失败。
  - 失败表现为登录接口异常返回，前端显示“请求失败”或未认证。
- 复现条件：
  - 使用默认 `app.jwt.secret=change-this-in-production` 启动后端。
  - 执行登录。
- 影响范围：
  - 登录链路
  - 所有依赖 token 的接口
  - 前端会话建立与持久化
- 根因：
  - JWT 密钥提供器优先按 Base64 解码处理，默认值不适合作为正式可用开发密钥。
  - 默认配置没有为开发环境提供明确、稳定的合法密钥。
- 修改目标：
  - 保证默认开发环境下 JWT 生成与校验稳定可用。
- 建议修改位置：
  - `springProject/src/main/resources/application.properties`
  - `springProject/src/main/resources/application-dev.properties`
  - `springProject/src/main/java/com/liyile/medication/security/JwtTokenProvider.java`
- 实施任务：
  1. 为开发环境提供合法且可复用的默认 JWT 密钥方案。
  2. 明确区分“开发默认密钥”和“生产环境必须显式注入”的密钥。
  3. 在 `JwtTokenProvider` 中补充更清晰的异常提示，避免只看到泛化认证错误。
- 验收标准：
  - 默认开发配置下，合法账号登录后能稳定返回 token。
  - token 可用于访问 `/api/auth/profile` 和其他鉴权接口。

### 问题 P0-03：真实数据库 schema 与代码实体、SQL 文档不一致

- 问题编号：`P0-03`
- 优先级：`P0`
- 现象：
  - 老年人登录成功后，首页依赖的 `/api/schedules`、`/api/intake-events`、`/api/alerts` 接口全部失败。
  - 典型报错为：
    - `Unknown column 'medicine_name' in 'field list'`
    - `Unknown column 'confirmed_by' in 'field list'`
- 复现条件：
  - 登录 elder 用户。
  - 进入 `/elder/home`。
- 影响范围：
  - elder 首页
  - 计划查询
  - 历史记录查询
  - 告警查询
  - 检测后事件与告警联动
- 根因：
  - 当前运行中的真实数据库表结构落后于仓库中的实体类和 SQL 文档。
  - `schedules`、`intake_events`、`alerts` 表与代码字段不一致。
- 修改目标：
  - 保证真实数据库 schema、实体类、Mapper 查询字段和 `docs/sql/schema.sql` 完全一致。
- 建议修改位置：
  - `springProject/docs/sql/schema.sql`
  - `springProject/docs/sql/seed.sql`
  - `springProject/src/main/java/com/liyile/medication/entity/*.java`
  - 相关 Mapper 与 Controller
- 实施任务：
  1. 对照实体类和 Mapper 查询语句，给出数据库 schema 基准版本。
  2. 为现有真实库补齐缺失字段或重新初始化开发库。
  3. 清点 `schedules`、`intake_events`、`alerts` 当前代码使用的所有字段。
  4. 更新种子数据，保证按最新表结构可直接初始化。
- 验收标准：
  - `/api/schedules?patientId=1` 返回成功且包含 `medicineName`。
  - `/api/intake-events?patientId=1&range=day` 返回成功。
  - `/api/alerts?patientId=1` 返回成功。
  - elder 首页不再因 schema 问题报错。

### 问题 P0-04：护工/子女链路依赖表缺失

- 问题编号：`P0-04`
- 优先级：`P0`
- 现象：
  - 护工登录后首页可进入，但加载患者列表时失败。
  - 接口报错：`Table 'lyl.user_patient_relation' doesn't exist`。
- 复现条件：
  - 登录 `care1`。
  - 进入 `/caregiver/home`。
- 影响范围：
  - 护工端首页
  - 子女端首页
  - 患者切换与跨角色查看
  - 多角色协同核心场景
- 根因：
  - 当前真实数据库缺失 `user_patient_relation` 表。
  - 数据初始化未覆盖护工/子女联动链路所需的最小数据集。
- 修改目标：
  - 保证多角色链路可在初始化后直接跑通。
- 建议修改位置：
  - `springProject/docs/sql/schema.sql`
  - `springProject/docs/sql/seed.sql`
  - `springProject/src/main/java/com/liyile/medication/entity/UserPatientRelation.java`
  - `springProject/src/main/java/com/liyile/medication/mapper/UserPatientRelationMapper.java`
- 实施任务：
  1. 确认关联表作为开发库必须存在的基础表。
  2. 在 seed 数据中补足 elder、caregiver、child 与 patient 的最小关联关系。
  3. 验证 caregiver 与 child 登录后都能查询到关联患者。
- 验收标准：
  - `/api/patients` 对 caregiver 和 child 返回成功。
  - 护工端和子女端首页均能展示患者选择列表。

### 问题 P1-01：`/auth/profile` 与前端 token 校验存在因果耦合错误

- 问题编号：`P1-01`
- 优先级：`P1`
- 现象：
  - 只要 `/auth/profile` 因业务表缺失或查询异常失败，前端就把它当成 token 无效并清除会话。
  - 用户会被强制跳回登录页。
- 复现条件：
  - 登录护工用户。
  - 在 profile 拉取过程中触发患者关联查询异常。
- 影响范围：
  - 启动时 token 校验
  - 会话持久化
  - 任意角色的页面刷新行为
- 根因：
  - 前端 `validateToken()` 将 `getProfile()` 的任意失败都解释为 token 失效。
  - 后端 `/auth/profile` 不只返回认证信息，还耦合了患者列表业务查询。
- 修改目标：
  - 将“认证有效性校验”和“附属业务数据加载”拆开，避免业务异常触发误登出。
- 建议修改位置：
  - `vue-project/src/main.ts`
  - `vue-project/src/services/authService.ts`
  - `springProject/src/main/java/com/liyile/medication/controller/AuthController.java`
- 实施任务：
  1. 将 `/auth/profile` 简化为纯认证信息接口，至少保证 token 有效时不因患者业务失败而整体失败。
  2. 前端 `validateToken()` 仅在明确 401/403 的鉴权失败时清会话。
  3. 患者列表等业务信息改由页面或独立接口单独拉取。
- 验收标准：
  - token 有效但业务接口 500 时，不会自动登出。
  - 刷新页面后，合法 token 仍能保持登录状态。

### 问题 P1-02：后端异常统一返回 200，导致前端误判和定位困难

- 问题编号：`P1-02`
- 优先级：`P1`
- 现象：
  - 浏览器网络面板中多个失败接口显示 `200 OK`，但响应体内实际包含 SQL 异常或业务错误。
  - 前端只能笼统提示“请求失败”，难以区分是 401、403、500 还是 schema 错误。
- 复现条件：
  - 任意触发后端异常的接口，例如 `/api/schedules`。
- 影响范围：
  - 所有前端错误感知
  - UI 调试效率
  - 自动化验收可靠性
- 根因：
  - `GlobalExceptionHandler` 返回普通对象但未明确设置 HTTP 状态码。
- 修改目标：
  - 保证异常响应的 HTTP 状态码与业务错误类型一致。
- 建议修改位置：
  - `springProject/src/main/java/com/liyile/medication/exception/GlobalExceptionHandler.java`
  - 相关异常类型定义
- 实施任务：
  1. 为参数错误、未认证、无权限、服务器异常等错误类型设置对应 HTTP 状态码。
  2. 保持错误响应体结构统一，便于前端解析。
  3. 保留 `traceId`，便于日志追踪。
- 验收标准：
  - SQL 异常返回 500。
  - 未认证返回 401。
  - 权限不足返回 403。
  - 浏览器网络面板与业务错误类型一致。

### 问题 P1-03：前端对计划/事件/告警失败的错误展示不够可诊断

- 问题编号：`P1-03`
- 优先级：`P1`
- 现象：
  - 页面上大多只显示“请求失败”或“暂无数据”，无法区分是真空数据还是接口异常。
  - 用户和开发者都难以判断问题来源。
- 复现条件：
  - elder 首页或 caregiver 首页加载失败。
- 影响范围：
  - 首页状态理解
  - 排错效率
  - 真实演示稳定性
- 根因：
  - 前端错误拦截器和页面层未充分利用后端的 `detail`、`traceId`、`code`。
  - 页面空态与错误态表现不够分离。
- 修改目标：
  - 提升接口失败时的可诊断性和页面反馈准确性。
- 建议修改位置：
  - `vue-project/src/lib/http.ts`
  - `vue-project/src/utils/errorHandler.ts`
  - `vue-project/src/views/ElderDashboardView.vue`
  - `vue-project/src/views/CaregiverDashboardView.vue`
  - `vue-project/src/views/ChildDashboardView.vue`
- 实施任务：
  1. 前端错误对象保留并展示更明确的错误类型。
  2. 页面区分“无数据”和“加载失败”两种状态。
  3. 在开发环境中保留 `traceId` 与错误摘要，便于排查。
- 验收标准：
  - 当接口异常时，页面能提示“加载失败”而不是误显示“暂无数据”。
  - 开发环境中可快速定位失败接口和错误原因。

### 问题 P1-04：自动化测试未覆盖真实数据库链路

- 问题编号：`P1-04`
- 优先级：`P1`
- 现象：
  - `npm run build`、`mvn test`、`pytest` 全部通过，但真实 UI 链路在登录后立即失败。
- 复现条件：
  - 执行自动化测试后，实际启动服务并进入 UI。
- 影响范围：
  - 质量判断失真
  - 项目验收与答辩风险
- 根因：
  - 自动化测试主要覆盖静态构建、局部单元逻辑和算法接口，而未覆盖真实 DB 驱动的关键用户路径。
- 修改目标：
  - 在测试体系中补足真实链路验证，降低“测试全绿但系统不可用”的风险。
- 建议修改位置：
  - `docs-project/F-测试与验收计划.md`
  - `docs-project/H-项目完善与验证报告.md`
  - 测试脚本或验收说明文档
- 实施任务：
  1. 增加一轮依赖真实开发数据库的接口验收。
  2. 增加至少一轮基于 Playwright 或固定手工脚本的主链路 UI 验收。
  3. 把“数据库 schema 一致性检查”纳入回归清单。
- 验收标准：
  - 自动化测试之外，至少存在一份可复用的真实链路验收流程。
  - 回归报告能明确区分“静态测试通过”和“真实链路通过”。

## 5. 修改任务拆解

### 5.1 Spring 配置与安全

#### 模块：运行配置
- 修改目标：
  - 让开发环境按文档即可启动，登录链路默认可用。
- 重点位置：
  - `application.properties`
  - `application-dev.properties`
- 具体任务：
  1. 统一数据库连接参数和文档说明。
  2. 为开发环境配置合法 JWT 密钥。
  3. 检查 Redis 是否为当前链路必需，若非必需则明确降级策略。

#### 模块：认证与 token
- 修改目标：
  - 区分鉴权失败和业务查询失败。
- 重点位置：
  - `JwtTokenProvider`
  - `AuthController`
  - `AuthEntryPoint`
- 具体任务：
  1. 确保登录只受认证逻辑本身影响。
  2. 明确 `/auth/profile` 的职责边界。
  3. 优化 JWT 异常信息的可观测性。

#### 模块：异常处理
- 修改目标：
  - 统一错误语义与真实 HTTP 状态码。
- 重点位置：
  - `GlobalExceptionHandler`
- 具体任务：
  1. 设置标准 HTTP 状态码。
  2. 保留统一错误 JSON 结构。
  3. 确保前端能通过状态码做正确分流。

### 5.2 Spring 数据库与 schema 对齐

#### 模块：数据库基线
- 修改目标：
  - 建立唯一可信的开发库结构基线。
- 重点位置：
  - `docs/sql/schema.sql`
  - `docs/sql/seed.sql`
- 具体任务：
  1. 以实体类和当前业务接口需要的字段为准修订 schema。
  2. 确认所有表都能在初始化后支持 elder、caregiver、child 三类链路。
  3. 补齐种子数据与真实页面依赖字段。

#### 模块：实体与 Mapper
- 修改目标：
  - 实体字段、SQL 字段和真实表字段完全一致。
- 重点位置：
  - `entity/*`
  - 相关 Mapper
- 具体任务：
  1. 逐表清点字段。
  2. 确认是否存在历史字段、未落库字段和已废弃字段。
  3. 修正查询语句与真实表结构不一致的问题。

### 5.3 Vue 前端鉴权与错误处理

#### 模块：启动鉴权
- 修改目标：
  - 避免业务错误触发误登出。
- 重点位置：
  - `src/main.ts`
  - `src/services/authService.ts`
- 具体任务：
  1. 将 token 校验改为只处理鉴权问题。
  2. 将患者、计划等业务数据的失败留在页面层处理。

#### 模块：HTTP 层与错误展示
- 修改目标：
  - 让前端错误提示更准确。
- 重点位置：
  - `src/lib/http.ts`
  - `src/utils/errorHandler.ts`
- 具体任务：
  1. 根据真实 HTTP 状态码做前端分流。
  2. 区分空态和错误态。
  3. 开发模式下输出更有用的诊断信息。

### 5.4 Vue 页面级回归

#### elder 端
- 页面：
  - `elder/home`
  - `plans`
  - `detection`
  - `history`
  - `alerts`
- 修改目标：
  - 登录后核心页面都能正确显示真实数据。

#### caregiver / child 端
- 页面：
  - `caregiver/home`
  - `child/home`
  - `history`
  - `alerts`
- 修改目标：
  - 能查看关联患者数据，但不暴露 elder 专属编辑能力。

## 6. 验收与回归测试要求

### 6.1 环境验收
- 默认配置能否启动后端。
- 登录链路是否开箱可用。
- 数据库初始化脚本能否直接建立最小可运行数据集。

### 6.2 接口验收
- `/api/auth/login`
- `/api/auth/profile`
- `/api/schedules`
- `/api/intake-events`
- `/api/alerts`
- `/api/patients`

### 6.3 UI 验收
- `elder1` 登录后能进入 `/elder/home`。
- elder 首页能加载计划、事件、告警。
- `care1` 登录后能加载患者列表。
- caregiver 无法进入 elder 专属编辑页面。
- child 链路与 caregiver 同步成立。

### 6.4 数据一致性验收
- 实体字段与真实库结构一致。
- `schema.sql` 与真实开发库一致。
- `seed.sql` 可直接生成演示所需的最小数据集。

### 6.5 回归测试
- `npm run build`
- `mvn test`
- `pytest`
- 一轮 Playwright 或手工 UI 主链路测试

## 7. 风险与依赖项

- 当前最大风险不是单个接口 bug，而是运行配置、数据库结构和代码版本没有收敛为同一基线。
- 若不先完成 schema 对齐，后续修复 UI 或错误提示只能掩盖问题，不能真正恢复业务闭环。
- 若不拆开 `/auth/profile` 与 token 校验耦合关系，任何业务异常都可能再次表现为“登录失效”。
- 若不修正 HTTP 状态码，前端和测试工具仍会继续误判失败类型。

## 8. 整改完成检查表

| 编号 | 修改项 | 负责人 | 是否完成 | 验证方式 | 验证结果 | 备注 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 默认数据库连接可直接运行 | 待填写 | 否 | 启动后端并执行登录 | 待填写 |  |
| 2 | 默认 JWT 配置可稳定生成 token | 待填写 | 否 | 调用 `/api/auth/login` | 待填写 |  |
| 3 | 数据库 schema 与代码一致 | 待填写 | 否 | 对照实体和表结构检查 | 待填写 |  |
| 4 | elder 首页计划接口恢复正常 | 待填写 | 否 | 访问 `/api/schedules` 与页面联调 | 待填写 |  |
| 5 | 历史与告警接口恢复正常 | 待填写 | 否 | 访问 `/api/intake-events`、`/api/alerts` | 待填写 |  |
| 6 | caregiver 患者列表链路恢复正常 | 待填写 | 否 | 登录 `care1` 并进入首页 | 待填写 |  |
| 7 | 无效 token 才触发登出 | 待填写 | 否 | 人工构造业务 500 场景 | 待填写 |  |
| 8 | 后端错误返回正确 HTTP 状态码 | 待填写 | 否 | 抓包或浏览器网络面板 | 待填写 |  |
| 9 | 前端错误提示可诊断 | 待填写 | 否 | 页面异常态检查 | 待填写 |  |
| 10 | 真实 UI 主链路回归通过 | 待填写 | 否 | Playwright 或手工回归 | 待填写 |  |

## 9. 附录：关键证据与定位

### 9.1 配置与安全
- `springProject/src/main/resources/application.properties`
- `springProject/src/main/resources/application-dev.properties`
- `springProject/src/main/java/com/liyile/medication/security/JwtTokenProvider.java`
- `springProject/src/main/java/com/liyile/medication/controller/AuthController.java`
- `springProject/src/main/java/com/liyile/medication/exception/GlobalExceptionHandler.java`

### 9.2 数据库与实体
- `springProject/docs/sql/schema.sql`
- `springProject/docs/sql/seed.sql`
- `springProject/src/main/java/com/liyile/medication/entity/Schedule.java`
- `springProject/src/main/java/com/liyile/medication/entity/IntakeEvent.java`
- `springProject/src/main/java/com/liyile/medication/entity/Alert.java`
- `springProject/src/main/java/com/liyile/medication/entity/UserPatientRelation.java`

### 9.3 前端鉴权与错误链路
- `vue-project/src/main.ts`
- `vue-project/src/lib/http.ts`
- `vue-project/src/services/authService.ts`
- `vue-project/src/utils/errorHandler.ts`
- `vue-project/src/views/ElderDashboardView.vue`
- `vue-project/src/views/CaregiverDashboardView.vue`
- `vue-project/src/views/ChildDashboardView.vue`

### 9.4 本次测试中已确认的重点事实
- 自动化测试全部通过，但真实 UI 主链路未跑通。
- elder 登录后首页接口因缺字段失败。
- caregiver 登录后患者列表因关联表缺失失败。
- 前端会把 profile 失败误判为 token 无效并清会话。
- 后端异常场景存在 HTTP 200 与业务失败并存的问题。
