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
package com.ctrip.framework.apollo.portal.service;

import com.google.gson.Gson;

import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.ServiceException;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.portal.component.UserPermissionValidator;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.entity.bo.ConfigBO;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.util.ConfigFileUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ConfigsExportService {

  private static final Logger logger = LoggerFactory.getLogger(ConfigsExportService.class);

  private final Gson gson = new Gson();

  private final AppService appService;

  private final ClusterService clusterService;

  private final NamespaceService namespaceService;

  private final AppNamespaceService appNamespaceService;

  private final PortalSettings portalSettings;

  private final UserPermissionValidator userPermissionValidator;

  public ConfigsExportService(
      AppService appService,
      ClusterService clusterService,
      final @Lazy NamespaceService namespaceService,
      final AppNamespaceService appNamespaceService,
      PortalSettings portalSettings,
      UserPermissionValidator userPermissionValidator) {
    this.appService = appService;
    this.clusterService = clusterService;
    this.namespaceService = namespaceService;
    this.appNamespaceService = appNamespaceService;
    this.portalSettings = portalSettings;
    this.userPermissionValidator = userPermissionValidator;
  }

  /**
   * Export all application which current user own them.
   * <p>
   * File Struts:
   * <p>
   *
   * List<AppNamespaceMetadata>
   * List<ownerName> -> List<App> -> List<Env> -> List<Namespace>
   * -----------------> app.metadata
   * -------------------------------------------> List<cluster.metadata>
   *
   * @param outputStream network file download stream to user
   */
  public void exportData(OutputStream outputStream, List<Env> exportEnvs) {
    if (CollectionUtils.isEmpty(exportEnvs)) {
      exportEnvs = portalSettings.getActiveEnvs();
    }

    exportApps(exportEnvs, outputStream);
  }

  private void exportApps(final Collection<Env> exportEnvs, OutputStream outputStream) {
    List<App> hasPermissionApps = findHasPermissionApps();

    if (CollectionUtils.isEmpty(hasPermissionApps)) {
      return;
    }

    try (final ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
      //write app info to zip
      writeAppInfoToZip(hasPermissionApps, zipOutputStream);

      //export app namespace
      exportAppNamespaces(zipOutputStream);

      //export app's clusters
      exportEnvs.parallelStream().forEach(env -> {
        try {
          this.exportClusters(env, hasPermissionApps, zipOutputStream);
        } catch (Exception e) {
          logger.error("export cluster error. env = {}", env, e);
        }
      });
    } catch (IOException e) {
      logger.error("export config error", e);
      throw new ServiceException("export config error", e);
    }
  }

  private List<App> findHasPermissionApps() {
    // get all apps
    final List<App> apps = appService.findAll();

    if (CollectionUtils.isEmpty(apps)) {
      return Collections.emptyList();
    }

    // permission check
    final Predicate<App> isAppAdmin =
        app -> {
          try {
            return userPermissionValidator.isAppAdmin(app.getAppId());
          } catch (Exception e) {
            logger.error("permission check failed. app = {}", app);
            return false;
          }
        };

    // app admin permission filter
    return apps.stream().filter(isAppAdmin).collect(Collectors.toList());
  }

  private void writeAppInfoToZip(List<App> apps, ZipOutputStream zipOutputStream) {
    logger.info("to import app size = {}", apps.size());

    final Consumer<App> appConsumer =
        app -> {
          try {
            synchronized (zipOutputStream) {
              String fileName = ConfigFileUtils.genAppInfoPath(app);
              String content = gson.toJson(app);

              writeToZip(fileName, content, zipOutputStream);
            }
          } catch (IOException e) {
            logger.error("Write error. {}", app);
            throw new ServiceException("Write app error. {}", e);
          }
        };

    apps.forEach(appConsumer);
  }

  private void exportAppNamespaces(ZipOutputStream zipOutputStream) {
    List<AppNamespace> appNamespaces = appNamespaceService.findAll();

    logger.info("to import appnamespace size = " + appNamespaces.size());

    Consumer<AppNamespace> appNamespaceConsumer = appNamespace -> {
      try {
        synchronized (zipOutputStream) {
          String fileName = ConfigFileUtils.genAppNamespaceInfoPath(appNamespace);
          String content = gson.toJson(appNamespace);

          writeToZip(fileName, content, zipOutputStream);
        }
      } catch (Exception e) {
        logger.error("Write appnamespace error. {}", appNamespace);
        throw new IllegalStateException(e);
      }
    };

    appNamespaces.forEach(appNamespaceConsumer);

  }

  private void exportClusters(final Env env, final List<App> exportApps, ZipOutputStream zipOutputStream) {
    exportApps.parallelStream().forEach(exportApp -> {
      try {
        this.exportCluster(env, exportApp, zipOutputStream);
      } catch (Exception e) {
        logger.error("export cluster error. appId = {}", exportApp.getAppId(), e);
      }
    });
  }

  private void exportCluster(final Env env, final App exportApp, ZipOutputStream zipOutputStream) {
    final List<ClusterDTO> exportClusters = clusterService.findClusters(env, exportApp.getAppId());

    if (CollectionUtils.isEmpty(exportClusters)) {
      return;
    }

    //write cluster info to zip
    writeClusterInfoToZip(env, exportApp, exportClusters, zipOutputStream);

    //export namespaces
    exportClusters.parallelStream().forEach(cluster -> {
      try {
        this.exportNamespaces(env, exportApp, cluster, zipOutputStream);
      } catch (BadRequestException badRequestException) {
        //ignore
      } catch (Exception e) {
        logger.error("export namespace error. appId = {}, cluster = {}", exportApp.getAppId(), cluster, e);
      }
    });
  }

  private void exportNamespaces(final Env env, final App exportApp, final ClusterDTO exportCluster,
                                ZipOutputStream zipOutputStream) {
    String clusterName = exportCluster.getName();

    List<NamespaceBO> namespaceBOS = namespaceService.findNamespaceBOs(exportApp.getAppId(), env, clusterName, true, false);

    if (CollectionUtils.isEmpty(namespaceBOS)) {
      return;
    }

    Stream<ConfigBO> configBOStream = namespaceBOS.stream()
        .map(
            namespaceBO -> new ConfigBO(env, exportApp.getOwnerName(), exportApp.getAppId(), clusterName, namespaceBO));

    writeNamespacesToZip(configBOStream, zipOutputStream);
  }

  private void writeNamespacesToZip(Stream<ConfigBO> configBOStream, ZipOutputStream zipOutputStream) {
    final Consumer<ConfigBO> configBOConsumer =
        configBO -> {
          try {
            synchronized (zipOutputStream) {
              String appId = configBO.getAppId();
              String clusterName = configBO.getClusterName();
              String namespace = configBO.getNamespace();
              String configFileContent = configBO.getConfigFileContent();
              ConfigFileFormat configFileFormat = configBO.getFormat();

              String
                  configFileName =
                  ConfigFileUtils.toFilename(appId, clusterName, namespace, configFileFormat);
              String filePath =
                  ConfigFileUtils.genNamespacePath(configBO.getOwnerName(), appId, configBO.getEnv(), configFileName);

              writeToZip(filePath, configFileContent, zipOutputStream);
            }
          } catch (IOException e) {
            logger.error("Write error. {}", configBO);
            throw new ServiceException("Write namespace error. {}", e);
          }
        };

    configBOStream.forEach(configBOConsumer);
  }

  private void writeClusterInfoToZip(Env env, App app, List<ClusterDTO> exportClusters,
                                     ZipOutputStream zipOutputStream) {
    final Consumer<ClusterDTO> clusterConsumer =
        cluster -> {
          try {
            synchronized (zipOutputStream) {
              String fileName = ConfigFileUtils.genClusterInfoPath(app, env, cluster);
              String content = gson.toJson(cluster);

              writeToZip(fileName, content, zipOutputStream);
            }
          } catch (IOException e) {
            logger.error("Write error. {}", cluster);
            throw new ServiceException("Write error. {}", e);
          }
        };

    exportClusters.forEach(clusterConsumer);
  }

  private void writeToZip(String filePath, String content, ZipOutputStream zipOutputStream)
      throws IOException {
    final ZipEntry zipEntry = new ZipEntry(filePath);
    try {
      zipOutputStream.putNextEntry(zipEntry);
      zipOutputStream.write(content.getBytes());
      zipOutputStream.closeEntry();
    } catch (IOException e) {
      String errorMsg = "write content to zip error. file = " + filePath + ", content = " + content;
      logger.error(errorMsg);
      throw new IOException(errorMsg, e);
    }
  }

}
