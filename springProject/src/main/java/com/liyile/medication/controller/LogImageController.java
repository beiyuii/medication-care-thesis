package com.liyile.medication.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyile.medication.common.ApiResponse;
import com.liyile.medication.entity.LogImage;
import com.liyile.medication.mapper.LogImageMapper;
import com.liyile.medication.util.FileStorageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 日志图片控制器。
 * <p>上传和查询服药事件的关键帧图片，用于事件留存与可追溯性。</p>
 *
 * @author Liyile
 */
@Tag(name = "日志图片管理", description = "服药事件关键帧图片的上传与查询接口")
@RestController
@RequestMapping("/api/logs/images")
public class LogImageController {
  /** 日志图片 Mapper */
  private final LogImageMapper logImageMapper;
  /** 文件存储工具 */
  private final FileStorageUtil fileStorageUtil;

  /** 构造方法注入依赖。 */
  public LogImageController(LogImageMapper logImageMapper, FileStorageUtil fileStorageUtil) {
    this.logImageMapper = logImageMapper;
    this.fileStorageUtil = fileStorageUtil;
  }

  /**
   * 上传日志图片。
   * <p>上传服药事件的关键帧图片，前端在检测到疑似/确认/异常事件时调用。
   * 图片将存储到本地目录，并返回访问URL。</p>
   *
   * @param file 图片文件，支持jpg/png/webp等格式
   * @param eventId 关联的服药事件ID
   * @return 图片访问URL，可用于前端显示或后续查询
   */
  @Operation(
      summary = "上传服药事件关键帧图片",
      description = "上传服药事件的关键帧图片（摄像头截帧），图片将存储到本地/logs目录，命名规则为patientId_scheduleId_timestamp.jpg")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "上传成功，返回图片访问URL")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "参数校验失败或文件格式不支持")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "文件存储失败")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<java.util.Map<String, String>> upload(
      @Parameter(description = "图片文件，支持jpg/png/webp格式，建议大小≤2MB", required = true,
          content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
              schema = @Schema(type = "string", format = "binary")))
      @RequestParam("file") MultipartFile file,
      @Parameter(name = "eventId", description = "关联的服药事件ID", required = true, example = "1")
      @RequestParam("eventId") Long eventId) {
    String url = fileStorageUtil.save(file);
    LogImage img = new LogImage();
    img.setEventId(eventId);
    img.setUrl(url);
    img.setTs(String.valueOf(System.currentTimeMillis()));
    logImageMapper.insert(img);
    return ApiResponse.success(java.util.Map.of("url", url));
  }

  /**
   * 查询事件图片列表。
   * <p>根据服药事件ID查询该事件关联的所有关键帧图片。</p>
   *
   * @param eventId 服药事件ID
   * @return 图片列表，包含URL和上传时间戳
   */
  @Operation(
      summary = "查询服药事件的图片列表",
      description = "根据服药事件ID查询该事件关联的所有关键帧图片，返回图片URL和时间戳")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功，返回图片列表")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "参数校验失败，eventId不能为空")
  @GetMapping
  public ApiResponse<List<LogImage>> list(
      @Parameter(name = "eventId", description = "服药事件ID", required = true, example = "1")
      @RequestParam("eventId") Long eventId) {
    List<LogImage> list =
        logImageMapper.selectList(new LambdaQueryWrapper<LogImage>().eq(LogImage::getEventId, eventId));
    return ApiResponse.success(list);
  }
}