-- 数据库初始化脚本（MySQL 8）
-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS lyl CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lyl;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
  username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名（唯一）',
  pwd_hash VARCHAR(255) NOT NULL COMMENT '登录密码哈希',
  role ENUM('elder','caregiver','child') NOT NULL COMMENT '角色（elder/caregiver/child）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 患者表
CREATE TABLE IF NOT EXISTS patients (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
  elder_user_id BIGINT NOT NULL COMMENT '关联的老年人用户 ID',
  name VARCHAR(64) NOT NULL COMMENT '被照护人姓名',
  age INT COMMENT '年龄',
  phone VARCHAR(20) COMMENT '联系电话',
  INDEX idx_patients_elder (elder_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者表';

-- 用药计划表
CREATE TABLE IF NOT EXISTS schedules (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
  patient_id BIGINT NOT NULL COMMENT '患者 ID',
  medicine_name VARCHAR(100) COMMENT '药品名称',
  type ENUM('PILL','BLISTER','BOTTLE','BOX') NOT NULL COMMENT '药品类型',
  dose VARCHAR(64) NOT NULL COMMENT '剂量描述',
  freq VARCHAR(64) NOT NULL COMMENT '服用频次',
  win_start VARCHAR(16) NOT NULL COMMENT '时间窗开始（HH:mm）',
  win_end VARCHAR(16) NOT NULL COMMENT '时间窗结束（HH:mm）',
  period VARCHAR(64) NOT NULL COMMENT '服药周期描述',
  status VARCHAR(16) NOT NULL COMMENT '计划状态（enabled/disabled）',
  INDEX idx_schedules_patient (patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用药计划表';

-- 服药事件表
CREATE TABLE IF NOT EXISTS intake_events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
  patient_id BIGINT NOT NULL COMMENT '患者 ID',
  schedule_id BIGINT COMMENT '关联的计划 ID',
  reminder_instance_id BIGINT COMMENT '关联提醒实例 ID',
  detection_job_id BIGINT COMMENT '关联检测任务 ID',
  ts VARCHAR(32) NOT NULL COMMENT '事件时间戳',
  status ENUM('suspected','confirmed','abnormal') NOT NULL COMMENT '事件状态',
  event_type VARCHAR(32) NULL COMMENT '事件类型',
  detection_status VARCHAR(32) NULL COMMENT '检测状态',
  review_decision VARCHAR(32) NULL COMMENT '审核决策',
  review_reason VARCHAR(255) NULL COMMENT '审核原因',
  confirmed_by VARCHAR(50) COMMENT '确认人',
  confirmed_at TIMESTAMP NULL COMMENT '确认时间',
  action VARCHAR(64) COMMENT '动作描述',
  targets_json JSON COMMENT '目标检测 JSON 数据',
  img_url VARCHAR(255) COMMENT '日志图片 URL',
  video_url VARCHAR(512) COMMENT '检测原始录像 URL（相对路径，如 /uploads/videos/...）',
  INDEX idx_events_patient (patient_id),
  INDEX idx_events_schedule (schedule_id),
  INDEX idx_events_instance (reminder_instance_id),
  INDEX idx_events_job (detection_job_id),
  INDEX idx_events_patient_ts (patient_id, ts DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服药事件表';

-- 提醒实例表
CREATE TABLE IF NOT EXISTS reminder_instances (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
  patient_id BIGINT NOT NULL COMMENT '患者 ID',
  schedule_id BIGINT NOT NULL COMMENT '计划模板 ID',
  scheduled_date DATE NOT NULL COMMENT '执行日期',
  window_start_at TIMESTAMP NOT NULL COMMENT '时间窗开始',
  window_end_at TIMESTAMP NOT NULL COMMENT '时间窗结束',
  status VARCHAR(32) NOT NULL COMMENT '审核状态：not_submitted/waiting_caregiver/abnormal_pending_review/evidence_required/caregiver_confirmed/caregiver_rejected/review_timeout/missed/waiting_caregiver_late/manual_intervention',
  detection_status VARCHAR(32) NOT NULL DEFAULT 'none' COMMENT '检测状态：none/suspected/confirmed/abnormal',
  parent_instance_id BIGINT NULL COMMENT '父实例 ID',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  review_deadline TIMESTAMP NULL COMMENT '审核截止时间',
  late_minutes INT NULL COMMENT '迟服分钟数',
  reviewed_by VARCHAR(64) NULL COMMENT '审核人',
  reviewed_at TIMESTAMP NULL COMMENT '审核时间',
  review_reason VARCHAR(255) NULL COMMENT '审核原因',
  confirmed_at TIMESTAMP NULL COMMENT '确认时间',
  last_event_id BIGINT NULL COMMENT '最后一次事件 ID',
  last_detection_job_id BIGINT NULL COMMENT '最后一次检测任务 ID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_patient_schedule_date_retry (patient_id, schedule_id, scheduled_date, retry_count),
  KEY idx_instances_patient_date (patient_id, scheduled_date),
  KEY idx_instances_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日提醒实例表';

-- 检测任务表
CREATE TABLE IF NOT EXISTS detection_jobs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
  patient_id BIGINT NOT NULL COMMENT '患者 ID',
  schedule_id BIGINT NOT NULL COMMENT '计划模板 ID',
  reminder_instance_id BIGINT NOT NULL COMMENT '关联提醒实例 ID',
  input_type VARCHAR(32) NOT NULL COMMENT '输入类型：video/image',
  source_filename VARCHAR(255) NULL COMMENT '原始文件名',
  status VARCHAR(32) NOT NULL COMMENT '任务状态：queued/processing/succeeded/failed',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测任务表';

-- 告警表
CREATE TABLE IF NOT EXISTS alerts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
  patient_id BIGINT NOT NULL COMMENT '患者 ID',
  reminder_instance_id BIGINT NULL COMMENT '关联提醒实例 ID',
  detection_job_id BIGINT NULL COMMENT '关联检测任务 ID',
  title VARCHAR(200) COMMENT '告警标题',
  description TEXT COMMENT '告警描述',
  severity VARCHAR(20) DEFAULT 'medium' COMMENT '严重程度：high/medium/low',
  type VARCHAR(32) NOT NULL COMMENT '告警类型',
  ts VARCHAR(32) NOT NULL COMMENT '触发时间戳',
  status VARCHAR(32) NOT NULL COMMENT '告警状态',
  resolved_at TIMESTAMP NULL COMMENT '处理时间',
  action_note TEXT COMMENT '处理备注',
  INDEX idx_alerts_patient (patient_id),
  INDEX idx_alerts_instance (reminder_instance_id),
  INDEX idx_alerts_job (detection_job_id),
  INDEX idx_alerts_patient_status (patient_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警表';

-- 日志图片表
CREATE TABLE IF NOT EXISTS log_images (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
  event_id BIGINT NOT NULL COMMENT '关联事件 ID',
  url VARCHAR(255) NOT NULL COMMENT '图片存储路径',
  ts VARCHAR(32) NOT NULL COMMENT '上传时间戳',
  INDEX idx_log_images_event (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志图片表';

-- 用户设置表
CREATE TABLE IF NOT EXISTS settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    reminder_enable_voice BOOLEAN DEFAULT TRUE COMMENT '提醒：启用语音',
    reminder_advance_minutes INT DEFAULT 5 COMMENT '提醒：提前分钟数',
    reminder_volume INT DEFAULT 80 COMMENT '提醒：音量（0-100）',
    detection_auto_start BOOLEAN DEFAULT TRUE COMMENT '检测：自动启动',
    detection_low_light_enhance BOOLEAN DEFAULT FALSE COMMENT '检测：低光增强',
    detection_fallback_mode VARCHAR(20) DEFAULT 'WASM' COMMENT '检测：回退模式（WebGPU/WebGL/WASM）',
    privacy_camera_permission BOOLEAN DEFAULT TRUE COMMENT '隐私：摄像头权限（应用内开关，默认允许；浏览器仍单独授权）',
    privacy_upload_consent BOOLEAN DEFAULT FALSE COMMENT '隐私：上传同意',
    privacy_share_to_caregiver BOOLEAN DEFAULT FALSE COMMENT '隐私：分享给护工',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设置表';

-- 用户患者关联表（护工/子女与患者关联）
CREATE TABLE IF NOT EXISTS user_patient_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '护工/子女用户 ID',
    patient_id BIGINT NOT NULL COMMENT '患者 ID',
    relation_type VARCHAR(20) NOT NULL COMMENT '关联类型：caregiver/child',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_patient (user_id, patient_id),
    KEY idx_user_id (user_id),
    KEY idx_patient_id (patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户患者关联表';
