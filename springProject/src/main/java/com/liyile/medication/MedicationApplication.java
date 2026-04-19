package com.liyile.medication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

/**
 * 应用入口类。
 * <p>负责启动 Spring Boot 应用。</p>
 *
 * @author Liyile
 */
@SpringBootApplication
@MapperScan("com.liyile.medication.mapper")
public class MedicationApplication {

  /**
   * 程序主入口方法。
   *
   * @param args 命令行参数
   */
  public static void main(String[] args) {
    SpringApplication.run(MedicationApplication.class, args);
  }
}