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
package com.ctrip.framework.apollo.portal.service.config.detection;


import com.ctrip.framework.apollo.portal.service.config.detection.adapter.DetectionModelAdapter;
import com.ctrip.framework.apollo.portal.service.config.detection.adapter.DetectionModelAdapterFactory;
import com.ctrip.framework.apollo.portal.service.config.detection.config.DetectionProperties;
import com.ctrip.framework.apollo.portal.service.config.detection.model.ChatMessage;
import com.ctrip.framework.apollo.portal.service.config.detection.model.ChatRequest;
import com.ctrip.framework.apollo.portal.service.config.detection.model.DetectionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.Map;

/**
 * Configuration detection service
 */
@Service
public class ConfigDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigDetectionService.class);

    private final DetectionProperties properties;
    private final DetectionModelAdapterFactory adapterFactory;
    private final PromptBuilder promptBuilder;

    public ConfigDetectionService(
            DetectionProperties properties,
            DetectionModelAdapterFactory adapterFactory,
            PromptBuilder promptBuilder) {
        this.properties = properties;
        this.adapterFactory = adapterFactory;
        this.promptBuilder = promptBuilder;
    }

    public Flux<String> detectStream(DetectionRequest request) {
        if (!properties.isEnabled()) {
            return Flux.error(new IllegalStateException("Detection is not enabled"));
        }
        try {
            String provider = properties.getActiveProvider();
            logger.info("Starting detection for key: {}, provider: {}", request.getKey(), provider);
            DetectionModelAdapter adapter = adapterFactory.getAdapter(provider);
            String systemPrompt = promptBuilder.getSystemPrompt();
            String userPrompt = promptBuilder.buildDetectionPrompt(request);

            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(Arrays.asList(
                            new ChatMessage("system", systemPrompt),
                            new ChatMessage("user", userPrompt)
                    ))
                    .stream(true)
                    .build();

            return adapter.streamChat(chatRequest)
                    .doOnComplete(() -> logger.info("Detection completed for key: {}", request.getKey()))
                    .doOnError(error -> logger.error("Detection failed for key: {}", request.getKey(), error));

        } catch (Exception e) {
            logger.error("Failed to start detection", e);
            return Flux.error(e);
        }
    }
}
