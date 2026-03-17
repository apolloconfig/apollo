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


import com.ctrip.framework.apollo.portal.service.config.detection.model.ChatMessage;
import com.ctrip.framework.apollo.portal.service.config.detection.model.ChatRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OpenAI compatible adapter implementation
 * Supports OpenAI, Qwen and other OpenAI-compatible APIs
 */
public class OpenAICompatibleAdapter implements DetectionModelAdapter {

    private static final Logger logger = LoggerFactory.getLogger(OpenAICompatibleAdapter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient webClient;
    private final String model;

    public OpenAICompatibleAdapter(String baseUrl, String apiKey, String model, Duration timeout) {
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json; charset=UTF-8")
                .defaultHeader("Accept-Charset", "UTF-8")
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    @Override
    public Flux<String> streamChat(ChatRequest request) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", convertMessages(request.getMessages()));
        requestBody.put("stream", request.isStream());

        logger.info("Sending request to detection model: {}, stream: {}", model, request.isStream());
        if (!request.isStream()) {
            return webClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .acceptCharset(StandardCharsets.UTF_8)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(this::parseNonStreamResponse)
                    .flatMapMany(content -> {
                        String[] lines = content.split("\n");
                        return Flux.fromArray(lines)
                                .map(line -> line + "\n");
                    })
                    .doOnNext(data -> logger.debug("Non-stream chunk: {}", data))
                    .doOnError(error -> logger.error("Error in non-stream chat", error));
        }
        StringBuilder lineBuffer = new StringBuilder();

        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .acceptCharset(StandardCharsets.UTF_8)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .doOnSubscribe(sub -> logger.info("Subscribed to AI stream"))
                .concatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    String chunk = new String(bytes, StandardCharsets.UTF_8);
                    lineBuffer.append(chunk);
                    String buffer = lineBuffer.toString();
                    int lastNewlineIndex = buffer.lastIndexOf('\n');
                    if (lastNewlineIndex == -1) {
                        return Flux.empty();
                    }
                    String completeLines = buffer.substring(0, lastNewlineIndex);
                    lineBuffer.setLength(0);
                    lineBuffer.append(buffer.substring(lastNewlineIndex + 1));
                    String[] lines = completeLines.split("\n");
                    return Flux.fromArray(lines);
                })
                .filter(line -> !line.trim().isEmpty())
                .doOnNext(line -> logger.debug("Received line: {}", line))
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6).trim())
                .filter(data -> !data.isEmpty() && !"[DONE]".equals(data))
                .mapNotNull(this::parseStreamChunk)
                .doOnNext(data -> logger.debug("Parsed content: [{}]", data))
                .doOnComplete(() -> logger.info("AI stream completed"))
                .doOnError(error -> logger.error("Error in AI stream chat", error));
    }

    private List<Map<String, String>> convertMessages(List<ChatMessage> messages) {
        return messages.stream()
                .map(msg -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("role", msg.getRole());
                    map.put("content", msg.getContent());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private String parseStreamChunk(String data) {
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                JsonNode delta = choices.get(0).get("delta");
                if (delta != null && delta.has("content")) {
                    return delta.get("content").asText();
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse stream chunk: {}", data, e);
        }
        return null;
    }

    private String parseNonStreamResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText();
                }
            }
        } catch (Exception e) {
            logger.error("Failed to parse non-stream response: {}", response, e);
        }
        return "";
    }
}
