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
package com.ctrip.framework.apollo.portal.controller;


import com.ctrip.framework.apollo.portal.service.config.detection.ConfigDetectionService;
import com.ctrip.framework.apollo.portal.service.config.detection.model.DetectionRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Item detection controller for intelligent configuration detection
 */
@RestController
@RequestMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}")
public class ItemDetectionController {

    private final ConfigDetectionService configDetectionService;

    public ItemDetectionController(ConfigDetectionService configDetectionService) {
        this.configDetectionService = configDetectionService;
    }

    @GetMapping(value = "/items/intelligent-detection")
    public void detectConfigStream(
            @PathVariable String appId,
            @PathVariable String env,
            @PathVariable String clusterName,
            @PathVariable String namespaceName,
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false) String comment,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/event-stream;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        PrintWriter writer = response.getWriter();

        DetectionRequest request = DetectionRequest.builder()
                .appId(appId)
                .env(env)
                .clusterName(clusterName)
                .namespaceName(namespaceName)
                .key(key)
                .value(value)
                .comment(comment)
                .build();

        configDetectionService.detectStream(request)
                .doOnNext(chunk -> {
                    try {
                        String[] lines = chunk.split("\n", -1);
                        writer.write("event: message\n");
                        for (String line : lines) {
                            writer.write("data: " + line + "\n");
                        }
                        writer.write("\n");
                        writer.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .doOnComplete(() -> {
                    try {
                        writer.write("event: done\n");
                        writer.write("data: [DONE]\n\n");
                        writer.flush();
                        writer.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .doOnError(e -> {
                    try {
                        writer.write("event: error\n");
                        writer.write("data: check error：" + e.getMessage() + "\n\n");
                        writer.flush();
                        writer.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                })
                .blockLast();
    }
}
