-- 开发库修复脚本
-- 用途：将已经初始化过的旧版 lyl 数据库补齐到当前代码所需的字段结构。
USE lyl;

SET @schema_name = 'lyl';

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'patients' AND column_name = 'age') = 0,
  "ALTER TABLE patients ADD COLUMN age INT COMMENT '年龄' AFTER name",
  "SELECT 'skip patients.age'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'patients' AND column_name = 'phone') = 0,
  "ALTER TABLE patients ADD COLUMN phone VARCHAR(20) COMMENT '联系电话' AFTER age",
  "SELECT 'skip patients.phone'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'schedules' AND column_name = 'medicine_name') = 0,
  "ALTER TABLE schedules ADD COLUMN medicine_name VARCHAR(100) COMMENT '药品名称' AFTER patient_id",
  "SELECT 'skip schedules.medicine_name'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'intake_events' AND column_name = 'confirmed_by') = 0,
  "ALTER TABLE intake_events ADD COLUMN confirmed_by VARCHAR(50) COMMENT '确认人' AFTER status",
  "SELECT 'skip intake_events.confirmed_by'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'intake_events' AND column_name = 'confirmed_at') = 0,
  "ALTER TABLE intake_events ADD COLUMN confirmed_at TIMESTAMP NULL COMMENT '确认时间' AFTER confirmed_by",
  "SELECT 'skip intake_events.confirmed_at'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'intake_events' AND column_name = 'reminder_instance_id') = 0,
  "ALTER TABLE intake_events ADD COLUMN reminder_instance_id BIGINT NULL COMMENT '关联提醒实例 ID' AFTER schedule_id",
  "SELECT 'skip intake_events.reminder_instance_id'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'intake_events' AND column_name = 'detection_job_id') = 0,
  "ALTER TABLE intake_events ADD COLUMN detection_job_id BIGINT NULL COMMENT '关联检测任务 ID' AFTER reminder_instance_id",
  "SELECT 'skip intake_events.detection_job_id'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'intake_events' AND column_name = 'video_url') = 0,
  "ALTER TABLE intake_events ADD COLUMN video_url VARCHAR(512) NULL COMMENT '检测原始录像 URL' AFTER img_url",
  "SELECT 'skip intake_events.video_url'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'alerts' AND column_name = 'title') = 0,
  "ALTER TABLE alerts ADD COLUMN title VARCHAR(200) COMMENT '告警标题' AFTER patient_id",
  "SELECT 'skip alerts.title'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'alerts' AND column_name = 'reminder_instance_id') = 0,
  "ALTER TABLE alerts ADD COLUMN reminder_instance_id BIGINT NULL COMMENT '关联提醒实例 ID' AFTER patient_id",
  "SELECT 'skip alerts.reminder_instance_id'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'alerts' AND column_name = 'detection_job_id') = 0,
  "ALTER TABLE alerts ADD COLUMN detection_job_id BIGINT NULL COMMENT '关联检测任务 ID' AFTER reminder_instance_id",
  "SELECT 'skip alerts.detection_job_id'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'alerts' AND column_name = 'description') = 0,
  "ALTER TABLE alerts ADD COLUMN description TEXT COMMENT '告警描述' AFTER title",
  "SELECT 'skip alerts.description'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'alerts' AND column_name = 'severity') = 0,
  "ALTER TABLE alerts ADD COLUMN severity VARCHAR(20) DEFAULT 'medium' COMMENT '严重程度：high/medium/low' AFTER description",
  "SELECT 'skip alerts.severity'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'alerts' AND column_name = 'resolved_at') = 0,
  "ALTER TABLE alerts ADD COLUMN resolved_at TIMESTAMP NULL COMMENT '处理时间' AFTER status",
  "SELECT 'skip alerts.resolved_at'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'alerts' AND column_name = 'action_note') = 0,
  "ALTER TABLE alerts ADD COLUMN action_note TEXT COMMENT '处理备注' AFTER resolved_at",
  "SELECT 'skip alerts.action_note'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.tables
   WHERE table_schema = @schema_name AND table_name = 'user_patient_relation') = 0,
  "CREATE TABLE user_patient_relation (
      id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
      user_id BIGINT NOT NULL COMMENT '护工/子女用户 ID',
      patient_id BIGINT NOT NULL COMMENT '患者 ID',
      relation_type VARCHAR(20) NOT NULL COMMENT '关联类型：caregiver/child',
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      UNIQUE KEY uk_user_patient (user_id, patient_id),
      KEY idx_user_id (user_id),
      KEY idx_patient_id (patient_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户患者关联表'",
  "SELECT 'skip user_patient_relation'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.tables
   WHERE table_schema = @schema_name AND table_name = 'reminder_instances') = 0,
  "CREATE TABLE reminder_instances (
      id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
      patient_id BIGINT NOT NULL COMMENT '患者 ID',
      schedule_id BIGINT NOT NULL COMMENT '计划模板 ID',
      scheduled_date DATE NOT NULL COMMENT '执行日期',
      window_start_at TIMESTAMP NOT NULL COMMENT '时间窗开始',
      window_end_at TIMESTAMP NOT NULL COMMENT '时间窗结束',
      status VARCHAR(32) NOT NULL COMMENT '实例状态',
      confirmed_at TIMESTAMP NULL COMMENT '确认时间',
      last_event_id BIGINT NULL COMMENT '最后一次事件 ID',
      last_detection_job_id BIGINT NULL COMMENT '最后一次检测任务 ID',
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      UNIQUE KEY uk_patient_schedule_date (patient_id, schedule_id, scheduled_date),
      KEY idx_instances_patient_date (patient_id, scheduled_date),
      KEY idx_instances_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日提醒实例表'",
  "SELECT 'skip reminder_instances'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.tables
   WHERE table_schema = @schema_name AND table_name = 'detection_jobs') = 0,
  "CREATE TABLE detection_jobs (
      id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
      patient_id BIGINT NOT NULL COMMENT '患者 ID',
      schedule_id BIGINT NOT NULL COMMENT '计划模板 ID',
      reminder_instance_id BIGINT NOT NULL COMMENT '关联提醒实例 ID',
      input_type VARCHAR(32) NOT NULL COMMENT '输入类型',
      source_filename VARCHAR(255) NULL COMMENT '原始文件名',
      status VARCHAR(32) NOT NULL COMMENT '任务状态',
      result_status VARCHAR(32) NULL COMMENT '检测结果状态',
      confidence DECIMAL(6,4) NULL COMMENT '置信度',
      action_detected BOOLEAN NULL COMMENT '是否检测到动作',
      targets_json JSON NULL COMMENT '目标检测 JSON',
      latency_ms INT NULL COMMENT '推理耗时毫秒',
      error_code VARCHAR(32) NULL COMMENT '错误码',
      error_message TEXT NULL COMMENT '错误信息',
      trace_id VARCHAR(64) NULL COMMENT '链路追踪 ID',
      started_at TIMESTAMP NULL COMMENT '开始处理时间',
      completed_at TIMESTAMP NULL COMMENT '完成时间',
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      KEY idx_jobs_patient (patient_id),
      KEY idx_jobs_instance (reminder_instance_id),
      KEY idx_jobs_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测任务表'",
  "SELECT 'skip detection_jobs'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

UPDATE schedules
SET medicine_name = COALESCE(medicine_name, '未命名药品')
WHERE medicine_name IS NULL OR medicine_name = '';

UPDATE alerts
SET title = COALESCE(title, CONCAT('告警: ', type)),
    description = COALESCE(description, '旧版数据缺少告警描述，已按开发修复脚本补齐。'),
    severity = COALESCE(severity, 'medium')
WHERE title IS NULL OR description IS NULL OR severity IS NULL;

INSERT IGNORE INTO user_patient_relation (user_id, patient_id, relation_type)
SELECT u.id, p.id, 'caregiver'
FROM users u
JOIN patients p ON p.elder_user_id IS NOT NULL
WHERE u.username = 'care1';

INSERT IGNORE INTO user_patient_relation (user_id, patient_id, relation_type)
SELECT u.id, p.id, 'child'
FROM users u
JOIN patients p ON p.elder_user_id IS NOT NULL
WHERE u.username = 'child1';
