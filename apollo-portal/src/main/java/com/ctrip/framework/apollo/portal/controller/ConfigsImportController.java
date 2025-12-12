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
package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.google.common.base.Splitter;

import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ConfigsImportService;
import com.ctrip.framework.apollo.portal.util.ConfigFileUtils;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

/**
 * Import the configs from file.
 * First version: move code from {@link ConfigsExportController}
 * @author wxq
 */
@RestController
public class ConfigsImportController {
  private static final String ENV_SEPARATOR = ",";
  private static final String CONFLICT_ACTION_IGNORE = "ignore";
  private static final String CONFLICT_ACTION_COVER = "cover";
  private final ConfigsImportService configsImportService;

  public ConfigsImportController(final ConfigsImportService configsImportService) {
    this.configsImportService = configsImportService;
  }

  /**
   * copy from old {@link ConfigsExportController}.
   * @param file Yml file's name must ends with {@code .yml}.
   *             Properties file's name must ends with {@code .properties}.
   *             etc.
   * @throws IOException
   */
  @PreAuthorize(
      value = "@unifiedPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @PostMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items/import")
  public void importConfigFile(@PathVariable String appId, @PathVariable String env,
      @PathVariable String clusterName, @PathVariable String namespaceName,
      @RequestParam("file") MultipartFile file) throws IOException {
    // check file
    ConfigFileUtils.check(file);
    final String format = ConfigFileUtils.getFormat(file.getOriginalFilename());
    final String standardFilename = ConfigFileUtils.toFilename(appId, clusterName, namespaceName,
        ConfigFileFormat.fromString(format));

    configsImportService.forceImportNamespaceFromFile(Env.valueOf(env), standardFilename,
        file.getInputStream());
  }

  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  @PostMapping(value = "/configs/import")
  public void importConfigByZip(@RequestParam(value = "envs") String envs,
      @RequestParam(defaultValue = CONFLICT_ACTION_IGNORE) String conflictAction,
      @RequestParam("file") MultipartFile file) throws IOException {
    validateConflictAction(conflictAction);
    boolean ignoreConflictNamespace = conflictAction.equals(CONFLICT_ACTION_IGNORE);

    List<Env> importEnvs = Splitter.on(ENV_SEPARATOR).splitToList(envs).stream().map(Env::valueOf)
        .collect(Collectors.toList());

    byte[] bytes = file.getBytes();
    try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
      configsImportService.importDataFromZipFile(importEnvs, zipInputStream,
          ignoreConflictNamespace);
    }
  }

  @PreAuthorize(value = "@unifiedPermissionValidator.isAppAdmin(#appId)")
  @PostMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/import")
  public void importAppConfigByZip(@PathVariable String appId, @PathVariable String env,
      @PathVariable String clusterName,
      @RequestParam(defaultValue = CONFLICT_ACTION_IGNORE) String conflictAction,
      @RequestParam("file") MultipartFile file) throws IOException {
    validateConflictAction(conflictAction);
    boolean ignoreConflictNamespace = conflictAction.equals(CONFLICT_ACTION_IGNORE);
    byte[] bytes = file.getBytes();
    try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
      configsImportService.importAppConfigFromZipFile(appId, Env.valueOf(env), clusterName,
          zipInputStream, ignoreConflictNamespace);
    }
  }

  private void validateConflictAction(String conflictAction) {
    if (!conflictAction.equals(CONFLICT_ACTION_COVER)
        && !conflictAction.equals(CONFLICT_ACTION_IGNORE)) {
      throw new BadRequestException("ConflictAction is incorrect.");
    }
  }
}
