# 数据库同步说明（给另一台电脑上的 AI）

这个仓库最近改过 `reminder_instances`、`detection_jobs`、`intake_events` 等表结构。  
如果你在另一台机器上 `git pull` 了最新代码，但本地 MySQL 还是旧库，前后端会出现这些典型问题：

- 老人提交后状态异常变成 `review_timeout`
- 护工驳回时出现 `Duplicate entry ... for key 'reminder_instances.uk_patient_schedule_date'`
- 新字段查询报错，例如 `Unknown column ...`
- 检测任务、历史详情、审核链路和重试链路显示异常

这不是代码没拉全，而是数据库没有同步到当前结构。

## 先看哪里拿数据库连接

默认连接配置在：

- [application.properties](/Users/beiyuii/Desktop/李怡蕾/毕设/springProject/src/main/resources/application.properties)

默认本地开发值是：

- 数据库：`lyl`
- 用户：`root`
- 密码：`btty123`

如果本机通过环境变量覆盖了 `DB_URL / DB_USER / DB_PASSWORD`，优先用环境变量。

## AI 应执行的步骤

### 1. 执行数据库修复脚本

不要手工一条条改表，优先直接执行：

```bash
mysql -u${DB_USER:-root} -p${DB_PASSWORD:-btty123} lyl < springProject/docs/sql/dev-repair.sql
```

如果本机没有把密码放进环境变量，也可以直接按本地默认值执行：

```bash
mysql -uroot -pbtty123 lyl < springProject/docs/sql/dev-repair.sql
```

### 2. 校验 `reminder_instances` 的唯一索引

这是这次最关键的点。  
当前业务允许“同一患者、同一天、同一计划”存在多条重试实例，所以旧索引必须替换掉。

执行：

```bash
mysql -u${DB_USER:-root} -p${DB_PASSWORD:-btty123} lyl -e "SHOW INDEX FROM reminder_instances;"
```

期望结果：

- 存在：`uk_patient_schedule_date_retry`
- 且字段顺序是：
  - `patient_id`
  - `schedule_id`
  - `scheduled_date`
  - `retry_count`
- 不应再存在：`uk_patient_schedule_date`

如果你还看见旧索引，说明数据库没有真正迁移成功。

### 3. 校验关键字段是否存在

执行：

```bash
mysql -u${DB_USER:-root} -p${DB_PASSWORD:-btty123} lyl -e "DESCRIBE reminder_instances; DESCRIBE detection_jobs; DESCRIBE intake_events;"
```

至少应看到这些字段：

`reminder_instances`

- `detection_status`
- `parent_instance_id`
- `retry_count`
- `review_deadline`
- `late_minutes`
- `reviewed_by`
- `reviewed_at`
- `review_reason`

`detection_jobs`

- `target_confidence`
- `action_confidence`
- `final_confidence`
- `reason_code`
- `reason_text`
- `risk_tag`
- `llm_provider`
- `llm_model`
- `llm_frame_count`
- `llm_decision_source`
- `frame_summary`

`intake_events`

- `event_type`
- `detection_status`
- `review_decision`
- `review_reason`
- `video_url`
- `reminder_instance_id`
- `detection_job_id`

### 4. 重启 Spring 后端

数据库修完后，要重启后端让新代码和新表结构一起生效：

```bash
cd springProject
mvn -DskipTests spring-boot:run
```

或者使用你本机已有的启动方式。

### 5. 做一次主链路验证

至少验证这三条：

1. 老人提交服药记录后，实例状态应变成 `waiting_caregiver`
2. 护工首页 `/api/dashboard/caregiver` 能看到 `pendingReviewInstances`
3. 护工驳回后，数据库里会新增一条：
   - `parent_instance_id = 原实例 id`
   - `retry_count = 1`
   - `status = not_submitted`

可用 SQL：

```bash
mysql -u${DB_USER:-root} -p${DB_PASSWORD:-btty123} lyl -e "SELECT id, patient_id, schedule_id, scheduled_date, status, parent_instance_id, retry_count FROM reminder_instances ORDER BY id DESC LIMIT 20;"
```

## 本次已知修复点

如果你在代码里排查逻辑，重点看这些文件：

- [ReminderInstanceService.java](/Users/beiyuii/Desktop/李怡蕾/毕设/springProject/src/main/java/com/liyile/medication/service/ReminderInstanceService.java)
- [dev-repair.sql](/Users/beiyuii/Desktop/李怡蕾/毕设/springProject/docs/sql/dev-repair.sql)
- [schema.sql](/Users/beiyuii/Desktop/李怡蕾/毕设/springProject/docs/sql/schema.sql)
- [DetectionRoomView.vue](/Users/beiyuii/Desktop/李怡蕾/毕设/vue-project/src/views/DetectionRoomView.vue)

这几处修过的点包括：

- `review_timeout` 不再重复刷超时事件
- 审核截止时间修正到当天 `23:59:59`
- 老人端取日期改为本地日期，不再误拿 UTC 前一天
- 护工驳回时支持创建重试实例
- 后端对重试实例插入增加了重复键兜底

## 如果另一台机器上仍然报错

优先检查这几类：

### 1. 还是旧唯一索引

报错类似：

- `Duplicate entry ... for key 'reminder_instances.uk_patient_schedule_date'`

结论：

- 数据库没迁好，继续看第 2 步的索引校验

### 2. 缺字段

报错类似：

- `Unknown column 'detection_status' in 'field list'`

结论：

- `dev-repair.sql` 没执行成功，或者连错数据库了

### 3. 提交后马上超时

结论：

- 很可能仍在跑旧后端进程，数据库虽然修了，但 Spring 没重启

## AI 执行建议

如果你是另一台机器上的 AI，请按这个顺序处理：

1. 先执行 `springProject/docs/sql/dev-repair.sql`
2. 再校验 `reminder_instances` 索引
3. 再重启 Spring
4. 再验证“老人提交 -> 护工待审核 -> 护工驳回 -> 新重试实例创建”这条链

不要直接假设“代码拉下来了数据库就自动对齐”。
