package com.liyile.medication.dto;

public class ReviewReminderInstanceDTO {
  private String decision;
  private String reviewedBy;
  private String reviewTime;
  private String reason;

  public String getDecision() {
    return decision;
  }

  public void setDecision(String decision) {
    this.decision = decision;
  }

  public String getReviewedBy() {
    return reviewedBy;
  }

  public void setReviewedBy(String reviewedBy) {
    this.reviewedBy = reviewedBy;
  }

  public String getReviewTime() {
    return reviewTime;
  }

  public void setReviewTime(String reviewTime) {
    this.reviewTime = reviewTime;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
