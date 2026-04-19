package com.liyile.medication.dto;

public class ConfirmReminderInstanceDTO {
  private String confirmedBy;
  private String confirmTime;

  public String getConfirmedBy() {
    return confirmedBy;
  }

  public void setConfirmedBy(String confirmedBy) {
    this.confirmedBy = confirmedBy;
  }

  public String getConfirmTime() {
    return confirmTime;
  }

  public void setConfirmTime(String confirmTime) {
    this.confirmTime = confirmTime;
  }
}
