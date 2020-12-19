package com.ctrip.framework.apollo.portal.entity.bo;

import java.util.List;

public class Email {

  private String senderEmailAddress;
  private List<String> recipients;
  private String subject;
  private String body;

  public String getSenderEmailAddress() {
    return senderEmailAddress;
  }

  public void setSenderEmailAddress(String senderEmailAddress) {
    this.senderEmailAddress = senderEmailAddress;
  }

  public List<String> getRecipients() {
    return recipients;
  }

  public String getRecipientsString() {
    return String.join(",", recipients);
  }

  public void setRecipients(List<String> recipients) {
    this.recipients = recipients;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }


}
