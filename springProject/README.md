# 后端框架搭建与运行文档（简版）

本工程为“基于目标检测的老年人用药提醒与管理系统”后端服务，采用 Spring Boot 3 + MyBatis-Plus + MySQL + Redis + JWT。

> 说明：该后端仅面向本机开发调试场景，不会部署上线，因此默认的跨域策略较为宽松。

## 一、环境要求
- JDK 17
- Maven 3.9+
- MySQL 8.x（数据库名：medication_db）
- Redis 6/7

## 二、配置说明
配置文件位于 `src/main/resources`：
- application.yml：通用配置
- application-dev.yml：开发环境（已填入提供的 MySQL/Redis 主机与账号）
- application-prod.yml：生产环境，使用环境变量注入（DB_URL/DB_USER/DB_PASSWORD/REDIS_HOST/REDIS_PASSWORD/JWT_SECRET）

JWT 有效期默认 1 天；不启用黑名单刷新机制，可在配置文件中调整。

## 三、启动步骤（开发环境）
1. 在 MySQL 上创建数据库与数据表（见 docs/sql/schema.sql）。
2. 插入预设数据（见 docs/sql/seed.sql，默认账号 `elder1/care1/child1` 的密码均为 123456，已使用 BCrypt 加密）。
3. 在工程根目录执行：`mvn spring-boot:run -Dspring-boot.run.profiles=dev`。
4. 基础 API：
   - POST /auth/register
   - POST /auth/login
   - GET /auth/profile
   - GET /schedules?patientId=... 等（详见控制器注释）。

## 四、日志与上传目录
- 日志路径：`logs/app.log`（Logback 配置）
- 图片上传目录：`./logs`（相对于项目根，可在配置中修改）

## 五、代码规范
- 集成 Spotless、Checkstyle、PMD（阿里 P3C 规则）。
- 类/方法/变量采用驼峰命名法；常量使用大写加下划线。
- 每个类的方法和变量均含注释（示例类已提供）。

## 六、常见问题
- 如无法连接数据库/Redis，请检查主机与账号密码，以及防火墙策略。
- 如 JWT 验证失败，请确认 `JWT_SECRET` 配置一致。
