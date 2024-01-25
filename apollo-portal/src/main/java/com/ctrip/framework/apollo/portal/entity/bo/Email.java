/*
 * Copyright 2024 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
