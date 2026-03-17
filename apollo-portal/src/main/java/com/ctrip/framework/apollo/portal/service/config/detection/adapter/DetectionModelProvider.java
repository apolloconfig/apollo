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
package com.ctrip.framework.apollo.portal.service.config.detection.adapter;

/**
 * Detection model provider enum
 */
public enum DetectionModelProvider {

  OPENAI("openai", "https://api.openai.com/v1"),
  QWEN("qwen", "https://dashscope.aliyuncs.com/compatible-mode/v1");

  private final String code;
  private final String defaultBaseUrl;

  DetectionModelProvider(String code, String defaultBaseUrl) {
    this.code = code;
    this.defaultBaseUrl = defaultBaseUrl;
  }

  public String getCode() {
    return code;
  }

  public String getDefaultBaseUrl() {
    return defaultBaseUrl;
  }

  public static DetectionModelProvider fromCode(String code) {
    for (DetectionModelProvider provider : values()) {
      if (provider.code.equalsIgnoreCase(code)) {
        return provider;
      }
    }
    throw new IllegalArgumentException("Unknown detection model provider: " + code);
  }
}
