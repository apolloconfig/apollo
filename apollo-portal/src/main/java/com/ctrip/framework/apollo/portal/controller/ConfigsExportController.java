package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.common.exception.ServiceException;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ConfigsExportService;
import com.ctrip.framework.apollo.portal.service.NamespaceService;
import com.ctrip.framework.apollo.portal.util.NamespaceBOUtils;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * jian.tan
 */
@RestController
public class ConfigsExportController {

  private static final Logger logger = LoggerFactory.getLogger(ConfigsExportController.class);

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
  @PreAuthorize(value = "!@permissionValidator.shouldHideConfigToCurrentUser(#appId, #env, #namespaceName)")
  @GetMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items/export")
  public void exportItems(@PathVariable String appId, @PathVariable String env,
      @PathVariable String clusterName, @PathVariable String namespaceName,
      HttpServletResponse res) {
    List<String> fileNameSplit = Splitter.on(".").splitToList(namespaceName);

    String fileName = fileNameSplit.size() <= 1 ? Joiner.on(".")
        .join(namespaceName, ConfigFileFormat.Properties.getValue()) : namespaceName;
    NamespaceBO namespaceBO = namespaceService.loadNamespaceBO(appId, Env.valueOf
        (env), clusterName, namespaceName);

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
   * Export all configs in a compressed file.
   * Just export namespace which current exists read permission.
   * The permission check in service.
   */
  @GetMapping("/export")
  public void exportAll(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // filename must contain the information of time
    final String filename = "apollo_config_export_" + DateFormatUtils.format(new Date(), "yyyy_MMdd_HH_mm_ss") + ".zip";
    // log who download the configs
    logger.info("Download configs, remote addr [{}], remote host [{}]. Filename is [{}]", request.getRemoteAddr(), request.getRemoteHost(), filename);
    // set downloaded filename
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + filename);

    try (OutputStream outputStream = response.getOutputStream()) {
      configsExportService.exportAllTo(outputStream);
    }
  }

}
