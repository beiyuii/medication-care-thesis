package com.liyile.medication.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 日志图片实体。
 * <p>对应数据表 log_images。</p>
 *
 * @author Liyile
 */
@TableName("log_images")
public class LogImage {
  /** 主键 ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 事件 ID */
  private Long eventId;

  /** 图片 URL */
  private String url;

  /** 时间戳 */
  private String ts;

  /** 获取主键 ID。 */
  public Long getId() {
    return id;
  }

  /** 设置主键 ID。 */
  public void setId(Long id) {
    this.id = id;
  }

  /** 获取事件 ID。 */
  public Long getEventId() {
    return eventId;
  }

  /** 设置事件 ID。 */
  public void setEventId(Long eventId) {
    this.eventId = eventId;
  }

  /** 获取图片 URL。 */
  public String getUrl() {
    return url;
  }

  /** 设置图片 URL。 */
  public void setUrl(String url) {
    this.url = url;
  }

  /** 获取时间戳。 */
  public String getTs() {
    return ts;
  }

  /** 设置时间戳。 */
  public void setTs(String ts) {
    this.ts = ts;
  }
}