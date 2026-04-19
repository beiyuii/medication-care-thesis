package com.liyile.medication.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import com.liyile.medication.common.CommonConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储工具类。
 * <p>用于存储上传的日志图片到本地目录。</p>
 *
 * @author Liyile
 */
@Component
public class FileStorageUtil {
  /** 上传目录路径（默认 ./uploads/images） */
  @Value("${app.upload-dir:./uploads/images}")
  private String uploadDir;

  /**
   * 保存上传文件并返回相对 URL。
   *
   * @param file 上传的文件
   * @return 文件相对路径
   */
  public String save(MultipartFile file) {
    try {
      File dir = new File(uploadDir);
      if (!dir.exists()) {
        dir.mkdirs();
      }
      String contentType = file.getContentType();
      if (contentType == null) {
        throw new RuntimeException("文件类型未知，拒绝上传");
      }
      boolean allowedMime = contentType.equals("image/png")
          || contentType.equals("image/jpeg")
          || contentType.equals("image/webp");
      if (!allowedMime) {
        throw new RuntimeException("仅支持上传PNG/JPEG/WEBP图片");
      }
      String ext = "";
      String original = file.getOriginalFilename();
      if (original != null && original.indexOf(CommonConstants.DOT) >= 0) {
        ext = original.substring(original.lastIndexOf(CommonConstants.DOT));
      }
      String lowerExt = ext.toLowerCase();
      boolean allowedExt = ".png".equals(lowerExt) || ".jpg".equals(lowerExt) || ".jpeg".equals(lowerExt) || ".webp".equals(lowerExt);
      if (!allowedExt) {
        throw new RuntimeException("文件扩展名不合法，必须为PNG/JPG/JPEG/WEBP");
      }
      String name = UUID.randomUUID() + ext;
      Path dest = new File(dir, name).toPath();
      Files.copy(file.getInputStream(), dest);
      // 返回以 / 开头的相对路径，便于前端拼接
      String relative = new File(uploadDir, name).getPath();
      if (relative.startsWith("./")) {
        relative = relative.substring(2);
      }
      // 确保路径以 / 开头
      if (!relative.startsWith("/")) {
        relative = "/" + relative;
      }
      // 统一使用正斜杠（Web路径）
      relative = relative.replace("\\", "/");
      return relative;
    } catch (IOException e) {
      throw new RuntimeException("文件保存失败: " + e.getMessage(), e);
    }
  }
}
