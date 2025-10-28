/*
 * Copyright 2025 Apollo Authors
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
package com.ctrip.framework.apollo.openapi.v1.controller;


import com.ctrip.framework.apollo.openapi.api.EnvironmentManagementApi;
import com.ctrip.framework.apollo.openapi.server.service.EnvOpenApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("openapiEnvController")
public class EnvController implements EnvironmentManagementApi {

  private final EnvOpenApiService envOpenApiService;

  public EnvController(EnvOpenApiService envOpenApiService) {
    this.envOpenApiService = envOpenApiService;
  }

  @Override
  public ResponseEntity<List<String>> getEnvs() {
    return ResponseEntity.ok(envOpenApiService.getEnvs());
  }
}
