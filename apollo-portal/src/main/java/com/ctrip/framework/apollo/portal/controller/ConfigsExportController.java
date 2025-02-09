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
package com.ctrip.framework.apollo.portal.controller;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import com.ctrip.framework.apollo.common.exception.ServiceException;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ConfigsExportService;
import com.ctrip.framework.apollo.portal.service.NamespaceService;
import com.ctrip.framework.apollo.portal.util.NamespaceBOUtils;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * jian.tan
 */
@RestController
public class ConfigsExportController {

  private static final Logger logger        = LoggerFactory.getLogger(ConfigsExportController.class);
  private static final String ENV_SEPARATOR = ",";

  private final ConfigsExportService configsExportService;

  private final NamespaceService namespaceService;

  public ConfigsExportController(
      final ConfigsExportService configsExportService,
      final @Lazy NamespaceService namespaceService
  ) {
    this.configsExportService = configsExportService;
    this.namespaceService = namespaceService;
  }

  /**
   * export one config as file.
   * keep compatibility.
   * file name examples:
   * <pre>
   *   application.properties
   *   application.yml
   *   application.json
   * </pre>
   */
  @PreAuthorize(value = "!@userPermissionValidator.shouldHideConfigToCurrentUser(#appId, #env, #clusterName, #namespaceName)")
  @GetMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items/export")
  public void exportItems(@PathVariable String appId, @PathVariable String env,
                                  @PathVariable String clusterName, @PathVariable String namespaceName,
                                  HttpServletResponse res) {
    List<String> fileNameSplit = Splitter.on(".").splitToList(namespaceName);

    String fileName = namespaceName;

    //properties file or public namespace has not suffix (.properties)
    if (fileNameSplit.size() <= 1 || !ConfigFileFormat.isValidFormat(fileNameSplit.get(fileNameSplit.size() - 1))) {
      fileName = Joiner.on(".").join(namespaceName, ConfigFileFormat.Properties.getValue());
    }

    NamespaceBO namespaceBO = namespaceService.loadNamespaceBO(appId, Env.valueOf
        (env), clusterName, namespaceName, true, false);

    //generate a file.
    res.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName);
    // file content
    final String configFileContent = NamespaceBOUtils.convert2configFileContent(namespaceBO);
    try {
      // write content to net
      res.getOutputStream().write(configFileContent.getBytes());
    } catch (Exception e) {
      throw new ServiceException("export items failed:{}", e);
    }
  }

  /**
   * Export all configs in a compressed file. Just export namespace which current exists read permission. The permission
   * check in service.
   */
  @PreAuthorize(value = "@userPermissionValidator.isSuperAdmin()")
  @GetMapping("/configs/export")
  public void exportAll(@RequestParam(value = "envs") String envs,
                        HttpServletRequest request, HttpServletResponse response) throws IOException {
    // filename must contain the information of time
    final String filename = "apollo_config_export_" + DateFormatUtils.format(new Date(), "yyyy_MMdd_HH_mm_ss") + ".zip";
    // log who download the configs
    logger.info("Download configs, remote addr [{}], remote host [{}]. Filename is [{}]", request.getRemoteAddr(),
                request.getRemoteHost(), filename);
    // set downloaded filename
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + filename);

    List<Env>
        exportEnvs =
        Splitter.on(ENV_SEPARATOR).splitToList(envs).stream().map(env -> Env.valueOf(env)).collect(Collectors.toList());

    try (OutputStream outputStream = response.getOutputStream()) {
      configsExportService.exportData(outputStream, exportEnvs);
    }
  }

}
