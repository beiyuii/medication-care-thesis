接口与数据库设计（合并版，简版）

一、数据库表（mysql）
- users(id, username, pwd_hash, role)
- patients(id, elder_user_id, name)
- schedules(id, patient_id, type, dose, freq, win_start, win_end, period, status)
- intake_events(id, patient_id, schedule_id, ts, status, action, targets_json, img_url)
- alerts(id, patient_id, type, ts, status)
- log_images(id, event_id, url, ts)

二、角色与权限
- 角色：elder（老年人）、caregiver（护工）、child（子女）。
- elder：读/写计划与确认事件；caregiver/child：只读查看。

三、API（REST 摘要）
- Auth：POST /auth/login → {token,role}；GET /auth/profile
- 计划：GET /schedules?patientId；POST /schedules；PATCH /schedules/{id}；POST /schedules/{id}/toggle
- 事件：POST /intake-events（status: suspected/confirmed/abnormal；action；targets_json）；GET /intake-events?patientId&range
- 日志：POST /logs/images（multipart: file,eventId）→ {url}；GET /logs/images?eventId
- 异常与统计：GET /alerts?patientId；GET /reports/summary?patientId&range

四、鉴权与错误
- 鉴权：Authorization: Bearer <token>；返回角色。
- 权限：elder 可写；caregiver/child 仅 GET。
- 错误：401/403/404/422。

五、字段约定
- type：枚举 PILL/BLISTER/BOTTLE/BOX。
- status：suspected/confirmed/abnormal。
- targets_json：目标与置信度数组。
- img_url：图片路径。

六、留存与审计
- 事件/图片带时间戳；按 patientId/scheduleId 检索。
- 留存周期与文案遵循常规模板；导出 CSV（可选）。