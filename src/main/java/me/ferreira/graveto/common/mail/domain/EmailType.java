package me.ferreira.graveto.common.mail.domain;

public enum EmailType {
  PASSWORD_RESET("email.subject.reset-password", "email.content.reset-password");

  private final String subjectPropertyKey;
  private final String contentPropertyKey;

  EmailType(final String subjectPropertyKey, final String contentPropertyKey) {
    this.subjectPropertyKey = subjectPropertyKey;
    this.contentPropertyKey = contentPropertyKey;
  }

  public String getSubjectPropertyKey() {
    return this.subjectPropertyKey;
  }

  public String getContentPropertyKey() {
    return this.contentPropertyKey;
  }

}
