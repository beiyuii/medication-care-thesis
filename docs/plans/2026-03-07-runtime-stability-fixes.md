# Runtime Stability Fixes Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修复真实运行测试中暴露的鉴权、异常处理和会话因果链问题，打通最小可演示主链路。

**Architecture:** 后端优先修正开发环境 JWT 配置、`/auth/profile` 的职责边界，以及全局异常返回的 HTTP 语义；前端同步调整 token 校验与错误展示逻辑，只在明确的鉴权失败时清理会话。数据库层先对齐开发 schema 与初始化文档，避免真实库结构漂移继续阻断首页接口。

**Tech Stack:** Spring Boot, MyBatis-Plus, Vue 3, TypeScript, Axios, JUnit 5, Vitest

---

### Task 1: Spring 鉴权与异常处理测试

**Files:**
- Create: `springProject/src/test/java/com/liyile/medication/security/JwtTokenProviderTest.java`
- Create: `springProject/src/test/java/com/liyile/medication/exception/GlobalExceptionHandlerTest.java`
- Create: `springProject/src/test/java/com/liyile/medication/controller/AuthControllerTest.java`

**Step 1: Write the failing tests**
- 覆盖开发默认 JWT 密钥可生成并校验 token。
- 覆盖 `/api/auth/profile` 在患者查询失败时仍返回基础认证信息。
- 覆盖未认证、无权限、服务器异常分别返回 401、403、500。

**Step 2: Run tests to verify they fail**

Run: `cd /Users/beiyuii/Desktop/李怡蕾/毕设/springProject && mvn -Dtest=JwtTokenProviderTest,GlobalExceptionHandlerTest,AuthControllerTest test`

Expected: FAIL，提示当前行为与测试不符。

**Step 3: Write minimal implementation**
- 调整 `JwtTokenProvider` 的密钥解析策略。
- 调整 `AuthController#profile` 的返回逻辑。
- 调整 `GlobalExceptionHandler` 返回 `ResponseEntity` 并设置状态码。

**Step 4: Run tests to verify they pass**

Run: `cd /Users/beiyuii/Desktop/李怡蕾/毕设/springProject && mvn -Dtest=JwtTokenProviderTest,GlobalExceptionHandlerTest,AuthControllerTest test`

Expected: PASS

### Task 2: Vue 会话校验与错误处理测试

**Files:**
- Create: `vue-project/src/main.test.ts`
- Create: `vue-project/src/lib/http.test.ts`

**Step 1: Write the failing tests**
- 覆盖 `validateToken()` 只在明确 401/403 时清理会话。
- 覆盖后端 `detail`/`message`/`traceId` 被正确透传。

**Step 2: Run tests to verify they fail**

Run: `cd /Users/beiyuii/Desktop/李怡蕾/毕设/vue-project && npm test -- --run src/main.test.ts src/lib/http.test.ts`

Expected: FAIL

**Step 3: Write minimal implementation**
- 抽出 `validateStoredSession()` 供测试与启动复用。
- 调整 `http` 错误解析逻辑与鉴权失败判定。

**Step 4: Run tests to verify they pass**

Run: `cd /Users/beiyuii/Desktop/李怡蕾/毕设/vue-project && npm test -- --run src/main.test.ts src/lib/http.test.ts`

Expected: PASS

### Task 3: 开发配置与 schema 文档对齐

**Files:**
- Modify: `springProject/src/main/resources/application.properties`
- Modify: `springProject/src/main/resources/application-dev.properties`
- Modify: `springProject/docs/sql/schema.sql`
- Modify: `springProject/docs/sql/seed.sql`

**Step 1: Write/update verification**
- 核对开发环境密钥、数据库默认值与文档一致。
- 核对 `user_patient_relation`、`medicine_name`、`confirmed_by` 等字段在 schema 和 seed 中完整可初始化。

**Step 2: Implement**
- 为开发环境提供稳定的 JWT 默认值与环境变量注释。
- 补充 schema/seed 注释，确保初始化路径明确。

**Step 3: Verify**

Run:
- `cd /Users/beiyuii/Desktop/李怡蕾/毕设/springProject && mvn test`
- `cd /Users/beiyuii/Desktop/李怡蕾/毕设/vue-project && npm run build`

Expected: PASS

### Task 4: 回归验证

**Files:**
- No new files expected

**Step 1: Run full regression**

Run:
- `cd /Users/beiyuii/Desktop/李怡蕾/毕设/springProject && mvn test`
- `cd /Users/beiyuii/Desktop/李怡蕾/毕设/vue-project && npm test -- --run`
- `cd /Users/beiyuii/Desktop/李怡蕾/毕设/vue-project && npm run build`
- `cd /Users/beiyuii/Desktop/李怡蕾/毕设/flask-project && pytest`

**Step 2: Validate main flow**
- elder 合法 token 刷新页面不误登出。
- 业务 500 不触发清会话。
- 错误信息包含可定位的 `traceId` 或 `detail`。

