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

import com.ctrip.framework.apollo.audit.ApolloAuditProperties;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.common.constants.ApolloServer;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.api.PortalManagementApi;
import com.ctrip.framework.apollo.openapi.entity.Consumer;
import com.ctrip.framework.apollo.openapi.entity.ConsumerRole;
import com.ctrip.framework.apollo.openapi.entity.ConsumerToken;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.component.RestTemplateFactory;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.ReleaseHistoryBO;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.entity.po.Favorite;
import com.ctrip.framework.apollo.portal.entity.po.ServerConfig;
import com.ctrip.framework.apollo.portal.entity.vo.EnvironmentInfo;
import com.ctrip.framework.apollo.portal.entity.vo.PageSetting;
import com.ctrip.framework.apollo.portal.entity.vo.SystemInfo;
import com.ctrip.framework.apollo.portal.entity.vo.consumer.ConsumerCreateRequestVO;
import com.ctrip.framework.apollo.portal.entity.vo.consumer.ConsumerInfo;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.environment.PortalMetaDomainService;
import com.ctrip.framework.apollo.portal.service.AppService;
import com.ctrip.framework.apollo.portal.service.CommitService;
import com.ctrip.framework.apollo.portal.service.ConfigsExportService;
import com.ctrip.framework.apollo.portal.service.ConfigsImportService;
import com.ctrip.framework.apollo.portal.service.FavoriteService;
import com.ctrip.framework.apollo.portal.service.GlobalSearchService;
import com.ctrip.framework.apollo.portal.service.NamespaceService;
import com.ctrip.framework.apollo.portal.service.ReleaseHistoryService;
import com.ctrip.framework.apollo.portal.service.ServerConfigService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.util.ConfigFileUtils;
import com.ctrip.framework.apollo.portal.util.NamespaceBOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.boot.health.contributor.Health;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * OpenAPI v1 controller for Portal UI-only management endpoints.
 */
@RestController("openapiPortalManagementController")
public class PortalManagementController implements PortalManagementApi {

  private static final String ENV_SEPARATOR = ",";
  private static final String CONFLICT_ACTION_IGNORE = "ignore";
  private static final String CONFLICT_ACTION_COVER = "cover";
  private static final String CONFIG_SERVICE_URL_PATH = "/services/config";
  private static final String ADMIN_SERVICE_URL_PATH = "/services/admin";
  private static final Date DEFAULT_EXPIRES =
      new GregorianCalendar(2099, Calendar.JANUARY, 1).getTime();

  private final ApolloAuditLogApi auditLogApi;
  private final ApolloAuditProperties auditProperties;
  private final CommitService commitService;
  private final UnifiedPermissionValidator unifiedPermissionValidator;
  private final PortalConfig portalConfig;
  private final ConsumerService consumerService;
  private final FavoriteService favoriteService;
  private final GlobalSearchService globalSearchService;
  private final ReleaseHistoryService releaseHistoryService;
  private final ServerConfigService serverConfigService;
  private final UserInfoHolder userInfoHolder;
  private final ConfigsExportService configsExportService;
  private final ConfigsImportService configsImportService;
  private final NamespaceService namespaceService;
  private final AppService appService;
  private final PortalSettings portalSettings;
  private final RestTemplateFactory restTemplateFactory;
  private final PortalMetaDomainService portalMetaDomainService;
  private final ObjectMapper objectMapper;

  private RestTemplate restTemplate;

