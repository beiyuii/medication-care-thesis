-- 预设数据（可按需调整）
USE lyl;

-- 预置用户（全部为 BCrypt 哈希，默认明文均为 123456，仅供本机调试使用）
INSERT INTO users (username, pwd_hash, role) VALUES
  ('elder1', '$2y$10$FsvF/CYP0fv09S4RJHIOAO9oC2BAGY7qP1ECemhi/U94J6dHcr9iC', 'elder'),
  ('care1', '$2y$10$FsvF/CYP0fv09S4RJHIOAO9oC2BAGY7qP1ECemhi/U94J6dHcr9iC', 'caregiver'),
  ('child1', '$2y$10$FsvF/CYP0fv09S4RJHIOAO9oC2BAGY7qP1ECemhi/U94J6dHcr9iC', 'child');

-- 预置患者（绑定 elder1）
INSERT INTO patients (elder_user_id, name, age, phone) VALUES
  ((SELECT id FROM users WHERE username='elder1'), '张三', 75, '13800138000');

-- 预置计划
INSERT INTO schedules (patient_id, medicine_name, type, dose, freq, win_start, win_end, period, status) VALUES
  ((SELECT id FROM patients WHERE name='张三'), '降压药', 'PILL', '1片', '每天', '08:00', '09:00', '1-30天', 'enabled');

-- 预置事件示例
INSERT INTO intake_events (patient_id, schedule_id, ts, status, action, targets_json, img_url) VALUES
  ((SELECT id FROM patients WHERE name='张三'), (SELECT id FROM schedules WHERE patient_id=(SELECT id FROM patients WHERE name='张三')), '2024-11-12T08:30:00', 'suspected', 'hand_to_mouth', JSON_ARRAY(JSON_OBJECT('target','PILL','score',0.95)), NULL);

-- 预置告警示例
INSERT INTO alerts (patient_id, title, description, severity, type, ts, status) VALUES
  ((SELECT id FROM patients WHERE name='张三'), '超时未确认', '用药时间窗结束仍未确认服药', 'medium', 'timeout', '2024-11-12T10:00:00', 'pending');

-- 预置用户患者关联（护工和子女关联到患者）
INSERT INTO user_patient_relation (user_id, patient_id, relation_type) VALUES
  ((SELECT id FROM users WHERE username='care1'), (SELECT id FROM patients WHERE name='张三'), 'caregiver'),
  ((SELECT id FROM users WHERE username='child1'), (SELECT id FROM patients WHERE name='张三'), 'child');

-- 预置用户设置（为 elder1 创建默认设置；开启摄像头开关以便检测页可调用 getUserMedia）
INSERT INTO settings (user_id, privacy_camera_permission) VALUES
  ((SELECT id FROM users WHERE username='elder1'), TRUE);

-- 若此前已导入旧种子数据，可手动修正 elder1 的摄像头应用内开关：
-- UPDATE settings s JOIN users u ON s.user_id = u.id SET s.privacy_camera_permission = TRUE WHERE u.username = 'elder1';
