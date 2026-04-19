package com.liyile.medication.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC配置类。
 * <p>配置静态资源映射，使上传的图片可以通过HTTP访问。</p>
 *
 * @author Liyile
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  /** 上传目录路径 */
  @Value("${app.upload-dir:./uploads/images}")
  private String uploadDir;

  @Override
  public void addResourceHandlers(@org.springframework.lang.NonNull ResourceHandlerRegistry registry) {
    // 获取项目根目录
    String projectRoot = System.getProperty("user.dir");
    
    // 获取uploads目录的绝对路径（用于映射 /uploads/**）
    // uploadDir 通常是 ./uploads/images，我们需要映射到 ./uploads 目录
    File uploadsRoot = new File(projectRoot, "uploads");
    String uploadsRootPath = uploadsRoot.getAbsolutePath().replace("\\", "/");
    if (!uploadsRootPath.endsWith("/")) {
      uploadsRootPath += "/";
    }
    
    // 映射 /uploads/** 到本地文件系统的uploads目录
    // 例如：/uploads/images/xxx.jpg -> file:/path/to/project/uploads/images/xxx.jpg
    registry.addResourceHandler("/uploads/**")
        .addResourceLocations("file:" + uploadsRootPath);
    
    // 兼容旧路径 /logs/images/**（如果有的话）
    File uploadDirFile = new File(uploadDir);
    String absolutePath;
    if (uploadDirFile.isAbsolute()) {
      absolutePath = uploadDirFile.getAbsolutePath();
    } else {
      absolutePath = new File(projectRoot, uploadDir).getAbsolutePath();
    }
    absolutePath = absolutePath.replace("\\", "/");
    if (!absolutePath.endsWith("/")) {
      absolutePath += "/";
    }
    registry.addResourceHandler("/logs/images/**")
        .addResourceLocations("file:" + absolutePath);
  }
}

