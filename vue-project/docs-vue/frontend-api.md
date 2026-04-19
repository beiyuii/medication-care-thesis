# 前端所需后端接口清单

> 说明：本文档聚焦于业务后端（计划、事件、告警、设置等）接口需求，摄像头检测/算法端 `/health` `/ready` `/v1/detections/predict` 已由算法服务提供，故不在此列。

## 统一约定
- **Base URL**：`/api`
- **鉴权方式**：`Authorization: Bearer <token>`
- **错误格式**：`{ "error": "错误摘要", "detail": "可选详细描述", "traceId": "请求链路 ID" }`
- **时间格式**：ISO8601（UTC）

## 1. 认证与角色
| 接口 | 描述 |
| --- | --- |
| `POST /auth/login` | Body `{username,password}` → 返回 `{token, role, userId, displayName}` |
| `GET /auth/profile` | 校验 token 有效性，返回当前用户信息 `{userId, role, name, patients: []}` |

## 2. 患者与角色关联
| 接口 | 描述 |
| --- | --- |
| `GET /patients` | 返回当前账号可见的老年人列表（护工/子女用）`[{id,name,nextIntakeTime,planStatus,alertCount}]` |
| `GET /patients/{id}` | 返回单个老年人详情（基础信息、关联计划摘要） |

## 3. 用药计划（老年人专属可写）
| 接口 | 描述 |
| --- | --- |
| `GET /schedules?patientId={id}` | 计划列表，字段包含 `id、medicineName、type、dosage、frequency、window{start,end}、period、status、nextIntake` |
| `POST /schedules` | 创建计划，Body 与字段同上，返回新 `id` |
| `PATCH /schedules/{id}` | 局部更新（剂量/时间窗等） |
| `POST /schedules/{id}/toggle` | 启停计划，Body `{status: "active"|"paused"}` |

## 4. 事件与历史
| 接口 | 描述 |
| --- | --- |
| `GET /intake-events?patientId={id}&range=day|week|month` | 历史事件列表，含 `status、targets、action、imageUrl` |
| `POST /intake-events` | 前端在检测后上报事件，Body `{patientId,scheduleId,status,action,targetsJson}` |
| `POST /intake-events/{id}/confirm` | 手动确认，Body `{confirmedBy, confirmTime}` |
| `GET /reports/summary?patientId={id}&range=day|week|month` | 统计信息：提醒次数、确认率、平均响应时间等 |

## 5. 异常与告警
| 接口 | 描述 |
| --- | --- |
| `GET /alerts?patientId={id}` | 列出未处理/已处理异常 `{id,title,description,severity,occurredAt,resolved}` |
| `POST /alerts/{id}/resolve` | 标记已处理，Body 可含 `{actionNote}` |

## 6. 日志图片
| 接口 | 描述 |
| --- | --- |
| `POST /logs/images` | `multipart/form-data`，字段 `file`、`eventId`，返回 `{url}` |
| `GET /logs/images?eventId={id}` | 返回图片列表（供历史详情查看） |

## 7. 设置（老年人可写，其他角色只读）
| 接口 | 描述 |
| --- | --- |
| `GET /settings` | 返回 `{reminder:{enableVoice,advanceMinutes,volume}, detection:{autoStart,lowLightEnhance,fallbackMode}, privacy:{cameraPermission,uploadConsent,shareToCaregiver}}` |
| `PUT /settings` | 更新设置，Body 同上 |

## 8. 状态检查
| 接口 | 描述 |
| --- | --- |
| `GET /status/ping` | 简单联通性测试，返回 `{timestamp, version}` |
| `GET /status/role` | 可选接口，返回当前角色权限矩阵（用于前端缓存） |

## 实现建议
- 所有接口需返回 `traceId` 便于用户在前端报错时反馈。
- 护工/子女角色对写接口应返回 `403`，前端根据错误提示展示“仅老年人可操作”。
- 历史/告警接口应支持分页或 `limit/offset`，以防数据量过大。
