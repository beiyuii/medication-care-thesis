# 测试实施指南 - 老年人用药提醒与管理系统

## 一、测试准备

### 1.1 环境准备
- **开发环境**: JDK 17+, Maven 3.9+, MySQL 8.0
- **测试工具**: JUnit 5, Mockito, MockMvc, Postman/Apifox
- **启动后端服务**: `mvn spring-boot:run`
- **访问Swagger文档**: http://localhost:8080/swagger-ui.html

### 1.2 测试数据准备
执行以下SQL脚本初始化测试数据：
```bash
mysql -u root -p medication < docs/sql/schema.sql
mysql -u root -p medication < docs/sql/seed.sql
```

## 二、单元测试方向

### 2.1 Controller层测试（优先级：P0）

#### 认证模块测试（AuthController）
**测试用例清单**：
1. **用户注册**
   - ✅ 正常注册（elder/caregiver/child三种角色）
   - ✅ 用户名重复（返回422错误）
   - ✅ 参数校验失败（用户名为空、密码为空）
   - ✅ 验证返回的JWT token有效性

2. **用户登录**
   - ✅ 正常登录（BCrypt加密密码）
   - ✅ 明文密码登录（开发模式）
   - ✅ 用户名不存在（返回401错误）
   - ✅ 密码错误（返回401错误）
   - ✅ 验证返回的角色信息正确

3. **个人信息查询**
   - ✅ 有效token查询（返回用户名和角色）
   - ✅ Token过期（返回401错误）
   - ✅ Token格式错误（缺少Bearer前缀）
   - ✅ Token伪造（返回401错误）

**测试代码示例**：
```java
@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testRegisterSuccess() throws Exception {
        String requestBody = "{\"username\":\"elder1\",\"password\":\"123456\",\"role\":\"elder\"}";
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.token").exists())
            .andExpect(jsonPath("$.data.role").value("elder"));
    }
}
```

#### 用药计划模块测试（ScheduleController）
**测试用例清单**：
1. **计划查询**
   - ✅ 查询存在的患者计划（返回列表）
   - ✅ 查询不存在的患者（返回空列表）
   - ✅ patientId为空（返回422错误）

2. **计划创建**
   - ✅ 正常创建（四类药品：PILL/BLISTER/BOTTLE/BOX）
   - ✅ 必填字段缺失（返回422错误）
   - ✅ 药品类型枚举错误（返回422错误）
   - ✅ 时间窗格式验证（winStart < winEnd）

3. **计划更新**
   - ✅ 正常更新（返回影响行数1）
   - ✅ 计划ID不存在（返回影响行数0）
   - ✅ 部分字段更新（PATCH语义）

4. **启停切换**
   - ✅ enabled → disabled切换
   - ✅ disabled → enabled切换
   - ✅ 计划ID不存在（返回404错误）

#### 服药事件模块测试（IntakeEventController）
**测试用例清单**：
1. **事件创建**
   - ✅ 创建疑似事件（status=suspected）
   - ✅ 创建已确认事件（status=confirmed）
   - ✅ 创建异常事件（status=abnormal）
   - ✅ targetsJson字段序列化验证
   - ✅ action字段（true/false）验证

2. **事件查询**
   - ✅ 按患者查询全部事件
   - ✅ 按时间范围过滤（当前未实现，待完善）
   - ✅ patientId为空（返回422错误）

#### 日志图片模块测试（LogImageController）
**测试用例清单**：
1. **图片上传**
   - ✅ 正常上传jpg/png/webp格式
   - ✅ 上传非图片文件（返回422错误）
   - ✅ 上传超大文件（>10MB，返回422错误）
   - ✅ 验证文件存储路径正确
   - ✅ 验证返回的URL可访问

2. **图片查询**
   - ✅ 按eventId查询（返回图片列表）
   - ✅ eventId不存在（返回空列表）
   - ✅ eventId为空（返回422错误）

#### 异常告警模块测试（AlertController）
**测试用例清单**：
1. **告警查询**
   - ✅ 查询存在的患者告警（返回列表）
   - ✅ 查询不存在的患者（返回空列表）
   - ✅ 按告警类型过滤（待实现功能）

