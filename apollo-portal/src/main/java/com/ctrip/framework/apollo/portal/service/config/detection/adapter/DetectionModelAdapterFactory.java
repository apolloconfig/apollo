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


import com.ctrip.framework.apollo.portal.service.config.detection.config.DetectionProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detection model adapter factory
 */
@Component
public class DetectionModelAdapterFactory {

  private final DetectionProperties properties;
  private final Map<String, DetectionModelAdapter> adapterCache = new ConcurrentHashMap<>();

  public DetectionModelAdapterFactory(DetectionProperties properties) {
    this.properties = properties;
  }

  public DetectionModelAdapter getAdapter(String providerCode) {
    if (providerCode == null || providerCode.isEmpty()) {
      providerCode = properties.getActiveProvider();
    }

    return adapterCache.computeIfAbsent(providerCode, this::createAdapter);
  }

  private DetectionModelAdapter createAdapter(String providerCode) {
    DetectionProperties.ProviderConfig config = properties.getProviders().get(providerCode);

    if (config == null) {
      throw new IllegalArgumentException("Detection model provider not configured: " + providerCode);
    }

    if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
      throw new IllegalStateException("API key not configured for provider: " + providerCode);
    }

    String baseUrl = config.getBaseUrl();
    if (baseUrl == null || baseUrl.isEmpty()) {
      DetectionModelProvider provider = DetectionModelProvider.fromCode(providerCode);
      baseUrl = provider.getDefaultBaseUrl();
    }

    Duration timeout = Duration.ofMillis(properties.getTimeout());

    return new OpenAICompatibleAdapter(baseUrl, config.getApiKey(), config.getModel(), timeout);
  }
}