  public PortalManagementController(ApolloAuditLogApi auditLogApi,
      ApolloAuditProperties auditProperties, CommitService commitService,
      UnifiedPermissionValidator unifiedPermissionValidator, PortalConfig portalConfig,
      ConsumerService consumerService, FavoriteService favoriteService,
      GlobalSearchService globalSearchService, ReleaseHistoryService releaseHistoryService,
      ServerConfigService serverConfigService, UserInfoHolder userInfoHolder,
      ConfigsExportService configsExportService, ConfigsImportService configsImportService,
      NamespaceService namespaceService, AppService appService, PortalSettings portalSettings,
      RestTemplateFactory restTemplateFactory, PortalMetaDomainService portalMetaDomainService,
      ObjectMapper objectMapper) {
    this.auditLogApi = auditLogApi;
    this.auditProperties = auditProperties;
    this.commitService = commitService;
    this.unifiedPermissionValidator = unifiedPermissionValidator;
    this.portalConfig = portalConfig;
    this.consumerService = consumerService;
    this.favoriteService = favoriteService;
    this.globalSearchService = globalSearchService;
    this.releaseHistoryService = releaseHistoryService;
    this.serverConfigService = serverConfigService;
    this.userInfoHolder = userInfoHolder;
    this.configsExportService = configsExportService;
    this.configsImportService = configsImportService;
    this.namespaceService = namespaceService;
    this.appService = appService;
    this.portalSettings = portalSettings;
    this.restTemplateFactory = restTemplateFactory;
    this.portalMetaDomainService = portalMetaDomainService;
    this.objectMapper = objectMapper;
  }

  @PostConstruct
  private void init() {
    restTemplate = restTemplateFactory.getObject();
  }