#### 统计报表模块测试（ReportController）
**测试用例清单**：
1. **统计摘要查询**
   - ✅ 按日统计（range=day）
   - ✅ 按周统计（range=week）
   - ✅ 按月统计（range=month）
   - ✅ 全部统计（range=all，默认）
   - ⚠️ 注意：当前为占位实现，需完善统计逻辑

### 2.2 Service层测试（优先级：P1）

#### UserService测试
**测试用例清单**：
1. ✅ findByUsername - 查询存在/不存在的用户
2. ✅ save - 创建新用户
3. ✅ 用户名唯一性校验
4. ✅ 角色权限验证（elder/caregiver/child）

### 2.3 安全模块测试（优先级：P0）

#### JWT Token测试
**测试用例清单**：
1. ✅ generateToken - 生成有效token
2. ✅ getUsername - 从token提取用户名
3. ✅ getRole - 从token提取角色
4. ✅ validateToken - token有效期校验
5. ✅ Token刷新机制（如已实现）

#### JwtAuthFilter测试
**测试用例清单**：
1. ✅ 白名单路径放行（/auth/*, /health, /swagger-ui/**）
2. ✅ 有效token通过认证
3. ✅ 无效token被拦截（返回401）
4. ✅ 缺少Authorization头部（返回401）

## 三、集成测试方向

### 3.1 完整业务流程测试（优先级：P0）

#### 用药提醒与确认流程
**测试步骤**：
1. 注册/登录elder用户 → 获取token
2. 创建用药计划 → POST /schedules
   ```json
   {
     "patientId": 1,
     "type": "PILL",
     "dose": "1片",
     "freq": "每日3次",
     "winStart": "08:00",
     "winEnd": "08:30",
     "period": "持续",
     "status": "enabled"
   }
   ```
3. 模拟到达时间窗 → 前端触发提醒
4. 摄像头检测生成疑似事件 → POST /intake-events
   ```json
   {
     "patientId": 1,
     "scheduleId": 1,
     "ts": "1699999999999",
     "status": "suspected",
     "action": true,
     "targetsJson": "{\"PILL\": 0.95}",
     "imgUrl": ""
   }
   ```
5. 上传关键帧图片 → POST /logs/images
6. 用户手动确认 → PATCH /intake-events/{id} (status=confirmed)
7. 查询历史记录 → GET /intake-events?patientId=1

**预期结果**：
- ✅ 计划创建成功，status=enabled
- ✅ 事件创建成功，status=suspected
- ✅ 图片上传成功，返回URL
- ✅ 事件状态更新为confirmed
- ✅ 历史记录包含该事件

#### 异常处理流程
**测试步骤**：
1. 创建用药计划（同上）
2. 时间窗结束仍未确认 → POST /intake-events (status=abnormal)
3. 创建告警记录 → POST /alerts（需实现）
4. 管理端查看异常 → GET /alerts?patientId=1

**预期结果**：
- ✅ 异常事件记录创建成功
- ✅ 告警列表包含该异常
- ✅ 护工/子女端可查看

#### 多角色协作流程
**测试步骤**：
1. elder用户创建计划（需要写权限）
2. caregiver用户查看计划（只读权限）
   - ✅ GET /schedules?patientId=1 - 成功
   - ❌ POST /schedules - 返回403错误
3. child用户查看统计（只读权限）
   - ✅ GET /reports/summary?patientId=1 - 成功
   - ❌ POST /schedules - 返回403错误

### 3.2 权限与鉴权测试（优先级：P1）

#### 角色权限矩阵测试
**测试矩阵**：

| 接口 | elder | caregiver | child | 未登录 |
|------|-------|-----------|-------|--------|
| POST /schedules | ✅ | ❌ 403 | ❌ 403 | ❌ 401 |
| GET /schedules | ✅ | ✅ | ✅ | ❌ 401 |
| PATCH /schedules/{id} | ✅ | ❌ 403 | ❌ 403 | ❌ 401 |
| POST /intake-events | ✅ | ❌ 403 | ❌ 403 | ❌ 401 |
| GET /intake-events | ✅ | ✅ | ✅ | ❌ 401 |
| GET /alerts | ✅ | ✅ | ✅ | ❌ 401 |
| POST /logs/images | ✅ | ❌ 403 | ❌ 403 | ❌ 401 |
| GET /health | ✅ | ✅ | ✅ | ✅ |

**注意**：当前权限控制需在SecurityConfig中配置，建议添加方法级权限注解（@PreAuthorize）

#### 跨用户数据隔离测试
**测试步骤**：
1. 创建两个elder用户（elder1、elder2）
2. elder1创建计划 → scheduleId=1 (patientId=1)
3. elder2创建计划 → scheduleId=2 (patientId=2)
4. elder1查询计划 → GET /schedules?patientId=2
5. **预期**：应返回403错误或空列表（需实现数据隔离逻辑）

### 3.3 数据库集成测试（优先级：P2）

#### 事务完整性测试
1. **事件创建与图片上传的事务一致性**
   - 图片上传失败时，事件应回滚
   - 事件创建失败时，图片应清理

2. **计划删除时关联数据的级联处理**
   - 删除计划时，关联的事件如何处理（软删除/级联删除）

#### 查询性能测试
1. **大量历史事件的分页查询**
   - 插入10万条事件记录
   - 测试查询响应时间 < 500ms
   - 验证分页参数（pageSize, pageNum）

2. **复杂条件的统计查询优化**
   - 按日/周/月维度统计
   - 多表关联查询优化
   - 索引优化验证

## 四、接口测试方向（Postman/Apifox）

### 4.1 测试集合构建

#### 环境变量配置
```json
{
  "baseUrl": "http://localhost:8080",
  "token": "",
  "elderToken": "",
  "caregiverToken": "",
  "patientId": "1"
}
```

#### 前置脚本（自动获取token）
```javascript
// 登录接口的Tests脚本
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.environment.set("token", jsonData.data.token);
    pm.environment.set("role", jsonData.data.role);
}

// 其他接口的Pre-request Script
pm.request.headers.add({
    key: 'Authorization',
    value: 'Bearer ' + pm.environment.get("token")
});
```

### 4.2 关键测试场景

#### 正常流程（Happy Path）
1. ✅ 注册 → 登录 → 创建计划 → 创建事件 → 上传图片 → 查询历史

#### 边界条件测试
1. ✅ patientId = 0（最小值）
2. ✅ patientId = Long.MAX_VALUE（最大值）
3. ✅ dose字段包含特殊字符（<script>alert(1)</script>）
4. ✅ 时间窗格式错误（winStart="25:00"）

#### 异常情况测试
1. ✅ 404错误：查询不存在的计划ID
2. ✅ 422错误：参数校验失败
3. ✅ 401错误：未登录访问受保护资源
4. ✅ 403错误：权限不足

#### 并发测试
1. ✅ 10个用户同时创建计划
2. ✅ 同一用户同时更新同一计划（乐观锁测试）

### 4.3 OpenAPI文档验证

**测试步骤**：
1. 启动项目 → 访问 http://localhost:8080/swagger-ui.html
2. 验证所有Controller的@Tag注解显示正确
3. 验证每个接口的@Operation注解（summary、description）
4. 验证参数的@Parameter注解（example值）
5. 使用"Try it out"功能测试每个接口
6. 验证响应示例与实际返回一致

## 五、性能测试方向

### 5.1 接口性能测试（JMeter/Gatling）

#### 响应时间指标
| 接口 | 平均响应时间 | P95响应时间 | P99响应时间 |
|------|--------------|-------------|-------------|
| POST /auth/login | < 100ms | < 200ms | < 300ms |
| GET /schedules | < 150ms | < 300ms | < 500ms |
| POST /intake-events | < 200ms | < 400ms | < 600ms |
| POST /logs/images | < 800ms | < 1000ms | < 1500ms |

#### 并发测试场景
**场景1：登录峰值测试**
- 并发用户数：100
- 持续时间：60秒
- 目标TPS：50+
- 成功率：> 99%

**场景2：多用户同时服药**
- 并发用户数：50
- 模拟5个老年人同时服药（各10次）
- 测试事件创建和图片上传的吞吐量
- 目标TPS：20+

### 5.2 数据库性能测试

#### 大量数据查询
1. **10万条事件记录查询**
   ```sql
   SELECT * FROM intake_events WHERE patient_id = 1 
   ORDER BY ts DESC LIMIT 20;
   ```
   - 响应时间：< 100ms
   - 需要添加索引：`idx_patient_ts`

2. **统计查询优化**
   ```sql
   SELECT DATE(ts) as date, COUNT(*) as count, 
          SUM(CASE WHEN status='confirmed' THEN 1 ELSE 0 END) as confirmed
   FROM intake_events 
   WHERE patient_id = 1 AND ts >= '2024-01-01'
   GROUP BY DATE(ts);
   ```
   - 响应时间：< 500ms

#### 建议添加的索引
```sql
-- 患者相关查询优化
CREATE INDEX idx_patient_id ON schedules(patient_id);
CREATE INDEX idx_patient_ts ON intake_events(patient_id, ts DESC);
CREATE INDEX idx_event_id ON log_images(event_id);

-- 复合索引优化统计查询
CREATE INDEX idx_patient_status_ts ON intake_events(patient_id, status, ts);
```

## 六、前后端联调测试方向

### 6.1 前端集成测试

#### 摄像头检测联调
**测试步骤**：
1. 前端启动ONNX模型推理
2. 检测到药品 → 生成targetsJson
   ```json
   {
     "PILL": 0.95,
     "BLISTER": 0.88
   }
   ```
3. 检测到吃药动作 → action=true
4. POST /intake-events创建疑似事件
5. 截取关键帧 → Blob转File对象
6. POST /logs/images上传图片
7. 前端显示事件状态和图片缩略图

**预期结果**：
- ✅ targetsJson正确序列化
- ✅ 图片上传成功且URL可访问
- ✅ 事件状态实时更新

#### 实时状态更新
**方案1：轮询**
```javascript
setInterval(() => {
  fetch('/intake-events?patientId=1')
    .then(res => res.json())
    .then(data => updateUI(data));
}, 5000); // 每5秒轮询
```

**方案2：WebSocket（建议实现）**
- 服务端推送事件状态变更
- 减少不必要的HTTP请求
- 提升用户体验

### 6.2 CORS与跨域测试

#### 前端本地开发环境测试
**配置验证**：
```java
// CorsConfig.java
@Bean
public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin("http://localhost:5173"); // Vite开发服务器
    config.addAllowedOrigin("http://localhost:3000"); // React开发服务器
    config.addAllowedMethod("*");
    config.addAllowedHeader("*");
    config.setAllowCredentials(true);
    // ...
}
```

**测试步骤**：
1. 前端运行在 http://localhost:5173
2. 后端运行在 http://localhost:8080
3. 前端发起跨域请求
4. 验证响应头包含：
   - `Access-Control-Allow-Origin: http://localhost:5173`
   - `Access-Control-Allow-Credentials: true`

## 七、安全测试方向

### 7.1 认证安全测试

#### Token安全性测试
1. **Token伪造攻击**
   - 尝试修改token中的用户名/角色
   - 验证签名校验有效
   - **预期**：返回401 Unauthorized

2. **Token过期测试**
   - 等待token过期（默认24小时）
   - 使用过期token访问接口
   - **预期**：返回401 Unauthorized

3. **密码加密存储验证**
   - 检查数据库pwd_hash字段
   - 验证使用BCrypt加密（$2a/$2b/$2y前缀）
   - 明文密码不应出现在数据库

### 7.2 文件上传安全测试

#### 恶意文件上传测试
1. **脚本注入测试**
   - 上传jsp/php/html文件
   - **预期**：返回422错误，文件类型不支持

2. **DOS攻击测试**
   - 上传超大文件（>100MB）
   - **预期**：返回413错误，文件过大

3. **文件路径遍历攻击**
   - 文件名包含`../`或`..\\`
   - **预期**：文件名被过滤或返回400错误

#### 建议的安全增强
```java
// FileStorageUtil.java
public String save(MultipartFile file) {
    // 1. 文件类型白名单
    List<String> allowedTypes = Arrays.asList("image/jpeg", "image/png", "image/webp");
    if (!allowedTypes.contains(file.getContentType())) {
        throw new IllegalArgumentException("不支持的文件类型");
    }
    
    // 2. 文件大小限制
    if (file.getSize() > 10 * 1024 * 1024) { // 10MB
        throw new IllegalArgumentException("文件过大");
    }
    
    // 3. 文件名过滤
    String originalName = file.getOriginalFilename();
    String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "");
    
    // 4. 生成唯一文件名
    String fileName = UUID.randomUUID().toString() + "_" + safeName;
    
    // ...
}
```

## 八、兼容性与环境测试

### 8.1 数据库兼容性

#### MySQL版本测试
1. **MySQL 5.7测试**
   - 执行schema.sql和seed.sql
   - 运行全部单元测试
   - 验证日期时间函数兼容性

2. **MySQL 8.0测试**
   - 验证新特性（如窗口函数）
   - 字符集测试（utf8mb4）
   - 时区处理测试

#### 字符集测试
```sql
-- 验证中文支持
INSERT INTO users (username, pwd_hash, role) 
VALUES ('张三', 'hash123', 'elder');

-- 验证emoji支持（需utf8mb4）
INSERT INTO schedules (patient_id, type, dose) 
VALUES (1, 'PILL', '💊 每日一片');
```

### 8.2 JDK版本测试

#### Java 17兼容性验证
- 验证record类型（如用于DTO）
- 验证sealed classes（如用于权限模型）
- 验证pattern matching（如用于switch表达式）

#### Java 21兼容性验证
- 验证virtual threads（如用于高并发场景）
- 验证string templates
- 性能对比测试

## 九、测试执行计划

### 9.1 测试优先级

**P0（阻断级）- 第1周执行**
- ✅ 认证模块（注册、登录、token验证）
- ✅ 核心业务流程（计划→事件→确认）
- ✅ 安全模块（JWT、权限控制）

**P1（重要级）- 第2周执行**
- ✅ 权限控制测试（角色权限矩阵）
- ✅ 数据隔离测试
- ✅ 文件上传测试
- ✅ Service层测试

**P2（一般级）- 第3周执行**
- ✅ 统计报表测试
- ✅ 性能测试
- ✅ 边界条件测试
- ✅ 兼容性测试

### 9.2 测试工具配置

#### JUnit 5 + Mockito配置
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

#### JaCoCo代码覆盖率配置
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**目标**：代码覆盖率 ≥ 70%

### 9.3 缺陷管理

#### 缺陷分级
- **阻断**：核心功能无法使用（如登录失败）
- **重要**：功能异常但有替代方案（如统计数据不准确）
- **一般**：界面问题、提示语错误

#### 缺陷报告模板
```markdown
**标题**：[模块名] 简短描述
**优先级**：阻断/重要/一般
**环境**：Java 17, MySQL 8.0, macOS 14
**重现步骤**：
1. 登录elder用户
2. 创建用药计划
3. ...
**预期结果**：...
**实际结果**：...
**截图/日志**：...
```

## 十、持续集成（CI/CD）

### 10.1 GitHub Actions配置

```yaml
# .github/workflows/test.yml
name: Test
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run tests
        run: mvn clean test
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
```

### 10.2 测试自动化

**每次提交自动执行**：
- ✅ 单元测试（Controller + Service）
- ✅ 代码覆盖率检查（≥70%）
- ✅ Linter检查（Checkstyle/SpotBugs）

**每日定时执行**：
- ✅ 集成测试
- ✅ 性能测试
- ✅ 安全扫描（OWASP Dependency Check）

## 十一、验收标准

### 11.1 功能验收

✅ **认证模块**
- 注册、登录、个人信息查询功能正常
- JWT token有效期24小时
- 三种角色权限正确

✅ **用药计划模块**
- 支持四类药品（PILL/BLISTER/BOTTLE/BOX）
- 计划启停功能正常
- 时间窗设置正确

✅ **服药事件模块**
- 支持三种状态（suspected/confirmed/abnormal）
- targetsJson正确序列化
- 手动确认功能正常

✅ **日志图片模块**
- 支持jpg/png/webp格式
- 文件大小限制≤10MB
- 图片URL可访问

✅ **异常告警模块**
- 超时未确认自动创建告警
- 护工/子女可查看

✅ **统计报表模块**
- 按日/周/月维度统计（待完善实现）

### 11.2 性能验收

✅ **响应时间**
- 接口平均响应时间 ≤ 200ms
- P95响应时间 ≤ 500ms
- 图片上传 ≤ 1s

✅ **并发能力**
- 支持100并发用户登录
- 50并发用户同时创建事件
- TPS ≥ 50

✅ **数据容量**
- 支持10万+事件记录查询
- 查询响应时间 ≤ 500ms

### 11.3 安全验收

✅ **认证安全**
- Token不可伪造
- Token过期自动失效
- 密码BCrypt加密存储

✅ **权限控制**
- elder可创建/编辑计划
- caregiver/child只能查看
- 未登录用户无法访问（除白名单）

✅ **文件上传安全**
- 文件类型白名单
- 文件大小限制
- 文件名过滤

## 十二、总结

### 12.1 已完成工作

✅ **接口注释完善**
- 所有Controller添加OpenAPI 3.0注解
- 所有方法添加完整JavaDoc注释
- Swagger文档可正常访问

✅ **测试方向规划**
- 单元测试方向（Controller + Service + Security）
- 集成测试方向（业务流程 + 权限 + 数据库）
- 接口测试方向（Postman + Swagger）
- 性能测试方向（JMeter + 数据库优化）
- 前后端联调方向（摄像头 + 实时状态）
- 安全测试方向（认证 + 文件上传）

### 12.2 待完善功能

⚠️ **高优先级**
1. 统计报表实际逻辑实现（当前为占位）
2. 时间范围过滤逻辑（IntakeEventController）
3. 方法级权限控制（@PreAuthorize注解）
4. 数据隔离逻辑（用户只能访问自己的数据）

⚠️ **中优先级**
1. 计划删除接口（DELETE /schedules/{id}）
2. 事件更新接口（PATCH /intake-events/{id}，用于手动确认）
3. WebSocket实时推送（替代轮询）
4. 文件上传安全增强（类型白名单、大小限制）

⚠️ **低优先级**
1. 分页查询支持（PageHelper集成）
2. 缓存优化（Redis集成）
3. 日志审计（操作日志记录）
4. 导出CSV功能（报表导出）

### 12.3 测试开始指南

**立即开始测试的步骤**：

1. **启动项目**
   ```bash
   cd /Users/beiyuii/Desktop/李怡蕾/毕设/springProject
   mvn spring-boot:run
   ```

2. **访问Swagger文档**
   - 打开浏览器：http://localhost:8080/swagger-ui.html
   - 查看所有接口文档
   - 使用"Try it out"测试接口

3. **使用Postman测试**
   - 导入OpenAPI规范：http://localhost:8080/v3/api-docs
   - 配置环境变量（baseUrl, token）
   - 按照本文档的测试用例清单逐个测试

4. **运行单元测试**
   ```bash
   mvn test
   mvn jacoco:report  # 查看覆盖率
   open target/site/jacoco/index.html
   ```

5. **查看测试报告**
   - JUnit测试报告：target/surefire-reports/
   - JaCoCo覆盖率报告：target/site/jacoco/

---

**文档版本**: 1.0  
**创建日期**: 2025-11-14  
**维护者**: Liyile  
**下次更新**: 待测试执行后补充实际测试结果

