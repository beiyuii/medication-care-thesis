package com.liyile.medication.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 绑定患者请求。
 */
public class BindPatientRequest {
  @NotBlank(message = "老人用户名不能为空")
  private String elderUsername;

  public String getElderUsername() {
    return elderUsername;
  }

  public void setElderUsername(String elderUsername) {
    this.elderUsername = elderUsername;
  }
}
