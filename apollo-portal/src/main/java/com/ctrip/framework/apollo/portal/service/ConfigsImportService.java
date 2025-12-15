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
package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

import com.ctrip.framework.apollo.common.constants.GsonType;
import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.ServiceException;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.listener.AppNamespaceCreationEvent;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.util.ConfigFileUtils;
import com.ctrip.framework.apollo.portal.util.ConfigToFileUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author wxq
 */
@Service
public class ConfigsImportService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigsImportService.class);

  private Gson gson = new Gson();

  private final ItemService itemService;
  private final AppService appService;
  private final ClusterService clusterService;
  private final NamespaceService namespaceService;
  private final AppNamespaceService appNamespaceService;
  private final ApplicationEventPublisher publisher;
  private final UserInfoHolder userInfoHolder;
  private final RoleInitializationService roleInitializationService;

  public ConfigsImportService(final ItemService itemService, final AppService appService,
      final ClusterService clusterService, final @Lazy NamespaceService namespaceService,
      final AppNamespaceService appNamespaceService, final ApplicationEventPublisher publisher,
      final UserInfoHolder userInfoHolder,
      final RoleInitializationService roleInitializationService) {
    this.itemService = itemService;
    this.appService = appService;
    this.clusterService = clusterService;
    this.namespaceService = namespaceService;
    this.appNamespaceService = appNamespaceService;
    this.publisher = publisher;
    this.userInfoHolder = userInfoHolder;
    this.roleInitializationService = roleInitializationService;
  }

  /**
   * force import, new items will overwrite existed items.
   */
  public void forceImportNamespaceFromFile(final Env env, final String standardFilename,
      final InputStream zipInputStream) {
    String configText;
    try (InputStream in = zipInputStream) {
      configText = ConfigToFileUtils.fileToString(in);
    } catch (IOException e) {
      throw new ServiceException("Read config file errors:{}", e);
    }

    String operator = userInfoHolder.getUser().getUserId();

    this.importNamespaceFromText(env, standardFilename, configText, false, operator);
  }

  /**
   * import all data include app、appnamespace、cluster、namespace、item
   */
  public void importDataFromZipFile(List<Env> importEnvs, ZipInputStream dataZip,
      boolean ignoreConflictNamespace) throws IOException {
    List<String> toImportApps = Lists.newArrayList();
    List<String> toImportAppNSs = Lists.newArrayList();
    List<ImportClusterData> toImportClusters = Lists.newArrayList();
    List<ImportNamespaceData> toImportNSs = Lists.newArrayList();

    ZipEntry entry;
    while ((entry = dataZip.getNextEntry()) != null) {
      if (entry.isDirectory()) {
        continue;
      }

      String filePath = entry.getName();
      String content = readContent(dataZip);

      String[] info = filePath.replace('\\', '/').split("/");

      String fileName;
      if (info.length == 1) {
        // app namespace metadata file. path format : ${namespaceName}.appnamespace.metadata
        fileName = info[0];
        if (fileName.endsWith(ConfigFileUtils.APP_NAMESPACE_METADATA_FILE_SUFFIX)) {
          toImportAppNSs.add(content);
        }
      } else if (info.length == 3) {
        fileName = info[2];
        if (fileName.equals(ConfigFileUtils.APP_METADATA_FILENAME)) {
          // app metadata file. path format : apollo/${appId}/app.metadata
          toImportApps.add(content);
        }
      } else {
        String env = info[2];
        fileName = info[3];
        for (Env importEnv : importEnvs) {
          if (Objects.equals(importEnv.getName(), env)) {
            if (fileName.endsWith(ConfigFileUtils.CLUSTER_METADATA_FILE_SUFFIX)) {
              // cluster metadata file. path format :
              // apollo/${appId}/${env}/${clusterName}.cluster.metadata
              toImportClusters.add(new ImportClusterData(Env.transformEnv(env), content));
            } else {
              // namespace file.path format :
              // apollo/${appId}/${env}/${appId}+${cluster}+${namespaceName}
              toImportNSs.add(new ImportNamespaceData(Env.valueOf(env), fileName, content,
                  ignoreConflictNamespace));
            }
          }
        }
      }
    }

    try {
      LOGGER.info("Import data. app = {}, appns = {}, cluster = {}, namespace = {}",
          toImportApps.size(), toImportAppNSs.size(), toImportClusters.size(), toImportNSs.size());

      doImport(importEnvs, toImportApps, toImportAppNSs, toImportClusters, toImportNSs);

    } catch (Exception e) {
      LOGGER.error("import config error.", e);
      throw new ServiceException("import config error.", e);
    }
  }

  /**
   * import all configurations of an application in a specified environment and cluster
   */
  public void importAppConfigFromZipFile(String appId, Env env, String clusterName,
      ZipInputStream dataZip, boolean ignoreConflictNamespace) throws IOException {
    ClusterDTO clusterDTO = clusterService.loadCluster(appId, env, clusterName);
    if (clusterDTO == null) {
      throw new BadRequestException(
          "The app does not exist in the specified environment and cluster.");
    }

    List<ImportNamespaceData> toImportNSs = Lists.newArrayList();
    ZipEntry entry;
    while ((entry = dataZip.getNextEntry()) != null) {
      if (entry.isDirectory()) {
        continue;
      }

      // file.path format :
      // ${appId}/${env}/${appId}+${cluster}+${namespaceName}
      String filePath = entry.getName();
      String content = readContent(dataZip);
      if (content == null) {
        throw new BadRequestException("Failed to read file content.");
      }
      String[] info = filePath.replace('\\', '/').split("/");
      if (info.length != 3) {
        throw new BadRequestException("Invalid file path in ZIP.");
      }
      String fileName = info[2];
      String fileNamePrefix = String.format("%s+%s+", appId, clusterName);

      if (!info[0].equals(appId) || !info[1].equalsIgnoreCase(env.getName())
          || !fileName.startsWith(fileNamePrefix)) {
        throw new BadRequestException("The content of the file to be imported is incorrect.");
      }
      if (!fileName.endsWith(ConfigFileUtils.CLUSTER_METADATA_FILE_SUFFIX)) {
        toImportNSs.add(new ImportNamespaceData(env, fileName, content, ignoreConflictNamespace));
      }
    }

    if (CollectionUtils.isEmpty(toImportNSs)) {
      throw new BadRequestException("The configuration to be imported is empty.");
    }

    try {
      LOGGER.info("Import namespace. namespace = {}", toImportNSs.size());
      doImport(Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList(),
          Lists.newArrayList(), toImportNSs);
    } catch (Exception e) {
      LOGGER.error("import app config error.", e);
      throw new ServiceException("import app config error.", e);
    }
  }

  private void doImport(List<Env> importEnvs, List<String> toImportApps,
      List<String> toImportAppNSs, List<ImportClusterData> toImportClusters,
      List<ImportNamespaceData> toImportNSs) throws InterruptedException {
    LOGGER.info("Start to import app. size = {}", toImportApps.size());

    String operator = userInfoHolder.getUser().getUserId();

    long startTime = System.currentTimeMillis();
    CountDownLatch appLatch = new CountDownLatch(toImportApps.size());
    toImportApps.parallelStream().forEach(app -> {
      try {
        importApp(app, importEnvs, operator);
      } catch (Exception e) {
        LOGGER.error("import app error. app = {}", app, e);
      } finally {
        appLatch.countDown();
      }
    });
    appLatch.await();

    LOGGER.info("Finish to import app. duration = {}", System.currentTimeMillis() - startTime);
    LOGGER.info("Start to import appnamespace. size = {}", toImportAppNSs.size());

    startTime = System.currentTimeMillis();
    CountDownLatch appNSLatch = new CountDownLatch(toImportAppNSs.size());
    toImportAppNSs.parallelStream().forEach(appNS -> {
      try {
        importAppNamespace(appNS, operator);
      } catch (Exception e) {
        LOGGER.error("import appnamespace error. appnamespace = {}", appNS, e);
      } finally {
        appNSLatch.countDown();
      }
    });
    appNSLatch.await();

    LOGGER.info("Finish to import appnamespace. duration = {}",
        System.currentTimeMillis() - startTime);
    LOGGER.info("Start to import cluster. size = {}", toImportClusters.size());

    startTime = System.currentTimeMillis();
    CountDownLatch clusterLatch = new CountDownLatch(toImportClusters.size());
    toImportClusters.parallelStream().forEach(cluster -> {
      try {
        importCluster(cluster, operator);
      } catch (Exception e) {
        LOGGER.error("import cluster error. cluster = {}", cluster, e);
      } finally {
        clusterLatch.countDown();
      }
    });
    clusterLatch.await();

    LOGGER.info("Finish to import cluster. duration = {}", System.currentTimeMillis() - startTime);
    LOGGER.info("Start to import namespace. size = {}", toImportNSs.size());

    startTime = System.currentTimeMillis();
    CountDownLatch nsLatch = new CountDownLatch(toImportNSs.size());
    toImportNSs.parallelStream().forEach(namespace -> {
      try {
        importNamespaceFromText(namespace.getEnv(), namespace.getFileName(), namespace.getContent(),
            namespace.isIgnoreConflictNamespace(), operator);
      } catch (Exception e) {
        LOGGER.error("import namespace error. namespace = {}", namespace, e);
      } finally {
        nsLatch.countDown();
      }
    });
    nsLatch.await();

    LOGGER.info("Finish to import namespace. duration = {}",
        System.currentTimeMillis() - startTime);
  }

  private void importApp(String appInfo, List<Env> importEnvs, String operator) {
    App toImportApp = gson.fromJson(appInfo, App.class);
    String appId = toImportApp.getAppId();

    toImportApp.setDataChangeCreatedBy(operator);
    toImportApp.setDataChangeLastModifiedBy(operator);
    toImportApp.setDataChangeCreatedTime(new Date());
    toImportApp.setDataChangeLastModifiedTime(new Date());

    App managedApp = appService.load(appId);
    if (managedApp == null) {
      appService.importAppInLocal(toImportApp);
    }

    importEnvs.parallelStream().forEach(env -> {
      try {
        appService.load(env, appId);
      } catch (Exception e) {
        // not existed
        appService.createAppInRemote(env, toImportApp);
      }
    });
  }

  private void importAppNamespace(String appNamespace, String operator) {
    AppNamespace toImportPubAppNS = gson.fromJson(appNamespace, AppNamespace.class);

    String appId = toImportPubAppNS.getAppId();
    String namespaceName = toImportPubAppNS.getName();
    boolean isPublic = toImportPubAppNS.isPublic();

    AppNamespace managedAppNamespace =
        isPublic ? appNamespaceService.findPublicAppNamespace(namespaceName)
            : appNamespaceService.findByAppIdAndName(appId, namespaceName);

    if (managedAppNamespace == null) {
      managedAppNamespace = new AppNamespace();
      managedAppNamespace.setAppId(toImportPubAppNS.getAppId());
      managedAppNamespace.setPublic(isPublic);
      managedAppNamespace.setFormat(toImportPubAppNS.getFormat());
      managedAppNamespace.setComment(toImportPubAppNS.getComment());
      managedAppNamespace.setDataChangeCreatedBy(operator);
      managedAppNamespace.setDataChangeLastModifiedBy(operator);
      managedAppNamespace.setName(namespaceName);

      AppNamespace createdAppNamespace =
          appNamespaceService.importAppNamespaceInLocal(managedAppNamespace);

      // application namespace will be auto created when creating app
      if (!ConfigConsts.NAMESPACE_APPLICATION.equals(namespaceName)) {
        publisher.publishEvent(new AppNamespaceCreationEvent(createdAppNamespace));
      }
    }
  }

  private void importCluster(ImportClusterData importClusterData, String operator) {
    Env env = importClusterData.getEnv();
    ClusterDTO toImportCluster = gson.fromJson(importClusterData.clusterInfo, ClusterDTO.class);

    toImportCluster.setDataChangeCreatedBy(operator);
    toImportCluster.setDataChangeLastModifiedBy(operator);
    toImportCluster.setDataChangeCreatedTime(new Date());
    toImportCluster.setDataChangeLastModifiedTime(new Date());

    String appId = toImportCluster.getAppId();
    String clusterName = toImportCluster.getName();

    try {
      clusterService.loadCluster(appId, env, clusterName);
    } catch (Exception e) {
      // not existed
      clusterService.createCluster(env, toImportCluster);
    }
  }

  /**
   * import a config file. the name of config file must be special like appId+cluster+namespace.format Example:
   * <pre>
   *   123456+default+application.properties (appId is 123456, cluster is default, namespace is application, format is properties)
   *   654321+north+password.yml (appId is 654321, cluster is north, namespace is password, format is yml)
   * </pre>
   * so we can get the information of appId, cluster, namespace, format from the file name.
   *
   * @param env              environment
   * @param standardFilename appId+cluster+namespace.format
   * @param configText       config content
   */
  private void importNamespaceFromText(final Env env, final String standardFilename,
      final String configText, boolean ignoreConflictNamespace, String operator) {
    final String appId = ConfigFileUtils.getAppId(standardFilename);
    final String clusterName = ConfigFileUtils.getClusterName(standardFilename);
    final String namespace = ConfigFileUtils.getNamespace(standardFilename);
    final String format = ConfigFileUtils.getFormat(standardFilename);

    this.importNamespace(appId, env, clusterName, namespace, configText, format,
        ignoreConflictNamespace, operator);
  }

  private void importNamespace(final String appId, final Env env, final String clusterName,
      final String namespaceName, final String configText, final String format,
      boolean ignoreConflictNamespace, String operator) {
    NamespaceDTO namespaceDTO;
    try {
      namespaceDTO = namespaceService.loadNamespaceBaseInfo(appId, env, clusterName, namespaceName);
    } catch (Exception e) {
      // not existed
      namespaceDTO = null;
    }

    if (namespaceDTO == null) {
      namespaceDTO = new NamespaceDTO();
      namespaceDTO.setAppId(appId);
      namespaceDTO.setClusterName(clusterName);
      namespaceDTO.setNamespaceName(namespaceName);
      namespaceDTO.setDataChangeCreatedBy(operator);
      namespaceDTO.setDataChangeLastModifiedBy(operator);
      namespaceDTO = namespaceService.createNamespace(env, namespaceDTO);

      roleInitializationService.initNamespaceRoles(appId, namespaceName, operator);
      roleInitializationService.initNamespaceEnvRoles(appId, namespaceName, operator);
    }

    List<ItemDTO> itemDTOS = itemService.findItems(appId, env, clusterName, namespaceName);
    // skip import if target namespace has existed items
    if (!CollectionUtils.isEmpty(itemDTOS) && ignoreConflictNamespace) {
      return;
    }

    importItems(appId, env, clusterName, namespaceName, configText, namespaceDTO, operator);
  }

  private void importItems(String appId, Env env, String clusterName, String namespaceName,
      String configText, NamespaceDTO namespaceDTO, String operator) {
    List<ItemDTO> toImportItems = gson.fromJson(configText, GsonType.ITEM_DTOS);

    toImportItems.parallelStream().forEach(newItem -> {
      String key = newItem.getKey();
      newItem.setNamespaceId(namespaceDTO.getId());
      newItem.setDataChangeCreatedBy(operator);
      newItem.setDataChangeLastModifiedBy(operator);
      newItem.setDataChangeCreatedTime(new Date());
      newItem.setDataChangeLastModifiedTime(new Date());

      if (StringUtils.hasText(key)) {
        // create or update normal item
        try {
          ItemDTO oldItem = itemService.loadItem(env, appId, clusterName, namespaceName, key);
          newItem.setId(oldItem.getId());
          // existed
          itemService.updateItem(appId, env, clusterName, namespaceName, newItem);
        } catch (Exception e) {
          if (e instanceof HttpStatusCodeException
              && ((HttpStatusCodeException) e).getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            // not existed
            itemService.createItem(appId, env, clusterName, namespaceName, newItem);
          } else {
            LOGGER.error(
                "Load or update item error. appId = {}, env = {}, cluster = {}, namespace = {}",
                appId, env, clusterName, namespaceDTO, e);
          }
        }
      } else if (StringUtils.hasText(newItem.getComment())) {
        // create comment item
        itemService.createCommentItem(appId, env, clusterName, namespaceName, newItem);
      }

    });
  }


  private String readContent(ZipInputStream zipInputStream) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[1024];
      int offset;
      while ((offset = zipInputStream.read(buffer)) != -1) {
        out.write(buffer, 0, offset);
      }
      return out.toString("UTF-8");
    } catch (IOException e) {
      LOGGER.error("Read file content from zip error.", e);
      return null;
    }
  }

  static class ImportNamespaceData {

    private Env env;
    private String fileName;
    private String content;
    private boolean ignoreConflictNamespace;

    public ImportNamespaceData(Env env, String fileName, String content,
        boolean ignoreConflictNamespace) {
      this.env = env;
      this.fileName = fileName;
      this.content = content;
      this.ignoreConflictNamespace = ignoreConflictNamespace;
    }

    public Env getEnv() {
      return env;
    }

    public void setEnv(Env env) {
      this.env = env;
    }

    public String getFileName() {
      return fileName;
    }

    public void setFileName(String fileName) {
      this.fileName = fileName;
    }

    public String getContent() {
      return content;
    }

    public void setContent(String content) {
      this.content = content;
    }

    public boolean isIgnoreConflictNamespace() {
      return ignoreConflictNamespace;
    }

    public void setIgnoreConflictNamespace(boolean ignoreConflictNamespace) {
      this.ignoreConflictNamespace = ignoreConflictNamespace;
    }

    @Override
    public String toString() {
      return "NamespaceImportData{" + "env=" + env + ", fileName='" + fileName + '\''
          + ", content='" + content + '\'' + ", ignoreConflictNamespace=" + ignoreConflictNamespace
          + '}';
    }
  }

  static class ImportClusterData {

    private Env env;
    private String clusterInfo;

    public ImportClusterData(Env env, String clusterInfo) {
      this.env = env;
      this.clusterInfo = clusterInfo;
    }

    public Env getEnv() {
      return env;
    }

    public void setEnv(Env env) {
      this.env = env;
    }

    public String getClusterInfo() {
      return clusterInfo;
    }

    public void setClusterInfo(String clusterInfo) {
      this.clusterInfo = clusterInfo;
    }

    @Override
    public String toString() {
      return "ImportClusterData{" + "env=" + env + ", clusterName='" + clusterInfo + '\'' + '}';
    }
  }
}