  @Override
  public ResponseEntity<Object> getAuditProperties() {
    requirePortalUserRequest();
    return ResponseEntity.ok(auditProperties);
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public ResponseEntity<List<Object>> findAuditLogs(Integer page, Integer size) {
    requirePortalUserRequest();
    return ResponseEntity.ok(asObjects(auditLogApi.queryLogs(page, size)));
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public ResponseEntity<List<Object>> findAuditLogsByOpName(String opName, Integer page,
      Integer size, String startDate, String endDate) {
    requirePortalUserRequest();
    return ResponseEntity.ok(asObjects(auditLogApi.queryLogsByOpName(opName,
        parseAuditDate(startDate), parseAuditDate(endDate), page, size)));
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public ResponseEntity<List<Object>> findAuditTraceDetails(String traceId) {
    requirePortalUserRequest();
    return ResponseEntity.ok(asObjects(auditLogApi.queryTraceDetails(traceId)));
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public ResponseEntity<List<Object>> findAuditDataInfluencesByField(String entityName,
      String entityId, String fieldName, Integer page, Integer size) {
    requirePortalUserRequest();
    return ResponseEntity.ok(asObjects(
        auditLogApi.queryDataInfluencesByField(entityName, entityId, fieldName, page, size)));
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public ResponseEntity<List<Object>> searchAuditLogs(String query, Integer page, Integer size) {
    requirePortalUserRequest();
    return ResponseEntity
        .ok(asObjects(auditLogApi.searchLogByNameOrTypeOrOperator(query, page, size)));
  }

  @Override
  public ResponseEntity<List<Object>> findCommits(String appId, String env, String clusterName,
      String namespaceName, String key, Integer page, Integer size) {
    requirePortalUserRequest();
    if (unifiedPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName,
        namespaceName)) {
      return ResponseEntity.ok(Collections.emptyList());
    }
    if (StringUtils.isEmpty(key)) {
      return ResponseEntity.ok(asObjects(
          commitService.find(appId, Env.valueOf(env), clusterName, namespaceName, page, size)));
    }
    return ResponseEntity.ok(asObjects(commitService.findByKey(appId, Env.valueOf(env), clusterName,
        namespaceName, key, page, size)));
  }

  @Override
  public ResponseEntity<Object> getPageSettings() {
    requirePortalUserRequest();
    PageSetting setting = new PageSetting();
    setting.setWikiAddress(portalConfig.wikiAddress());
    setting.setCanAppAdminCreatePrivateNamespace(portalConfig.canAppAdminCreatePrivateNamespace());
    return ResponseEntity.ok(setting);
  }

  @Override
  @Transactional
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public ResponseEntity<Object> createConsumer(Object body, String expires) {
    requirePortalUserRequest();
    ConsumerCreateRequestVO request = convertBody(body, ConsumerCreateRequestVO.class);
    validateConsumerCreateRequest(request);

    String operator = currentUserId();
    Consumer createdConsumer = consumerService.createConsumer(convertToConsumer(request), operator);
    Date resolvedExpires = parseConsumerExpires(expires);
    ConsumerToken consumerToken = consumerService.generateAndSaveConsumerToken(createdConsumer,
        request.getRateLimit(), resolvedExpires, operator);
    if (request.isAllowCreateApplication()) {
      consumerService.assignCreateApplicationRoleToConsumer(consumerToken.getToken(), operator);
    }
    return ResponseEntity.ok(consumerService.getConsumerInfoByAppId(request.getAppId()));
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public ResponseEntity<Object> getConsumerTokenByAppId(String appId) {
    requirePortalUserRequest();
    return ResponseEntity.ok(consumerService.getConsumerTokenByAppId(appId));
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public ResponseEntity<List<Object>> assignRoleToConsumer(String token, String type, Object body,
      String envs) {
    requirePortalUserRequest();
    NamespaceDTO namespace = convertBody(body, NamespaceDTO.class);
    List<ConsumerRole> consumerRoles = new ArrayList<>(8);
    String appId = namespace.getAppId();
    String namespaceName = namespace.getNamespaceName();

    if (StringUtils.isEmpty(appId)) {
      throw new BadRequestException("Params(AppId) can not be empty.");
    }
    if (Objects.equals("AppRole", type)) {
      return ResponseEntity.ok(asObjects(Collections
          .singletonList(consumerService.assignAppRoleToConsumer(token, appId, currentUserId()))));
    }
    if (StringUtils.isEmpty(namespaceName)) {
      throw new BadRequestException("Params(NamespaceName) can not be empty.");
    }
    if (envs != null) {
      for (String env : envs.split(",")) {
        if (StringUtils.isEmpty(env)) {
          continue;
        }
        if (Env.UNKNOWN.equals(Env.transformEnv(env))) {
          throw BadRequestException.invalidEnvFormat(env);
        }
        consumerRoles.addAll(consumerService.assignNamespaceRoleToConsumer(token, appId,
            namespaceName, env, currentUserId()));
      }
      return ResponseEntity.ok(asObjects(consumerRoles));
    }

    consumerRoles.addAll(consumerService.assignNamespaceRoleToConsumer(token, appId, namespaceName,
        currentUserId()));
    return ResponseEntity.ok(asObjects(consumerRoles));
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public ResponseEntity<List<Object>> getConsumerList(Integer page, Integer size) {
    requirePortalUserRequest();
    List<ConsumerInfo> consumers = consumerService.findConsumerInfoList(pageable(page, size));
    return ResponseEntity.ok(asObjects(consumers));
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public ResponseEntity<Void> deleteConsumer(String appId) {
    requirePortalUserRequest();
    consumerService.deleteConsumer(appId);
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public ResponseEntity<Void> importConfig(String envs, String conflictAction, MultipartFile file) {
    return importAllConfigs(envs, conflictAction, file);
  }

  @Override
  public ResponseEntity<List<Object>> findFavorites(String userId, String appId, Integer page,
      Integer size) {
    requirePortalUserRequest();
    List<Favorite> favorites =
        favoriteService.search(userId, appId, pageable(page, size), currentUserId());
    return ResponseEntity.ok(asObjects(favorites));
  }

  @Override
  public ResponseEntity<Object> addFavorite(Object body) {
    requirePortalUserRequest();
    Favorite favorite = convertBody(body, Favorite.class);
    return ResponseEntity.ok(favoriteService.addFavorite(favorite, currentUserId()));
  }

  @Override
  public ResponseEntity<Void> deleteFavorite(Long favoriteId) {
    requirePortalUserRequest();
    favoriteService.deleteFavorite(favoriteId, currentUserId());
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> topFavorite(Long favoriteId) {
    requirePortalUserRequest();
    favoriteService.adjustFavoriteToFirst(favoriteId, currentUserId());
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public ResponseEntity<Object> searchItemInfoByKeyOrValue(String key, String value) {
    requirePortalUserRequest();
    if (StringUtils.isEmpty(key) && StringUtils.isEmpty(value)) {
      throw new BadRequestException(
          "Please enter at least one search criterion in either key or value.");
    }
    return ResponseEntity.ok(globalSearchService.getAllEnvItemInfoBySearch(key, value, 0,
        portalConfig.getPerEnvSearchMaxResults()));
  }

  @Override
  public ResponseEntity<List<Object>> findReleaseHistoriesByNamespace(String appId, String env,
      String clusterName, String namespaceName, Integer page, Integer size) {
    requirePortalUserRequest();
    if (unifiedPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName,
        namespaceName)) {
      return ResponseEntity.ok(Collections.emptyList());
    }
    List<ReleaseHistoryBO> histories = releaseHistoryService.findNamespaceReleaseHistory(appId,
        Env.valueOf(env), clusterName, namespaceName, page, size);
    return ResponseEntity.ok(asObjects(histories));
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  @ApolloAuditLog(type = OpType.CREATE, name = "ServerConfig.createOrUpdatePortalDBConfig")
  public ResponseEntity<Object> createOrUpdatePortalDBConfig(Object body) {
    requirePortalUserRequest();
    ServerConfig serverConfig = convertBody(body, ServerConfig.class);
    return ResponseEntity
        .ok(serverConfigService.createOrUpdatePortalDBConfig(serverConfig, currentUserId()));
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  @ApolloAuditLog(type = OpType.CREATE, name = "ServerConfig.createOrUpdateConfigDBConfig")
  public ResponseEntity<Object> createOrUpdateConfigDBConfig(String env, Object body) {
    requirePortalUserRequest();
    ServerConfig serverConfig = convertBody(body, ServerConfig.class);
    return ResponseEntity.ok(serverConfigService.createOrUpdateConfigDBConfig(Env.transformEnv(env),
        serverConfig, currentUserId()));
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  @ApolloAuditLog(type = OpType.DELETE, name = "ServerConfig.deletePortalDBConfig")
  public ResponseEntity<Void> deletePortalDBConfig(String key) {
    requirePortalUserRequest();
    serverConfigService.deletePortalDBConfig(key, currentUserId());
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  @ApolloAuditLog(type = OpType.DELETE, name = "ServerConfig.deleteConfigDBConfig")
  public ResponseEntity<Void> deleteConfigDBConfig(String env, String key, String cluster) {
    requirePortalUserRequest();
    serverConfigService.deleteConfigDBConfig(Env.transformEnv(env), key, cluster, currentUserId());
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public ResponseEntity<List<Object>> findAllPortalDBConfig() {
    requirePortalUserRequest();
    return ResponseEntity.ok(asObjects(serverConfigService.findAllPortalDBConfig()));
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public ResponseEntity<List<Object>> findAllConfigDBConfig(String env) {
    requirePortalUserRequest();
    return ResponseEntity
        .ok(asObjects(serverConfigService.findAllConfigDBConfig(Env.transformEnv(env))));
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public ResponseEntity<Object> getSystemInfo() {
    requirePortalUserRequest();
    SystemInfo systemInfo = new SystemInfo();

    String version = ApolloServer.VERSION;
    if (!Objects.equals(version, "java-null")) {
      systemInfo.setVersion(version);
    }

    for (Env env : portalSettings.getAllEnvs()) {
      systemInfo.addEnvironment(adaptEnv2EnvironmentInfo(env));
    }
    return ResponseEntity.ok(systemInfo);
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public ResponseEntity<Object> checkSystemHealth(String instanceId) {
    requirePortalUserRequest();
    ServiceDTO service = findServiceByInstanceId(instanceId);
    if (service == null) {
      throw new IllegalArgumentException("No such instance of instanceId: " + instanceId);
    }
    Health health =
        getRestTemplate().getForObject(service.getHomepageUrl() + "/health", Health.class);
    return ResponseEntity.ok(health);
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public ResponseEntity<Resource> exportAllConfigs(String envs) {
    requirePortalUserRequest();
    String filename =
        "apollo_config_export_" + DateFormatUtils.format(new Date(), "yyyy_MMdd_HH_mm_ss") + ".zip";
    List<Env> exportEnvs = Splitter.on(ENV_SEPARATOR).splitToList(envs).stream().map(Env::valueOf)
        .collect(Collectors.toList());

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      configsExportService.exportData(outputStream, exportEnvs);
      return resourceResponse(filename, outputStream.toByteArray());
    } catch (IOException e) {
      throw new BadRequestException("export configs failed: %s", e.getMessage());
    }
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public ResponseEntity<Void> importAllConfigs(String envs, String conflictAction,
      MultipartFile file) {
    requirePortalUserRequest();
    validateConflictAction(conflictAction);
    List<Env> importEnvs = Splitter.on(ENV_SEPARATOR).splitToList(envs).stream().map(Env::valueOf)
        .collect(Collectors.toList());
    try (ZipInputStream zipInputStream =
        new ZipInputStream(new ByteArrayInputStream(file.getBytes()))) {
      configsImportService.importDataFromZipFile(importEnvs, zipInputStream,
          CONFLICT_ACTION_IGNORE.equals(conflictAction), currentUserId());
      return ResponseEntity.ok().build();
    } catch (IOException e) {
      throw new BadRequestException("import configs failed: %s", e.getMessage());
    }
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isAppAdmin(#appId)")
  public ResponseEntity<Void> checkExportAppConfig(String appId, String env, String clusterName) {
    requirePortalUserRequest();
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isAppAdmin(#appId)")
  public ResponseEntity<Resource> exportAppConfig(String appId, String env, String clusterName) {
    requirePortalUserRequest();
    String filename = String.format("%s+%s+%s+%s.zip", appId, env, clusterName,
        DateFormatUtils.format(new Date(), "yyyy_MMdd_HH_mm_ss"));
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      configsExportService.exportAppConfigByEnvAndCluster(appId, Env.valueOf(env), clusterName,
          outputStream);
      return resourceResponse(filename, outputStream.toByteArray());
    } catch (IOException e) {
      throw new BadRequestException("export app configs failed: %s", e.getMessage());
    }
  }

  @Override
  public ResponseEntity<Resource> exportNamespaceItems(String appId, String env, String clusterName,
      String namespaceName) {
    requirePortalUserRequest();
    if (unifiedPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName,
        namespaceName)) {
      throw new AccessDeniedException("Access is denied");
    }
    List<String> fileNameSplit = Splitter.on(".").splitToList(namespaceName);
    String fileName = namespaceName;
    if (fileNameSplit.size() <= 1
        || !ConfigFileFormat.isValidFormat(fileNameSplit.get(fileNameSplit.size() - 1))) {
      fileName = Joiner.on(".").join(namespaceName, ConfigFileFormat.Properties.getValue());
    }

    NamespaceBO namespaceBO = namespaceService.loadNamespaceBO(appId, Env.valueOf(env), clusterName,
        namespaceName, true, false);
    String configFileContent = NamespaceBOUtils.convert2configFileContent(namespaceBO);
    return resourceResponse(fileName, configFileContent.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isAppAdmin(#appId)")
  public ResponseEntity<Void> importAppConfig(String appId, String env, String clusterName,
      String conflictAction, MultipartFile file) {
    requirePortalUserRequest();
    validateConflictAction(conflictAction);
    try (ZipInputStream zipInputStream =
        new ZipInputStream(new ByteArrayInputStream(file.getBytes()))) {
      configsImportService.importAppConfigFromZipFile(appId, Env.valueOf(env), clusterName,
          zipInputStream, CONFLICT_ACTION_IGNORE.equals(conflictAction), currentUserId());
      return ResponseEntity.ok().build();
    } catch (IOException e) {
      throw new BadRequestException("import app configs failed: %s", e.getMessage());
    }
  }

  @Override
  public ResponseEntity<Object> searchAppsByAppIdOrName(String query, Integer page, Integer size) {
    requirePortalUserRequest();
    Pageable pageable = pageable(page, size);
    if (org.springframework.util.StringUtils.isEmpty(query)) {
      return ResponseEntity.ok(appService.findAll(pageable));
    }

    PageDTO<App> appPage = appService.searchByAppIdOrAppName(query, pageable);
    if (appPage.hasContent()) {
      return ResponseEntity.ok(appPage);
    }

    if (!portalConfig.supportSearchByItem()) {
      return ResponseEntity.ok(new PageDTO<>(Collections.emptyList(), pageable, 0));
    }
    return ResponseEntity.ok(searchByItem(query, pageable));
  }

  @Override
  @PreAuthorize(
      value = "@unifiedPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  public ResponseEntity<Void> importNamespaceItems(String appId, String env, String clusterName,
      String namespaceName, MultipartFile file) {
    requirePortalUserRequest();
    try {
      ConfigFileUtils.check(file);
      String format = ConfigFileUtils.getFormat(file.getOriginalFilename());
      String standardFilename = ConfigFileUtils.toFilename(appId, clusterName, namespaceName,
          ConfigFileFormat.fromString(format));
      configsImportService.forceImportNamespaceFromFile(Env.valueOf(env), standardFilename,
          file.getInputStream(), currentUserId());
      return ResponseEntity.ok().build();
    } catch (IOException e) {
      throw new BadRequestException("import namespace items failed: %s", e.getMessage());
    }
  }

  private Consumer convertToConsumer(ConsumerCreateRequestVO request) {
    Consumer consumer = new Consumer();
    consumer.setAppId(request.getAppId());
    consumer.setName(request.getName());
    consumer.setOwnerName(request.getOwnerName());
    consumer.setOrgId(request.getOrgId());
    consumer.setOrgName(request.getOrgName());
    return consumer;
  }

  private void validateConsumerCreateRequest(ConsumerCreateRequestVO request) {
    if (StringUtils.isBlank(request.getAppId())) {
      throw BadRequestException.appIdIsBlank();
    }
    if (StringUtils.isBlank(request.getName())) {
      throw BadRequestException.appNameIsBlank();
    }
    if (StringUtils.isBlank(request.getOwnerName())) {
      throw BadRequestException.ownerNameIsBlank();
    }
    if (StringUtils.isBlank(request.getOrgId())) {
      throw BadRequestException.orgIdIsBlank();
    }
    if (request.isRateLimitEnabled()) {
      if (request.getRateLimit() <= 0) {
        throw BadRequestException.rateLimitIsInvalid();
      }
    } else {
      request.setRateLimit(0);
    }
  }

  private PageDTO<App> searchByItem(String itemKey, Pageable pageable) {
    List<App> result = new ArrayList<>();
    if (org.springframework.util.StringUtils.isEmpty(itemKey)) {
      return new PageDTO<>(result, pageable, 0);
    }

    long maxTotal = 0;
    for (Env env : portalSettings.getActiveEnvs()) {
      PageDTO<NamespaceDTO> namespacePage =
          namespaceService.findNamespacesByItem(env, itemKey, pageable);
      if (!namespacePage.hasContent()) {
        continue;
      }
      maxTotal = Math.max(maxTotal, namespacePage.getTotal());
      for (NamespaceDTO namespaceDTO : namespacePage.getContent()) {
        App app = new App();
        app.setAppId(namespaceDTO.getAppId());
        app.setName(env.getName() + " / " + namespaceDTO.getClusterName() + " / "
            + namespaceDTO.getNamespaceName());
        app.setOrgId(env.getName() + "+" + namespaceDTO.getClusterName() + "+"
            + namespaceDTO.getNamespaceName());
        app.setOrgName("SearchByItem" + "+" + itemKey);
        result.add(app);
      }
    }
    return new PageDTO<>(result, pageable, maxTotal);
  }

  private ServiceDTO findServiceByInstanceId(String instanceId) {
    for (Env env : portalSettings.getAllEnvs()) {
      EnvironmentInfo envInfo = adaptEnv2EnvironmentInfo(env);
      ServiceDTO service = findService(envInfo.getAdminServices(), instanceId);
      if (service != null) {
        return service;
      }
      service = findService(envInfo.getConfigServices(), instanceId);
      if (service != null) {
        return service;
      }
    }
    return null;
  }

  private ServiceDTO findService(ServiceDTO[] services, String instanceId) {
    if (services == null) {
      return null;
    }
    for (ServiceDTO service : services) {
      if (instanceId.equals(service.getInstanceId())) {
        return service;
      }
    }
    return null;
  }

  private EnvironmentInfo adaptEnv2EnvironmentInfo(Env env) {
    EnvironmentInfo environmentInfo = new EnvironmentInfo();
    String metaServerAddresses = portalMetaDomainService.getMetaServerAddress(env);

    environmentInfo.setEnv(env);
    environmentInfo.setActive(portalSettings.isEnvActive(env));
    environmentInfo.setMetaServerAddress(metaServerAddresses);

    String selectedMetaServerAddress = portalMetaDomainService.getDomain(env);
    try {
      environmentInfo
          .setConfigServices(getServerAddress(selectedMetaServerAddress, CONFIG_SERVICE_URL_PATH));
      environmentInfo
          .setAdminServices(getServerAddress(selectedMetaServerAddress, ADMIN_SERVICE_URL_PATH));
    } catch (Throwable ex) {
      String errorMessage = "Loading config/admin services from meta server: "
          + selectedMetaServerAddress + " failed!";
      environmentInfo.setErrorMessage(errorMessage + " Exception: " + ex.getMessage());
    }
    return environmentInfo;
  }

  private ServiceDTO[] getServerAddress(String metaServerAddress, String path) {
    return getRestTemplate().getForObject(metaServerAddress + path, ServiceDTO[].class);
  }

  private RestTemplate getRestTemplate() {
    if (restTemplate == null) {
      restTemplate = restTemplateFactory.getObject();
    }
    return restTemplate;
  }

  private ResponseEntity<Resource> resourceResponse(String filename, byte[] content) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + filename)
        .contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(content.length)
        .body(new ByteArrayResource(content));
  }

  private void validateConflictAction(String conflictAction) {
    if (!CONFLICT_ACTION_COVER.equals(conflictAction)
        && !CONFLICT_ACTION_IGNORE.equals(conflictAction)) {
      throw new BadRequestException("ConflictAction is incorrect.");
    }
  }

  private Date parseConsumerExpires(String expires) {
    if (!org.springframework.util.StringUtils.hasText(expires)) {
      return DEFAULT_EXPIRES;
    }
    try {
      return new SimpleDateFormat("yyyyMMddHHmmss").parse(expires);
    } catch (ParseException e) {
      throw new BadRequestException("Invalid expires format: %s", expires);
    }
  }

  private Date parseAuditDate(String value) {
    if (!org.springframework.util.StringUtils.hasText(value)) {
      return null;
    }
    try {
      return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").parse(value);
    } catch (ParseException e) {
      throw new BadRequestException("Invalid audit date format: %s", value);
    }
  }

  private Pageable pageable(Integer page, Integer size) {
    return PageRequest.of(page == null ? 0 : page, size == null ? 10 : size);
  }

  private String currentUserId() {
    return userInfoHolder.getUser().getUserId();
  }

  private <T> T convertBody(Object body, Class<T> clazz) {
    return objectMapper.convertValue(body, clazz);
  }

  @SuppressWarnings("unchecked")
  private List<Object> asObjects(List<?> values) {
    if (values == null) {
      return Collections.emptyList();
    }
    return (List<Object>) (List<?>) values;
  }

  private void requirePortalUserRequest() {
    if (!UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())) {
      throw new AccessDeniedException("Portal user session is required");
    }
  }
}
