/*
 * Copyright 2026 Apollo Authors
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
package com.ctrip.framework.apollo.portal.service.config.detection.model;

import java.util.List;

/**
 * Chat request for detection model
 */
public class ChatRequest {

  private List<ChatMessage> messages;
  private boolean stream;

  public ChatRequest() {
  }

  private ChatRequest(Builder builder) {
    this.messages = builder.messages;
    this.stream = builder.stream;
  }

  public static Builder builder() {
    return new Builder();
  }

  public List<ChatMessage> getMessages() {
    return messages;
  }

  public void setMessages(List<ChatMessage> messages) {
    this.messages = messages;
  }

  public boolean isStream() {
    return stream;
  }

  public void setStream(boolean stream) {
    this.stream = stream;
  }

  public static class Builder {
    private List<ChatMessage> messages;
    private boolean stream = true;

    public Builder messages(List<ChatMessage> messages) {
      this.messages = messages;
      return this;
    }

    public Builder stream(boolean stream) {
      this.stream = stream;
      return this;
    }

    public ChatRequest build() {
      return new ChatRequest(this);
    }
  }
}
