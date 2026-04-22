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
      UNIQUE KEY uk_patient_schedule_date_retry (patient_id, schedule_id, scheduled_date, retry_count),
      KEY idx_instances_patient_date (patient_id, scheduled_date),
      KEY idx_instances_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日提醒实例表'",
  "SELECT 'skip reminder_instances'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = @schema_name AND table_name = 'reminder_instances' AND index_name = 'uk_patient_schedule_date') > 0,
  "ALTER TABLE reminder_instances DROP INDEX uk_patient_schedule_date",
  "SELECT 'skip reminder_instances.drop_old_unique'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = @schema_name AND table_name = 'reminder_instances' AND index_name = 'uk_patient_schedule_date_retry') = 0,
  "ALTER TABLE reminder_instances ADD UNIQUE KEY uk_patient_schedule_date_retry (patient_id, schedule_id, scheduled_date, retry_count)",
  "SELECT 'skip reminder_instances.add_retry_unique'"
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
      target_confidence DECIMAL(6,4) NULL COMMENT '药品目标证据强度',
      action_confidence DECIMAL(6,4) NULL COMMENT '动作证据强度',
      final_confidence DECIMAL(6,4) NULL COMMENT '最终综合置信度',
      reason_code VARCHAR(64) NULL COMMENT '检测原因码',
      reason_text VARCHAR(255) NULL COMMENT '检测解释文案',
      risk_tag VARCHAR(64) NULL COMMENT '风险标签',
      action_detected BOOLEAN NULL COMMENT '是否检测到动作',
      targets_json JSON NULL COMMENT '目标检测 JSON',
      latency_ms INT NULL COMMENT '推理耗时毫秒',
      error_code VARCHAR(32) NULL COMMENT '错误码',
      error_message TEXT NULL COMMENT '错误信息',
      trace_id VARCHAR(64) NULL COMMENT '链路追踪 ID',
      llm_provider VARCHAR(64) NULL COMMENT '多模态判定提供方',
      llm_model VARCHAR(128) NULL COMMENT '多模态模型名称',
      llm_frame_count INT NULL COMMENT '发送给多模态模型的关键帧数量',
      llm_decision_source VARCHAR(32) NULL COMMENT '判定来源：deepseek_vl/fallback_rules',
      frame_summary VARCHAR(255) NULL COMMENT '关键帧摘要',
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

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'detection_jobs' AND column_name = 'target_confidence') = 0,
  'ALTER TABLE detection_jobs ADD COLUMN target_confidence DECIMAL(6,4) NULL COMMENT ''药品目标证据强度'' AFTER confidence',
  "SELECT 'skip detection_jobs.target_confidence'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'detection_jobs' AND column_name = 'action_confidence') = 0,
  'ALTER TABLE detection_jobs ADD COLUMN action_confidence DECIMAL(6,4) NULL COMMENT ''动作证据强度'' AFTER target_confidence',
  "SELECT 'skip detection_jobs.action_confidence'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'detection_jobs' AND column_name = 'final_confidence') = 0,
  'ALTER TABLE detection_jobs ADD COLUMN final_confidence DECIMAL(6,4) NULL COMMENT ''最终综合置信度'' AFTER action_confidence',
  "SELECT 'skip detection_jobs.final_confidence'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'detection_jobs' AND column_name = 'reason_code') = 0,
  'ALTER TABLE detection_jobs ADD COLUMN reason_code VARCHAR(64) NULL COMMENT ''检测原因码'' AFTER final_confidence',
  "SELECT 'skip detection_jobs.reason_code'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'detection_jobs' AND column_name = 'reason_text') = 0,
  'ALTER TABLE detection_jobs ADD COLUMN reason_text VARCHAR(255) NULL COMMENT ''检测解释文案'' AFTER reason_code',
  "SELECT 'skip detection_jobs.reason_text'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'detection_jobs' AND column_name = 'risk_tag') = 0,
  'ALTER TABLE detection_jobs ADD COLUMN risk_tag VARCHAR(64) NULL COMMENT ''风险标签'' AFTER reason_text',
  "SELECT 'skip detection_jobs.risk_tag'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'detection_jobs' AND column_name = 'llm_provider') = 0,
  'ALTER TABLE detection_jobs ADD COLUMN llm_provider VARCHAR(64) NULL COMMENT ''多模态判定提供方'' AFTER trace_id',
  "SELECT 'skip detection_jobs.llm_provider'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'detection_jobs' AND column_name = 'llm_model') = 0,
  'ALTER TABLE detection_jobs ADD COLUMN llm_model VARCHAR(128) NULL COMMENT ''多模态模型名称'' AFTER llm_provider',
  "SELECT 'skip detection_jobs.llm_model'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'detection_jobs' AND column_name = 'llm_frame_count') = 0,
  'ALTER TABLE detection_jobs ADD COLUMN llm_frame_count INT NULL COMMENT ''发送给多模态模型的关键帧数量'' AFTER llm_model',
  "SELECT 'skip detection_jobs.llm_frame_count'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'detection_jobs' AND column_name = 'llm_decision_source') = 0,
  'ALTER TABLE detection_jobs ADD COLUMN llm_decision_source VARCHAR(32) NULL COMMENT ''判定来源：deepseek_vl/fallback_rules'' AFTER llm_frame_count',
  "SELECT 'skip detection_jobs.llm_decision_source'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'detection_jobs' AND column_name = 'frame_summary') = 0,
  'ALTER TABLE detection_jobs ADD COLUMN frame_summary VARCHAR(255) NULL COMMENT ''关键帧摘要'' AFTER llm_decision_source',
  "SELECT 'skip detection_jobs.frame_summary'"
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

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'intake_events' AND column_name = 'event_type') = 0,
  "ALTER TABLE intake_events ADD COLUMN event_type VARCHAR(32) NULL COMMENT '事件类型' AFTER status",
  "SELECT 'skip intake_events.event_type'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'intake_events' AND column_name = 'detection_status') = 0,
  "ALTER TABLE intake_events ADD COLUMN detection_status VARCHAR(32) NULL COMMENT '检测状态' AFTER event_type",
  "SELECT 'skip intake_events.detection_status'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'intake_events' AND column_name = 'review_decision') = 0,
  "ALTER TABLE intake_events ADD COLUMN review_decision VARCHAR(32) NULL COMMENT '审核决策' AFTER detection_status",
  "SELECT 'skip intake_events.review_decision'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'intake_events' AND column_name = 'review_reason') = 0,
  "ALTER TABLE intake_events ADD COLUMN review_reason VARCHAR(255) NULL COMMENT '审核原因' AFTER review_decision",
  "SELECT 'skip intake_events.review_reason'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'reminder_instances' AND column_name = 'detection_status') = 0,
  "ALTER TABLE reminder_instances ADD COLUMN detection_status VARCHAR(32) NOT NULL DEFAULT 'none' COMMENT '检测状态' AFTER status",
  "SELECT 'skip reminder_instances.detection_status'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'reminder_instances' AND column_name = 'parent_instance_id') = 0,
  "ALTER TABLE reminder_instances ADD COLUMN parent_instance_id BIGINT NULL COMMENT '父实例 ID' AFTER detection_status",
  "SELECT 'skip reminder_instances.parent_instance_id'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'reminder_instances' AND column_name = 'retry_count') = 0,
  "ALTER TABLE reminder_instances ADD COLUMN retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数' AFTER parent_instance_id",
  "SELECT 'skip reminder_instances.retry_count'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'reminder_instances' AND column_name = 'review_deadline') = 0,
  "ALTER TABLE reminder_instances ADD COLUMN review_deadline TIMESTAMP NULL COMMENT '审核截止时间' AFTER retry_count",
  "SELECT 'skip reminder_instances.review_deadline'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'reminder_instances' AND column_name = 'late_minutes') = 0,
  "ALTER TABLE reminder_instances ADD COLUMN late_minutes INT NULL COMMENT '迟服分钟数' AFTER review_deadline",
  "SELECT 'skip reminder_instances.late_minutes'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'reminder_instances' AND column_name = 'reviewed_by') = 0,
  "ALTER TABLE reminder_instances ADD COLUMN reviewed_by VARCHAR(64) NULL COMMENT '审核人' AFTER late_minutes",
  "SELECT 'skip reminder_instances.reviewed_by'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'reminder_instances' AND column_name = 'reviewed_at') = 0,
  "ALTER TABLE reminder_instances ADD COLUMN reviewed_at TIMESTAMP NULL COMMENT '审核时间' AFTER reviewed_by",
  "SELECT 'skip reminder_instances.reviewed_at'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @stmt = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'reminder_instances' AND column_name = 'review_reason') = 0,
  "ALTER TABLE reminder_instances ADD COLUMN review_reason VARCHAR(255) NULL COMMENT '审核原因' AFTER reviewed_at",
  "SELECT 'skip reminder_instances.review_reason'"
);
PREPARE repair_stmt FROM @stmt;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;
